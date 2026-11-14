package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.rememberVisualWorkActive
import com.android.purebilibili.feature.video.playback.policy.VideoUiWorkConsumer
import com.android.purebilibili.feature.video.playback.policy.VideoUiWorkKind
import com.android.purebilibili.feature.video.playback.policy.VideoUiWorkMedia
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private class VideoUiWorkActions(val update: (Any, VideoUiWorkKind, Boolean) -> Unit)
private val LocalVideoUiWorkActions = compositionLocalOf<VideoUiWorkActions?> { null }

/** 只传播当前内容身份与需求回调；手机、大屏、竖屏及玻璃共用。 */
@Composable
internal fun ProvideVideoUiWork(viewModel: VideoPlaybackViewModel, content: @Composable () -> Unit) {
    val media by remember(viewModel) {
        viewModel.uiState.map { state ->
            (state as? VideoPlaybackUiState.Success)?.info?.let { VideoUiWorkMedia(it.bvid, it.cid) }
        }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val actions = remember(viewModel, media) {
        VideoUiWorkActions { owner, kind, active ->
            viewModel.setVideoUiWorkConsumer(owner, media?.takeIf { active }?.let { VideoUiWorkConsumer(kind, it) })
        }
    }
    CompositionLocalProvider(LocalVideoUiWorkActions provides actions) {
        VideoUiWorkConsumerEffect(VideoUiWorkKind.DETAIL)
        content()
    }
}

/** 面板在自己的窗口内判断焦点，避免宿主失焦时误停当前可见的 AI 面板。 */
@Composable
internal fun VideoUiWorkConsumerEffect(kind: VideoUiWorkKind) {
    val actions = LocalVideoUiWorkActions.current ?: return
    val owner = remember { Any() }
    val active = rememberVisualWorkActive()
    DisposableEffect(actions, owner, kind, active) {
        if (active) actions.update(owner, kind, true)
        onDispose { actions.update(owner, kind, false) }
    }
}
