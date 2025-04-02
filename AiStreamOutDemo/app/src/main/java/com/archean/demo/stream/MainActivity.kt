package com.archean.demo.stream


import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.archean.demo.stream.databinding.ActivityMainBinding
import com.archean.demo.stream.dto.AiChatMessageDto
import com.archean.demo.stream.http.AiHttpRequest
import com.archean.demo.stream.util.AiChatStateType
import com.archean.demo.stream.util.AiStreamOutHelper
import com.archean.demo.stream.util.AppKtx
import com.archean.demo.stream.util.KeyBoardUtils
import com.archean.demo.stream.util.LogHelper
import com.archean.demo.stream.util.ScrollBoundaryDeciderAdapter
import com.archean.demo.stream.util.TouchEmptyCloseKeyBoardUtils
import com.archean.demo.stream.util.initAdapter
import com.archean.demo.stream.util.linear
import com.archean.demo.stream.util.onScrollToLastItemBottom
import com.archean.demo.stream.util.onVisibility
import com.archean.demo.stream.util.scrollToBottom
import com.archean.demo.stream.util.uiDelayShow
import com.archean.demo.stream.util.updateWidth
import com.archean.demo.stream.util.verDivider
import com.archeanx.libx.adapter.binding.OnItemClickListener
import com.archeanx.libx.adapter.binding.XRvBindingHolder
import com.archeanx.libx.base.XBaseBindingActivity
import com.archeanx.libx.util.ToastUtil
import com.archeanx.libx.util.isTrue
import com.archeanx.libx.util.setOnClickNoDouble
import com.archeanx.libx.util.toDefault
import com.gyf.immersionbar.ImmersionBar
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import java.util.UUID

class MainActivity : XBaseBindingActivity<ActivityMainBinding>(R.layout.activity_main) {

//    /**
//     * 会话列表 适配器
//     */
//    private val mConversationListAdapter by lazy { AiConversationListAdapter() }


    /**
     * 聊天记录 适配器
     */
    private val mChatAdapter by lazy { AiChatAdapter(this, this) }

    /**
     * 是否需要自动滚动到底部
     */
    private var isAutoScrollBottom = true


    /**
     * 点击非输入框区域，关闭软键盘
     */
    private val mTouchEmptyCloseKeyBoardUtils by lazy { TouchEmptyCloseKeyBoardUtils() }

    /**
     *  提问请求
     */
    private val mAiHttpRequest by lazy { AiHttpRequest() }

