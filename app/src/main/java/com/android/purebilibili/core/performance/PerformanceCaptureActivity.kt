package com.android.purebilibili.core.performance

import android.os.Handler
import android.os.HandlerThread
import android.view.ViewTreeObserver
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject

internal fun ComponentActivity.bindPerformanceCapture(kind: String, initialRoute: String? = null) {
    val windowOwner = window.decorView
    PerformanceCapture.registerWindow(windowOwner, kind, initialRoute) {
        JSONObject()
            .put("panelRefreshRateHz", windowOwner.display?.refreshRate ?: JSONObject.NULL)
            .put("windowPreferredRefreshRateHz", window.attributes.preferredRefreshRate)
            .put("windowPreferredDisplayModeId", window.attributes.preferredDisplayModeId)
            .put("hasWindowFocus", windowOwner.hasWindowFocus())
    }
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            PerformanceCapture.unregisterWindow(windowOwner)
        }
    })
    lifecycleScope.launch { PerformanceCapture.observeProcessSamples(applicationContext, windowOwner) }
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
            PerformanceCapture.setWindowResumed(windowOwner, true)
            try {
                PerformanceCapture.status.map { it.recording }.distinctUntilChanged().collectLatest { recording ->
                    if (!recording) return@collectLatest
                    val tree = windowOwner.viewTreeObserver
                    val scrollListener = ViewTreeObserver.OnScrollChangedListener { PerformanceCapture.onScroll(windowOwner) }
                    tree.addOnScrollChangedListener(scrollListener)
                    try {
                        val supported = windowOwner.isHardwareAccelerated
                        PerformanceCapture.setHwuiAvailable(windowOwner, supported)
                        if (!supported) awaitCancellation()
                        // JankStats 不公开阶段明细；专项前台开启时才添加此窗口的阶段监听。
                        val thread = HandlerThread("BiliPai-HwuiCapture").apply { start() }
                        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, dropped ->
                            PerformanceCapture.onHwuiFrame(windowOwner, metrics, dropped)
                        }
                        window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
                        try {
                            awaitCancellation()
                        } finally {
                            window.removeOnFrameMetricsAvailableListener(listener)
                            thread.quitSafely()
                        }
                    } finally {
                        if (tree.isAlive) tree.removeOnScrollChangedListener(scrollListener)
                    }
                }
            } finally {
                PerformanceCapture.setWindowResumed(windowOwner, false)
            }
        }
    }
}
