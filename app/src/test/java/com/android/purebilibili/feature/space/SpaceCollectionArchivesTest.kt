package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SeasonArchiveItem
import com.android.purebilibili.data.model.response.SeriesArchiveItem
import com.android.purebilibili.data.model.response.SpaceUserInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpaceCollectionArchivesTest {
    @Test
    fun latePreviewsKeepLongerArchivesAndSeparateSeasonAndSeriesWithSameId() {
        val initial = SpaceUiState.Success(
            userInfo = SpaceUserInfo(mid = 42L),
            seasonArchives = mapOf(1L to listOf(SeasonArchiveItem(bvid = "BV1"), SeasonArchiveItem(bvid = "BV2"))),
            seriesArchives = mapOf(1L to listOf(SeriesArchiveItem(bvid = "BV3"), SeriesArchiveItem(bvid = "BV4"))),
        )
        assertSame(initial, applySpaceSeasonArchives(initial, 1L, listOf(SeasonArchiveItem(bvid = "BV5"))))
        assertSame(initial, applySpaceSeriesArchives(initial, 1L, listOf(SeriesArchiveItem(bvid = "BV6"))))
        val updated = applySpaceSeriesArchives(initial, 2L, listOf(SeriesArchiveItem(bvid = "BV7")))
        assertEquals(initial.seasonArchives, updated.seasonArchives)
        assertEquals(initial.seriesArchives.getValue(1L), updated.seriesArchives.getValue(1L))
        assertEquals("BV7", updated.seriesArchives.getValue(2L).single().bvid)
        assertEquals(initial.userInfo, updated.userInfo)
    }

    @Test
    fun successfulEmptyArchiveResultsAreRemembered() {
        val initial = SpaceUiState.Success(userInfo = SpaceUserInfo(mid = 42L))
        val updated = applySpaceSeriesArchives(applySpaceSeasonArchives(initial, 1L, emptyList()), 1L, emptyList())
        assertTrue(updated.seasonArchives.containsKey(1L))
        assertTrue(updated.seriesArchives.containsKey(1L))
        assertTrue(updated.seasonArchives.getValue(1L).isEmpty())
        assertTrue(updated.seriesArchives.getValue(1L).isEmpty())
        val refreshed = applySpaceSupplementalData(
            updated, emptyList(), emptyList(), emptyList(), emptyList(), emptyMap(), emptyMap(),
        )
        assertTrue(refreshed.seasonArchives.containsKey(1L))
        assertTrue(refreshed.seriesArchives.containsKey(1L))
    }

    @Test
    fun equalLengthFreshResultsCanReplaceChangedVideos() {
        val initial = SpaceUiState.Success(
            userInfo = SpaceUserInfo(mid = 42L),
            seasonArchives = mapOf(1L to listOf(SeasonArchiveItem(bvid = "BVOLD"))),
            seriesArchives = mapOf(1L to listOf(SeriesArchiveItem(bvid = "BVOLD"))),
        )
        val updated = applySpaceSeriesArchives(
            applySpaceSeasonArchives(initial, 1L, listOf(SeasonArchiveItem(bvid = "BVNEW"))),
            1L, listOf(SeriesArchiveItem(bvid = "BVNEW")),
        )
        assertEquals("BVNEW", updated.seasonArchives.getValue(1L).single().bvid)
        assertEquals("BVNEW", updated.seriesArchives.getValue(1L).single().bvid)
    }

    @Test
    fun completedEmptyDirectoryIsLoadedAndStopsLoading() {
        val initial = SpaceUiState.Success(
            userInfo = SpaceUserInfo(mid = 42L),
            tabShellState = buildInitialTabShellState(SpaceMainTab.COLLECTIONS)
                .withUpdatedTab(SpaceMainTab.COLLECTIONS) { it.copy(isLoading = true) },
        )
        val updated = applySpaceSupplementalData(
            initial, emptyList(), emptyList(), emptyList(), emptyList(), emptyMap(), emptyMap(),
        )
        val tab = updated.tabShellState.tabStates.getValue(SpaceMainTab.COLLECTIONS)
        assertTrue(tab.hasLoaded)
        assertFalse(tab.isLoading)
        assertEquals(SpaceMainTab.COLLECTIONS, updated.tabShellState.selectedTab)
    }
}
