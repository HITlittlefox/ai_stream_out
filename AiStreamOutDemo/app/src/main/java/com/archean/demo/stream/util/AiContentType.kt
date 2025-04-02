package com.archean.demo.stream.util

import androidx.annotation.StringDef


@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@MustBeDocumented
@StringDef(AiContentType.reasoning, AiContentType.reply)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class AiContentType {
    companion object {
        /**
         * 推导内容
         */
        const val reasoning = "1"

        /**
         * 回复内容
         */
        const val reply = "2"
    }
}