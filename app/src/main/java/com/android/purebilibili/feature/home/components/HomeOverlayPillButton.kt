// 文件路径: feature/home/components/HomeOverlayPillButton.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.ui.AppContentColors

import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppButton

/**
 * 首页/动态信息流悬浮胶囊按钮的统一样式：不透明容器色 + 白色描边 + 药丸形状。
 *
 * 「撤销刷新」「定位上次刷新」等悬浮提示必须共用本组件。悬浮玻璃（真实 backdrop
 * 采样）在此不可用：胶囊挂在首页 chrome backdrop 源的记录子树之内，任何消费该
 * 源的玻璃都会形成 RenderNode 自采样环，触发小米 HWUI MiBackgroundBlurBlend 的
 * 无限递归（RenderThread 栈溢出闪退，0.8.0 回归）；壁纸源又依赖卡片毛玻璃与
 * 静态壁纸开关，覆盖不了全部场景。因此固定不透明实色，与玻璃/模糊设置解耦。
 */
@Composable
internal fun HomeOverlayPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    AppButton(
        onClick = onClick,
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Pill),
        containerColor = AppSurfaceTokens.cardContainer(),
        contentColor = AppContentColors.primaryText(),
        border = BorderStroke(AppSpacingTokens.Micro * 0.4f, Color.White.copy(alpha = 0.12f)),
        defaultElevation = AppSpacingTokens.ExtraSmall,
        pressedElevation = AppSpacingTokens.Micro,
        contentPadding = PaddingValues(
            horizontal = AppSpacingTokens.Large,
            vertical = AppSpacingTokens.Small + AppSpacingTokens.Micro,
        ),
        content = content,
    )
}
