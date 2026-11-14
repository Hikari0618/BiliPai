package com.android.purebilibili.feature.video.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import com.android.purebilibili.feature.video.policy.resolveVideoDetailCollapseProgress
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class InlinePlayerCollapseMotion(
    val progress: State<Float>,
    val viewportHeight: State<Dp>,
)

private data class InlinePlayerCollapseTarget(
    val progress: Float,
    val heightDp: Float,
    val animate: Boolean,
)

/** 跟手位移直接供布局/绘制读取；只有恢复与布局校正启动动画，不订阅整页重组。 */
@Composable
internal fun rememberInlinePlayerCollapseMotion(
    collapseState: InlinePortraitPlayerCollapseState,
    expandedHeight: Dp,
    collapseRangePx: Float,
    compactForCommentTab: Boolean,
    hidePausedPlayerForComments: Boolean,
    isPortraitFullscreen: Boolean,
    expandForSharedReturn: Boolean,
    durationMillis: Int,
): InlinePlayerCollapseMotion {
    val density = LocalDensity.current
    val commentProgress = animateFloatAsState(
        targetValue = if (compactForCommentTab) 1f else 0f,
        animationSpec = tween(durationMillis, easing = AppMotionEasing.M1Standard),
        label = "inline_portrait_comment_tab_collapse",
    )
    val target = remember(
        collapseState, expandedHeight, collapseRangePx, hidePausedPlayerForComments,
        isPortraitFullscreen, expandForSharedReturn, density, commentProgress,
    ) {
        derivedStateOf {
            val offset = if (isPortraitFullscreen) 0f else collapseState.offsetPx
            InlinePlayerCollapseTarget(
                progress = resolvePlayerCollapseProgressForLayout(
                    manualOrCompactCollapseProgress = resolveInlinePortraitPlayerCollapseProgress(
                        manualCollapseProgress = resolveVideoDetailCollapseProgress(
                            playerHeightOffsetPx = offset,
                            collapseRangePx = collapseRangePx,
                            isPortraitFullscreen = isPortraitFullscreen,
                        ),
                        compactForCommentTabProgress = commentProgress.value,
                        restoreRequested = collapseState.restoreRequested,
                        hidePausedPlayerForComments = hidePausedPlayerForComments,
                    ),
                    expandForSharedReturn = expandForSharedReturn,
                ),
                heightDp = if (expandForSharedReturn || isPortraitFullscreen) expandedHeight.value
                    else (expandedHeight + with(density) { offset.toDp() })
                        .coerceIn(0.dp, expandedHeight).value,
                animate = !expandForSharedReturn && !isPortraitFullscreen &&
                    (collapseState.restoreRequested || collapseState.layoutReconciled),
            )
        }
    }
    val progress = remember(collapseState) {
        Animatable(Snapshot.withoutReadObservation { target.value.progress })
    }
    val height = remember(collapseState) {
        Animatable(Snapshot.withoutReadObservation { target.value.heightDp })
    }
    LaunchedEffect(target, durationMillis) {
        snapshotFlow { target.value }.collectLatest { next ->
            if (next.animate) {
                coroutineScope {
                    launch { progress.animateTo(next.progress, tween(durationMillis, easing = AppMotionEasing.M1Standard)) }
                    launch { height.animateTo(next.heightDp, tween(durationMillis, easing = AppMotionEasing.M1Standard)) }
                }
            } else {
                progress.snapTo(next.progress)
                height.snapTo(next.heightDp)
            }
        }
    }
    return remember(target, progress, height) {
        InlinePlayerCollapseMotion(
            progress = derivedStateOf { if (target.value.animate) progress.value else target.value.progress },
            viewportHeight = derivedStateOf { (if (target.value.animate) height.value else target.value.heightDp).dp },
        )
    }
}
