package com.android.purebilibili.core.performance

internal const val PERFORMANCE_PREVIEW_SETTING_ID = "diagnostics.performance_preview"
internal const val PERFORMANCE_PREVIEW_TITLE = "查看性能摘要"

internal data class PerformanceReportPreview(
    val version: String,
    val buildType: String,
    val device: String,
    val startedWallMs: Long,
    val elapsedMs: Long,
    val stopReason: String,
    val summary: String?,
)
