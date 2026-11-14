package com.android.purebilibili.core.performance

import android.os.Build
import android.view.FrameMetrics
import org.json.JSONObject

/** 各项为系统公开的 HWUI FrameMetrics；不同阶段可能重叠，不能相加推算 GPU 时间。 */
internal class PerformanceHwuiSummary {
    private val stages = linkedMapOf<String, PerformanceFrameSummary>()
    var frames = 0L
        private set
    var missedDeadlines = 0L
        private set
    var deadlineAvailableFrames = 0L
        private set
    var droppedCallbacks = 0L
        private set
    var firstDrawFrames = 0L
        private set

    fun add(metrics: FrameMetrics, dropped: Int) {
        frames++
        droppedCallbacks += dropped
        if (metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) firstDrawFrames++
        val total = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
        val deadline = if (Build.VERSION.SDK_INT >= 31) metrics.getMetric(FrameMetrics.DEADLINE) else -1
        val missed = total >= 0 && deadline >= 0 && total > deadline
        if (total >= 0 && deadline >= 0) {
            deadlineAvailableFrames++
            if (missed) missedDeadlines++
        }
        for ((name, metric) in BASE_STAGES) {
            val duration = metrics.getMetric(metric)
            if (duration >= 0) stages.getOrPut(name) { PerformanceFrameSummary() }.add(duration, missed)
        }
        if (Build.VERSION.SDK_INT >= 31) {
            val gpu = metrics.getMetric(FrameMetrics.GPU_DURATION)
            if (gpu >= 0) stages.getOrPut("gpu") { PerformanceFrameSummary() }.add(gpu, missed)
        }
    }

    fun cumulativeJson() = JSONObject().put("frames", frames)
        .put("deadlineAvailableFrames", deadlineAvailableFrames).put("missedDeadlineFrames", missedDeadlines)
        .put("droppedMetricCallbacks", droppedCallbacks)
        .put("stages", JSONObject().apply {
            stages.forEach { (name, stage) ->
                put(name, JSONObject().put("availableFrames", stage.frames).put("sumNanos", stage.totalDurationNanos))
            }
        })

    fun snapshot() = PerformanceHwuiSummary().also { copy ->
        stages.forEach { (name, summary) -> copy.stages[name] = summary.snapshot() }
        copy.frames = frames
        copy.missedDeadlines = missedDeadlines
        copy.deadlineAvailableFrames = deadlineAvailableFrames
        copy.droppedCallbacks = droppedCallbacks
        copy.firstDrawFrames = firstDrawFrames
    }

    fun json() = JSONObject().put("frames", frames).put("droppedMetricCallbacks", droppedCallbacks)
        .put("firstDrawFrames", firstDrawFrames).put("deadlineAvailableFrames", deadlineAvailableFrames)
        .put("missedDeadlineFrames", if (deadlineAvailableFrames == 0L) JSONObject.NULL else missedDeadlines)
        .put("stages", JSONObject().apply {
            for (name in BASE_STAGES.keys + "gpu") {
                val stage = stages[name]
                put(name, if (stage == null) JSONObject.NULL else JSONObject()
                    .put("availableFrames", stage.frames)
                    .put("p50MsUpperBound", stage.percentileUpperBoundMs(.50))
                    .put("p95MsUpperBound", stage.percentileUpperBoundMs(.95))
                    .put("p99MsUpperBound", stage.percentileUpperBoundMs(.99))
                    .put("maxMs", stage.maxDurationNanos / 1_000_000.0))
            }
        })

    companion object {
        val BASE_STAGES = linkedMapOf(
            "unknownDelay" to FrameMetrics.UNKNOWN_DELAY_DURATION,
            "input" to FrameMetrics.INPUT_HANDLING_DURATION,
            "animation" to FrameMetrics.ANIMATION_DURATION,
            "layoutMeasure" to FrameMetrics.LAYOUT_MEASURE_DURATION,
            "draw" to FrameMetrics.DRAW_DURATION,
            "sync" to FrameMetrics.SYNC_DURATION,
            "commandIssue" to FrameMetrics.COMMAND_ISSUE_DURATION,
            "swapBuffers" to FrameMetrics.SWAP_BUFFERS_DURATION,
            "total" to FrameMetrics.TOTAL_DURATION,
        )
    }
}
