# Changelog — هِمّة | Himma

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.2.1-beta] — 2026-11-14

### 🎭 Choreographed M3 Motion, Frost Theme & Performance Overhaul

This release introduces a two-stage choreographed navigation and content reveal motion across all pages, the brand-new Frost («ثلجي») theme, a zero-lag performance overhaul for the exercise library, and streamlined appearance settings.

#### Added
- **Two-Stage Choreographed Page Motion:**
  - Fluid Material 3 Fade Through and Shared-Axis page transitions across all app destinations.
  - Expressive, GPU-accelerated secondary content reveal animation (`PageContentEntrance`) that smoothly translates (`+28dp`), scales (`0.97 -> 1.0`), and fades in page content once navigation settles.
  - Animated crossfade for the top app bar title when switching between primary tabs.
  - RTL-native directional slide transitions for drill-down/editor screens and modal upward presentation for active workout sessions.
- **Frost Theme («ثلجي»):**
  - Brand-new premium theme featuring icy cyan (`#38BDF8`), radiant teal (`#2DD4BF`), and subtle indigo accents on deep dark oceanic frosted glass surfaces (`#060B12`).
  - Added seamless backwards compatibility for existing theme preferences.

#### Performance & Optimization
- **Exercise Library Zero-Lag Architecture:**
  - Pre-warmed exercise catalog database seeding in the background on app startup (`HimmaApplication.onCreate`).
  - Replaced redundant 200KB JSON asset parsing with instant SQLite record count verification.
  - Converted exercise library state flow to persistent lazy sharing (`SharingStarted.Lazily`), eliminating loading spinner flicker and list rebuild frame drops during tab navigation.
  - Replaced heavy expanding search bar with a lightweight, ergonomic Material 3 rounded search field.
  - Eliminated nested `LazyRow` subcomposition overhead by switching muscle and equipment filters to single-pass scrollable rows.
  - Memoized Coil image requests and pruned unnecessary crossfade passes on local bundled WebP assets.

#### Refined
- **Streamlined Appearance Settings:**
  - Simplified the section title to clean **"Appearance"** («المظهر»).
  - Removed the Android 12+ dynamic color toggle for a consistent, hand-crafted AMOLED aesthetic.
  - Pruned unused experimental theme codes and resources.

---

## [0.2.0-beta] — 2026-11-14

### 🚀 In-App Updates & UI Refinements

This release introduces direct in-app update checks and installations via GitHub Releases, an RTL-native analytics tab underline indicator, and streamlined Today and Settings screens.

#### Added
- **In-App Updater:**
  - Automated and manual update checking directly against GitHub Releases (`/releases/latest`).
  - Background APK download with SHA-256 integrity verification.
  - Seamless package installer intent integration via `FileProvider`.
  - Notification badge and dialog in Settings when a new update is available.
- **Localized Launcher Name:**
  - App launcher title on home screen now displays in Arabic as «همة».

#### Fixed & Refined
- **Analytics Tab Bar RTL Alignment:**
  - Replaced default Material 3 indicator offset with an RTL-aware animated underline.
  - Thin, elegant 3dp emerald underline accurately centered under the active tab label with smooth spring transitions.
- **Streamlined Today Screen:**
  - Removed redundant setup summary cards, prioritizing immediate workout starting and core metrics.
- **Simplified Backup Settings:**
  - Retained essential manual JSON backup, CSV workout history export, and JSON restore actions while decluttering automated snapshot controls.

---

## [0.1.1-beta] — 2026-11-14

### 🚀 UI Refresh & Floating Navigation

This release introduces an expressive floating navigation bar, refreshed Today screen layout, and refined Material Design 3 card shapes.

#### Added
- **Material 3 Expressive Floating Navigation Bar:**
  - Modern capsule pill floating container with AMOLED dark surface styling, subtle elevation, and rounded corners (`RoundedCornerShape(28.dp)`).
  - Smooth active capsule indicator behind the selected tab with instant, responsive transitions.
