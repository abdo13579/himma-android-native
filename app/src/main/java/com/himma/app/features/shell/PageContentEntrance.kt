package com.himma.app.features.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Material 3 Choreographed Page Content Entrance.
 *
 * Runs exclusively on the GPU RenderThread via [Modifier.graphicsLayer] with 0 recomposition overhead:
 * 1. Waits for the page navigation transition to settle ([delayMillis]).
 * 2. Elegantly resolves page content via upward slide, subtle scale expansion, and alpha fade.
 */
@Composable
fun PageContentEntrance(
    modifier: Modifier = Modifier,
    delayMillis: Int = 100,
    durationMillis: Int = 280,
    slideDistance: Dp = 32.dp,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val slideDistancePx = with(density) { slideDistance.toPx() }

    LaunchedEffect(Unit) {
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val value = progress.value
                alpha = value
                translationY = (1f - value) * slideDistancePx
                scaleX = 0.97f + (0.03f * value)
                scaleY = 0.97f + (0.03f * value)
            },
    ) {
        content()
    }
}
