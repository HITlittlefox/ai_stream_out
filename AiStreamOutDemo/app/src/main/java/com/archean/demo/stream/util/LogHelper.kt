package com.archean.demo.stream.util

import android.util.Log
import com.archeanx.libx.util.toDefault

/**
 * 日志类
 */
object LogHelper {
    const val TAG = "LogHelper"

    const val showDebug=true

    @JvmStatic
    fun d(content: String?) {
        if (showDebug) {
            Log.d(TAG, content.toDefault(TAG))
        }
    }

    @JvmStatic
    fun d(tag: String?, content: String?) {
        if (showDebug) {
            Log.d(tag.toDefault(TAG), content.toDefault(TAG))
        }
    }

    @JvmStatic
    fun i(tag: String?, text: String?) {
        if (showDebug) {
            Log.i(tag.toDefault(TAG), text.toDefault(""))
        }
    }


    @JvmStatic
    fun e(tag: String?, content: String?) {
        if (showDebug) {
            Log.e(tag.toDefault(TAG) + "000", content.toDefault(TAG))
        }
    }

    fun logError(t: Throwable, content: String) {
        if (showDebug) {
            Log.e("LogHelper-error->", t.toString() + "  " + content.toDefault(""))
        }
    }

    fun e(t: Throwable?) {
        if (showDebug) {
            Log.e("LogHelper-error->", t.toString())
        }
    }

    fun e(str: String?) {
        if (showDebug) {
            Log.e("LogHelper-error->", str.toDefault(""))
        }
    }

    fun Throwable?.e(tag: String? = null) {
        this ?: return
        if (showDebug) {
            Log.e(tag.toDefault("LogHelper-error->"), this.toString())
        }
    }
}