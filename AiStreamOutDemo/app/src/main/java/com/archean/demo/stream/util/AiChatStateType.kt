package com.archean.demo.stream.util

import androidx.annotation.StringDef


/**
 * 支付方式
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@MustBeDocumented
@StringDef(AiChatStateType.wait, AiChatStateType.think, AiChatStateType.reply_ing, AiChatStateType.reqeust_end, AiChatStateType.out_end, AiChatStateType.error)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class AiChatStateType {
    companion object {
        /**
         * 等待回复
         */
        const val wait = "1"

        /**
         * 思考内容输出
         */
        const val think="2"

        /**
         * 回答内容输出
         */
        const val reply_ing = "3"

        /**
         * 回复结束，（不对应回复完整）
         */
        const val reqeust_end = "4"

        /**
         * 输出结束
         */
        const val out_end="5"

        /**
         * 回复出错
         */
        const val error = "7"


    }
}