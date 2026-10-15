package io.github.fvory.psdk.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.fvory.psdk.config.SkeletonOptions

/** Full-viewer loading overlay from `showSkeleton` (B-SK-01/02). Fades in and out in 200 ms. */
@Composable
internal fun SkeletonOverlay(options: SkeletonOptions?) {
    val colors = LocalPsdkColors.current
    AnimatedVisibility(
        visible = options != null,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.fillMaxSize(),
    ) {
        val shown = options ?: SkeletonOptions()
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = shown.backgroundOpacity ?: 0.7f))
                // Swallow touches so the pages underneath cannot be used while it is shown
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            contentAlignment = Alignment.Center,
        ) {
            shown.content?.invoke() ?: CircularProgressIndicator(color = colors.accent)
        }
    }
}