- **Enhanced Today Screen Cards:**
  - Hero Quick Workout card with one-tap access to start empty workout sessions.
  - Expressive quick metrics row displaying current body weight, default rest interval, and routines count.

#### Changed
- **Modernized Card Shapes:**
  - Refined theme corner radiuses in `HimmaShapes` for a more expressive, cohesive Material Design 3 aesthetic.

#### Community
- Merged community contribution from @biZenX (PR #1).

---

## [0.1.0-beta] — 2026-11-12

### 🎉 Initial Public Beta Release

Himma (هِمّة) is a 100% offline, privacy-first, free native Android gym workout logger and progressive overload companion built with modern Jetpack Compose and Material Design 3.

#### Added
- **Material Design 3 AMOLED Architecture:**
  - Dark-first emerald AMOLED palette (`#000000` / `#22C55E`), Light, System, and Android 12+ Dynamic Color support.
  - Native RTL (Arabic) and LTR (English) layout direction with bundled `IBM Plex Sans Arabic` and `Inter` typography.
  - Onboarding flow with unit preferences (`kg` / `lbs`), rest timer defaults, and name/body weight initialization.
- **Local Exercise Catalog (302 Exercises):**
  - Fully bundled local offline database pre-populated from `yuhonas/free-exercise-db` (Public Domain / CC0).
  - 302 high-quality 2-frame demonstration WebP assets stored locally in assets with Coil rendering.
  - Accurate Arabic gym terminology and step-by-step Arabic instructions.
  - Diacritic-insensitive instant offline Arabic and English search and filtering by 6 muscle groups and 5 equipment types.
  - Custom exercise creator with equipment badges.
- **Unlimited Routine Builder:**
  - Create, edit, duplicate, and organize unlimited workout routines with zero paywalls.
  - Configure target sets, target reps ranges, and exercise-specific rest timers.
  - Soft deletion with instant Snack-bar Undo restoration.
- **Active Workout Session & Background Rest Timer:**
  - Fast workout logging with large gym-friendly touch targets ($\ge 48\text{ dp}$) and `+1`, `+2.5`, `+5` quick-increment assist chips.
  - Set categorization badges (`Normal`, `Warmup`, `Drop Set`, `Failure`).
  - Intelligent automatic completion fill from previous session's same-index set.
  - Persistent Android Foreground Service (`RestTimerForegroundService`) with live Lock Screen countdown, notification controls (`-15s`, `+15s`, `Skip`), and Doze-resilient sound & vibration alerts.
  - Unfinished workout draft preservation across app restarts and crash recovery.
- **Local Backup, Restore & Data Portability:**
  - Storage Access Framework (SAF) JSON backup export (`.himma.json`) with versioning and integrity validation.
  - Smart Merge (non-duplicating) and Full Replace restore modes with detailed preview dialogs.
  - LibreOffice / Excel / Google Sheets compatible `.csv` workout history export with UTF-8 BOM encoding.
  - Automated local snapshot scheduler via `WorkManager` (daily 24h background backup).
- **Analytics, Personal Records & Progression Engine:**
  - Brzycki 1RM formula calculation: $1RM = \frac{\text{weight}}{1.0278 - 0.0278 \times \text{reps}}$.
  - Automatic Personal Record (PR) detection upon completing sets and finishing workouts with 🏆 badges.
  - Native Compose Canvas line charts for Estimated 1RM and Max Weight progression over time.
  - Weekly muscle volume distribution bar chart across all 6 core muscle groups.
  - Transparent 100% offline progressive overload hints and muscle imbalance warnings.
  - Body weight trend tracker with 7 / 30 / 90 / All days time range filters.
- **Zero Third-Party Telemetry:**
  - No Google Play Billing, no ads, no trackers, no third-party cloud requirements, 100% on-device SQLite Room database.
