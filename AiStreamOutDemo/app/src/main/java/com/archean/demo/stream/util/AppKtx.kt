package com.archean.demo.stream.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.ArrayMap
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.reflect.TypeToken
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import java.util.*
import java.util.concurrent.TimeUnit


object AppKtx {
    @JvmStatic
    val handler = Handler(Looper.getMainLooper())

    /**
     * 得到手机宽度 返回px
     */
    @JvmStatic
    fun getPhoneWidth(context: Context?): Int {
        context ?: return 0
        val windowManager = (context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager) ?: return 0
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    /**
     * 得到手机高度 返回px
     */
    @JvmStatic
    fun getPhoneHeight(context: Context?): Int {
        context ?: return 0
        val windowManager = (context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager) ?: return 0
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    /**
     * 获取app版本
     */
    fun getVersionCode(context: Context?): Int {
        context ?: return 0
        return context.packageManager.getPackageInfo(context.packageName, 0).versionCode
    }

    /**
     * 获取app版本
     */
    fun getVersionName(context: Context?): String {
        context ?: return "1"
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    fun getAppName(context: Context?): String {
        context ?: return ""
        val packageManager = context.packageManager
        val applicationInfo = context.applicationInfo
        val appName = packageManager.getApplicationLabel(applicationInfo).toString()
        return appName
    }



    /**
     * @param recyclerView rv
     * @param manager      LinearLayoutManager
     * @param position     指定移动的position
     */
    fun onRecyclerviewMoveToPosition(
        recyclerView: RecyclerView,
        manager: LinearLayoutManager?,
        position: Int
    ) {
        if (manager == null) {
            recyclerView.scrollToPosition(position)
            return
        }
        if (position >= manager.findFirstVisibleItemPosition() && position <= manager.findLastVisibleItemPosition()) {
            val moveView = manager.findViewByPosition(position)
            if (moveView != null) {
                recyclerView.scrollBy(0, manager.getDecoratedTop(moveView))
            }
        } else {
            manager.scrollToPositionWithOffset(position, 0)
//            recyclerView.scrollToPosition(position)
        }
    }



    /**
     * 获取状态栏高度
     *
     * return px
     */
    @JvmStatic
    fun getStatusBarHeight(context: Context?): Int {
        var result = 24
        context ?: return result
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        result = if (resId > 0) {
            context.resources.getDimensionPixelSize(resId)
        } else {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_PX,
                result.toFloat(),
                Resources.getSystem().displayMetrics
            ).toInt()
        }
        return result
    }

    /**
     * 非全面屏下 虚拟键高度(无论是否隐藏)
     * @param context
     * @return
     */
    fun getNavigationBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * 复制到剪贴板
     *
     * @param context context
     * @param tip     标识语
     * @param text    内容
     */
    @JvmStatic
    fun putTextIntoClip(
        context: Context,
        tip: String?,
        text: String?
    ) {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        //创建ClipData对象
        val clipData = ClipData.newPlainText(tip.toDefault(""), text.toDefault(""))
        //添加ClipData对象到剪切板中
        clipboardManager.setPrimaryClip(clipData)
    }

    fun isInRect(event: MotionEvent, rect: Rect): Boolean {
        return event.x >= rect.left && event.x <= rect.right && event.y >= rect.top && event.y <= rect.bottom
    }


    /**
     * 点击 防抖动
     */
    fun debounce(delay: Long, block: () -> Unit): () -> Unit {
        val handler = Handler(Looper.getMainLooper())
        var currentRunnable: Runnable? = null
        return {
            currentRunnable?.let { handler.removeCallbacks(it) }
            val newRunnable = Runnable { block() }
            currentRunnable = newRunnable
            handler.postDelayed(newRunnable, delay)
        }
    }

    /**
     * 节流计时
     */
    var lastClickTime = 0L

    /**
     * 点击 节流
     */
    fun throttle(throttleDuration: Long = 500, block: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= throttleDuration) {
            block()
            lastClickTime = currentTime
        }
    }

    /**
     * 函数内使用，节流
     * @return false 拦截处理
     */
    fun isThrottle(throttleDuration: Long = 500): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= throttleDuration) {
            lastClickTime = currentTime
            return true
        } else {
            return false
        }
    }
}


/**
 * 当数据为空或为null 时返回null
 */
fun <T> MutableList<T>?.emptyToNull(): MutableList<T>? {
    if (isNullOrEmpty()) {
        return null
    }
    return this
}


/**
 * 截取list中 部分数据，处理超过或者不够
 */
fun <T> MutableList<T>?.subListX(fromIndex: Int, toIndex: Int = this?.size.toDefault(0)): MutableList<T>? {
    this ?: return null
    if (fromIndex >= this.size) return null
    if (fromIndex > toIndex) return null
    return if (this.size > toIndex) {
        subList(fromIndex, toIndex)
    } else {
        subList(fromIndex, this.size)
    }
}


/**
 * 当数据为空或为null 时返回空数组
 */
fun <T> MutableList<T>?.nullToEmpty(): MutableList<T> {
    return this ?: mutableListOf()
}


fun String?.toEmptyString(): String {
    return this ?: ""
}


