package top.yukonga.miuix.kmp.nav.core

import androidx.compose.runtime.compositionLocalOf

/** 实际曝光许可；保留在导航栈中不代表仍可见，返回露出时恢复。 */
val LocalNavEntryContentVisible = compositionLocalOf { true }

internal fun navEntryContentVisible(
    relativeDepth: Float,
    opaqueDepth: Float,
    isRemoving: Boolean,
): Boolean = relativeDepth < opaqueDepth &&
    (relativeDepth > -1f || (relativeDepth == -1f && !isRemoving))
