package com.android.purebilibili.core.performance

import kotlin.test.Test
import kotlin.test.assertEquals

class PerformanceSceneTimelineTest {
    @Test
    fun `fling events extend scrolling window and become idle after tail`() {
        val timeline = PerformanceSceneTimeline(0, "home", false)
        timeline.scroll(10_000_000)
        timeline.scroll(100_000_000)
        assertEquals("scroll", timeline.sceneAt(150_000_000, 160_000_000).interaction)
        assertEquals("idle", timeline.sceneAt(221_000_000, 230_000_000).interaction)
    }

    @Test
    fun `navigation takes priority over scrolling and includes boundary frame`() {
        val timeline = PerformanceSceneTimeline(0, "home", false)
        timeline.scroll(1)
        timeline.boundary(10, "settings", true)
        timeline.boundary(30, "settings", false)
        assertEquals(PerformanceScene("settings", "transition"), timeline.sceneAt(5, 15))
        assertEquals("transition", timeline.sceneAt(20, 35).interaction)
        assertEquals("scroll", timeline.sceneAt(40, 50).interaction)
    }

    @Test
    fun `evicted frame context is unknown instead of attributed to current page`() {
        val timeline = PerformanceSceneTimeline(0, "home", false)
        repeat(300) { index -> timeline.boundary(index + 1L, "page$index", false) }
        assertEquals(PerformanceScene("unknown", "unknown"), timeline.sceneAt(1, 2))
    }

    @Test
    fun `delayed frame uses its original page and scene instead of callback time`() {
        val timeline = PerformanceSceneTimeline(0, "home", false)
        timeline.boundary(100, "settings", true)
        timeline.boundary(200, "settings", false)
        assertEquals(PerformanceScene("home", "idle"), timeline.sceneAt(10, 20))
        assertEquals(PerformanceScene("settings", "transition"), timeline.sceneAt(110, 120))
        assertEquals(PerformanceScene("settings", "idle"), timeline.sceneAt(210, 220))
    }
}
