package fumi.day.literalmemo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import fumi.day.literalmemo.R
import fumi.day.literalmemo.data.prefs.AppFont
import fumi.day.literalmemo.data.prefs.UserPrefs

val ScopeOneFamily = FontFamily(
    Font(R.font.scopeone, FontWeight.Normal)
)

data class AppThemeState(
    val fontFamily: FontFamily = FontFamily.Default,
    val fontSize: Float = 16f,
    val backgroundColor: Color = Color.Unspecified,
    val textColor: Color = Color.Unspecified,
    val accentColor: Color = Color.Unspecified
)

val LocalAppTheme = compositionLocalOf { AppThemeState() }

fun parseColor(hex: String): Color {
    if (hex.isBlank()) return Color.Unspecified
    return try {
        Color(hex.toColorInt())
    } catch (e: Exception) {
        Color.Unspecified
    }
}

fun AppFont.toFontFamily(): FontFamily {
    return when (this) {
        AppFont.DEFAULT -> FontFamily.Default
        AppFont.SERIF -> FontFamily.Serif
        AppFont.MONOSPACE -> FontFamily.Monospace
        AppFont.SCOPE_ONE -> ScopeOneFamily
    }
}

@Composable
fun LiteralMemoTheme(
    userPrefs: UserPrefs = UserPrefs(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val accentColor = parseColor(userPrefs.accentColorHex).let {
        if (it == Color.Unspecified) Color(0xFF6650A4) else it
    }

    val defaultBackgroundColor = if (darkTheme) Color.Black else Color.White
    val defaultTextColor = if (darkTheme) Color.White else Color.Black

    val backgroundColor = parseColor(userPrefs.backgroundColorHex).let {
        if (it == Color.Unspecified) defaultBackgroundColor else it
    }
    val textColor = parseColor(userPrefs.textColorHex).let {
        if (it == Color.Unspecified) defaultTextColor else it
    }

    val baseColorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    val colorScheme = baseColorScheme.copy(
        primary = accentColor,
        secondary = accentColor,
        tertiary = accentColor
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val appThemeState = AppThemeState(
        fontFamily = userPrefs.font.toFontFamily(),
        fontSize = userPrefs.fontSize,
        backgroundColor = backgroundColor,
        textColor = textColor,
        accentColor = accentColor
    )

    val typography = Typography(
        bodyLarge = TextStyle(
            fontFamily = appThemeState.fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = appThemeState.fontSize.sp,
            lineHeight = (appThemeState.fontSize * 1.5f).sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = appThemeState.fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (appThemeState.fontSize - 2).sp,
            lineHeight = (appThemeState.fontSize * 1.4f).sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = appThemeState.fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (appThemeState.fontSize - 4).sp,
            lineHeight = (appThemeState.fontSize * 1.3f).sp,
            letterSpacing = 0.4.sp
        ),
        titleLarge = TextStyle(
            fontFamily = appThemeState.fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = appThemeState.fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        labelSmall = TextStyle(
            fontFamily = appThemeState.fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )

    CompositionLocalProvider(LocalAppTheme provides appThemeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
