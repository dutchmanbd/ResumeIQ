package com.dutchman.resumeiq.presentation.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun GoogleTranslateFloatingButton(
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    var offset by remember {
        mutableStateOf(Offset(screenWidth - 120f, 50f))
    }

    val animatedOffset by animateOffsetAsState(
        targetValue = offset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetAnimation"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = with(LocalDensity.current) { animatedOffset.x.toDp() },
                    y = with(LocalDensity.current) { animatedOffset.y.toDp() }
                )
                .size(60.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (offset.x + dragAmount.x).coerceIn(0f, screenWidth - 100f)
                        val newY = (offset.y + dragAmount.y).coerceIn(0f, screenHeight - 100f)
                        offset = Offset(newX, newY)
                    }
                }
                .clickable(
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4285F4))  // Google Blue
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Translate",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun GoogleTranslateButton(
    iconBitmap: Bitmap?,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

    // State to track the position of the floating button with initial position set directly
    var offset by remember {
        mutableStateOf(Offset(screenWidth - 120f, 50f))
    }

    // Animation for smooth movement ONLY DURING DRAGGING (not initial placement)
    val animatedOffset by animateOffsetAsState(
        targetValue = offset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // State to control expanded menu visibility
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // The Google Translate floating button
        Box(
            modifier = Modifier
                .offset(
                    x = with(LocalDensity.current) { animatedOffset.x.toDp() },
                    y = with(LocalDensity.current) { animatedOffset.y.toDp() }
                )
                .size(60.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Update position with drag
                        val newX = (offset.x + dragAmount.x).coerceIn(0f, screenWidth - 100f)
                        val newY = (offset.y + dragAmount.y).coerceIn(0f, screenHeight - 100f)
                        offset = Offset(newX, newY)
                    }
                }
                .clickable(
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = "Google Translate",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4))  // Google Blue
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Translate",
                        tint = Color.White
                    )
                }
            }
        }
    }

}

