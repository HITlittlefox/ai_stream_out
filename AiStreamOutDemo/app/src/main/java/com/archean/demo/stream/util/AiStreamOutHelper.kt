package com.archean.demo.stream.util

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.archean.demo.stream.dto.AiChatMessageDto
import com.archean.demo.stream.http.AiHttpRequest
import com.archean.demo.stream.http.AiResponseDto
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

/**
 * 流式输出辅助类
 * 这个页面有多个延迟改变状态，是为了处理 rv中，不同输出时，ui上有大变动，  如果ui上，不需要大变动，那么可以自行修改是否需要延迟
 */
class AiStreamOutHelper(val owner: LifecycleOwner) : LifecycleEventObserver {
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 推导内容 buffer  没使用打字机效果
     */
    val reasoningBuilder = StringBuilder()

    /**
     * 回复内容 string  使用了打字机效果
     */
    val replyBuilder = StringBuilder()

    /**
     * 回复内容输出 string
     */
    val replyOutBuilder = StringBuilder()

    /**
     * 当前操作item
     */
    var mAiChatMessageDto: AiChatMessageDto? = null

    /**
     * 定时器
     */
    private var disposable: Disposable? = null

    /**
     * 输出流式内容
     */
    var out: ((contentType: String, content: String) -> Unit)? = null

    /**
     * 完成请求
     */
    var complete: (() -> Unit)? = null

    /**
     * 错误
     */
    var error: ((Throwable) -> Unit)? = null

    /**
     * 变更回复状态展示
     */
    var updateType: ((stateType: String) -> Unit)? = null


    /**
     * 等待刷新type中..
     */
    var waitRefreshType = false

    /**
     * 状态类型
     */
    @AiChatStateType
    private var stateType: String = ""
        set(value) {
            field = value
            LogHelper.e("AiHttpRequest", "stateType：" + stateType)
        }

    private var firstResponseDto: AiResponseDto? = null

    fun isRequesting(): Boolean {
        LogHelper.e("AiHttpRequest", "onError-stateType" + stateType)
        return !(stateType.isEmpty() || stateType == AiChatStateType.out_end || stateType == AiChatStateType.error)
    }


    fun onInit(dto: AiChatMessageDto, httpRequest: AiHttpRequest) {
        reset()
        stateType = AiChatStateType.wait
        this.mAiChatMessageDto = dto
        firstResponseDto = null

        replyOutBuilder.clear()
        disposable?.dispose()

        //初始化 思考内容
        reasoningBuilder.append("思考中...")

        reasoningBuilder.append("\n\n")
        dto.reasoning_content = reasoningBuilder.toString()
        handler.post { updateType?.invoke(AiChatStateType.wait) }


        httpRequest.apply {
            onNext = { tag: Any?, isStream: Boolean, data: AiResponseDto? ->
                appendData(isStream, data)
            }
            onComplete = { tag: Any? ->
                //因为是打字机效果，所以完成时，需要判断，是否输出完毕
                if (replyBuilder.length == replyOutBuilder.length) {
                    LogHelper.e("AiHttpRequest", "onComplete1")
                    if (stateType != AiChatStateType.out_end) {
                        if (!waitRefreshType) {
                            waitRefreshType = true
                            disposable?.dispose()

                            handler.post { updateType?.invoke(AiChatStateType.out_end) }

                            handler.postDelayed({
                                stateType = AiChatStateType.out_end
                                waitRefreshType = false
                                complete?.invoke()
                            }, 1600)
                        }
                    }
                } else {
                    handler.postDelayed({
                        //这是里设置状态延迟，是考虑到回复状态，可能不足一秒，就完成了，但是状态会延迟一秒更新，所以这里也需要延迟一下
                        stateType = AiChatStateType.reqeust_end
                    }, 1100)
                    LogHelper.e("AiHttpRequest", "onComplete2")
                }
            }
            onError = { tag: Any?, throwable ->
                LogHelper.e("AiHttpRequest", "onError-outHelper")
                throwable.printStackTrace()
                if (stateType != AiChatStateType.error) {
                    if (!waitRefreshType) {
                        waitRefreshType = true

                        handler.post {
                            updateType?.invoke(AiChatStateType.error)
                            LogHelper.e("AiHttpRequest", "onError-updateType")
                        }

                        handler.postDelayed({
                            stateType = AiChatStateType.error
                            waitRefreshType = false
                            error?.invoke(throwable)
                            LogHelper.e("AiHttpRequest", "onError-error?.invoke")
                        }, 1600)
                    }
                }
            }
        }
    }