fun String?.emptyToNull(): String? {
    if (isNullOrEmpty()) {
        return null
    }
    return this
}

/**
 * 对某个对象 做非null处理
 */
fun <T> T?.toDefault(default: T): T = this ?: default

/**
 * 对某个对象 做非null处理
 */
fun <T> T?.toStringOrDef(default: String = ""): String {
    return this?.toString() ?: default
}


fun <T> T?.toJsonString(): String? {
    this ?: return null
    return GsonUtil.GsonString(this)
}

/**
 * 列表转json
 */
fun <T> MutableList<T>?.toJsonString(): String? {
    this ?: return null

    val type = object : TypeToken<List<T>>() {}.type //SUPPRESS CHECKSTYLE
    return try {
        GsonUtil.getGSON().toJson(this, type)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * string 转列表
 */
inline fun <reified T> String?.stringToList(): MutableList<T> {
    val result = mutableListOf<T>()
    if (this.isNullOrEmpty()) {
        return result
    }
    try {
        return GsonUtil.jsonArrayStringToList(this, T::class.java)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return result

}

/**
 * string 转对象
 */
inline fun <reified T> String?.stringToObj(): T? {
    if (this.isNullOrEmpty()) {
        return null
    }
    return try {
        GsonUtil.getGSON().fromJson(this, T::class.java)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * string 转对象
 * 支持泛型对象
 */
inline fun <reified T> String?.stringToGenericObj(): T? {
    if (this.isNullOrEmpty()) {
        return null
    }
    return try {
        GsonUtil.jsonStringToObject(this, object : TypeToken<T>() {})
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * string 转对象
 */
inline fun <reified T> String?.stringToMap(): ArrayMap<String, T>? {
    if (this.isNullOrEmpty()) {
        return null
    }
    return try {
        GsonUtil.GsonToArrayMaps(this)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 如果最后一位是某个字符，则删除
 */
fun String.deleteLastChar(default: Char): String {
    if (this.isEmpty()) {
        return this
    }
    if (this.lastIndexOf(default) == this.length - 1) {
        return this.substring(0, this.length - 1)
    }
    return this
}

/**
 * 如果最开始一位是某个字符，则删除
 */
fun String.deleteFirstChar(default: Char): String {
    if (this.isEmpty()) {
        return this
    }
    if (this[0] == default) {
        return this.substring(1, this.length)
    }
    return this
}



fun String?.toFloatDef(default: Float): Float {
    this ?: return default
    return if (this.toFloatOrNull() == null) {
        default
    } else {
        this.toFloat()
    }
}




/**
 * @param milliSeconds 毫秒
 * @param owner        必须要fragment 或者 activity ，可以跟随生命周期关闭此事件
 * @param consumer     监听
 */
fun LifecycleOwner.uiDelayShow(consumer: Consumer<Long>, milliSeconds: Int = 300): Disposable {
    return Observable.timer(milliSeconds.toLong(), TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_DESTROY)))
        .subscribe(consumer)
}



/**
 * 输入法是否被打开
 */
fun Context?.keyboardIsShow(default: Boolean = false): Boolean {
    (this as? Activity)?.let { activity ->
        //获取当屏幕内容的高度
        val screenHeight: Int = activity.window.decorView.height
        //获取View可见区域的bottom
        val rect = Rect()
        //DecorView即为activity的顶级view
        activity.window.decorView.getWindowVisibleDisplayFrame(rect)
        //考虑到虚拟导航栏的情况（虚拟导航栏情况下：screenHeight = rect.bottom + 虚拟导航栏高度）
        //选取screenHeight*2/3进行判断
        return screenHeight * 2 / 3 > rect.bottom
    }
    return default
}

/**
 * 截取字符串
 */
fun String?.substringKtx(startIndex: Int, endIndex: Int = startIndex): String {
    if (this.isNullOrEmpty()) return ""
    if (startIndex >= this.length) return ""
    val start = if (startIndex < 0) 0 else startIndex
    val end = if (endIndex >= this.length) this.length - 1 else endIndex
    if (start > end) return ""
    return this.substring(start, end + 1)
}

fun Context?.toActivity(): Activity? {
    val context = this ?: return null
    return (context as? Activity) ?: (((context as? ContextThemeWrapper)?.baseContext) as? Activity)
}

/**
 * 获取小数点之前的字符串
 */
fun String?.getStringBeforeDot(): String {
    this ?: return ""
    return this.split(".").getOrNull(0) ?: ""
}

fun String?.getStringAfterDot(): String {
    this ?: return ""
    return this.split(".").getOrNull(1) ?: ""
}


//<editor-fold desc="Dimension">

/*converts dp value into px*/
val Number.dpToPx
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics).toInt()

//val Number.dpToPx
//    get() = (this.toFloat() * Resources.getSystem().displayMetrics.density).toInt()

/*converts sp value into px*/
val Number.sp
    get() = (this.toFloat() * Resources.getSystem().displayMetrics.scaledDensity).toInt()

//</editor-fold>


//<editor-fold desc="Memory">

val Number.KB: Long
    get() = this.toLong() * 1024L

val Number.MB: Long
    get() = this.KB * 1024

val Number.GB: Long
    get() = this.MB * 1024

