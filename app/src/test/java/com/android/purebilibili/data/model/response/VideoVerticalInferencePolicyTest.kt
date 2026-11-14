package com.android.purebilibili.data.model.response

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoVerticalInferencePolicyTest {

    @Test
    fun inferVerticalFromCoverUrl_whenHeightExceedsWidth() {
        assertTrue(
            inferVerticalVideoFromCoverUrl(
                "https://i0.hdslb.com/bfs/archive/demo.jpg@480w_720h.webp"
            )
        )
    }

    @Test
    fun inferVerticalFromCoverUrl_returnsFalseForLandscapeCover() {
        assertFalse(
            inferVerticalVideoFromCoverUrl(
                "https://i0.hdslb.com/bfs/archive/demo.jpg@720w_480h.webp"
            )
        )
    }
}