    /**
     * 流式输出辅助类
     */
    private val mAiStreamOutHelper by lazy { AiStreamOutHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).transparentStatusBar().statusBarDarkFont(true).keyboardEnable(true).init()
    }


    override fun initView() {
        binding.drawerIv.setOnClickNoDouble {
            if (!binding.drawerLayout.isDrawerOpen(binding.navigationDrawer)) {
                binding.drawerLayout.openDrawer(binding.navigationDrawer)
            }
        }
        //因为footer 是全局的，默认是null的，所有需要等待一会儿之后，再去设置footer的旋转度
        binding.smartLayout.postDelayed({
            binding.smartLayout.refreshFooter?.view?.scaleY = -1f
        }, 500)
        binding.smartLayout.setScrollBoundaryDecider(object : ScrollBoundaryDeciderAdapter() {
            override fun canLoadMore(content: View?): Boolean {
                return super.canRefresh(content)
            }
        })

        binding.recyclerView.linear().initAdapter(mChatAdapter).verDivider(60f, com.archeanx.libx.style.R.color.transparent)


        //监听滑动，显示底部按钮
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        isAutoScrollBottom = false
                        //  mHelper.onScrollViewVisibility(binding.scrollBottomIv, !isAutoScrollBottom)
                        binding.scrollBottomIv.onVisibility(false)
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        // 滑动停止
                        isAutoScrollBottom = !binding.recyclerView.canScrollVertically(1)
                        binding.scrollBottomIv.onVisibility(!isAutoScrollBottom)
                    }

                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        // 惯性滑动
                        isAutoScrollBottom = !binding.recyclerView.canScrollVertically(1)
                        binding.scrollBottomIv.onVisibility(!isAutoScrollBottom)
                    }
                }
                LogHelper.e("addOnScrollListener", "newState:" + newState + " isAutoScrollBottom:" + isAutoScrollBottom)
            }
        })

        binding.scrollBottomIv.setOnClickNoDouble {
            binding.recyclerView.post {
                //停止滚动，然后滚动到底部
                binding.recyclerView.stopScroll()
                binding.recyclerView.scrollToBottom()
                isAutoScrollBottom = true
                binding.scrollBottomIv.onVisibility(!isAutoScrollBottom)
            }
        }

        //抽屉宽度
        binding.navigationDrawer.updateWidth((AppKtx.getPhoneWidth(this) * 0.76f).toInt())

        //滑动抽屉时，对主布局进行位移
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // 根据滑动进度计算主布局的偏移量

                // 获取抽屉的 Gravity 方向
                val layoutParams = drawerView.layoutParams as DrawerLayout.LayoutParams
                val gravity = layoutParams.gravity

                // 根据滑动进度和抽屉方向计算主布局的偏移量
                val translationX = if (gravity and Gravity.START == Gravity.START) {
                    // 抽屉在左边，主布局向右偏移
                    drawerView.width * slideOffset
                } else {
                    // 抽屉在右边，主布局向左偏移
                    -drawerView.width * slideOffset
                }
                binding.mainLayout.translationX = translationX
            }

            override fun onDrawerOpened(drawerView: View) {
                // 抽屉打开时的操作
            }

            override fun onDrawerClosed(drawerView: View) {
                // 抽屉关闭时，将主布局复位
                binding.mainLayout.translationX = 0f
            }

            override fun onDrawerStateChanged(newState: Int) {
                // 抽屉状态改变时的操作
            }
        })
        //发送按钮
        binding.sendTv.setOnClickNoDouble {
            if (onSubmitQuestion(binding.inputEt.editableText.toString())) {
                binding.inputEt.setText("")
            }
        }

        //流输出
        mAiStreamOutHelper.apply {
            //type 变更回调
            updateType = { stateType: String ->
                mAiStreamOutHelper.mAiChatMessageDto?.let { msgDto ->
                    updateItem(msgDto, stateType)
                }
            }
            //思考，回复内容输出
            out = { stateTypee: String, content: String ->
                mAiStreamOutHelper.mAiChatMessageDto?.let { msgDto ->
                    when (stateTypee) {
                        AiChatStateType.think -> {
                            msgDto.reasoning_content = content
//                            LogHelper.e("AiHttpRequest:out","AiChatStateType.think"+msgDto.reasoning_content)
                            updateItem(msgDto, AiChatStateType.think)
                        }

                        AiChatStateType.reply_ing -> {
                            msgDto.content = content
                            updateItem(msgDto, AiChatStateType.reply_ing)
                        }
                    }
                }
            }
            complete = {
                mAiStreamOutHelper.mAiChatMessageDto?.let { msgDto ->
                    //完成
                }
            }
            error = {
                LogHelper.e("error")
            }
        }


        mTouchEmptyCloseKeyBoardUtils.filterViews.add(binding.sendTv)


        binding.titleTv.setOnClickNoDouble {
            mAiHttpRequest.cancel()
            mAiStreamOutHelper.reset()
        }
    }

    override fun initData() {
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        initData()
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        mTouchEmptyCloseKeyBoardUtils.autoClose(this, ev)
        return super.dispatchTouchEvent(ev)
    }

    /**
     * 提交提问
     * @param msgTag 本地请求tag，错误请求时，需要刷新当前这一条
     */
    @Synchronized
    private fun onSubmitQuestion(content: String?, msgTag: String? = null): Boolean {
        //节流处理
        if (!AppKtx.isThrottle(500)) return false
        //请求状态处理
        if (mAiHttpRequest.isRequesting() || mAiStreamOutHelper.isRequesting()) return false

        if (content.isNullOrEmpty()) {
            ToastUtil.show("请输入问题")
            return false
        }

        var msgDto: AiChatMessageDto? = null
        if (!msgTag.isNullOrEmpty()) {
            //判断最后一条消息 的唯一标识，是不是参数msgTag,是的话，就用当前这条消息，不是的话，就重新生成一条消息
            mChatAdapter.listData.getOrNull(mChatAdapter.listData.lastIndex)?.let { lastDto ->
                if (lastDto.solevar == msgTag) {
                    msgDto = lastDto
                }
            }
        }
        if (msgDto == null) {
            msgDto = AiChatMessageDto()
            msgDto?.solevar = UUID.randomUUID().toString()
            msgDto?.question = content
            mChatAdapter.addData(msgDto!!)
        }


        //输出交给 AiStreamOutHelper 统一处理
        mAiStreamOutHelper.onInit(msgDto!!, mAiHttpRequest)


        //关闭软键盘
        KeyBoardUtils.closeKeyboard(binding.inputEt, this)

        uiDelayShow({
            //需要判断有选中的会话，如果没有，则是需要生成新的对话id和对话内容
            mAiHttpRequest.onRequest(content)
        }, 300)
        return true
    }

    private fun updateItem(item: AiChatMessageDto, @AiChatStateType stateType: String? = null) {
//        LogHelper.e("updateItem", item.toJsonString())
        val oldStateType = item.localReplyState
//        LogHelper.e("AiHttpRequest", "error-item-tag" + item.localRequestTag.toDefault(""))
        val index = mChatAdapter.listData.indexOfFirst { it.solevar == item.solevar }
//        LogHelper.e("AiHttpRequest", "error-item-tag2" + mChatAdapter.getItemOrNull(index)?.localRequestTag.toDefault(""))
//        LogHelper.e("AiHttpRequest", "error-updateItem" + index)

        LogHelper.e("AiHttpRequest:out", "updateItem" + stateType)

        if (index != -1) {
            if (oldStateType != stateType) {
                item.localReplyState = stateType
                binding.recyclerView.post {
                    mChatAdapter.notifyItemChanged(index)
                }
            } else {
                binding.recyclerView.post {
                    when (stateType) {
                        AiChatStateType.think -> {

                            //刷新tv的两外一种方式
                            item.onReasonChange(item.solevar)
                            // mChatAdapter.notifyItemChanged(index, AiChatAdapter.REFRESH_CONTENT_REASONING)
                        }

                        AiChatStateType.reply_ing -> {
                            //刷新tv的两外一种方式
                            item.onReplyChange(item.solevar)
                            // mChatAdapter.notifyItemChanged(index, AiChatAdapter.REFRESH_CONTENT_REPLY)
                        }

                        else -> {
                            mChatAdapter.notifyItemChanged(index)
                        }
                    }
                }
            }

            LogHelper.e("updateItem", "isAutoScrollBottom:" + isAutoScrollBottom.toString() + " s-state:" + stateType + " oldStateType:" + oldStateType)
            //提示滚动按钮展示时，不自动滚动到底部
            if (isAutoScrollBottom) {
                binding.recyclerView.post {
                    if (oldStateType != stateType) {
                        //判断rv是否已经滑动到底部，没有则滑动到底部
//                    LogHelper.e("updateItem", "state:" + stateType + " scrollToBottom")
                        binding.recyclerView.scrollToBottom()
                    } else {
                        if (binding.recyclerView.canScrollVertically(1)) {
//                        LogHelper.e("updateItem", "state:" + stateType + " onScrollToLastItemBottom")
                            binding.recyclerView.onScrollToLastItemBottom()
                        }
                    }
                }
            } else {
                //就算当前不需要自动滑动到底部，当有新的提问时，也需要滑动到底部
                if (stateType == AiChatStateType.wait) {
                    binding.scrollBottomIv.performClick()
                }
            }
        }
    }


    override fun onDestroy() {
        mAiHttpRequest.cancel()
        super.onDestroy()
    }

}