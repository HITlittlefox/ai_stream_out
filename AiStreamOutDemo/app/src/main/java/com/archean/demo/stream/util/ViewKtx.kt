package com.archean.demo.stream.util

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Layout
import android.text.StaticLayout
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.animation.DecelerateInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.archean.demo.stream.R
import com.archeanx.libx.adapter.divider.XRvVerticalDivider
import java.lang.ref.WeakReference

/**
 * 给 文字 染渐变色
 */
fun TextView.setLinearGradientTextColor(
    orientation: GradientDrawable.Orientation,
    @ColorRes vararg resIds: Int,
) {
    val colors = IntArray(resIds.size)
    for (i in resIds.indices) {
        // 通过ContextCompat获取颜色资源对应的实际颜色值，并将其存入colors数组
        colors[i] = ContextCompat.getColor(context, resIds[i])
    }
    this.post {
        val paint = this.paint
        val viewWidth = this.width.toFloat()
        val viewHeight = this.height.toFloat()
        val startX: Float
        val startY: Float
        val endX: Float
        val endY: Float

        when (orientation) {
            GradientDrawable.Orientation.LEFT_RIGHT -> {
                startX = 0f
                startY = 0f
                endX = viewWidth
                endY = 0f
            }

            GradientDrawable.Orientation.RIGHT_LEFT -> {
                startX = viewWidth
                startY = 0f
                endX = 0f
                endY = 0f
            }

            GradientDrawable.Orientation.TOP_BOTTOM -> {
                startX = 0f
                startY = 0f
                endX = 0f
                endY = viewHeight
            }

            GradientDrawable.Orientation.BOTTOM_TOP -> {
                startX = 0f
                startY = viewHeight
                endX = 0f
                endY = 0f
            }

            else -> {
                // 对于其他未处理的方向或者自定义方向，可以设置一个默认方向，例如从左到右
                startX = 0f
                startY = 0f
                endX = viewWidth
                endY = 0f
            }
        }

        val linearGradient = LinearGradient(
            startX,
            startY,
            endX,
            endY,
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = linearGradient
        this.requestLayout()
    }
}

/**
 * view 的alpha动画，
 * @param duration 执行时间
 */
fun View?.onAnimatorAlpha(duration: Long) {
    this ?: return
    val fadeInAnimator = ObjectAnimator.ofFloat(this, "alpha", 0.0f, 1.0f)
    fadeInAnimator.duration = duration
    fadeInAnimator.start()
}


/**
 * RecyclerView linear 初始化
 */
fun RecyclerView.onScrollTop(): RecyclerView {
    if (this.adapter?.itemCount.toDefault(0) > 0) {
        this.scrollToPosition(0)
    } else {
        this.scrollTo(0, 0)
    }
    return this
}

fun RecyclerView.scrollToBottom() {
    val adapter = adapter ?: return
    if (adapter.itemCount == 0) return
    if (layoutManager !is LinearLayoutManager) return
    post {
        val lastPosition = adapter.itemCount - 1
        if (lastPosition >= 0) {
            scrollToPosition(lastPosition)
            post {
                val layoutManager = layoutManager as? LinearLayoutManager
                layoutManager?.let {
                    val lastVisibleView = it.getChildAt(it.childCount - 1)
                    lastVisibleView?.let { view ->
                        val offset = view.bottom - height + paddingBottom
                        if (offset > 0) {
                            scrollBy(0, offset)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 滑动到屏幕中最后一个item的地步
 */
fun RecyclerView.onScrollToLastItemBottom() {
    val adapter = adapter ?: return
    if (adapter.itemCount == 0) return
    if (layoutManager !is LinearLayoutManager) return
    post {
        val lastPosition = adapter.itemCount - 1
        if (lastPosition >= 0) {
            val layoutManager = layoutManager as? LinearLayoutManager
            layoutManager?.let {
                val lastVisibleView = it.getChildAt(it.childCount - 1)
                lastVisibleView?.let { view ->
                    val offset = view.bottom - height + paddingBottom
                    if (offset > 0) {
                        scrollBy(0, offset)
                    }
                }
            }
        }
    }
}

/**
 * rv的item 滑动到 rv的中间
 * 目前实现了 LinearLayoutManager 的滑动处理
 */
fun RecyclerView.onScrollCenter(position: Int) {
    val layoutManager = this.layoutManager ?: return

    if (this.adapter?.itemCount.toDefault(0) == 0) return

    val viewHolder = findViewHolderForAdapterPosition(position)
    //为null，说明不在屏幕内，需要先滑动到指定位置，再居中
    if (viewHolder == null) {
        scrollToPosition(position)
        post {
            val viewHolderReal = findViewHolderForAdapterPosition(position) ?: return@post
            if (layoutManager is LinearLayoutManager) {
                when (layoutManager.orientation) {
                    RecyclerView.HORIZONTAL -> {
                        val offset = (width / 2) - (viewHolderReal.itemView.width / 2)
                        layoutManager.scrollToPositionWithOffset(position, offset)
                    }

                    RecyclerView.VERTICAL -> {
                        val offset = (height / 2) - (viewHolderReal.itemView.height / 2)
                        layoutManager.scrollToPositionWithOffset(position, offset)
                    }
                }
            }
        }

    } else {
        if (layoutManager is LinearLayoutManager) {
            when (layoutManager.orientation) {
                RecyclerView.HORIZONTAL -> {
                    val offset = (width / 2) - (viewHolder.itemView.width / 2)
                    layoutManager.scrollToPositionWithOffset(position, offset)
                }

                RecyclerView.VERTICAL -> {
                    val offset = (height / 2) - (viewHolder.itemView.height / 2)
                    layoutManager.scrollToPositionWithOffset(position, offset)
                }
            }
        }
    }
}

/**
 * @param recyclerView rv
 * @param manager      LinearLayoutManager
 * @param position     指定移动的position
 */
fun RecyclerView.doRecyclerviewMoveToPosition(position: Int) {
    val manager = (layoutManager as? LinearLayoutManager)
    if (manager == null) {
        scrollToPosition(position)
        return
    }
    if (position >= manager.findFirstVisibleItemPosition() && position <= manager.findLastVisibleItemPosition()) {
        val moveView = manager.findViewByPosition(position)
        if (moveView != null) {
            scrollBy(0, manager.getDecoratedTop(moveView))
        }
    } else {
        manager.scrollToPositionWithOffset(position, 0)
//            recyclerView.scrollToPosition(position)
    }
}


/**
 * RecyclerView linear 初始化
 */
fun RecyclerView.linear(@RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL, reverseLayout: Boolean = false): RecyclerView {
    this.layoutManager = LinearLayoutManager(context, orientation, reverseLayout)
    return this
}

/**
 * RecyclerView linear 初始化
 */
fun RecyclerView.grid(
    spanCount: Int, @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false
): RecyclerView {
    this.layoutManager = GridLayoutManager(context, spanCount, orientation, reverseLayout).apply {

    }
    return this
}

fun String?.containsKtx(other: String?, ignoreCase: Boolean = false): Boolean {
    if (other.isNullOrEmpty()) return false
    if (this.isNullOrEmpty()) return false
    return this.contains(other, ignoreCase)
}

/**
 * RecyclerView 瀑布流 初始化
 */
fun RecyclerView.staggered(spanCount: Int, orientation: Int = StaggeredGridLayoutManager.VERTICAL): RecyclerView {
    this.layoutManager = StaggeredGridLayoutManager(spanCount, orientation)
    return this
}


/**
 * RecyclerView adapter 初始化
 * 方便直接使用
 */
fun <T : RecyclerView.Adapter<*>> RecyclerView.initAdapter(ada: T): RecyclerView {
    this.adapter = ada
    return this
}

/**
 * RecyclerView adapter 初始化
 * 方便直接使用
 */
fun RecyclerView.verDivider(height: Float = 1f, @ColorRes color: Int = android.R.color.transparent): XRvVerticalDivider {
    val vDivider = XRvVerticalDivider(context).apply {
        spaceColor = this.context.getColorInt(R.color.ai_ffffff)
        dividerHeight = height
        setDividerColorRes(color)
    }
    this.addItemDecoration(vDivider)
    return vDivider
}

/**
 * linearLayoutManager 的时候判断是否滑动到顶部了
 *
 */
fun RecyclerView.linearCanPullDown(): Boolean {
    (layoutManager as? LinearLayoutManager)?.let { linear ->
        getChildAt(0)?.let {
            return (it.y == 0f && linear.findFirstVisibleItemPosition() == 0)
        }
    }
    return false
}

/**
 * linearLayoutManager 的时候判断是否可以滑动
 *
 */
fun RecyclerView.linearCanPullUp(): Boolean {

//    (layoutManager as? LinearLayoutManager)?.let { linear ->
//        val firstItem: Int = linear.findFirstVisibleItemPosition()
//        val lastItem: Int = linear.findLastVisibleItemPosition()
//        val itemCount: Int = linear.itemCount
//        val lastChild = getChildAt(lastItem - firstItem)
//        if (lastItem == itemCount - 1 && lastChild != null && lastChild.bottom <= measuredHeight) {
//            return true
//        }
//    }
//    return false

    return canScrollVertically(1)
}

/**
 *
 */
@ColorInt
fun Context?.getColorInt(@ColorRes colorRes: Int): Int {
    this ?: return Color.BLACK
    return ContextCompat.getColor(this, colorRes)
}

/**
 *
 */
@ColorInt
fun View?.getColorInt(@ColorRes colorRes: Int): Int {
    this ?: return Color.BLACK
    return ContextCompat.getColor(this.context, colorRes)
}

/**
 *
 */
@ColorInt
fun Fragment?.getColorInt(@ColorRes colorRes: Int): Int {
    this ?: return Color.BLACK
    context ?: return Color.BLACK
    return ContextCompat.getColor(requireContext(), colorRes)
}

/**
 * 设置View 背景色 染色
 */
fun View?.setBackgroundTint(@ColorRes colorRes: Int) {
    this ?: return
    ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)))
}

/**
 * 设置图片 染色
 */
fun AppCompatImageView?.setImageTint(@ColorRes colorRes: Int) {
    this ?: return
    if (colorRes == -1) {
        ImageViewCompat.setImageTintList(this, null)
    } else {
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)))
    }
}

/**
 * 设置图片 染色
 */
fun AppCompatImageView?.setImageTintColor(@ColorInt color: Int) {
    this ?: return
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(color))
}

fun View?.setOnClickNoDouble(time: Int = 300, block: (view: View) -> Unit) {
    this ?: return
    var oldTime = System.currentTimeMillis()
    this.setOnClickListener {
        if (System.currentTimeMillis() > oldTime + time) {
            oldTime = System.currentTimeMillis()
            block(this)
        }
    }
}


fun <T : View> T.onVisibility(visibility: Int): T {
    this.visibility = visibility
    return this
}

/**
 * 直接设置 view 是否隐藏
 */
fun <T : View> T.onVisibility(isVisibility: Boolean): T {
    if (isVisibility) {
        if (this.visibility != View.VISIBLE) {
            this.visibility = View.VISIBLE
        }
    } else {
        if (this.visibility != View.GONE) {
            this.visibility = View.GONE
        }
    }
    return this
}


fun <T : View> T.isVisibility(): Boolean = this.visibility == View.VISIBLE


fun TextView.setTextKtx(txt: CharSequence?): TextView {
    text = txt ?: ""
    return this
}

fun Any?.isNotNullKtx(): Boolean = this != null
fun View.updateWidth(width: Int) {
    val params = layoutParams
    params.width = width
    layoutParams = params
}


inline fun View?.updateRatio(isWidth: Boolean, width: Int, height: Int) {
    this ?: return
    if (this.parent !is ConstraintLayout) return
    (this.layoutParams as? ConstraintLayout.LayoutParams)?.let {
        val wh = if (isWidth) "W," else "H,"
        it.dimensionRatio = "$wh$width:$height"
        this.layoutParams = it
    }
}
