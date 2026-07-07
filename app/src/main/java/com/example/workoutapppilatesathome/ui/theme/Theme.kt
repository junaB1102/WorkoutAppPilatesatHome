package com.example.workoutapppilatesathome.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Farbschema für den (seltenen) Fall, dass ein Gerät im System-Dunkelmodus läuft.
// Die Vorgabe aus der Aufgabenstellung ("weiß, grün mit pinken Akzenten") bezieht sich auf
// den Hellmodus (LightColorScheme unten) - hier sorgen wir nur dafür, dass die App auch im
// Dunkelmodus nicht "falsch" aussieht (dunkler statt weißer Hintergrund, aber gleiche Akzentfarben).
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    secondary = AccentPink,
    tertiary = AccentPink,
    background = Color(0xFF1B1B18),
    surface = Color(0xFF232320),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

// Das eigentliche, in der Aufgabenstellung geforderte Farbschema: Weiß/heller Hintergrund,
// Grün als Primärfarbe (z.B. der "Workout starten"-Button) und Pink/Terracotta als Akzent
// (z.B. Fortschrittsbalken).
private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    secondary = AccentPink,
    tertiary = AccentPink,
    background = BackgroundWhite,
    surface = BackgroundWhite,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextBlack,
    onSurface = TextBlack
)

/**
 * Zentrales App-Theme. Wird in MainActivity ganz oben um den gesamten Compose-Inhalt gelegt
 * und stellt darüber (via MaterialTheme.colorScheme/typography) allen darunterliegenden
 * Composables automatisch die passenden Farben/Schriftarten zur Verfügung.
 *
 * @param darkTheme Ob der System-Dunkelmodus aktiv ist. Per Default automatisch erkannt
 *                  (isSystemInDarkTheme()), kann aber z.B. für Compose-Previews überschrieben werden.
 */
@Composable
fun WorkoutAppPilatesAtHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    val context = view.context

    // isInEditMode ist true, wenn diese Funktion gerade nur in der Android-Studio-Vorschau
    // (nicht auf einem echten Gerät) gerendert wird - dort gibt es keine echte Activity/Window,
    // also überspringen wir den Statusleisten-Code, um Abstürze in der Preview zu vermeiden.
    if (!view.isInEditMode) {
        // SideEffect führt den Block nach jeder erfolgreichen Komposition aus - der richtige Ort,
        // um Nicht-Compose-("imperative") Android-APIs wie window.statusBarColor anzusprechen.
        SideEffect {
            val window = (context as? Activity)?.window
            window?.let {
                // Statusleiste im Hellmodus in der dunkleren Grün-Variante einfärben (etwas mehr
                // Kontrast/Tiefe als der reguläre Primärton), im Dunkelmodus passend zum dunklen
                // Hintergrund.
                it.statusBarColor = if (darkTheme) colorScheme.background.toArgb() else PrimaryGreenDark.toArgb()
                // Der Statusleisten-Hintergrund ist in BEIDEN Themes dunkel -> wir wollen in
                // beiden Fällen helle (weiße) Status-Icons (Uhrzeit, Akku, ...).
                // isAppearanceLightStatusBars = false bedeutet "helle Icons für dunklen Hintergrund".
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
