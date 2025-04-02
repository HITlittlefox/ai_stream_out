package com.archean.demo.stream

import android.app.Application
import android.content.Context
import com.archeanx.libx.util.ToastUtil
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.DefaultRefreshHeaderCreator

class App : Application() {
    init {
        //设置全局的Header构建器QuestionAskActivity
        SmartRefreshLayout.setDefaultRefreshHeaderCreator(object :DefaultRefreshHeaderCreator{
            override fun createRefreshHeader(context: Context, layout: RefreshLayout): RefreshHeader {
                return  ClassicsHeader(context)
            }

        })
        //设置全局的Footer构建器
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context: Context, refreshLayout: RefreshLayout ->ClassicsFooter(context)}
    }

    override fun onCreate() {
        super.onCreate()
        ToastUtil.init(this)
    }
}