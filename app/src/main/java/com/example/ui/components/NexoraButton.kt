package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class ButtonRole {
    NUMBER,
    SECONDARY,
    PRIMARY_OP,
    EQUAL,
    ACCENT
}

@Composable
fun NexoraButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: ImageVector? = null,
    role: ButtonRole = ButtonRole.NUMBER,
    height: Dp = 54.dp,
    testTag: String = text ?: "btn",
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        label = "button_scale"
    )

    val bgColor = when (role) {
        ButtonRole.NUMBER -> NexoraCalcButton
        ButtonRole.SECONDARY -> NexoraSecondaryButton
        ButtonRole.PRIMARY_OP -> NexoraPrimaryOp
        ButtonRole.EQUAL -> NexoraEqualButton
        ButtonRole.ACCENT -> NexoraAccentBlue
    }

    val textColor = when (role) {
        ButtonRole.NUMBER -> NexoraMainText
        ButtonRole.SECONDARY -> NexoraAccentBlue
        ButtonRole.PRIMARY_OP -> NexoraMainText
        ButtonRole.EQUAL -> NexoraEqualText
        ButtonRole.ACCENT -> NexoraMainBg
    }

    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .clip(shape)
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (role == ButtonRole.EQUAL) NexoraAccentBlue.copy(alpha = 0.5f) else NexoraBorder.copy(alpha = 0.4f),
                shape = shape
            )
            .testTag(testTag)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = {
                        if (onLongClick != null) {
                            onLongClick()
                        } else {
                            onClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text ?: "icon",
                tint = textColor,
                modifier = Modifier.size(22.dp)
            )
        } else if (text != null) {
            val fontSize = when {
                text.length > 3 -> 15.sp
                text.length > 1 -> 18.sp
                role == ButtonRole.PRIMARY_OP || role == ButtonRole.EQUAL -> 24.sp
                else -> 22.sp
            }
            Text(
                text = text,
                color = textColor,
                fontSize = fontSize,
                fontWeight = if (role == ButtonRole.EQUAL || role == ButtonRole.PRIMARY_OP) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
