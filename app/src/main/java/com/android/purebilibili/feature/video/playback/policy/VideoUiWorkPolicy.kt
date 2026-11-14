package com.android.purebilibili.feature.video.playback.policy

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest

internal enum class VideoUiWorkKind { DETAIL, AI_SUMMARY, SEEK_PREVIEW }
internal data class VideoUiWorkMedia(val bvid: String, val cid: Long)
internal data class VideoUiWorkConsumer(val kind: VideoUiWorkKind, val media: VideoUiWorkMedia)
internal enum class VideoUiWorkPermission { Visible, Hidden, Stale }

/** AI/缩略图按分 P 隔离；kind=null 的标签、作者及关注信息在同一视频内复用。 */
internal fun resolveVideoUiWorkPermission(
    media: VideoUiWorkMedia,
    requestToken: Long,
    currentMedia: VideoUiWorkMedia,
    currentRequestToken: Long,
    consumers: Collection<VideoUiWorkConsumer>,
    inBackground: Boolean,
    kind: VideoUiWorkKind? = null,
): VideoUiWorkPermission = when {
    media.bvid != currentMedia.bvid || requestToken != currentRequestToken -> VideoUiWorkPermission.Stale
    kind != null && media.cid != currentMedia.cid -> VideoUiWorkPermission.Stale
    inBackground -> VideoUiWorkPermission.Hidden
    consumers.any { it.media == currentMedia && (kind == null || it.kind == kind) } -> VideoUiWorkPermission.Visible
    else -> VideoUiWorkPermission.Hidden
}

/** 隐藏时取消在途请求，恢复后仅重做尚未完成的工作；旧内容不等待新页面来唤醒。 */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun <T> runVisibleVideoUiWork(
    permissions: Flow<VideoUiWorkPermission>,
    work: suspend () -> T,
): T = permissions.transformLatest { permission ->
    when (permission) {
        VideoUiWorkPermission.Visible -> emit(work())
        VideoUiWorkPermission.Hidden -> Unit
        VideoUiWorkPermission.Stale -> throw CancellationException("Video UI work belongs to previous media")
    }
}.first()
