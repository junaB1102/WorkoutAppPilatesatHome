package com.example.workoutapppilatesathome

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * =========================================================================================
 *  WorkoutViewModel
 * =========================================================================================
 *
 * Das ViewModel ist das "Gehirn" der App. Es hält den kompletten Zustand des Workouts
 * (welche Übung gerade dran ist, ob gerade die Pause läuft, Kalorien, Historie, ...) und
 * überlebt Konfigurationsänderungen (z.B. Bildschirmrotation), weil es unabhängig von den
 * Composables lebt.
 *
 * Warum AndroidViewModel statt ViewModel?
 *  -> Wir brauchen Zugriff auf den Application-Context (für SharedPreferences und den
 *     MediaPlayer zum Abspielen der Sounds). AndroidViewModel bekommt die Application
 *     automatisch injiziert, ohne dass wir uns um einen Context-Leak kümmern müssen.
 *
 * Warum LiveData statt reinem Compose-State (mutableStateOf)?
 *  -> Das ist eine Vorgabe aus der Aufgabenstellung ("Nutze ViewModel & LiveData").
 *     LiveData ist lifecycle-aware: Beobachter (hier unsere Composables via observeAsState())
 *     werden nur benachrichtigt, wenn die UI tatsächlich sichtbar/aktiv ist. Für die Compose-UI
 *     nutzen wir dazu die Erweiterungsfunktion `observeAsState()` aus dem Artefakt
 *     androidx.compose.runtime:runtime-livedata.
 *
 * Persistenz:
 *  Wir verwenden SharedPreferences (Datei "workout_prefs"), um folgende Dinge über
 *  App-Neustarts hinweg zu behalten:
 *   - an welcher Übung ein unterbrochenes Workout stand (current_exercise_index)
 *   - in welcher Reihenfolge die Übungen für das laufende/letzte Workout angeordnet waren
 *     (current_workout_order) - wichtig, damit "Workout fortsetzen" bei zufälliger
 *     Reihenfolge nicht plötzlich eine andere Reihenfolge liefert
 *   - wie viele Kalorien heute schon "verbrannt" wurden (calories_today)
 *   - die Historie der letzten 7 Tage (history) als komma-separierter String
 *   - ob der Nutzer "Zufällige Reihenfolge" aktiviert hat (random_order_enabled)
 */
