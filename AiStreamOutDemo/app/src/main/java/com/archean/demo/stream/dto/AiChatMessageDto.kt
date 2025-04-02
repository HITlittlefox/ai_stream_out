package com.archean.demo.stream.dto


/**
 * 单个会话聊天记录
 */
data class AiChatMessageDto(
    /**
     * 消息唯一标识
     */
    var solevar: String? = null,
    /**
     * 提问内容
     */
    var question: String? = null,

    /**
     * 提问回复
     */
    var content: String? = null,

    /**
     * 回复推导过程
     */
    var reasoning_content: String? = null,


    /**
     * 回复状态
     */
    var localReplyState: String? = null
) {

    var reasonChange: ((solevar:String?) -> Unit)? = null


    var replyChange: ((solevar:String?) -> Unit)? = null

    /**
     * 测试刷新 tv的另一种方式
     */
    fun onReasonChange(solevar: String?) {
        reasonChange?.invoke(solevar)
    }

    fun onReplyChange(solevar: String?) {
        replyChange?.invoke(solevar)
    }

}