package com.example.appfinanceiro.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import br.com.sobraai.designsystem.resources.*

/** Same Android-reference font binaries and Material text metrics on both platforms. */
@Composable
internal fun sharedFinanceTypography(): Typography {
    val regular = Font(Res.font.roboto_regular, FontWeight.Normal)
    val medium = Font(Res.font.roboto_medium, FontWeight.Medium)
    val semibold = Font(Res.font.roboto_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.roboto_bold, FontWeight.Bold)
    val extrabold = Font(Res.font.roboto_extrabold, FontWeight.ExtraBold)
    val family = remember(regular, medium, semibold, bold, extrabold) { FontFamily(regular, medium, semibold, bold, extrabold) }
    return remember(family) {
        val baseline = Typography()
        baseline.copy(
            displayLarge = baseline.displayLarge.copy(fontFamily = family),
            displayMedium = baseline.displayMedium.copy(fontFamily = family),
            displaySmall = baseline.displaySmall.copy(fontFamily = family),
            headlineLarge = baseline.headlineLarge.copy(fontFamily = family),
            headlineMedium = baseline.headlineMedium.copy(fontFamily = family),
            headlineSmall = baseline.headlineSmall.copy(fontFamily = family),
            titleLarge = baseline.titleLarge.copy(fontFamily = family),
            titleMedium = baseline.titleMedium.copy(fontFamily = family),
            titleSmall = baseline.titleSmall.copy(fontFamily = family),
            bodyLarge = baseline.bodyLarge.copy(fontFamily = family),
            bodyMedium = baseline.bodyMedium.copy(fontFamily = family),
            bodySmall = baseline.bodySmall.copy(fontFamily = family),
            labelLarge = baseline.labelLarge.copy(fontFamily = family),
            labelMedium = baseline.labelMedium.copy(fontFamily = family),
            labelSmall = baseline.labelSmall.copy(fontFamily = family)
        )
    }
}
