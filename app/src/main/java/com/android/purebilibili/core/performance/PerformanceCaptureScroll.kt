package com.android.purebilibili.core.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

/** 观察子列表和 Pager 已消费的真实位移，不消费手势、不读逐帧 Compose 状态。 */
@Composable
internal fun Modifier.performanceCaptureScroll(): Modifier {
    val windowOwner = LocalView.current.rootView
    val observer = remember(windowOwner) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (consumed != Offset.Zero) PerformanceCapture.onScroll(windowOwner)
                return Offset.Zero
            }
        }
    }
    return nestedScroll(observer)
}
