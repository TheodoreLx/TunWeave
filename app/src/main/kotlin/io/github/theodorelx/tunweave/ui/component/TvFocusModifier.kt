package io.github.theodorelx.tunweave.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modifier that adds an animated scale and border highlight when the element receives D-pad focus.
 */
fun Modifier.tvFocusHighlight(
    shape: Shape = RoundedCornerShape(12.dp),
    focusedBorderWidth: Dp = 3.dp,
    focusedBorderColor: Color? = null,
    focusedScale: Float = 1.03f,
    onFocusChange: ((Boolean) -> Unit)? = null,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "tvFocusScale",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) {
            focusedBorderColor ?: MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "tvFocusBorder",
    )

    this
        .onFocusChanged {
            isFocused = it.isFocused
            onFocusChange?.invoke(it.isFocused)
        }
        .scale(scale)
        .border(
            width = if (isFocused) focusedBorderWidth else 0.dp,
            color = borderColor,
            shape = shape,
        )
}

/**
 * Modifier for clickable TV cards/items with built-in focus scale, outline, and D-pad click support.
 */
fun Modifier.tvClickableItem(
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1.03f,
    enabled: Boolean = true,
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "tvItemScale",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "tvItemBorder",
    )

    this
        .onFocusChanged { isFocused = it.isFocused }
        .scale(scale)
        .border(
            border = if (isFocused) BorderStroke(3.dp, borderColor) else BorderStroke(0.dp, Color.Transparent),
            shape = shape,
        )
        .clickable(
            enabled = enabled,
            onClick = onClick,
        )
        .focusable(enabled = enabled)
}
