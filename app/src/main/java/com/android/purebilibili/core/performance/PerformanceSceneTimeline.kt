package com.android.purebilibili.core.performance

internal data class PerformanceScene(val page: String, val interaction: String) {
    val key: String get() = "$page/$interaction"
}

/** 只保留边界与滚动区间，不启动帧循环。使用与 HWUI vsync 相同的单调纳秒时钟。 */
internal class PerformanceSceneTimeline(startNs: Long, page: String, transitioning: Boolean) {
    private data class Boundary(val timeNs: Long, val page: String, val transitioning: Boolean)
    private data class ScrollWindow(val startNs: Long, var endNs: Long)
    private val boundaries = ArrayDeque<Boundary>().apply { add(Boundary(startNs, page, transitioning)) }
    private val scrolls = ArrayDeque<ScrollWindow>()

    fun boundary(timeNs: Long, page: String, transitioning: Boolean) {
        val previous = boundaries.last()
        if (previous.page == page && previous.transitioning == transitioning) return
        boundaries.addLast(Boundary(timeNs, page, transitioning))
        if (boundaries.size > 256) boundaries.removeFirst()
    }

    fun scroll(timeNs: Long) {
        // 覆盖惯性滚动事件之间的间隔，并明确把最后一次实际位移后的 120ms 计入滚动尾段。
        val end = timeNs + 120_000_000L
        val previous = scrolls.lastOrNull()
        if (previous != null && previous.endNs >= timeNs) previous.endNs = end
        else scrolls.addLast(ScrollWindow(timeNs, end))
        if (scrolls.size > 256) scrolls.removeFirst()
    }

    fun sceneAt(startNs: Long, endNs: Long): PerformanceScene {
        val atStart = boundaries.lastOrNull { it.timeNs <= startNs }
        val atEnd = boundaries.lastOrNull { it.timeNs <= endNs }
            ?: return PerformanceScene("unknown", "unknown")
        val transitioning = atStart?.transitioning == true ||
            boundaries.any { it.timeNs in startNs..endNs && it.transitioning }
        val scrolling = scrolls.any { it.startNs <= endNs && it.endNs >= startNs }
        return PerformanceScene(atEnd.page, when {
            transitioning -> "transition"
            scrolling -> "scroll"
            atStart == null -> "unknown"
            else -> "idle"
        })
    }
}
