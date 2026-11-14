package com.android.purebilibili.feature.space

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.android.purebilibili.data.model.response.SeasonArchiveItem
import com.android.purebilibili.data.model.response.SeriesArchiveItem
import kotlinx.coroutines.flow.collect

internal enum class SpaceCollectionKind { SEASON, SERIES }

internal data class SpaceCollectionPreviewKey(val kind: SpaceCollectionKind, val id: Long)

/** 只按实际视口发起请求，Lazy 预组合或相邻页预加载不算曝光。 */
@Composable
internal fun ObserveSpaceCollectionPreviews(
    gridState: LazyStaggeredGridState,
    targets: Map<Any, SpaceCollectionPreviewKey>,
    active: Boolean,
    onVisible: (SpaceCollectionPreviewKey) -> Unit,
) {
    val latestOnVisible by rememberUpdatedState(onVisible)
    LaunchedEffect(gridState, targets, active) {
        if (!active || targets.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val layout = gridState.layoutInfo
            layout.visibleItemsInfo.mapNotNull { item ->
                if (item.offset.y < layout.viewportEndOffset &&
                    item.offset.y + item.size.height > layout.viewportStartOffset
                ) targets[item.key] else null
            }
        }.collect { visible -> visible.forEach { latestOnVisible(it) } }
    }
}

/** 已加载的较长列表不能被迟到的短预览覆盖，空结果也记作已经加载。 */
internal fun applySpaceSeasonArchives(
    state: SpaceUiState.Success,
    id: Long,
    archives: List<SeasonArchiveItem>,
): SpaceUiState.Success {
    val existing = state.seasonArchives[id]
    if (existing != null && (existing.size > archives.size || existing == archives)) return state
    return state.copy(seasonArchives = state.seasonArchives + (id to archives))
}

internal fun applySpaceSeriesArchives(
    state: SpaceUiState.Success,
    id: Long,
    archives: List<SeriesArchiveItem>,
): SpaceUiState.Success {
    val existing = state.seriesArchives[id]
    if (existing != null && (existing.size > archives.size || existing == archives)) return state
    return state.copy(seriesArchives = state.seriesArchives + (id to archives))
}
