package com.example.workoutapppilatesathome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutapppilatesathome.ui.theme.PrimaryGreen
import com.example.workoutapppilatesathome.ui.theme.PrimaryGreenDark

/**
 * Seite 3: Abschluss-/Feier-Bildschirm.
 *
 * Wird angezeigt, sobald alle Übungen eines Workouts abgeschlossen wurden. Zeigt:
 *  - einen grünen Farbverlauf-Hintergrund (statt einer einzigen flachen Farbe, für etwas
 *    mehr visuelle Tiefe, aber weiterhin einfarbig im Sinne der Aufgabenstellung)
 *  - eine sich freuende Figur (Bild "celebration")
 *  - simuliertes Konfetti (bunte, zufällig positionierte Punkte)
 *  - einen Hinweistext "Tippe zum Fortfahren"
 *
 * Der gesamte Screen ist klickbar (clickable auf der äußersten Box) - ein Tap irgendwo auf dem
 * Bildschirm löst onBackToMain() aus und bringt den Nutzer zurück zu Seite 1.
 */
@Composable
fun FinishScreen(onBackToMain: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryGreenDark, PrimaryGreen)))
            .clickable { onBackToMain() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.celebration),
                contentDescription = null,
                modifier = Modifier.size(250.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.workout_finished),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tap_to_continue),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }

        // Simuliert Konfetti mit 20 einfachen, zufällig positionierten und eingefärbten Punkten.
        // "Simuliert" deshalb, weil es sich um rein statische Positionen handelt (keine echte
        // Fall-/Physik-Animation) - für den Zweck dieser App (kurzer Feier-Moment) aber völlig
        // ausreichend und ohne zusätzliche Animations-Bibliothek umsetzbar.
        repeat(20) {
            Box(
                modifier = Modifier
                    .offset(
                        x = ((-150)..150).random().dp,
                        y = ((-250)..250).random().dp
                    )
                    .size(8.dp)
                    .background(
                        color = listOf(Color.Yellow, Color.Red, Color.Blue, Color.Magenta).random(),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}
