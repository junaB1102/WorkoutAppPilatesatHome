package com.example.workoutapppilatesathome

/**
 * Datenmodell für eine einzelne Pilates-Übung.
 *
 * Wir speichern hier bewusst nur *Resource-IDs* (also z.B. R.string.xxx und R.drawable.xxx)
 * und nicht direkt die fertigen Texte oder Bilder. Das hat zwei Gründe:
 *  1. Resource-IDs sind einfache Int-Werte -> die Klasse bleibt leichtgewichtig und
 *     lässt sich problemlos in SharedPreferences als Zahl speichern/wiederherstellen.
 *  2. Der eigentliche Text/das Bild wird erst dort geladen, wo er gebraucht wird
 *     (in einem @Composable mit stringResource()/painterResource()). So funktioniert
 *     z.B. auch Mehrsprachigkeit automatisch, falls später eine strings.xml (en) ergänzt wird.
 *
 * @property id             Eindeutige, fortlaufende Nummer der Übung (1..10). Aktuell nur
 *                           zur besseren Lesbarkeit/Debugging genutzt, nicht als Schlüssel.
 * @property nameResId       Resource-ID des Übungsnamens (z.B. R.string.ex_plank_on_knees_title)
 * @property descriptionResId Resource-ID der Übungsbeschreibung (Anleitungstext)
 * @property imageResId      Resource-ID des Piktogramms/Bilds der Übung (aus res/drawable)
 * @property calories        Kalorien, die pauschal pro absolvierter Übung "verbrannt" werden.
 *                           Das ist eine grobe Schätzung/Spielerei für die Fortschrittsanzeige,
 *                           keine medizinisch korrekte Berechnung.
 */
data class Exercise(
    val id: Int,
    val nameResId: Int,
    val descriptionResId: Int,
    val imageResId: Int,
    val calories: Int = 10,
)

/**
 * Die feste Liste aller Übungen, aus denen sich ein Workout zusammensetzt.
 *
 * Diese Liste ist die "Quelle der Wahrheit" für die Übungsreihenfolge bei fester Reihenfolge.
 * Bei zufälliger Reihenfolge werden im [WorkoutViewModel] lediglich die *Indizes* dieser Liste
 * durchmischt (die Liste selbst bleibt unverändert) - so bleibt z.B. das Kalorienzählen anhand
 * von exercise.calories unabhängig von der gewählten Reihenfolge korrekt.
 *
 * Reihenfolge hier = Standard-/"feste" Reihenfolge, wie sie ohne aktivierten
 * "Zufällige Reihenfolge"-Schalter abgespielt wird.
 */
val pilatesExercises = listOf(
    Exercise(1, R.string.ex_side_lying_leg_lifts_title, R.string.ex_side_lying_leg_lifts_desc, R.drawable.leg_lifts),
    Exercise(2, R.string.ex_plank_on_knees_title, R.string.ex_plank_on_knees_desc, R.drawable.plank_knees),
    Exercise(3, R.string.ex_leg_circles_title, R.string.ex_leg_circles_desc, R.drawable.leg_circles),
    Exercise(4, R.string.ex_clamshell_title, R.string.ex_clamshell_desc, R.drawable.clamshell),
    Exercise(5, R.string.ex_shoulder_bridge_title, R.string.ex_shoulder_bridge_desc, R.drawable.bridge),
    Exercise(6, R.string.ex_cat_cow_stretch_title, R.string.ex_cat_cow_stretch_desc, R.drawable.cat_cow),
    Exercise(7, R.string.ex_hundred_modified_title, R.string.ex_hundred_modified_desc, R.drawable.hundred),
    Exercise(8, R.string.ex_swimming_modified_title, R.string.ex_swimming_modified_desc, R.drawable.swimming),
    Exercise(9, R.string.ex_mermaid_stretch_title, R.string.ex_mermaid_stretch_desc, R.drawable.mermaid),
    Exercise(10, R.string.ex_full_plank_title, R.string.ex_full_plank_desc, R.drawable.full_plank)
)
