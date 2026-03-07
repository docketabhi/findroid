package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Shapes as ShapesTv

val shapes = Shapes(extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(4.dp))

val shapesTv = ShapesTv(extraSmall = shapes.extraSmall, small = shapes.small)
