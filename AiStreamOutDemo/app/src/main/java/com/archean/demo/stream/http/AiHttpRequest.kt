package com.archean.demo.stream.http

import android.util.ArrayMap
import com.archean.demo.stream.util.LogHelper
import com.archean.demo.stream.util.stringToGenericObj
import com.archean.demo.stream.util.stringToObj
import com.archean.demo.stream.util.toJsonString
import com.archeanx.libx.util.toDefault
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.TimeUnit


class AiHttpRequest {

    private var mRequestCall: Call? = null


    private val okHttpClient by lazy {
        // 创建 OkHttpClient 实例
        OkHttpClient.Builder()
            .apply {
                connectTimeout(60, TimeUnit.MINUTES)
                readTimeout(60, TimeUnit.MINUTES)
                proxy(Proxy.NO_PROXY)
                //不能加 HttpLoggingInterceptor log拦截器，会导致流式效果无效
            }
            .build()
    }

    /**
     * 输出
     */
    var onNext: ((tag: Any?, isStream: Boolean, data: AiResponseDto?) -> Unit)? = null

    /**
     * 完成输出
     */
    var onComplete: ((tag: Any?) -> Unit)? = null

    /**
     * 错误
     */
    var onError: ((tag: Any?, Throwable) -> Unit)? = null

    /**
     * 取消
     */
    fun cancel() {
        mRequestCall?.cancel()
    }

    fun isRequesting(): Boolean {
        return mRequestCall != null && mRequestCall?.isExecuted().toDefault(false) && !mRequestCall?.isCanceled().toDefault(true)
    }

    /**
     * 请求数据
     * @param type 提问类型
     * @param birthid 档案编号
     * @param groupId 对话id
     * @param question 提问内容
     */
    fun onRequest(question: String): Call {
        //这是硅基流动的
        val hostUrl = "https://api.siliconflow.cn/v1/chat/completions"
        // 构建包含参数的 URL
        //这是deepseek

        val requestMap = ArrayMap<String, Any>()
        val list = mutableListOf<ArrayMap<String, String>>()
        val arrayMap = ArrayMap<String, String>()
        arrayMap.put("role", "user")
        arrayMap.put("content", question)
        list.add(arrayMap)
        requestMap.put("model", "Pro/deepseek-ai/DeepSeek-R1")
        requestMap.put("messages", list)
        requestMap.put("stream", true)

        val request = Request.Builder()
            .url(hostUrl)
            .post(requestMap.toJsonString().toDefault("").toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addHeader("Authorization", "Bearer sk-eqjbubxuupeqoolmxhhtxsxygzcjbocyrwllixcuhovdrpub")
            .addHeader("Connection", "keep-alive")
            .build()


        // 构建包含参数的 URL
        //这是deepseek
//        val hostUrl = "https://api.deepseek.com/chat/completions"
//
//        val requestMap = ArrayMap<String, Any>()
//        val list = mutableListOf<ArrayMap<String, String>>()
//        val arrayMap = ArrayMap<String, String>()
//        arrayMap.put("role", "user")
//        arrayMap.put("content", question)
//        list.add(arrayMap)
//        requestMap.put("model", "deepseek-reasoner")
//        requestMap.put("messages", list)
//        requestMap.put("stream", true)

//        requestMap.put("model", "Pro/deepseek-ai/DeepSeek-R1")

//        val request = Request.Builder()
//            .url(hostUrl)
//            .post(requestMap.toJsonString().toDefault("").toRequestBody("application/json; charset=utf-8".toMediaType()))
//            .addHeader("Authorization", "Bearer sk-0264fb39a0d74b5e9d1541d3ae0e571e")
//            .addHeader("Connection", "keep-alive")
//            .build()



        LogHelper.e("AiHttpRequest", request.url.toString())

        //取消上次的请求
        if (isRequesting()) {
            cancel()
        }

        val call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError?.invoke(call.request().tag(), e)
                LogHelper.e("AiHttpRequest", e.message)
                e.printStackTrace()
                mRequestCall = null
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        if (isStreamingResponse(response.body)) {
                            handleStreamingResponse(call, response.body)
                        } else {
                            handleNonStreamingResponse(call, response.body)
                        }
                    } else {

                        onError?.invoke(call.request().tag(), Throwable("Request failed"))
                        LogHelper.e("AiHttpRequest", "Request failed with code: " + response.code)
                        mRequestCall = null
                    }
                }
            }
        })

        mRequestCall = call
        return call
    }

    private fun isStreamingResponse(responseBody: ResponseBody?): Boolean {
        return try {
            responseBody != null && responseBody.contentLength() == -1L
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 处理流式响应
     */
    private fun handleStreamingResponse(call: Call, responseBody: ResponseBody?) {
        if (responseBody != null) {
            try {
                responseBody.source().use { source ->
                    LogHelper.e("AiHttpRequest", "start")
                    while (!source.exhausted()) {
                        val msgLine = source.readUtf8Line().toDefault("").removePrefix("data:")

                        LogHelper.e("AiHttpRequest-result", msgLine)
                        if (msgLine == ": keep-alive") {
                            continue
                        }

                        val resDto = msgLine.stringToObj<AiResponseDto>()

                        if (resDto?.choices?.lastOrNull()?.finish_reason == "stop") {
                            break
                        }
                        onNext?.invoke(call.request().tag(), true, resDto)
                    }

                    LogHelper.e("AiHttpRequest", "end")
                    onComplete?.invoke(call.request().tag())
                }
            } catch (e: IOException) {
                onError?.invoke(call.request().tag(), Throwable(e))
                LogHelper.e("AiHttpRequest", "IOException")
                e.printStackTrace()
            } catch (e: InterruptedException) {
                onError?.invoke(call.request().tag(), Throwable(e))
                LogHelper.e("AiHttpRequest", "IOException")
                e.printStackTrace()
            } finally {
                mRequestCall = null
            }
        }
    }

    /**
     * 处理非流式响应
     */
    private fun handleNonStreamingResponse(call: Call, responseBody: ResponseBody?) {
        if (responseBody != null) {
            try {
                val responseString = responseBody.string()
                println("Non - streaming response body: $responseString")

            } catch (e: IOException) {
                onError?.invoke(call.request().tag(), Throwable(e))
                println("Error reading non - streaming response: " + e.message)
            } finally {
                responseBody.close()
                mRequestCall = null
            }

        } else {
            onError?.invoke(call.request().tag(), Throwable("streaming response body is null"))
            println("Non - streaming response body is null.")
            mRequestCall = null
        }
    }
}
