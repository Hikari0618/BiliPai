// 文件路径: core/performance/PerformanceCaptureOverlay.kt
package com.android.purebilibili.core.performance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.AppText

/**
 * 性能采集全局悬浮提示：采集开启期间在任意页面顶部显示录制标识胶囊，
 * 避免使用者离开设置页后忘记采集仍在运行。
 * 点击展开确认对话框，可标记问题时间点或直接停止采集；采集未开启时不组合任何内容。
 */
@Composable
fun PerformanceCaptureOverlayHost(modifier: Modifier = Modifier) {
    val status by PerformanceCapture.status.collectAsStateWithLifecycle()
    AnimatedVisibility(
        visible = status.recording,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val windowOwner = LocalView.current.rootView
        var showActions by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                    .clickable { showActions = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5484D)),
                )
                Spacer(modifier = Modifier.width(6.dp))
                AppText(
                    text = "性能采集中",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppContentColors.primaryText(),
                )
            }
        }
        if (showActions) {
            AppAlertDialog(
                onDismissRequest = { showActions = false },
                title = { AppText("正在采集性能数据") },
                text = { AppText(status.message) },
                dismissButton = {
                    AppDialogAction(onClick = {
                        showActions = false
                        PerformanceCapture.mark(windowOwner)
                    }) { AppText("标记刚才的问题") }
                },
                confirmButton = {
                    AppDialogAction(onClick = {
                        showActions = false
                        PerformanceCapture.stop()
                    }) { AppText("停止采集") }
                },
            )
        }
    }
}
