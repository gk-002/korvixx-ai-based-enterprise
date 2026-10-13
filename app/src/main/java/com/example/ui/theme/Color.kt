package com.example.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

val isSystemDark = mutableStateOf(false)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF4F46E5) // Korvix primary indigo
val PurpleGrey40 = Color(0xFF818CF8) // Korvix secondary indigo
val Pink40 = Color(0xFF7D5260)

// Korvix Brand Colors
val KorvixPrimary: Color
    get() = if (isSystemDark.value) Color(0xFFA5B4FC) else Color(0xFF4F46E5) // Brighter high-contrast indigo

val KorvixSecondary: Color
    get() = if (isSystemDark.value) Color(0xFFC7D2FE) else Color(0xFF818CF8)

val KorvixPrimaryDark: Color
    get() = if (isSystemDark.value) Color(0xFFEEF2FF) else Color(0xFF3730A3)

val KorvixPrimaryLight: Color
    get() = if (isSystemDark.value) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)

val KorvixBackground: Color
    get() = if (isSystemDark.value) Color(0xFF090D16) else Color(0xFFFBF8FF) // Ultra dark high-contrast background

val KorvixSurface: Color
    get() = if (isSystemDark.value) Color(0xFF151F32) else Color(0xFFFFFFFF)

val KorvixBorder: Color
    get() = if (isSystemDark.value) Color(0xFF2E3B4E) else Color(0xFFF5F3FF)

val KorvixBorderAccent: Color
    get() = if (isSystemDark.value) Color(0xFF3F4E64) else Color(0xFFF3E8FF)

val KorvixTextDark: Color
    get() = if (isSystemDark.value) Color(0xFFF8FAFC) else Color(0xFF0F172A)

val KorvixTextMuted: Color
    get() = if (isSystemDark.value) Color(0xFF94A3B8) else Color(0xFF64748B)

// Frosted Glassmorphism Colors
val KorvixGlassSurface: Color
    get() = if (isSystemDark.value) Color(0xE0111827) else Color(0xC0FFFFFF)

val KorvixGlassBorder: Color
    get() = if (isSystemDark.value) Color(0x66818CF8) else Color(0x664F46E5)

val KorvixGlassBorderLight: Color
    get() = if (isSystemDark.value) Color(0x22FFFFFF) else Color(0x40FFFFFF)

val KorvixGlassAccent: Color
    get() = if (isSystemDark.value) Color(0x22818CF8) else Color(0x1AE9D5FF)

val KorvixGreen: Color
    get() = if (isSystemDark.value) Color(0xFF34D399) else Color(0xFF10B981)

val KorvixGreenLight: Color
    get() = if (isSystemDark.value) Color(0xFF064E3B) else Color(0xFFECFDF5)

val KorvixOrange: Color
    get() = if (isSystemDark.value) Color(0xFFFBBF24) else Color(0xFFF59E0B)

val KorvixOrangeLight: Color
    get() = if (isSystemDark.value) Color(0xFF78350F) else Color(0xFFFEF3C7)

val KorvixRed: Color
    get() = if (isSystemDark.value) Color(0xFFF87171) else Color(0xFFEF4444)

val KorvixRedLight: Color
    get() = if (isSystemDark.value) Color(0xFF7F1D1D) else Color(0xFFFEF2F2)

val KorvixBlue: Color
    get() = if (isSystemDark.value) Color(0xFF60A5FA) else Color(0xFF3B82F6)

val KorvixBlueLight: Color
    get() = if (isSystemDark.value) Color(0xFF1E3A8A) else Color(0xFFEFF6FF)
