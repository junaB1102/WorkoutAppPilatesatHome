package com.example.workoutapppilatesathome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.workoutapppilatesathome.ui.theme.WorkoutAppPilatesAtHomeTheme

/**
 * Einziger Einstiegspunkt (Single-Activity-Architektur) der App.
 *
 * Statt für jede "Seite" aus der Aufgabenstellung eine eigene Activity zu erstellen, nutzen
 * wir Jetpack Compose Navigation: Alle drei Seiten (Hauptseite / Workout-Ablauf / Abschluss)
 * sind eigenständige @Composable-Funktionen, zwischen denen ein NavController wechselt.
 * Boilerplate-Code (z.B. Intents, mehrere Activities in der AndroidManifest.xml, ...).
 *
 * Das WorkoutViewModel wird genau EINMAL hier oben erzeugt (via viewModel()) und an alle
 * drei Seiten weitergereicht. Dadurch teilen sich alle Seiten denselben Zustand - z.B. weiß
 * die Hauptseite nach einem abgeschlossenen Workout sofort über caloriesToday/last7DaysHistory
 * Bescheid, ohne dass Daten manuell zwischen Screens hin- und hergereicht werden müssten.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lässt den Inhalt der App bis unter die System-Statusleiste/Navigationsleiste laufen
        // (moderner, "randloser" Look). Das eigentliche Padding dafür übernimmt weiter unten
        // der Scaffold über "innerPadding".
        enableEdgeToEdge()

        setContent {
            // Eigenes Farb-/Typografie-Theme (siehe ui/theme/Theme.kt) für die gesamte App.
            WorkoutAppPilatesAtHomeTheme {
                // Ein einziges, gemeinsames ViewModel für alle drei Seiten (siehe Klassen-Kommentar oben).
                val viewModel: WorkoutViewModel = viewModel()
                // Steuert, welche der drei Seiten (Composables) gerade sichtbar ist.
                val navController = rememberNavController()

                // Scaffold liefert uns automatisch das korrekte Padding, damit unsere Inhalte
                // nicht z.B. hinter der Statusleiste verschwinden.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "main", // App startet immer auf Seite 1
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // --- Seite 1: Übersicht / "Workout starten" ---
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onStartWorkout = { continueInterrupted ->
                                    // continueInterrupted kommt aus dem "Fortsetzen?"-Dialog
                                    // auf Seite 1 (true = Ja, false = Nein/kein Dialog nötig).
                                    viewModel.startWorkout(continueInterrupted)
                                    navController.navigate("workout")
                                }
                            )
                        }
                        // --- Seite 2: Laufendes Workout (Übung + Pause/Countdown) ---
                        composable("workout") {
                            WorkoutScreen(
                                viewModel = viewModel,
                                onCancel = {
                                    // X-Button: Workout abbrechen und zurück zu Seite 1.
                                    // popBackStack statt navigate("main"), damit wir nicht
                                    // unnötig neue Einträge im Back-Stack aufbauen.
                                    viewModel.cancelWorkout()
                                    navController.popBackStack("main", inclusive = false)
                                },
                                onFinish = {
                                    // Wird ausgelöst, sobald das ViewModel isWorkoutFinished=true meldet.
                                    navController.navigate("finish")
                                }
                            )
                        }
                        // --- Seite 3: Abschluss-Feier mit Konfetti ---
                        composable("finish") {
                            FinishScreen(
                                onBackToMain = {
                                    // Tippen irgendwo auf den Bildschirm -> zurück zu Seite 1.
                                    navController.popBackStack("main", inclusive = false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
