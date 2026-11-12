# AGENTS.md — هِمّة | Himma (Native Android)

Guidance for AI coding agents working in this repository.

## 1. Critical Workflow Rules

- **Git Policy (STRICT):** Do **NOT** run `git commit` or `git push` before explicit user permission/order. Creating or modifying files is fine; staging/committing/pushing must wait for the user's explicit instruction.
- **No Unrequested Tooling:** Do **NOT** initialize Spec Kit (`.specify/`) or similar meta-scaffolding unless the user explicitly asks for it.
- **Language with User:** Speak with the user in **Arabic** when reporting progress, asking questions, or explaining what to test.
- **Verification Before Reporting:** Always run unit tests and build/install checks after edits before reporting completion.

---

## 2. Project Overview & Open-Source Principles

- **App Name:** هِمّة | Himma (`com.himma.app`)
- **Model:** **100% Free & Open Source (FOSS)** — no paywalls, no Pro tiers, no Google Play Billing, no ads, no analytics/telemetry, and no cloud servers or mandatory sign-in.
- **Scope:** Native Android mobile app only (no web target).
- **Local-First:** All user data lives 100% locally on-device using Room (SQLite) and Jetpack DataStore Preferences, with local backup/restore planned via Storage Access Framework (SAF).
- **Roadmap:** 7 vertical-slice specs defined in `PLAN.md`:
  1. `Spec 1`: Foundation & Material Design 3 Architecture *(Implemented)*
  2. `Spec 2`: Exercise Library (Local Bundled Catalog)
  3. `Spec 3`: Routine Builder (Unlimited)
  4. `Spec 4`: Active Workout Session & Background Rest Timer (`ForegroundService`)
  5. `Spec 5`: Local Backup, Restore & Data Portability (SAF JSON & CSV)
  6. `Spec 6`: Analytics, Progression Insights & Personal Records (Brzycki 1RM & PRs)
  7. `Spec 7`: Story Card (`1080x1920`), Native Sharing & Open-Source Release Polish

---

## 3. Tech Stack & Versions

- **Language & UI:** Kotlin `2.0.21`, Jetpack Compose (BOM `2024.12.01`), Material Design 3 (`material3`, `material-icons-extended`)
- **Build System:** Gradle `8.11.1` (Kotlin DSL + `./gradlew`), AGP `8.7.3`, KSP `2.0.21-1.0.27`
- **SDK Targets:** `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`, Java/Kotlin target `17`
- **Architecture & DI:** Clean Architecture + MVVM + Hilt (`2.52`) + Navigation Compose (`2.8.5`)
- **Persistence:** Room (`2.6.1`, schema exported to `app/schemas/`) + DataStore Preferences (`1.1.1`)
- **Localization & Splash:** `AppCompatActivity` (`androidx.appcompat:1.7.0`), `AppCompatDelegate.setApplicationLocales()`, `androidx.core:core-splashscreen:1.0.1`

---

## 4. Architecture & Package Layout

Base package: `app/src/main/java/com/himma/app/`

- `core/theme/`: `Color.kt`, `Shape.kt`, `Type.kt`, `Theme.kt`
  - AMOLED Dark-First emerald palette (`#22C55E` / `#16A34A` on `#000000` / `#111816`), Dark, Light, System, and Android 12+ Dynamic Color.
  - Fonts bundled in `app/src/main/res/font/`: `IBM Plex Sans Arabic` (Regular/Medium/SemiBold/Bold) and `Inter` (variable TTF).
- `core/database/`: `HimmaDatabase.kt` (explicit `MIGRATIONS` array; do not use blind destructive upgrades), entities (`entity/`), DAOs (`dao/`), and Room repositories.
- `core/datastore/`: `UserPreferences.kt`, `UserPreferencesRepository.kt`.
- `core/locale/`: `LocaleHelper.kt` (idempotent locale switching).
- `core/di/`: Hilt modules (`DatabaseModule.kt`, `DataStoreModule.kt`).
- `domain/model/`: Pure Kotlin domain models & unit/math helpers (`AppLanguage`, `WeightUnit`, `ThemeMode`, `UserProfile`). Store weights canonically in **kg** in Room and convert at UI boundaries.
- `features/`: Feature-sliced UI + ViewModels (`onboarding/`, `shell/`, `settings/`, and upcoming specs).

---

## 5. UI, Accessibility & Localization Standards

- **Arabic Primary (RTL) + English (LTR):**
  - Maintain string parity between `app/src/main/res/values-ar/strings.xml` and `app/src/main/res/values/strings.xml`.
  - `HimmaTheme` enforces `LocalLayoutDirection` (`Rtl` for Arabic, `Ltr` for English) alongside `AppCompatDelegate.setApplicationLocales()`.
- **Gym Ergonomics:** Minimum touch target size $\ge 48\text{ dp}$ for buttons, segmented controls, chips, and list actions.
- **Performance:** All critical local reads/writes (preferences, set logging, profile updates) must execute asynchronously via Coroutines/Flow with fast local persistence ($< 10\text{ ms}$).

---

## 6. Local Build, Test & Device Commands

Set environment variables when running Gradle from shell on this machine:

```zsh
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/jdk-17.0.21+9/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Common commands:
- **Unit tests:** `./gradlew testDebugUnitTest`
- **Debug APK:** `./gradlew assembleDebug`
- **Install on USB-connected device:** `./gradlew installDebug`
- **Launch app via ADB:** `adb shell am start -n com.himma.app/.MainActivity`
- **Check connected devices:** `adb devices -l`

Note: `local.properties` holds the local `sdk.dir` path and is gitignored; do not commit it.
