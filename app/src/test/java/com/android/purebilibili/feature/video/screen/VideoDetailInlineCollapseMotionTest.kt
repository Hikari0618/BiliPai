package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.android.purebilibili.feature.video.policy.VideoDetailScrollUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoDetailInlineCollapseMotionTest {
    @Test
    fun dragUpdatesViewportImmediatelyWithoutWaitingForAnimationFrame() = runTest {
        withMotion { state, motion, _ ->
            state.updateOffset(-120f)
            assertEquals(180.dp, motion.viewportHeight.value)
            assertEquals(0.5f, motion.progress.value, 0.001f)
            state.updateOffset(-240f)
            assertEquals(60.dp, motion.viewportHeight.value)
            assertEquals(1f, motion.progress.value, 0.001f)
        }
    }

    @Test
    fun restoreAnimatesFromCollapsedViewportAndNewDragImmediatelyTakesOver() = runTest {
        withMotion { state, motion, clock ->
            state.updateOffset(-240f)
            Snapshot.sendApplyNotifications()
            runCurrent()
            state.restore()
            Snapshot.sendApplyNotifications()
            runCurrent()
            assertEquals(60.dp, motion.viewportHeight.value)
            clock.sendFrame(0L)
            runCurrent()
            clock.sendFrame(150_000_000L)
            runCurrent()
            assertTrue(motion.viewportHeight.value > 60.dp)
            assertTrue(motion.viewportHeight.value < 300.dp)

            state.consumeScroll(VideoDetailScrollUpdate(nextOffsetPx = -48f, consumedDeltaPx = -48f))
            assertEquals(252.dp, motion.viewportHeight.value)
            assertEquals(0.2f, motion.progress.value, 0.001f)
            Snapshot.sendApplyNotifications()
            runCurrent()
            clock.sendFrame(400_000_000L)
            runCurrent()
            assertEquals(252.dp, motion.viewportHeight.value)
        }
    }

    private suspend fun TestScope.withMotion(
        block: suspend (InlinePortraitPlayerCollapseState, InlinePlayerCollapseMotion, BroadcastFrameClock) -> Unit,
    ) {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + clock)
        val composition = Composition(UnitApplier(), recomposer)
        val state = InlinePortraitPlayerCollapseState()
        lateinit var motion: InlinePlayerCollapseMotion
        try {
            composition.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    motion = rememberInlinePlayerCollapseMotion(
                        collapseState = state,
                        expandedHeight = 300.dp,
                        collapseRangePx = 240f,
                        compactForCommentTab = false,
                        hidePausedPlayerForComments = false,
                        isPortraitFullscreen = false,
                        expandForSharedReturn = false,
                        durationMillis = 300,
                    )
                }
            }
            runCurrent()
            block(state, motion, clock)
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }

    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
