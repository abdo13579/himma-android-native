# Privacy Policy — هِمّة | Himma

**Last Updated:** November 21, 2026

**هِمّة (Himma)** is a 100% free, open-source, and offline-first workout logging and progressive overload companion for Android. 

Your privacy is our highest priority. We believe your health and fitness data belongs exclusively to you. This Privacy Policy outlines our principles, how the application handles data, and the permissions required to operate.

---

## 1. Zero Data Collection & Tracking

* **No Accounts or Registration:** You are never required to create an account, log in, or provide any personal identification (such as name, email address, phone number, or social media profiles).
* **No Cloud Storage or Remote Databases:** All workout sessions, routines, set weights, reps, rest intervals, custom exercises, and personal records are stored **100% locally on your device** in a private SQLite database.
* **No Analytics or Telemetry:** We do not include any tracking software, third-party analytics (such as Google Analytics or Firebase), crash reporting SDKs, or session recorders.
* **No Advertising:** Himma contains zero advertisements and zero ad-tracking networks.

---

## 2. Device Permissions & Usage

Himma requests only the minimum set of permissions necessary to deliver essential gym workout tracking features:

| Permission | Purpose |
| :--- | :--- |
| `POST_NOTIFICATIONS` | To display the live rest timer notification and alert you when your rest countdown finishes. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` | To maintain an uninterrupted countdown rest timer while your phone screen is locked or while using other apps between sets. |
| `VIBRATE` | To provide tactile haptic feedback and alert vibrations when a rest interval completes. |
| `INTERNET` | In non-store releases, this is used exclusively to check for newer open-source releases hosted on GitHub when explicitly requested. No personal data, workout metrics, or device identifiers are ever transmitted. |

Himma does **not** request access to your contacts, camera, microphone, GPS location, or device identity.

---

## 3. Data Storage, Backup & Export

* **Local Storage:** All app data is maintained within Android's sandboxed local application storage. No external app can access this data without your explicit permission.
* **User-Controlled Export & Backup:** When you use the Backup & Restore feature (JSON) or the CSV Export feature, the exported files are saved strictly to the folder or cloud drive you select via Android's native Storage Access Framework (SAF). You maintain full ownership and control over these backup files.
* **Data Deletion:** Uninstalling the application completely deletes all locally stored data, databases, and preferences from your device.

---

## 4. Children's Privacy

Himma is designed for general audiences and does not knowingly collect, store, or solicit any personal information from children under the age of 13 (or under the applicable age in your jurisdiction). Because no personal data is collected from any user, Himma is fully compliant with the Children's Online Privacy Protection Act (COPPA) and global privacy standards.

---

## 5. Changes to This Privacy Policy

Because Himma operates entirely offline and does not collect user data, our core privacy commitments will not change. If any updates are made to this document (such as documenting new offline features), the revised policy will be posted within this repository with an updated date.

---

## 6. Open Source Transparency & Contact

Himma is open-source software licensed under the **PolyForm Noncommercial License 1.0.0**. You can inspect the complete source code, review all permissions, and audit data handling directly on GitHub:

* **Source Code:** [https://github.com/abdo13579/himma-android-native](https://github.com/abdo13579/himma-android-native)
* **Developer / Contact:** For questions, feedback, or support regarding this policy, please open an issue on the GitHub repository or contact:  
  `abdulrahman.a.alfeel@gmail.com`
