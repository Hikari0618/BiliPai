package com.android.purebilibili.feature.space

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SpaceCollectionPreviewVisibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onlyActiveViewportRequestsPreviewsAndReactivationUsesCurrentViewport() {
        val active = mutableStateOf(false)
        val requested = mutableSetOf<SpaceCollectionPreviewKey>()
        val targets: Map<Any, SpaceCollectionPreviewKey> = (1L..3L).associate {
            "season_$it" to SpaceCollectionPreviewKey(SpaceCollectionKind.SEASON, it)
        }
        lateinit var grid: LazyStaggeredGridState
        lateinit var scope: CoroutineScope
        composeRule.setContent {
            grid = rememberLazyStaggeredGridState()
            scope = rememberCoroutineScope()
            ObserveSpaceCollectionPreviews(grid, targets, active.value) { requested.add(it) }
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(1),
                state = grid,
                modifier = Modifier.width(120.dp).height(100.dp),
            ) {
                for (id in 1L..3L) {
                    item(key = "season_$id") { Box(Modifier.height(100.dp)) }
                }
            }
        }
        composeRule.runOnIdle { assertEquals(emptySet<SpaceCollectionPreviewKey>(), requested) }
        composeRule.runOnIdle { active.value = true }
        composeRule.runOnIdle { assertEquals(setOf(targets.getValue("season_1")), requested) }
        composeRule.runOnIdle { scope.launch { grid.scrollToItem(2) } }
        composeRule.runOnIdle {
            assertEquals(setOf(targets.getValue("season_1"), targets.getValue("season_3")), requested)
            active.value = false
        }
        composeRule.runOnIdle { scope.launch { grid.scrollToItem(1) } }
        composeRule.runOnIdle {
            assertEquals(setOf(targets.getValue("season_1"), targets.getValue("season_3")), requested)
            active.value = true
        }
        composeRule.runOnIdle { assertEquals(targets.values.toSet(), requested) }
    }
}
