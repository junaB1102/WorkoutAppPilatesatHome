package com.example.workoutapppilatesathome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutapppilatesathome.ui.theme.AccentPink
import com.example.workoutapppilatesathome.ui.theme.PrimaryGreen

/**
 * Seite 2: Der eigentliche Workout-Ablauf.
 *
 * Zeigt abwechselnd zwei Zustände (beide auf demselben Screen, gesteuert über [isCountdownActive]):
 *  1. Die aktuelle Übung: Bild + Titel + Beschreibung + "Weiter"-Button.
 *  2. Ein schwarzes Pausen-Overlay mit 15-Sekunden-Countdown vor der jeweils nächsten Übung.
 *
 * Zusätzlich (Fortschrittsanzeige-Feature):
 *  Oben auf dem Screen steht permanent "Übung X von Y" mit einem schmalen Fortschrittsbalken,
 *  damit der Nutzer auch während des laufenden Workouts jederzeit weiß, wie weit er schon ist.
 *
 * Navigation weg von diesem Screen passiert über zwei Wege:
 *  - Der X-Button oben rechts ruft onCancel() auf (Workout abbrechen -> zurück zu Seite 1).
 *  - Sobald das ViewModel isWorkoutFinished=true meldet (letzte Übung abgeschlossen), lösen wir
 *    per LaunchedEffect() automatisch onFinish() aus (-> Navigation zu Seite 3).
 */
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onCancel: () -> Unit,
    onFinish: () -> Unit
) {
    val currentIndex by viewModel.currentExerciseIndex.observeAsState(-1)
    val isCountdownActive by viewModel.isCountdownActive.observeAsState(false)
    val countdownTime by viewModel.countdownTime.observeAsState(15)
    val isFinished by viewModel.isWorkoutFinished.observeAsState(false)
    val total = viewModel.totalExerciseCount

    // LaunchedEffect(isFinished) reagiert auf Änderungen von isFinished (LiveData -> State).
    // Wichtig: resetFinishedState() danach, damit dieser Effekt nicht erneut feuert, falls
    // der Screen z.B. durch eine Konfigurationsänderung neu komponiert wird.
    LaunchedEffect(isFinished) {
        if (isFinished) {
            onFinish()
            viewModel.resetFinishedState()
        }
    }

    // Sicherheitsnetz: Sollte dieser Screen jemals ohne gültigen currentIndex angezeigt werden
    // (z.B. direkt nach cancelWorkout(), kurz bevor die Navigation greift), geben wir einfach
    // nichts aus, statt mit einem ungültigen Listenzugriff abzustürzen.
    if (currentIndex < 0 || currentIndex >= total) return

    // Die tatsächliche Übung anhand der (ggf. zufälligen) Reihenfolge aus dem ViewModel holen.
    val exercise = viewModel.exerciseAt(currentIndex)

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Haupt-Inhalt: aktuelle Übung ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Fortschrittsanzeige: "Übung 3 von 10" + schmaler Balken
            Text(
                text = stringResource(R.string.exercise_progress, currentIndex + 1, total),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryGreen,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccentPink,
                trackColor = Color.LightGray,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Piktogramm der aktuellen Übung (aus res/drawable, referenziert über imageResId)
            Image(
                painter = painterResource(id = exercise.imageResId),
                contentDescription = null, // rein dekorativ, Beschreibung steht als Text daneben
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEDEAE3)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = exercise.nameResId),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = exercise.descriptionResId),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // weight(1f) schiebt den "Weiter"-Button ans untere Ende, egal wie lang der
            // Beschreibungstext der jeweiligen Übung ist.
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.nextExercise() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = stringResource(R.string.next),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- X-Button oben rechts: Workout abbrechen ---
        // Liegt als eigenes Element ÜBER der Column (durch die umschließende Box möglich),
        // damit er unabhängig vom Scroll-/Layout-Fluss immer oben rechts fixiert bleibt.
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_workout_description))
        }

        // --- Pausen-Overlay mit Countdown ---
        // Liegt ebenfalls als eigenes Element in der Box, komplett über dem restlichen Inhalt,
        // und wird nur eingeblendet, solange isCountdownActive true ist.
        if (isCountdownActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.break_label),
                        color = AccentPink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.get_ready),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Die große Countdown-Zahl (15 -> 0), zählt einmal pro Sekunde runter.
                    Text(
                        text = countdownTime.toString(),
                        color = PrimaryGreen,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    // Zeigt schon während der Pause, welche Übung als Nächstes kommt,
                    // damit sich der Nutzer entsprechend in Position bringen kann.
                    Text(
                        text = stringResource(id = exercise.nameResId),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
