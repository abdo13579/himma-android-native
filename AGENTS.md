# AGENTS.md — هِمّة | Himma (Native Android)

Guidance for AI coding agents working in this repository.

---

## 1. Critical Workflow Rules

- **Git Policy (STRICT):** Do **NOT** run `git commit` or `git push` before explicit user permission/order. Creating or modifying files is fine; staging/committing/pushing must wait for the user's explicit instruction.
- **Language with User:** Speak with the user in **Arabic** when reporting progress, asking questions, or explaining what to test.
- **Verification Before Reporting:** Always run unit tests (`./gradlew testDebugUnitTest`) and build/install checks after edits before reporting completion.
- **Uploaded APK Policy:** Do **NOT** upload debug APKs to GitHub Releases. Always generate and upload the **production release APK** (`./gradlew assembleRelease`).

---

## 2. Project Overview & Open-Source Principles

- **App Name:** هِمّة | Himma (`com.himma.fitness`, namespace `com.himma.app`)
- **Current Version:** `v1.3.0` (versionCode 9)
- **License:** **PolyForm Noncommercial License 1.0.0** — free for personal, educational, and non-commercial use.
- **Model:** **100% Free & Open Source (FOSS)** — no paywalls, no Pro tiers, no Google Play Billing, no ads, no analytics/telemetry, and no cloud servers or mandatory sign-in.
- **Scope:** Native Android mobile application (Jetpack Compose, Kotlin).
- **Offline-First & Privacy:** All user data lives 100% locally on-device using Room (SQLite) and Jetpack DataStore Preferences. Zero analytics or cloud tracking. Local backup/restore via Android's Storage Access Framework (SAF).

---

## 3. Tech Stack & Dependencies

- **Language & UI:** Kotlin `2.0.21`, Jetpack Compose (BOM `2024.12.01`), Material Design 3 (`material3`, `material-icons-extended`)
- **Build System:** Gradle `8.11.1` (Kotlin DSL + `./gradlew`), Android Gradle Plugin (AGP) `8.7.3`, KSP `2.0.21-1.0.27`
- **SDK Targets:** `minSdk = 26` (Android 8.0 Oreo), `compileSdk = 36`, `targetSdk = 36` (Android 16), Java/Kotlin JVM target `17`
- **Architecture & DI:** Clean Architecture + MVVM + Hilt (`2.52`) + Navigation Compose (`2.8.5`)
- **Persistence:** Room (`2.6.1`, `DATABASE_VERSION = 9`, schema exported to `app/schemas/`) + DataStore Preferences (`1.1.1`)
- **Background Tasks & Services:** AndroidX WorkManager (`2.10.0`) + Hilt Work (`1.2.0`), `ForegroundService` (`RestTimerForegroundService`)
- **Media & Serialization:** Coil Compose (`2.7.0`) for local 2-frame WebP demo assets, Kotlinx Serialization JSON (`1.7.3`)
- **Localization & Splash:** `AppCompatActivity` (`androidx.appcompat:1.7.0`), `AppCompatDelegate.setApplicationLocales()`, `androidx.core:core-splashscreen:1.0.1`

---

## 4. Architecture & Package Layout

Base package: `app/src/main/java/com/himma/app/`

### `core/` — Cross-cutting concerns & infrastructure
- `core/theme/`:
  - `Color.kt`: AMOLED Dark-First emerald palette (`#22C55E` / `#16A34A` on `#000000` / `#111816`), Dark, Light, System, and Frost (`#38BDF8` / `#2DD4BF` on `#060B12`).
  - `Shape.kt`: Custom Material 3 shapes (capsules, pills, rounded container cards).
  - `Type.kt`: Bundled typography (`IBM Plex Sans Arabic` and `Inter` in `res/font/`).
  - `Theme.kt`: `HimmaTheme` enforcing RTL/LTR layout direction and dynamic/color palette switching.
