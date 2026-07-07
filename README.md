# Workout App: Pilates at Home

Eine Android-App (Kotlin, Jetpack Compose), die durch ein 10-teiliges Pilates-Workout
führt: Übung anzeigen → Pause mit Countdown → nächste Übung → ... → Abschluss-Feier.
Fortschritt, Kalorien und ein 7-Tage-Verlauf werden lokal auf dem Gerät gespeichert.

---

## Inhaltsverzeichnis

1. [Funktionsumfang](#funktionsumfang)
2. [Screens im Detail](#screens-im-detail)
3. [Architektur](#architektur)
4. [Projektstruktur](#projektstruktur)
5. [Verwendete Assets](#verwendete-assets)
6. [Design](#design)
7. [Setup / Ausführen](#setup--ausführen)
8. [Abgleich mit der Aufgabenstellung](#abgleich-mit-der-aufgabenstellung)
9. [Bekannte Grenzen / mögliche Erweiterungen](#bekannte-grenzen--mögliche-erweiterungen)

---

## Funktionsumfang

- **10 Pilates-Übungen** mit Bild (Piktogramm) und Textbeschreibung
- **Feste oder zufällige Übungsreihenfolge** (per Schalter auf der Startseite einstellbar)
- **15-Sekunden-Pause/Countdown** vor jeder Übung, als Overlay über dem Bildschirm
- **Zwei unterschiedliche Signaltöne**:
  - ein Ton für Beginn *und* Ende jeder **Übung**
  - ein anderer, ruhigerer Ton für Beginn *und* Ende jeder **Pause**
- **Fortschrittsanzeige**
  - während eines laufenden Workouts: "Übung X von Y" + Fortschrittsbalken
  - auf der Startseite: Balkendiagramm der letzten 7 Tage + heutige Kalorien
  - bei einem unterbrochenen Workout: eigene Fortschrittsanzeige über dem Start-Button
- **Workout unterbrechen & fortsetzen**: Beim erneuten Öffnen fragt die App nach, ob an
  der unterbrochenen Stelle weitergemacht oder neu gestartet werden soll
- **Persistenz** über SharedPreferences: Fortschritt, Kalorien, 7-Tage-Historie und die
  gewählte Übungsreihenfolge überleben einen App-Neustart
- **Abschluss-Screen** mit Feier-Grafik und simuliertem Konfetti

## Screens im Detail

### Seite 1 – Übersicht (`MainScreen.kt`)
- Balkendiagramm: Anzahl absolvierter Übungen pro Tag, letzte 7 Tage
- Anzeige der heute verbrannten Kalorien
- Schalter "Zufällige Reihenfolge" (an/aus)
- Falls ein Workout unterbrochen wurde: Fortschrittsbalken ("x von y") direkt über dem Button
- Zentraler grüner Button **"Workout starten"**
  - kein unterbrochenes Workout → startet sofort ein neues Workout
  - unterbrochenes Workout vorhanden → Popup **"Willst du dieses Workout fortsetzen?"**
    mit "Ja" (an unterbrochener Stelle weiter, inkl. Countdown) und "Nein" (von vorne)

### Seite 2 – Workout-Ablauf (`WorkoutScreen.kt`)
- Fortschrittsanzeige oben ("Übung X von Y" + Balken)
- Bild + Name + Beschreibung der aktuellen Übung
- Button **"Weiter"**: markiert die Übung als erledigt, spielt den Übungs-Endton ab,
  vergibt Kalorien und geht zur nächsten Übung (inkl. neuer Pause) bzw. zum Abschluss-Screen
- **X-Button** oben rechts: bricht das Workout ab, Fortschritt bleibt gespeichert, zurück zu Seite 1
- **Pausen-Overlay**: schwarzes Overlay mit 15-Sekunden-Countdown vor jeder Übung,
  zeigt zusätzlich schon den Namen der kommenden Übung an

### Seite 3 – Abschluss (`FinishScreen.kt`)
- Farbverlauf-Hintergrund, freudige Grafik, simuliertes Konfetti
- Tippen irgendwo auf den Bildschirm → zurück zu Seite 1

## Architektur

```
MainActivity
 └─ NavHost (Jetpack Compose Navigation)
     ├─ "main"    → MainScreen
     ├─ "workout" → WorkoutScreen
     └─ "finish"  → FinishScreen
              │
              ▼
     WorkoutViewModel (ein gemeinsames ViewModel für alle drei Screens)
              │
              ▼
     SharedPreferences ("workout_prefs")
```

- **Ein einziges `WorkoutViewModel`** (siehe `WorkoutViewModel.kt`) wird in `MainActivity`
  erzeugt und an alle drei Screens weitergereicht. Dadurch teilen sich alle Screens denselben
  Zustand, ohne Daten manuell zwischen Screens hin- und herzureichen.
- **State-Management über `LiveData`**: Der komplette Zustand (aktuelle Übung, Countdown,
  Kalorien, Historie, …) liegt als `MutableLiveData`/`LiveData` im ViewModel und wird in den
  Composables über `observeAsState()` beobachtet (Vorgabe: *"Nutze ViewModel & LiveData"*).
- **Persistenz über `SharedPreferences`**: Datei `workout_prefs`, gespeichert werden u. a.
  der aktuelle Übungsindex, die verwendete Übungsreihenfolge, die heutigen Kalorien, die
  7-Tage-Historie und die Einstellung "Zufällige Reihenfolge".
- **Timer**: `android.os.CountDownTimer` zählt die 15-Sekunden-Pause runter.
- **Sounds**: `android.media.MediaPlayer` spielt die beiden `.mp3`/`.wav`-Dateien aus
  `res/raw` ab (zwei getrennte Player-Instanzen für Übungs- und Pausenton, damit sich
  beide Sounds nicht gegenseitig abschneiden).

Der Code ist durchgehend mit ausführlichen Kommentaren (KDoc + Inline) versehen, die erklären,
**was** eine Funktion tut und – wichtiger – **warum** sie so und nicht anders umgesetzt wurde.

## Projektstruktur

```
app/src/main/java/com/example/workoutapppilatesathome/
├── MainActivity.kt        Einstiegspunkt, Navigation zwischen den 3 Screens
├── WorkoutViewModel.kt    State-Management, Timer, Sounds, Persistenz (Herzstück der App)
├── Exercise.kt            Datenmodell + feste Liste der 10 Übungen
├── MainScreen.kt          Seite 1: Übersicht, Balkendiagramm, Start-Button
├── WorkoutScreen.kt       Seite 2: laufende Übung + Pausen-Overlay
├── FinishScreen.kt        Seite 3: Abschluss-Feier
└── ui/theme/
    ├── Color.kt           Farbpalette (grün/weiß/pink-Akzent)
    ├── Theme.kt           Material3-Theme (Light/Dark), Statusleisten-Farbe
    └── Type.kt            Typografie

app/src/main/res/
├── drawable/              Piktogramme der Übungen + Feier-Grafik + App-Icon-Vektoren
├── raw/                   Sound-Dateien (Übungs- und Pausensignal)
├── values/strings.xml     Alle Übungsnamen, -beschreibungen und UI-Texte
└── mipmap-*/              App-Icon in allen Auflösungen (eckig & rund)
```

## Verwendete Assets

**Piktogramme** (`res/drawable`, PNG): `leg_lifts`, `plank_knees`, `leg_circles`, `clamshell`,
`bridge`, `cat_cow`, `hundred`, `swimming`, `mermaid`, `full_plank`, dazu `celebration.png`
für den Abschluss-Screen.

**Sounds** (`res/raw`):
- `exercise_signal.mp3` – hochgeladener Sound, markiert **Beginn und Ende jeder Übung**
- `break_signal.wav` – programmatisch erzeugter, ruhigerer Ton, markiert **Beginn und Ende
  jeder Pause** (bewusst anders als der Übungston, damit beide akustisch unterscheidbar sind)

**App-Icon** (`res/drawable/ic_launcher_*.xml`, `res/mipmap-*/`): eigens gestaltetes,
minimalistisches Blüten-Icon (weiß auf Salbeigrün, Terracotta-Kern) statt des
Android-Studio-Standardsymbols; sowohl als modernes Adaptive Icon (Vektor) als auch als
klassisches PNG/WebP-Icon für ältere Android-Versionen vorhanden.

## Design

Vorgabe: *"weiß, grün mit pinken Akzenten"*. Umgesetzt als ruhige Wellness-Palette
(`ui/theme/Color.kt`):

| Zweck                     | Farbe               |
|---------------------------|----------------------|
| Hintergrund                | warmes Off-White (`#FBF9F5`) |
| Primärfarbe (Buttons etc.) | gedämpftes Salbeigrün (`#5B8C6E`) |
| Akzent (Fortschrittsbalken)| Terracotta/Koralle (`#E2897E`) |

## Setup / Ausführen

1. Projekt in **Android Studio** öffnen (Ordner `WorkoutAppPilatesatHome`).
2. Gradle-Sync abwarten (Internetverbindung nötig, u. a. für `androidx.lifecycle:lifecycle-livedata-ktx`
   und `androidx.compose.runtime:runtime-livedata`).
3. App auf einem Emulator oder echten Gerät starten (min. Android 7.0 / API 24).

## Abgleich mit der Aufgabenstellung

| Anforderung | Umsetzung |
|---|---|
| ViewModel & LiveData für Timer/Ablauf | `WorkoutViewModel` mit `MutableLiveData`/`LiveData`, `CountDownTimer` |
| SharedPreferences für Fortschritt | Datei `workout_prefs`, siehe `WorkoutViewModel.kt` |
| Übung mit Text + Bild | `Exercise`-Datenklasse, angezeigt in `WorkoutScreen.kt` |
| Reihenfolge fest oder zufällig | Schalter auf Seite 1, Logik in `startWorkout()` |
| Pause mit Countdown | 15 s Overlay in `WorkoutScreen.kt`, gesteuert über `startBreak()` |
| Soundeffekte Start/Ende Übung & Pause | zwei getrennte Sounds, `playExerciseSound()`/`playBreakSound()` |
| Fortschrittsanzeige | Balkendiagramm (7 Tage), Kalorien, "x von y" während & nach Unterbrechung |
| Design weiß/grün/pink | `ui/theme/Color.kt` |
| Seite 1 (Übersicht + Start-Button) | `MainScreen.kt` |
| Seite 2 (Übung + Pause + Abbrechen) | `WorkoutScreen.kt` |
| Seite 3 (Abschluss + Konfetti) | `FinishScreen.kt` |

## Bekannte Grenzen / mögliche Erweiterungen

- Die Kalorienangabe pro Übung (`Exercise.calories`, standardmäßig 10) ist eine grobe,
  frei gewählte Schätzung ohne medizinischen Anspruch.
- Das Konfetti auf dem Abschluss-Screen ist statisch positioniert (keine Fall-Animation).
- Aktuell nur eine Sprache (Deutsch) in `strings.xml` hinterlegt.
- Mögliche Erweiterung: eigene Pausendauer/Übungsauswahl einstellbar machen, Statistik über
  mehr als 7 Tage, Erinnerungs-Benachrichtigungen.
