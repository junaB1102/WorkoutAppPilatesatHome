package com.example.workoutapppilatesathome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workoutapppilatesathome.ui.theme.AccentPink
import com.example.workoutapppilatesathome.ui.theme.PrimaryGreen
import com.example.workoutapppilatesathome.ui.theme.SurfaceCard
import com.example.workoutapppilatesathome.ui.theme.TextMuted

/**
 * Seite 1: Übersicht + Einstiegspunkt ins Workout.
 *
 * Zeigt:
 *  - ein Balkendiagramm der letzten 7 Tage (wie viele Übungen pro Tag absolviert wurden)
 *  - die heute schon verbrannten Kalorien
 *  - einen Schalter für "Zufällige Reihenfolge" (Zusatz-Feature, siehe README)
 *  - falls ein Workout unterbrochen wurde: eine Fortschrittsanzeige ("x von y") direkt über
 *    dem "Workout starten"-Button
 *  - den zentralen grünen "Workout starten"-Button
 *
 * Alle Werte kommen als LiveData aus dem WorkoutViewModel und werden hier über
 * `observeAsState()` in normalen Compose-State umgewandelt, sodass sich die UI automatisch
 * neu zeichnet, sobald sich z.B. caloriesToday nach einem abgeschlossenen Workout ändert.
 *
 * @param onStartWorkout Callback nach oben (an MainActivity/NavHost), der true übergibt, wenn
 *                        ein unterbrochenes Workout fortgesetzt werden soll, sonst false.
 */
@Composable
fun MainScreen(
    viewModel: WorkoutViewModel,
    onStartWorkout: (Boolean) -> Unit
) {
    // observeAsState(initialValue) sorgt dafür, dass wir sofort einen sinnvollen Startwert haben
    // (z.B. 0 Kalorien), auch bevor das ViewModel seine Daten aus SharedPreferences geladen hat.
    val caloriesToday by viewModel.caloriesToday.observeAsState(0)
    val history by viewModel.last7DaysHistory.observeAsState(List(7) { 0 })
    val currentExerciseIndex by viewModel.currentExerciseIndex.observeAsState(-1)
    val randomOrderEnabled by viewModel.randomOrderEnabled.observeAsState(false)

    // Steuert, ob gerade das "Willst du dieses Workout fortsetzen?"-Popup angezeigt wird.
    // Das ist reiner UI-Zustand (nur relevant, solange dieser Screen sichtbar ist) und muss
    // deshalb NICHT im ViewModel/LiveData liegen - normaler Compose-State reicht hier völlig.
    var showContinueDialog by remember { mutableStateOf(false) }

    // Ein "unterbrochenes Workout" liegt vor, wenn currentExerciseIndex auf eine gültige
    // Übungs-Position zeigt (nicht -1, was "kein aktives/unterbrochenes Workout" bedeutet).
    val hasPausedWorkout = currentExerciseIndex != -1 && currentExerciseIndex < viewModel.totalExerciseCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.app_subtitle),
            fontSize = 14.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- Karte 1: Balkendiagramm + Kalorien ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.overview_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                BarChart(history = history)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    // %1$d in strings.xml wird hier durch caloriesToday ersetzt.
                    text = stringResource(R.string.calories_burned, caloriesToday),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Karte 2: Schalter für zufällige/feste Übungsreihenfolge ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.random_order_label),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = randomOrderEnabled,
                    // Die eigentliche Logik (inkl. Speichern in SharedPreferences) steckt im ViewModel.
                    onCheckedChange = { viewModel.setRandomOrderEnabled(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen)
                )
            }
        }

        // Spacer mit weight(1f) schiebt alles Folgende (Fortschrittsanzeige + Start-Button)
        // an das untere Ende des Bildschirms, unabhängig von der Bildschirmgröße.
        Spacer(modifier = Modifier.weight(1f))

        // Fortschrittsanzeige NUR sichtbar, wenn tatsächlich ein Workout unterbrochen wurde.
        if (hasPausedWorkout) {
            WorkoutProgressBar(
                current = currentExerciseIndex + 1, // +1, weil Index bei 0 beginnt, Anzeige bei 1
                total = viewModel.totalExerciseCount
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Zentraler "Workout starten"-Button (grün, weißer Text) ---
        Button(
            onClick = {
                if (hasPausedWorkout) {
                    // Es gibt ein unterbrochenes Workout -> erst nachfragen, ob fortgesetzt
                    // oder neu gestartet werden soll.
                    showContinueDialog = true
                } else {
                    // Kein unterbrochenes Workout -> direkt ganz normal neu starten.
                    onStartWorkout(false)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = stringResource(R.string.start_workout),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // --- Popup: "Willst du dieses Workout fortsetzen?" mit "Ja"/"Nein" ---
    if (showContinueDialog) {
        AlertDialog(
            onDismissRequest = { showContinueDialog = false },
            title = { Text(stringResource(R.string.continue_workout_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showContinueDialog = false
                    onStartWorkout(true) // "Ja" -> an der unterbrochenen Stelle fortsetzen
                }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showContinueDialog = false
                    onStartWorkout(false) // "Nein" -> ganz normal von vorne beginnen
                }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}

/**
 * Einfaches Balkendiagramm für die 7-Tage-Historie.
 *
 * Jeder Balken wird proportional zum jeweils höchsten Wert in der Liste skaliert
 * (maxVal = höchster Tageswert). Damit ein Tag mit 0 Übungen nicht komplett unsichtbar ist,
 * wird die minimale Höhe über coerceAtLeast(0.06f) auf mindestens 6% der verfügbaren Höhe
 * begrenzt - so bleibt erkennbar, dass an diesem Tag überhaupt ein Balken "existiert".
 */
@Composable
fun BarChart(history: List<Int>) {
    val maxVal = (history.maxOrNull() ?: 1).coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(Color(0x145B8C6E), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom // Balken wachsen von unten nach oben
    ) {
        history.forEach { value ->
            val heightFactor = value.toFloat() / maxVal
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .fillMaxHeight(heightFactor.coerceAtLeast(0.06f))
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(PrimaryGreen)
            )
        }
    }
}

/**
 * Fortschrittsanzeige für ein unterbrochenes Workout (wird auf Seite 1 über dem
 * "Workout starten"-Button eingeblendet).
 *
 * @param current Wie viele Übungen bereits absolviert wurden (1-basiert für die Anzeige, z.B. "3").
 * @param total   Gesamtzahl der Übungen im Workout (z.B. "10").
 */
@Composable
fun WorkoutProgressBar(current: Int, total: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            // Nutzt die Pluralform/Platzhalter aus strings.xml: "Übung %1$d von %2$d"
            text = stringResource(R.string.exercise_progress, current, total),
            fontSize = 13.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LinearProgressIndicator(
            // progress als Lambda (statt fixem Float) ist die aktuelle Compose-Material3-API
            // und vermeidet unnötige Rekompositionen, wenn sich current/total nicht ändern.
            progress = { current.toFloat() / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = AccentPink, // pinker Akzent laut Design-Vorgabe
            trackColor = Color.LightGray,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
