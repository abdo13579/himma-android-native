# Changelog — هِمّة | Himma

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] — 2026-11-12

### 🎉 Initial Public Release

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
