package me.lemonhall.worddragon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val WordDragonTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontSize = 38.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold,
            ),
        headlineMedium =
            TextStyle(
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        titleLarge =
            TextStyle(
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        bodyLarge =
            TextStyle(
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
            ),
        labelLarge =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
            ),
    )