- `core/database/`:
  - `HimmaDatabase.kt`: Room database (`DATABASE_VERSION = 9`) with an explicit, non-destructive `MIGRATIONS` array (`MIGRATION_1_2` through `MIGRATION_8_9`).
  - `entity/`: `UserProfileEntity`, `ExerciseEntity`, `RoutineEntity`, `RoutineExerciseEntity`, `WorkoutEntity`, `WorkoutSetEntity`, `PersonalRecordEntity`, `BodyWeightEntity`.
  - `dao/`: `UserProfileDao`, `ExerciseDao`, `RoutineDao`, `WorkoutDao`, `AnalyticsDao`.
  - Repositories: `UserProfileRepository`, `ExerciseRepository`, `RoutineRepository`, `WorkoutRepository`, `AnalyticsRepository`, `ExerciseSeedLoader`.
- `core/datastore/`:
  - `UserPreferences.kt`, `UserPreferencesRepository.kt`: Stores app language, weight units, default rest interval, theme mode, animation toggle (`animationsEnabled`), auto-backup settings, and updater check toggle.
- `core/backup/`:
  - `BackupRepository.kt`, `BackupSerializer.kt`: SAF-driven JSON export/import with **Smart Merge** and **Replace All** strategies.
  - `CsvWorkoutExporter.kt`: UTF-8 with BOM CSV export for Excel / Google Sheets compatibility.
  - `AutoBackupScheduler.kt`, `AutoBackupWorker.kt`: Periodic 24h snapshot backups via WorkManager.
- `core/updater/`:
  - In-app GitHub release updater (`AppUpdateRepository`, `GitHubAppUpdateRepository`, `SemVerHelper`, `AppInstallerHelper`).
- `core/locale/`:
  - `LocaleHelper.kt`: Idempotent in-app language switching between Arabic (RTL) and English (LTR).
- `core/di/`:
  - Hilt modules: `DatabaseModule`, `DataStoreModule`, `BackupModule`, `ServiceModule`, `UpdaterModule`.

### `service/` — Background Services
- `RestTimerForegroundService.kt`: Persistent foreground rest timer with Live Lock Screen countdown, notification quick actions (`-15s`, `+15s`, `Skip`), and Doze-mode resilient audio/vibration cues.
- `RestTimerController.kt`: Flow-based controller interfacing between active UI and service.

### `domain/` — Pure Kotlin Business Models & Logic
- `domain/model/`:
  - `Exercise.kt`: Exercise model and `ExerciseType` (`REPS`, `TIME`).
  - `WeightUnit.kt`: `WeightUnit` (`KG`, `LBS`) conversion math and `WeightUnitMode` (`WEIGHT`, `STACK_PLATES`).
  - `Workout.kt`: Workout session, `WorkoutSet`, `SetType` (`NORMAL`, `WARMUP`, `DROP_SET`, `FAILURE`), Brzycki 1RM formula.
  - `Routine.kt`: Workout routine and target configuration models.
  - `Analytics.kt`: Progression metrics, volume distribution, 1RM progression data points.
  - `UserProfile.kt`, `ThemeMode.kt`, `AppLanguage.kt`, `BackupModels.kt`.
- `domain/text/`:
  - `ArabicTextNormalizer.kt`: Diacritic (tashkeel) stripping and normalization for Alef, Yaa, Taa-Marbuta.

### `features/` — UI & ViewModels (MVVM)
- `shell/`: `HimmaAppShell`, `HimmaFloatingNavigationBar`, `HimmaDestination`, `TodayViewModel`, `PageContentEntrance` (two-stage choreographed motion).
- `exercises/`: `ExerciseLibraryScreen`, `ExerciseLibraryViewModel`, `ExerciseDetailScreen`, `ExerciseDetailViewModel`, `CustomExerciseScreen`, `CustomExerciseViewModel`, `ExerciseUiLabels`.
- `routines/`: `RoutinesScreen`, `RoutinesViewModel`, `RoutineEditorScreen`, `RoutineEditorViewModel`.
- `workout/`: `ActiveWorkoutScreen`, `ActiveWorkoutViewModel`, `WorkoutLauncherViewModel` (real-time progress bar, stack plates toggle, set re-indexing, previous set pre-fill).
- `analytics/`: `AnalyticsScreen`, `AnalyticsViewModel`, `AnalyticsCharts` (Compose Canvas curves, muscle volume bars, body weight trends, PR ledger).
- `settings/`: `SettingsScreen`, `SettingsViewModel`.
- `onboarding/`: `OnboardingScreen`, `OnboardingViewModel`.