    fun reset() {
        stateType = ""
        this.mAiChatMessageDto = null
        reasoningBuilder.clear()
        replyBuilder.clear()
        disposable?.dispose()
        waitRefreshType = false
    }


    private fun appendData(isStream: Boolean, data: AiResponseDto?) {
        data ?: return
        if (firstResponseDto == null) {
            firstResponseDto = data
        }
        if (isStream) {
            when (data.getType()) {
                AiContentType.reasoning -> {
                    data.choices?.forEach {
                        reasoningBuilder.append(it.delta?.reasoning_content.toDefault(""))
                    }


                    if (stateType != AiChatStateType.think) {
                        if (!waitRefreshType) {
                            waitRefreshType = true

                            handler.post { updateType?.invoke(AiChatStateType.think) }

                            handler.postDelayed({
                                stateType = AiChatStateType.think
                                waitRefreshType = false
                            }, 1000)
                        }

                    } else {
                        handler.post { out?.invoke(AiChatStateType.think, reasoningBuilder.toString()) }
                    }
                }

                AiContentType.reply -> {
                    data.choices?.forEach {
                        replyBuilder.append(it.delta?.content.toDefault(""))
                    }

                    if (stateType != AiChatStateType.reply_ing) {

                        LogHelper.e("AiHttpRequest", "reply_ing")
                        if (!waitRefreshType) {
                            waitRefreshType = true
                            handler.post { updateType?.invoke(AiChatStateType.reply_ing) }
                            handler.postDelayed({
                                stateType = AiChatStateType.reply_ing
                                waitRefreshType = false
                                outStream()
                            }, 1000)
                        }
                    }
                }
            }
        }
//        LogHelper.e("AiStreamOutHelper", "reasoningBuffer:" + reasoningBuffer.toString())
//        LogHelper.e("AiStreamOutHelper", "replyBuffer:" + replyBuffer.toString())
    }


    private fun outStream() {
        disposable = Observable.interval(0, 32, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(owner, Lifecycle.Event.ON_DESTROY)))
            .subscribe {
                LogHelper.e("AiHttpRequest", "length:  out" + replyOutBuilder.length + " replyBuilder" + replyBuilder.length)
                if (replyOutBuilder.length < replyBuilder.length) {
                    val nowChar = replyBuilder[replyOutBuilder.length]
//                    LogHelper.e("outStream", nowChar.toString())
                    replyOutBuilder.append(nowChar)

//                    val pattern1 = Regex("([*:：])\\s*\n")
//                    val resultData = pattern1.replace(replyOutBuilder.toString().toDefault("")) { rep ->
//                        "${rep.value}\n\n"
//                    }
                    out?.invoke(AiChatStateType.reply_ing, replyOutBuilder.toString().toDefault(""))
                } else {
                    LogHelper.e("AiHttpRequest", "outStream1" + stateType)
                    //判断请求状态时是否完成
                    if (stateType == AiChatStateType.reqeust_end) {
                        LogHelper.e("AiHttpRequest", "outStream2")
                        if (!waitRefreshType) {
                            waitRefreshType = true
                            disposable?.dispose()

                            handler.post { updateType?.invoke(AiChatStateType.out_end) }

                            handler.postDelayed({
                                stateType = AiChatStateType.out_end
                                waitRefreshType = false
                                complete?.invoke()
                            }, 1600)
                        }
                    }
                }
            }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_DESTROY -> {
                reset()
                updateType = null
                out = null
                error = null
                complete = null
                handler.removeCallbacksAndMessages(null)
            }

            Lifecycle.Event.ON_PAUSE -> {

            }

            Lifecycle.Event.ON_RESUME -> {

            }

            else -> {}
        }
    }


}