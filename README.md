# Savior — Mindful Digital Consumption for Android

**Savior** is a calm, lightweight, 100% offline-first native Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3**. It introduces a thoughtful, non-judgmental pause between an impulse and an instant-commerce or food-delivery order—paired with an evening grocery-planning routine, optional manual spending awareness, streak-free habit summaries, and the **BEYOND CONVENIENCE** digital-literacy manifesto.

> **Central Philosophy:** *Don't make the user feel guilty. Help them become conscious of their choices.*

---

## 1. Current Implementation Status

All core modules and requirements are implemented and verified:

| Module | Status | Key Capabilities |
| :--- | :--- | :--- |
| **Design System & Theme** | Complete | Calming muted blue (`#426B91`), deep blue (`#294968`), off-white (`#F7F9FC`), light blue (`#EAF1F8`), and sage green (`#6F9D87`) palette; locally bundled **Newsreader** serif + **Plus Jakarta Sans** typography; Light Mode default with Dark & System options; adaptive Compact (`NavigationBar`) and Tablet/Foldable (`NavigationRail`) layouts. |
| **Onboarding & Prominent Disclosure** | Complete | 3-step calm onboarding without upfront permission demands; standalone Google Play-compliant **Prominent Disclosure** modal shown before opening Android Accessibility Settings. |
| **Intelligent App Pausing (`blocking/`)** | Complete | Installed-app selector with native icons, search, category filters (*Quick Commerce, Food Delivery, Shopping, Entertainment*), custom app entry, and real-time `SaviorAccessibilityService` (`typeWindowStateChanged`, `canRetrieveWindowContent="false"`). |
| **Pause Reflection & Unlock Flow** | Complete | Serene full-screen `PauseInterruptionActivity` with rotating non-judgmental copy, **Go Back**, **Give Me a Minute** (60s breathing reflection), and **Unlock Anyway** (5 / 15 / 30 min or Until End of Day, configurable cooldown, optional reason tag, plus manual global pause override). |
| **Blocking Schedules & Diagnostics** | Complete | *Always*, *Evening Cutoff*, *Work/Study Hours*, and *Custom Hours* schedules with day-of-week masks; live service health & battery-optimization diagnostics panel. |
| **Evening Routine & Groceries (`Plan`)** | Complete | Multiple shopping lists, full item CRUD (quantity, unit, category, price, replenishment cycle days, last purchased date, favourites, checked, running-low, and **"Available at home"** status), offline explainable suggestions, and an Evening Check-In for tomorrow's meals & household needs. |
| **Financial & Habit Awareness (`Insights`)** | Complete | Optional manual spending & avoided-order logging with Keystore-encrypted personal memos, neutral weekly/monthly/impulsive/avoided summaries, 6-month Compose Canvas trend chart, and streak-free weekly & monthly habit summaries. |
| **Manifesto (`BEYOND CONVENIENCE`)** | Complete | Editorial reading experience covering *The Economics of Convenience*, *Your Digital Footprint* (5 expandable topics with explicit *Verified Technical Concept*, *Reported Claim • Unverified*, and *General Privacy Risk* badges + official Android developer documentation links), and *Leave Room for Intention*. |
| **Privacy & Legal Center** | Complete | Zero `INTERNET` permission; Android Keystore (`AES-256-GCM`) local encryption; Auto Backup exclusion rules; JSON data export via Storage Access Framework (`CreateDocument`); Reset Settings; Delete All Data; and 6 clear-language legal drafts (*Privacy Policy, Terms, Disclaimer, Accessibility Disclosure, Trademark Disclaimer, Data Rights*). |

---

## 2. Android Versions Supported

- **Minimum SDK (`minSdk`)**: **Android 7.0 (API 24)**
- **Target & Compile SDK (`targetSdk` / `compileSdk`)**: **Android 16 (API 36)**
- Handles runtime `POST_NOTIFICATIONS` permission on **Android 13+ (API 33+)** and package visibility `<queries>` on **Android 11+ (API 30+)**.

---

## 3. Required Permissions & Why They Are Needed

Savior follows strict least-privilege principles and **does not request `android.permission.INTERNET`**.

1. **`android.permission.BIND_ACCESSIBILITY_SERVICE`** *(Optional, user-enabled in Android Settings after Prominent Disclosure)*:
   - **Why**: Detects when an application explicitly selected by the user enters the foreground (`TYPE_WINDOW_STATE_CHANGED`) so Savior can display the calm pause screen.
   - **Privacy boundary**: Configured with `android:canRetrieveWindowContent="false"` and `android:canPerformGestures="false"`. Savior inspects only `event.packageName` against the user's local list and never reads screen text, messages, passwords, or keystrokes.
2. **`android.permission.POST_NOTIFICATIONS`** *(Optional, requested on Android 13+ only when reminders are enabled)*:
   - **Why**: Delivers user-configured evening grocery planning reminders, running-low item reminders, and optional weekly reflections while respecting quiet sleep hours and daily notification caps.
3. **`android.permission.RECEIVE_BOOT_COMPLETED`** *(Install-time)*:
   - **Why**: Restores local `WorkManager` reminder schedules after a device restart.
4. **Targeted `<queries>` for `ACTION_MAIN` + `CATEGORY_LAUNCHER`**:
   - **Why**: Enumerates user-launchable installed applications for the app selector without requesting the restricted `QUERY_ALL_PACKAGES` permission.

---

## 4. Known Technical Limitations & Google Play Compliance Notes

- **Android Operating System Boundaries**: No Android application can make another application impossible to open. Users can always revoke Accessibility access, force-stop the app, or uninstall it. Furthermore, aggressive OEM battery savers (Xiaomi MIUI/HyperOS, Samsung OneUI, Oppo/Vivo/OnePlus) may suspend background services unless the user marks Savior as *Unrestricted* in Android Battery Optimization settings (accessible directly from Savior's **Pause > Diagnostics** tab).
- **Google Play Store Distribution Notes**:
  - Savior does **not** set `isAccessibilityTool="true"` because it is a digital well-being tool rather than an assistive tool for users with disabilities.
  - Before submitting to Google Play, developers must complete the **AccessibilityService Permission Declaration Form** in Google Play Console, include an explicit description of AccessibilityService usage in the public store listing, and provide a short video demonstrating the in-app `AccessibilityDisclosureDialog` flow.
- **Verification Environment Honesty**: Automated tests in this build container run via **JUnit 4** and **Robolectric JVM** (`gradle :app:testDebugUnitTest`) and full APK compilation (`gradle assembleDebug`). Physical device endurance and OEM-specific battery management should be verified on real hardware before store distribution.

---

## 5. How to Build, Test & Install

### Run Unit & Robolectric Integration Tests
```bash
gradle :app:testDebugUnitTest
```

### Build a Debug APK
```bash
gradle assembleDebug
```
The compiled debug APK is output to:
`app/build/outputs/apk/debug/app-debug.apk`

### Generate a Signed Release Build (APK / AAB)
1. Generate an upload keystore (if you do not already have one):
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Provide the signing environment variables expected by `app/build.gradle.kts` and build the release APK or Android App Bundle:
   ```bash
   KEYSTORE_PATH=/path/to/my-upload-key.jks \
   STORE_PASSWORD=your_store_password \
   KEY_PASSWORD=your_key_password \
   gradle assembleRelease bundleRelease
   ```
3. Before publishing publicly, have a qualified lawyer review the draft documents in `LegalDocuments.kt` for your target jurisdictions.