---

## 5. Domain Invariants & Development Guidelines

1. **Database Migrations (STRICT):**
   - Never use destructive schema updates or drop tables.
   - Any schema modification requires:
     1. Updating the corresponding entity.
     2. Bumping `HimmaDatabase.DATABASE_VERSION`.
     3. Writing a strict Room `Migration(N, N+1)` in `HimmaDatabase.kt`.
     4. Registering the migration in the `MIGRATIONS` array.
     5. Exporting updated schemas via KSP to `app/schemas/`.

2. **Weight & Time Storage Standards:**
   - **Weight Canonicalization:** All weight values must be stored canonically in **kg** in the SQLite database (`weight_kg`) and converted to user preference (`kg` / `lbs`) only at UI and export/import boundaries using `WeightUnit.toDisplay()` / `toCanonicalKg()`.
   - **Stack Plates Mode:** Exercises set to `WeightUnitMode.STACK_PLATES` record plate counts directly instead of kg/lbs.
   - **Duration Canonicalization:** All durations must be stored canonically in integer **seconds** (`duration_seconds`, `target_duration_seconds`).

3. **Arabic Text Search:**
   - Always normalize Arabic search queries and target names with `ArabicTextNormalizer.normalize()` to handle diacritics and letter variations (أ, إ, آ -> ا; ى -> ي; ة -> ه).

4. **Offline-First & Zero-Telemetry:**
   - Do NOT add third-party analytics, crash trackers, or cloud sync services.
   - The only network interaction allowed is the opt-in GitHub release check in `GitHubAppUpdateRepository`.

5. **Gym Ergonomics & Accessibility:**
   - Interactive elements must maintain a touch target of $\ge 48\text{ dp}$.
   - Quick increment assistance chips for weight (`+1`, `+2.5`, `+5 kg/lbs`), stack plates (`+1`, `+2`, `+5`), and time (`+15s`, `+30s`, `+1m`, `+2m`, `+5m`).
   - Local DB reads/writes must complete asynchronously via Coroutines/Flow in $< 10\text{ ms}$.

6. **Bilingual String Parity:**
   - Maintain 100% parity between Arabic strings (`res/values-ar/strings.xml`) and English fallback strings (`res/values/strings.xml`).

7. **Motion & Low-End Device Support:**
   - UI animations are controlled by `UserPreferences.animationsEnabled` (disabled by default for maximum responsiveness on budget devices).
   - Any new animated transitions should observe this preference to ensure zero overhead when disabled.

---

## 6. Build, Test & Device Commands

Always export the environment variables before running Gradle commands from the terminal:

```zsh
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/jdk-17.0.21+9/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Common commands:
- **Run Unit Tests:**
  ```zsh
  ./gradlew testDebugUnitTest
  # or ./gradlew testGithubDebugUnitTest / testStoreDebugUnitTest
  ```
- **Build GitHub Production Release APK:**
  ```zsh
  ./gradlew assembleGithubRelease
  # Output: app/build/outputs/apk/github/release/app-github-release.apk
  ```
- **Build Google Play Production Release Bundle (AAB):**
  ```zsh
  ./gradlew bundleStoreRelease
  # Output: app/build/outputs/bundle/storeRelease/app-store-release.aab
  ```
- **Install Debug APK on Device:**
  ```zsh
  ./gradlew installGithubDebug
  # or ./gradlew installStoreDebug
  ```
- **Launch App via ADB:**
  ```zsh
  adb shell am start -n com.himma.fitness/com.himma.app.MainActivity
  ```
- **List Connected Devices:**
  ```zsh
  adb devices -l
  ```
