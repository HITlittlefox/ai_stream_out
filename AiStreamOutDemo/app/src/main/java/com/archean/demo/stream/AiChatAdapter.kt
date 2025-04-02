package com.archean.demo.stream

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import androidx.lifecycle.LifecycleOwner
import com.archean.demo.stream.databinding.ItemMainBinding
import com.archean.demo.stream.dto.AiChatMessageDto
import com.archeanx.libx.adapter.binding.XRvBindingHolder
import com.archeanx.libx.adapter.binding.XRvBindingPureDataAdapter
import com.archean.demo.stream.util.AiChatStateType
import com.archean.demo.stream.util.AppKtx
import com.archean.demo.stream.util.LogHelper
import com.archean.demo.stream.util.dpToPx
import com.archeanx.libx.util.toDefault
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import io.noties.markwon.movement.MovementMethodPlugin


/**
 * 聊天记录适配器
 */
class AiChatAdapter(val context: Context, val owner: LifecycleOwner) : XRvBindingPureDataAdapter<AiChatMessageDto>(R.layout.item_main) {
    companion object {
        /**
         * 刷新 思考内容
         */
        const val REFRESH_CONTENT_REASONING = "refresh_content_reasoning"

        /**
         * 刷新 回复内容
         */
        const val REFRESH_CONTENT_REPLY = "refresh_content_reply"

        /**
         * 重试 回复
         */
        const val CLICK_RESET = 11111111

        /**
         * 复制内容
         */
        const val CLICK_COPY = 11111112

        var copyContent = ""
    }


    private val mMarkDown: Markwon = Markwon.builder(context)
        .usePlugin(CorePlugin.create())
//                .usePlugin(HtmlPlugin.create())
//                .usePlugin(ImagesPlugin.create())
        .usePlugin(TablePlugin.create(TableTheme.create(context).apply {
            tableMaxWidth(AppKtx.getPhoneWidth(context) - 32.dpToPx)
        }))
//                .usePlugin(LinkifyPlugin.create())
        .usePlugin(MovementMethodPlugin.create())
        .build()

//    private val mTextOptionPopup by lazy { TextOptionPopup(context) }


    override fun onBindViewHolder(holder: XRvBindingHolder, position: Int, data: AiChatMessageDto, payloads: MutableList<Any>) {
        if (payloads.size > 0) {
            val binding = holder.getBinding<ItemMainBinding>()
            when (payloads.getOrNull(0)) {
                REFRESH_CONTENT_REASONING -> {
                    mMarkDown.setMarkdown(binding.reasoningContentTv, data.reasoning_content.toDefault(""))
                }

                REFRESH_CONTENT_REPLY -> {
                    mMarkDown.setMarkdown(binding.replyContentTv, data.content.toDefault(""))
                }
            }
        }
    }

    override fun onBindViewHolder(holder: XRvBindingHolder, position: Int, data: AiChatMessageDto) {
        val binding = holder.getBinding<ItemMainBinding>()


        binding.questionContentTv.text = data.question.toDefault("")

        binding.replyContentTv.setOnLongClickListener { view ->
            //长按处理
            true
        }
//        binding.replyContentTv.setOnTouchListener { view, event ->
//            view.setTag(R.id.ai_motion_event_tag, event)
//            false
//        }

        mMarkDown.setMarkdown(binding.reasoningContentTv, data.reasoning_content.toDefault(""))
        mMarkDown.setMarkdown(binding.replyContentTv, data.content.toDefault(""))

        //两种实现方式刷新textview 的方式
        data.reasonChange = {
            if (it == data.solevar) {
                mMarkDown.setMarkdown(binding.reasoningContentTv, data.reasoning_content.toDefault(""))
            }
        }
        data.replyChange = {
            if (it == data.solevar) {
                mMarkDown.setMarkdown(binding.replyContentTv, data.content.toDefault(""))
            }
        }
    }

}