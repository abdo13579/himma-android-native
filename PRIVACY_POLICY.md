# Privacy Policy — هِمّة | Himma

**Last Updated:** November 26, 2026

**هِمّة (Himma)** is a free, open-source, and offline-first workout logging and progressive overload companion for Android.

Your privacy is our highest priority. We believe your health and fitness data belongs exclusively to you. This Privacy Policy outlines our principles, how the application handles data, and the permissions required to operate.

---

## 1. Offline-First Privacy & Data Handling

* **No Mandatory Accounts or Registration:** You can use all core workout tracking, routines, and analytics features without creating an account or providing personal identification.
* **Local-First Storage:** All workout sessions, routines, set weights, reps, rest intervals, custom exercises, and personal records are stored **locally on your device** in a private SQLite database, and all core features operate fully offline.
* **No Third-Party Analytics or Advertising:** We do not include any third-party tracking SDKs, advertising networks, or behavioral telemetry.
* **Optional Connected Features & User-Initiated Actions:** Any optional online features (now or in future updates) and any user-initiated external actions remain strictly optional, are triggered only by your explicit choice, and transmit only what is necessary for that action. All offline features continue to function normally without an internet connection.

---

## 2. Device Permissions & Usage

Himma requests only the minimum set of permissions necessary to deliver essential gym workout tracking features:

| Permission | Purpose |
| :--- | :--- |
| `POST_NOTIFICATIONS` | To display the live rest timer notification and alert you when your rest countdown finishes. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` | To maintain an uninterrupted countdown rest timer while your phone screen is locked or while using other apps between sets. |
| `VIBRATE` | To provide tactile haptic feedback and alert vibrations when a rest interval completes. |
| `INTERNET` | Declared only in standalone GitHub APK builds (`github` flavor) to check GitHub Releases for open-source updates when triggered or enabled by the user. Google Play Store builds (`store` flavor) omit `INTERNET` completely. Core workout tracking, routines, and analytics always work completely offline. |
| `REQUEST_INSTALL_PACKAGES` | Declared only in standalone GitHub APK builds (`github` flavor) to allow installing downloaded open-source update APKs from GitHub Releases when explicitly approved by the user. Google Play Store builds omit this permission. |

Himma does **not** request access to your contacts, camera, microphone, GPS location, or device identity.

---

## 3. Data Storage, Backup & Export

* **Local Storage:** All app data is maintained within Android's sandboxed local application storage. No external app can access this data without your explicit permission.
* **Android System Backup & Device Transfer:** Android's built-in system backup and device-to-device transfer (`allowBackup="true"` with configured backup rules for local database, preferences, and auto-snapshots) is supported solely through Android OS when enabled in your device settings. Himma does not operate or access any developer-hosted cloud servers.
* **User-Controlled Export & Backup:** When you use the Backup & Restore feature (JSON) or the CSV Export feature, the exported files are saved strictly to the folder or cloud drive you select via Android's native Storage Access Framework (SAF). You maintain full ownership and control over these backup files.
* **Data Deletion:** Uninstalling the application completely deletes all locally stored data, databases, and preferences from your device.

---

## 4. Children's Privacy

Himma is designed for general audiences and does not knowingly collect, store, or solicit any personal information from children under the age of 13 (or under the applicable age in your jurisdiction). Because no personal data is collected from any user, Himma is fully compliant with the Children's Online Privacy Protection Act (COPPA) and global privacy standards.

---

## 5. Changes to This Privacy Policy

Himma is built around an offline-first design where your local workout data stays under your control. If new optional connected features are introduced in the future, this document will be updated in the repository with a revised date to reflect how those features work.

---

## 6. Open Source Transparency & Contact

Himma is open-source software licensed under the **PolyForm Noncommercial License 1.0.0**. You can inspect the complete source code, review all permissions, and audit data handling directly on GitHub:

* **Source Code:** [https://github.com/abdo13579/himma-android-native](https://github.com/abdo13579/himma-android-native)
* **Developer / Contact:** For questions, feedback, or support regarding this policy, please open an issue on the GitHub repository or contact:  
  `abdulrahman.a.alfeel@gmail.com`
