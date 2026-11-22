package com.quriva.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

val LocalReduceMotion = staticCompositionLocalOf { false }

fun Modifier.interactiveClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (!reduceMotion && enabled && pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "interactiveScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Small entrance animation used by list/grid rows. The first visible batch is
 * staggered; later rows start immediately as they enter composition.
 */
@Composable
fun AnimatedListItem(
    modifier: Modifier = Modifier,
    index: Int = 0,
    content: @Composable () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    var visible by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible || reduceMotion) 1f else 0f,
        animationSpec = tween(260),
        label = "listItemAlpha"
    )
    val animatedOffset by animateFloatAsState(
        targetValue = if (visible || reduceMotion) 0f else 14f,
        animationSpec = tween(300),
        label = "listItemOffset"
    )
    LaunchedEffect(Unit) {
        val stagger = if (!reduceMotion && index in 1..8) index * 28L else 0L
        delay(stagger)
        visible = true
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = animatedAlpha
            translationY = animatedOffset
        }
    ) {
        content()
    }
}
