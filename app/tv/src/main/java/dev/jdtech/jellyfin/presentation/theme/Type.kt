package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography as TypographyTv

val Typography =
    Typography(
        displayMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 44.sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
        bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
        labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
    )

val TypographyTv =
    TypographyTv(
        displayMedium = Typography.displayMedium,
        headlineMedium = Typography.headlineMedium,
        titleLarge = Typography.titleLarge,
        titleMedium = Typography.titleMedium,
        titleSmall = Typography.titleSmall,
        bodyMedium = Typography.bodyMedium,
        labelMedium = Typography.labelMedium,
    )
