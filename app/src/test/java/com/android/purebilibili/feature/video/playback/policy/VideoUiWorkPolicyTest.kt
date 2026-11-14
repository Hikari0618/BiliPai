package com.android.purebilibili.feature.video.playback.policy

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertSame
import com.android.purebilibili.data.model.response.AiModelResult
import com.android.purebilibili.data.model.response.AiSummaryData
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.clearTransientPlaybackPreviewData
import com.android.purebilibili.feature.video.viewmodel.initialAiSummaryPromptState

@OptIn(ExperimentalCoroutinesApi::class)
class VideoUiWorkPolicyTest {
    @Test
    fun hiddenWorkCancelsInflightRequestAndResumesOnlyUnfinishedWork() = runTest {
        val permission = MutableStateFlow(VideoUiWorkPermission.Hidden)
        val responses = Channel<Int>(Channel.UNLIMITED)
        var requests = 0
        var cancellations = 0
        val result = async {
            runVisibleVideoUiWork(permission) {
                requests++
                try {
                    responses.receive()
                } catch (cancelled: CancellationException) {
                    cancellations++
                    throw cancelled
                }
            }
        }
        runCurrent()
        assertEquals(0, requests)
        permission.value = VideoUiWorkPermission.Visible
        runCurrent()
        assertEquals(1, requests)
        permission.value = VideoUiWorkPermission.Hidden
        runCurrent()
        assertEquals(1, cancellations)
        responses.send(42)
        runCurrent()
        assertEquals(1, requests)
        permission.value = VideoUiWorkPermission.Visible
        runCurrent()
        assertEquals(42, result.await())
        assertEquals(2, requests)

        permission.value = VideoUiWorkPermission.Hidden
        runCurrent()
        permission.value = VideoUiWorkPermission.Visible
        runCurrent()
        assertEquals(2, requests)
        responses.close()
    }

    @Test
    fun mediaChangeCancelsOldWaitingOrInflightWork() = runTest {
        for (initial in listOf(VideoUiWorkPermission.Hidden, VideoUiWorkPermission.Visible)) {
            val permission = MutableStateFlow(initial)
            val result = async { runVisibleVideoUiWork(permission) { awaitCancellation() } }
            runCurrent()
            permission.value = VideoUiWorkPermission.Stale
            runCurrent()
            assertTrue(result.isCancelled)
        }
    }

    @Test
    fun requestsRequireCurrentMediaForegroundAndTheActualConsumer() {
        val media = VideoUiWorkMedia("BV1", 10L)
        val ai = VideoUiWorkConsumer(VideoUiWorkKind.AI_SUMMARY, media)
        val old = VideoUiWorkConsumer(VideoUiWorkKind.SEEK_PREVIEW, VideoUiWorkMedia("BV1", 9L))
        assertEquals(VideoUiWorkPermission.Hidden, resolveVideoUiWorkPermission(
            media, 2L, media, 2L, listOf(old), false,
        ))
        assertEquals(VideoUiWorkPermission.Visible, resolveVideoUiWorkPermission(
            media, 2L, media, 2L, listOf(old, ai), false, VideoUiWorkKind.AI_SUMMARY,
        ))
        assertEquals(VideoUiWorkPermission.Hidden, resolveVideoUiWorkPermission(
            media, 2L, media, 2L, listOf(ai), false, VideoUiWorkKind.SEEK_PREVIEW,
        ))
        assertEquals(VideoUiWorkPermission.Hidden, resolveVideoUiWorkPermission(
            media, 2L, media, 2L, listOf(ai), true,
        ))
        assertEquals(VideoUiWorkPermission.Stale, resolveVideoUiWorkPermission(
            media, 2L, media.copy(cid = 11L), 2L, listOf(ai), false, VideoUiWorkKind.AI_SUMMARY,
        ))
        assertEquals(VideoUiWorkPermission.Stale, resolveVideoUiWorkPermission(
            media, 2L, media, 3L, listOf(ai), false,
        ))
        assertEquals(VideoUiWorkPermission.Stale, resolveVideoUiWorkPermission(
            media, 2L, VideoUiWorkMedia("BV2", 10L), 2L, listOf(ai), false,
        ))
        val nextPart = media.copy(cid = 11L)
        assertEquals(VideoUiWorkPermission.Visible, resolveVideoUiWorkPermission(
            media, 2L, nextPart, 2L, listOf(VideoUiWorkConsumer(VideoUiWorkKind.DETAIL, nextPart)), false,
        ))
        assertEquals(VideoUiWorkPermission.Hidden, resolveVideoUiWorkPermission(
            media, 2L, nextPart, 2L, listOf(ai), false,
        ))
    }

    @Test
    fun switchingPartClearsSummaryAndPromptEvenWhenNoPreviewImageWasLoaded() {
        val state = VideoPlaybackUiState.Success(
            info = ViewInfo(bvid = "BV1", cid = 10L),
            playUrl = "https://example.com/video.mp4",
            aiSummary = AiSummaryData(modelResult = AiModelResult(summary = "第一段内容")),
            aiSummaryPrompt = initialAiSummaryPromptState(),
            ownerFollowerCount = 42,
        )
        val cleared = clearTransientPlaybackPreviewData(state)
        assertNull(cleared.aiSummary)
        assertNull(cleared.aiSummaryPrompt)
        assertNull(cleared.videoshotData)
        assertSame(state.info, cleared.info)
        assertEquals(42, cleared.ownerFollowerCount)
    }
}