class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    // Zugriff auf die SharedPreferences-Datei, in der wir den kompletten Fortschritt ablegen.
    // MODE_PRIVATE = nur unsere eigene App kann diese Datei lesen/schreiben.
    private val prefs = application.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)

    // -----------------------------------------------------------------------------------
    // Reihenfolge der Übungen
    // -----------------------------------------------------------------------------------
    // activeOrder enthält die Indizes von pilatesExercises in der Reihenfolge, in der sie
    // im *aktuellen* (bzw. zuletzt gestarteten) Workout abgespielt werden.
    // Bei "fester" Reihenfolge ist das einfach [0, 1, 2, ..., n-1].
    // Bei "zufälliger" Reihenfolge ist das eine durchmischte Version davon, z.B. [3, 0, 7, ...].
    // Diese Liste wird NICHT als LiveData exponiert, weil die UI sie nicht direkt braucht -
    // sie fragt stattdessen über exerciseAt(position) die jeweils passende Übung ab.
    private var activeOrder: List<Int> = pilatesExercises.indices.toList()

    // -----------------------------------------------------------------------------------
    // Zustand, der von der UI beobachtet wird (alles als LiveData)
    // -----------------------------------------------------------------------------------

    // Index (Position in activeOrder) der aktuell laufenden Übung.
    // -1 bedeutet: gerade läuft kein Workout / kein unterbrochenes Workout vorhanden.
    private val _currentExerciseIndex = MutableLiveData(-1)
    val currentExerciseIndex: LiveData<Int> = _currentExerciseIndex

    // Wie viele Kalorien heute schon durch abgeschlossene Übungen "verbrannt" wurden.
    private val _caloriesToday = MutableLiveData(0)
    val caloriesToday: LiveData<Int> = _caloriesToday

    // Anzahl abgeschlossener Übungen pro Tag für die letzten 7 Tage (Index 0 = vor 6 Tagen,
    // Index 6 = heute). Wird für das Balkendiagramm auf Seite 1 verwendet.
    private val _last7DaysHistory = MutableLiveData(List(7) { 0 })
    val last7DaysHistory: LiveData<List<Int>> = _last7DaysHistory

    // true, solange gerade der 15-Sekunden-Countdown (Pause) vor einer Übung läuft.
    // Steuert das Countdown-Overlay auf dem Workout-Screen.
    private val _isCountdownActive = MutableLiveData(false)
    val isCountdownActive: LiveData<Boolean> = _isCountdownActive

    // Verbleibende Sekunden des aktuellen Countdowns (zählt von BREAK_DURATION_SECONDS auf 0).
    private val _countdownTime = MutableLiveData(BREAK_DURATION_SECONDS)
    val countdownTime: LiveData<Int> = _countdownTime

    // Wird kurz auf true gesetzt, sobald die letzte Übung abgeschlossen wurde, damit die UI
    // (WorkoutScreen) per LaunchedEffect zur Finish-Seite navigieren kann. Danach über
    // resetFinishedState() sofort wieder zurückgesetzt, damit es kein "Re-Trigger" gibt.
    private val _isWorkoutFinished = MutableLiveData(false)
    val isWorkoutFinished: LiveData<Boolean> = _isWorkoutFinished

    // Merkt sich, ob der Nutzer den Schalter "Zufällige Reihenfolge" auf Seite 1 aktiviert hat.
    private val _randomOrderEnabled = MutableLiveData(false)
    val randomOrderEnabled: LiveData<Boolean> = _randomOrderEnabled

    // Gesamtzahl der Übungen in einem Workout-Durchlauf (aktuell immer 10, da wir nur die
    // Reihenfolge mischen, aber keine Übungen weglassen). Öffentlich für die UI, um z.B.
    // "3 von 10" anzuzeigen, ohne dass die UI die interne pilatesExercises-Liste kennen muss.
    val totalExerciseCount: Int get() = pilatesExercises.size

    // Der System-Timer, der den Countdown runterzählt. Wird bei jedem neuen Countdown
    // zuerst gecancelt, damit nicht zwei Timer parallel laufen (z.B. wenn man sehr schnell
    // hintereinander "Weiter" drücken würde).
    private var timer: CountDownTimer? = null

    // Zwei getrennte MediaPlayer-Instanzen, da Übungs- und Pausensound theoretisch
    // kurz nacheinander (nicht überlappend, aber unmittelbar) abgespielt werden.
    // Getrennte Player verhindern, dass sich ein laufender Sound selbst "abwürgt".
    private var exerciseSoundPlayer: MediaPlayer? = null
    private var breakSoundPlayer: MediaPlayer? = null

    companion object {
        // Dauer der Pause/des Countdowns vor jeder Übung in Sekunden (Vorgabe: 15 Sekunden).
        private const val BREAK_DURATION_SECONDS = 15

        // Schlüssel-Konstanten für SharedPreferences, damit man sich nicht an mehreren
        // Stellen im Code vertippen kann.
        private const val KEY_LAST_UPDATE = "last_update_date"
        private const val KEY_HISTORY = "history"
        private const val KEY_CALORIES_TODAY = "calories_today"
        private const val KEY_CURRENT_INDEX = "current_exercise_index"
        private const val KEY_ORDER = "current_workout_order"
        private const val KEY_RANDOM_ORDER = "random_order_enabled"
    }

    // Wird einmalig beim Erzeugen des ViewModels ausgeführt (z.B. beim ersten Öffnen der App
    // bzw. nach einem Prozessneustart) - lädt den gespeicherten Zustand aus SharedPreferences.
    init {
        loadData()
    }

    /**
     * Liefert die Übung, die an der gegebenen Position der *aktuellen Reihenfolge* steht.
     *
     * Wichtig: `position` ist NICHT der Index in pilatesExercises, sondern der Index in
     * activeOrder! Beispiel: Bei zufälliger Reihenfolge [3, 0, 7, ...] liefert
     * exerciseAt(0) die Übung mit Index 3 aus pilatesExercises.
     *
     * Der `getOrElse`-Fallback greift nur in einem theoretischen Edge-Case (z.B. korrupte
     * SharedPreferences-Daten) und verhindert einen Crash durch IndexOutOfBounds.
     */
    fun exerciseAt(position: Int): Exercise {
        val exerciseIndex = activeOrder.getOrElse(position) { position.coerceIn(0, pilatesExercises.lastIndex) }
        return pilatesExercises[exerciseIndex]
    }

    /** Wird von der UI aufgerufen, nachdem sie auf isWorkoutFinished reagiert hat (Navigation
     *  zur Finish-Seite ausgelöst hat), damit der "Finished"-Zustand nicht erneut auslöst. */
    fun resetFinishedState() {
        _isWorkoutFinished.value = false
    }

    /** Wird vom Schalter "Zufällige Reihenfolge" auf Seite 1 aufgerufen. Die Einstellung wird
     *  sofort persistiert, damit sie auch nach einem Neustart der App erhalten bleibt. */
    fun setRandomOrderEnabled(enabled: Boolean) {
        _randomOrderEnabled.value = enabled
        prefs.edit { putBoolean(KEY_RANDOM_ORDER, enabled) }
    }

    /**
     * Lädt beim Start des ViewModels den kompletten gespeicherten Zustand aus SharedPreferences.
     *
     * Enthält zusätzlich die Logik für den "Tageswechsel":
     *  - Wenn seit dem letzten gespeicherten Datum (last_update_date) ein neuer Tag begonnen hat,
     *    werden die heutigen Kalorien auf 0 zurückgesetzt und die 7-Tage-Historie um die Anzahl
     *    der vergangenen Tage nach links verschoben (die ältesten Tage fallen raus, neue Tage
     *    mit 0 Übungen kommen rechts rein).
     *  - So funktioniert das Balkendiagramm auch dann korrekt, wenn die App z.B. mehrere Tage
     *    nicht geöffnet wurde.
     */
    private fun loadData() {
        val today = getTodayDate()
        val lastUpdate = prefs.getString(KEY_LAST_UPDATE, "")

        // Historie aus dem gespeicherten komma-separierten String parsen, z.B. "0,2,5,0,3,1,4"
        val historyStr = prefs.getString(KEY_HISTORY, "0,0,0,0,0,0,0") ?: "0,0,0,0,0,0,0"
        val history = historyStr.split(",").asSequence()
            .map { it.toIntOrNull() ?: 0 }
            .toList()
            .toMutableList()

        // Sicherheitsnetz: Sicherstellen, dass die Liste immer exakt 7 Einträge hat,
        // egal was (fehlerhaft) in den SharedPreferences steht.
        while (history.size < 7) history.add(0)
        while (history.size > 7) history.removeAt(0)

        if (lastUpdate != today && !lastUpdate.isNullOrEmpty()) {
            // Es ist (mindestens) ein neuer Tag seit dem letzten Öffnen der App.
            _caloriesToday.value = 0

            try {
                val lastDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastUpdate)
                val todayDate = Date()
                if (lastDate != null) {
                    // Anzahl der vergangenen Tage berechnen, um die Historie entsprechend
                    // "weiterzuschieben" (max. 7, weil mehr Verschiebungen ohnehin alles auf 0 setzen würden).
                    val diff = ((todayDate.time - lastDate.time) / (1000 * 60 * 60 * 24)).toInt()
                    if (diff > 0) {
                        repeat(diff.coerceAtMost(7)) {
                            history.removeAt(0)
                            history.add(0)
                        }
                    }
                }
            } catch (e: Exception) {
                // Falls das Datum aus irgendeinem Grund nicht geparst werden kann, machen wir
                // einfach mit der bisherigen Historie weiter, statt die App abstürzen zu lassen.
                e.printStackTrace()
            }

            prefs.edit {
                putString(KEY_LAST_UPDATE, today)
                putInt(KEY_CALORIES_TODAY, 0)
                putString(KEY_HISTORY, history.joinToString(","))
            }
        } else if (lastUpdate.isNullOrEmpty()) {
            // Allererster App-Start überhaupt: es gibt noch kein gespeichertes Datum.
            prefs.edit { putString(KEY_LAST_UPDATE, today) }
            _caloriesToday.value = 0
        } else {
            // Gleicher Tag wie beim letzten Öffnen -> einfach die gespeicherten Kalorien laden.
            _caloriesToday.value = prefs.getInt(KEY_CALORIES_TODAY, 0)
        }

        _randomOrderEnabled.value = prefs.getBoolean(KEY_RANDOM_ORDER, false)

        // Reihenfolge und Fortschritt eines eventuell unterbrochenen Workouts wiederherstellen.
        activeOrder = loadOrder()
        val savedIndex = prefs.getInt(KEY_CURRENT_INDEX, -1)
        _currentExerciseIndex.value = if (savedIndex in 0 until pilatesExercises.size) savedIndex else -1
        _last7DaysHistory.value = history
    }

    /**
     * Startet ein Workout - entweder ganz neu von vorne oder an der Stelle, an der zuletzt
     * unterbrochen wurde.
     *
     * @param continueInterrupted true = das zuvor unterbrochene Workout fortsetzen (Seite 1,
     *                             Dialog "Willst du dieses Workout fortsetzen?" -> "Ja"),
     *                             false = neues Workout bei Übung 1 beginnen ("Nein" oder gar
     *                             kein unterbrochenes Workout vorhanden).
     */
    fun startWorkout(continueInterrupted: Boolean) {
        if (!continueInterrupted) {
            // Neue Reihenfolge festlegen: entweder 0..9 der Reihe nach, oder zufällig gemischt,
            // je nachdem was der Nutzer im Schalter auf Seite 1 eingestellt hat.
            activeOrder = if (_randomOrderEnabled.value == true) {
                pilatesExercises.indices.shuffled()
            } else {
                pilatesExercises.indices.toList()
            }
            saveOrder(activeOrder)
            _currentExerciseIndex.value = 0
            saveCurrentProgress(0)
        } else {
            // Fortsetzen: die zuletzt gespeicherte Reihenfolge und Position wiederverwenden,
            // damit man nicht plötzlich eine andere/neue Zufallsreihenfolge bekommt.
            activeOrder = loadOrder()
            val savedIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)
            _currentExerciseIndex.value = savedIndex.coerceIn(0, activeOrder.size - 1)
        }
        // Jedes Workout (egal ob neu oder fortgesetzt) startet mit der 15-Sekunden-Pause/Countdown,
        // damit sich der Nutzer in Position bringen kann, bevor die erste Übung losgeht.
        startBreak()
    }

    /**
     * Startet die Pause (den 15-Sekunden-Countdown) vor der nächsten Übung.
     *
     * Sound-Logik (siehe Aufgabenstellung: "ein anderer Sound signalisiert den Beginn und
     * das Ende der Pausen"):
     *  - playBreakSound() wird HIER aufgerufen -> signalisiert den BEGINN der Pause.
     *  - playBreakSound() wird nochmal in onFinish() aufgerufen -> signalisiert das ENDE der Pause.
     *  - Direkt danach spielt playExerciseSound(), was gleichzeitig den BEGINN der nächsten
     *    Übung ankündigt (siehe Aufgabenstellung: "Ein Signalton kündigt den Start und das
     *    Ende jeder Übung an").
     */
    private fun startBreak() {
        _isCountdownActive.value = true
        _countdownTime.value = BREAK_DURATION_SECONDS
        playBreakSound() // Signalton: Beginn der Pause

        // Einen eventuell noch laufenden alten Timer zuerst stoppen (Sicherheitsnetz).
        timer?.cancel()
        timer = object : CountDownTimer(BREAK_DURATION_SECONDS * 1000L, 1000L) {
            // Wird einmal pro Sekunde aufgerufen, solange der Countdown läuft.
            override fun onTick(millisUntilFinished: Long) {
                _countdownTime.value = (millisUntilFinished / 1000).toInt()
            }

            // Wird einmalig aufgerufen, wenn der Countdown bei 0 ankommt.
            override fun onFinish() {
                _countdownTime.value = 0
                _isCountdownActive.value = false
                playBreakSound()    // Signalton: Ende der Pause
                playExerciseSound() // Signalton: Beginn der (nächsten) Übung
            }
        }.start()
    }

    /**
     * Wird aufgerufen, wenn der Nutzer auf Seite 2 den "Weiter"-Button drückt, also die
     * aktuell angezeigte Übung als erledigt markiert.
     *
     * Ablauf:
     *  1. Signalton fürs Ende der aktuellen Übung abspielen.
     *  2. Kalorien der abgeschlossenen Übung gutschreiben und speichern.
     *  3. Entweder zur nächsten Übung springen (inkl. neuer Pause) oder, falls das die letzte
     *     Übung war, das gesamte Workout als abgeschlossen markieren (finishWorkout()).
     */
    fun nextExercise() {
        val currentIndex = _currentExerciseIndex.value ?: return
        if (currentIndex < 0 || currentIndex >= activeOrder.size) return

        playExerciseSound() // Signalton: Ende der (gerade abgeschlossenen) Übung

        val finishedExercise = exerciseAt(currentIndex)
        val newCalories = (_caloriesToday.value ?: 0) + finishedExercise.calories
        _caloriesToday.value = newCalories
        saveCalories(newCalories)

        val nextIndex = currentIndex + 1
        if (nextIndex < activeOrder.size) {
            _currentExerciseIndex.value = nextIndex
            saveCurrentProgress(nextIndex)
            startBreak()
        } else {
            finishWorkout()
        }
    }

    /**
     * Bricht das laufende Workout ab (X-Button auf Seite 2).
     *
     * Der Fortschritt (aktueller Index, Kalorien, Reihenfolge) wurde bereits laufend durch
     * startWorkout()/nextExercise() in SharedPreferences gesichert - hier müssen wir also nur
     * noch den Live-Zustand zurücksetzen (Timer stoppen, Countdown-Overlay ausblenden) und
     * currentExerciseIndex auf -1 setzen, damit Seite 1 wieder den "Workout starten"-Button
     * statt der Fortschrittsanzeige zeigt. Beim erneuten Start würde man sonst fälschlich
     * direkt wieder mitten im (abgebrochenen) Workout landen.
     */
    fun cancelWorkout() {
        timer?.cancel()
        _isCountdownActive.value = false
        _currentExerciseIndex.value = -1
        saveCurrentProgress(-1)
    }

    /** Wird intern aufgerufen, sobald die letzte Übung eines Workouts abgeschlossen wurde. */
    private fun finishWorkout() {
        updateHistory()
        _currentExerciseIndex.value = -1
        saveCurrentProgress(-1)
        _isWorkoutFinished.value = true // Trigger für die Navigation zur Finish-Seite (Seite 3)
    }

    /** Erhöht den heutigen Eintrag (letzter Wert) der 7-Tage-Historie um die Anzahl der
     *  gerade absolvierten Übungen und speichert das Ergebnis in SharedPreferences. */
    private fun updateHistory() {
        val history = (_last7DaysHistory.value ?: List(7) { 0 }).toMutableList()
        if (history.size == 7) {
            history[6] = history[6] + activeOrder.size
            _last7DaysHistory.value = history
            prefs.edit { putString(KEY_HISTORY, history.joinToString(",")) }
        }
    }

    // --- Kleine private Hilfsfunktionen rund um das Lesen/Schreiben von SharedPreferences ---

    private fun saveCurrentProgress(index: Int) {
        prefs.edit { putInt(KEY_CURRENT_INDEX, index) }
    }

    private fun saveCalories(calories: Int) {
        prefs.edit { putInt(KEY_CALORIES_TODAY, calories) }
    }

    /** Speichert die aktuelle Übungsreihenfolge als komma-separierten String, z.B. "3,0,7,1,...". */
    private fun saveOrder(order: List<Int>) {
        prefs.edit { putString(KEY_ORDER, order.joinToString(",")) }
    }

    /**
     * Lädt die zuletzt gespeicherte Übungsreihenfolge. Falls noch nie eine gespeichert wurde,
     * oder die gespeicherten Daten nicht mehr zur aktuellen Anzahl an Übungen passen (z.B. weil
     * pilatesExercises in einem App-Update erweitert wurde), fällt die Funktion sicher auf die
     * normale, aufsteigende Reihenfolge zurück statt einen Crash zu riskieren.
     */
    private fun loadOrder(): List<Int> {
        val stored = prefs.getString(KEY_ORDER, null) ?: return pilatesExercises.indices.toList()
        val parsed = stored.split(",").mapNotNull { it.toIntOrNull() }
            .filter { it in pilatesExercises.indices }
        return if (parsed.size == pilatesExercises.size) parsed else pilatesExercises.indices.toList()
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // --- Sound-Wiedergabe ---

    private fun playExerciseSound() {
        playSound(R.raw.exercise_signal, isExerciseSound = true)
    }

    private fun playBreakSound() {
        playSound(R.raw.break_signal, isExerciseSound = false)
    }

    /**
     * Spielt eine Sound-Datei aus res/raw ab.
     *
     * Wir verwenden zwei getrennte MediaPlayer-Felder (exerciseSoundPlayer / breakSoundPlayer),
     * damit ein noch abklingender Übungs-Sound nicht versehentlich einen kurz danach gestarteten
     * Pausen-Sound (oder umgekehrt) abschneidet. Ein zuvor noch laufender Player *der gleichen
     * Kategorie* wird vor dem Neustart sauber freigegeben (release()), um Speicherlecks zu
     * vermeiden. setOnCompletionListener sorgt dafür, dass die MediaPlayer-Ressource auch dann
     * freigegeben wird, wenn der Sound einfach ganz normal zu Ende spielt.
     *
     * try/catch, weil MediaPlayer.create() theoretisch null zurückgeben bzw. eine Exception
     * werfen kann (z.B. bei einer beschädigten Audiodatei) - das soll niemals das ganze Workout
     * zum Absturz bringen, ein fehlender Sound ist im Zweifel kein kritischer Fehler.
     */
    private fun playSound(resId: Int, isExerciseSound: Boolean) {
        try {
            if (isExerciseSound) {
                exerciseSoundPlayer?.release()
                exerciseSoundPlayer = MediaPlayer.create(getApplication(), resId)
                exerciseSoundPlayer?.apply {
                    setVolume(1.0f, 1.0f)
                    setOnCompletionListener { it.release(); exerciseSoundPlayer = null }
                    start()
                }
            } else {
                breakSoundPlayer?.release()
                breakSoundPlayer = MediaPlayer.create(getApplication(), resId)
                breakSoundPlayer?.apply {
                    setVolume(1.0f, 1.0f)
                    setOnCompletionListener { it.release(); breakSoundPlayer = null }
                    start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Wird von Android aufgerufen, kurz bevor das ViewModel endgültig zerstört wird
     * (z.B. wenn die zugehörige Activity endgültig geschlossen wird). Hier geben wir alle
     * Ressourcen frei, die sonst ein Speicherleck verursachen könnten: den Countdown-Timer
     * und die beiden MediaPlayer-Instanzen.
     */
    override fun onCleared() {
        timer?.cancel()
        exerciseSoundPlayer?.release()
        breakSoundPlayer?.release()
    }
}
