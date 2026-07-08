package com.dutchman.resumeiq.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.font.Font
import com.dutchman.resumeiq.R

val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex)
)

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = RobotoFlex),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = RobotoFlex),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = RobotoFlex),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = RobotoFlex),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = RobotoFlex),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = RobotoFlex),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = RobotoFlex),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = RobotoFlex),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = RobotoFlex),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = RobotoFlex),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = RobotoFlex),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = RobotoFlex),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = RobotoFlex),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = RobotoFlex),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = RobotoFlex)
)