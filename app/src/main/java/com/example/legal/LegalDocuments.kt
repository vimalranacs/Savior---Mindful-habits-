package com.example.legal

import com.example.ui.theme.BrandConfig

data class LegalDocument(
    val id: String,
    val title: String,
    val subtitle: String,
    val lastUpdatedNote: String,
    val sections: List<LegalSection>
)

data class LegalSection(
    val heading: String,
    val body: String
)

/**
 * Practical starting-point legal and policy documents written in clear language.
 * Explicitly notes that these drafts should be reviewed by a qualified lawyer before public release.
 */
object LegalDocuments {

    private const val APP = BrandConfig.APP_NAME

    val draftNotice =
        "Note: These documents are provided as a practical starting draft for clarity and transparency. " +
            "They are not legally certified and should be reviewed by a qualified lawyer familiar with the " +
            "jurisdictions where $APP is distributed."

    val all: List<LegalDocument> = listOf(
        LegalDocument(
            id = "privacy_policy",
            title = "Privacy Policy",
            subtitle = "Local-first storage, zero network transmission, and full user control",
            lastUpdatedNote = "Draft v1.0 • Local-only architecture",
            sections = listOf(
                LegalSection(
                    heading = "1. Core Privacy Principle",
                    body = "$APP is designed to operate completely offline on your device. It does not require an account, does not include advertisements, does not embed third-party analytics SDKs, and does not request Android's INTERNET permission."
                ),
                LegalSection(
                    heading = "2. Information Stored Locally on Your Device",
                    body = "All information you enter or configure—including your selected restricted applications, blocking schedules, pause history, grocery lists, evening meal notes, optional spending entries, and reminder settings—is stored strictly in your device's local Room database and Preferences DataStore. Optional personal notes on spending entries are encrypted locally using Android Keystore (AES-256-GCM) when supported by your device."
                ),
                LegalSection(
                    heading = "3. What $APP Never Collects or Transmits",
                    body = "$APP never transmits your installed-app list, never reads screen contents, messages, passwords, or keystrokes, never scrapes banking or transaction data, never tracks browsing history, and never sells or shares user data with any third party."
                ),
                LegalSection(
                    heading = "4. Backups and Security Honesty",
                    body = "Sensitive local databases and preferences are excluded from Android cloud backup by default. While local-only storage eliminates server-side exposure, no local storage mechanism guarantees absolute security if a device itself is unlocked or compromised. Protect your phone with a strong screen lock and keep Android updated."
                ),
                LegalSection(
                    heading = "5. Your Rights and Controls",
                    body = "At any time inside Settings > Privacy & Data, you can export your local records to a JSON file, reset settings to defaults, or permanently delete all local data stored by $APP."
                )
            )
        ),
        LegalDocument(
            id = "terms",
            title = "Terms and Conditions",
            subtitle = "Voluntary use, personal responsibility, and honest technical boundaries",
            lastUpdatedNote = "Draft v1.0 • For legal review",
            sections = listOf(
                LegalSection(
                    heading = "1. Voluntary Digital Well-Being Tool",
                    body = "$APP is provided as a personal self-management tool to help you introduce an intentional pause before using applications you voluntarily select. You remain fully in control of your device, permissions, and choices at all times."
                ),
                LegalSection(
                    heading = "2. No Guaranteed Outcomes",
                    body = "$APP does not guarantee that you will save a specific amount of money, reduce spending, change habits, or improve personal well-being. Outcomes depend entirely on your own choices and consistency."
                ),
                LegalSection(
                    heading = "3. Operating System Limitations",
                    body = "App-pausing functionality relies on Android Accessibility APIs voluntarily enabled by you. Android OS updates, device manufacturer battery restrictions, permission revocations, or safe mode may interrupt or stop blocking. $APP does not claim to make any application impossible to open."
                ),
                LegalSection(
                    heading = "4. No Financial or Legal Advice",
                    body = "Optional spending logs, grocery planning tools, and educational articles inside $APP are for personal awareness and general informational purposes only. They do not constitute financial, legal, medical, or professional advice."
                )
            )
        ),
        LegalDocument(
            id = "disclaimer",
            title = "Disclaimer",
            subtitle = "Independence, third-party boundaries, and informational scope",
            lastUpdatedNote = "Draft v1.0",
            sections = listOf(
                LegalSection(
                    heading = "Independent Application",
                    body = "$APP is an independent digital well-being and personal consumption management application. It is designed to help users voluntarily manage access to selected applications and make more informed decisions about their purchases."
                ),
                LegalSection(
                    heading = "No Third-Party Affiliation",
                    body = "$APP is not affiliated with, endorsed by, sponsored by or officially connected with Blinkit, Zepto, Swiggy, Zomato, Instamart, Amazon, Flipkart or any other third-party application.\n\nAll third-party names and trademarks belong to their respective owners and are used only where necessary to identify applications or explain their functionality."
                ),
                LegalSection(
                    heading = "No Interference with External Services",
                    body = "$APP does not access, modify, interfere with or control the internal systems, servers, transactions, services or operations of third-party applications."
                ),
                LegalSection(
                    heading = "Results & Platform Dependencies",
                    body = "The application does not guarantee that users will save money, reduce spending, improve their health or change their behaviour. These outcomes depend on individual choices.\n\nApplication-blocking functionality depends on Android operating system capabilities and permissions voluntarily granted by the user. Restrictions may be affected by operating system updates, permission changes, device settings or other technical limitations."
                ),
                LegalSection(
                    heading = "Educational Content Standards",
                    body = "Educational content about digital privacy and consumer behaviour is provided for general informational purposes. Company-specific statements must be supported by reliable evidence and clearly distinguish established facts from reported claims and interpretations."
                )
            )
        ),
        LegalDocument(
            id = "accessibility_disclosure",
            title = "Accessibility & Permission Disclosure",
            subtitle = "Exact scope of Android permissions requested by $APP",
            lastUpdatedNote = "Prominent Disclosure Reference",
            sections = listOf(
                LegalSection(
                    heading = "1. AccessibilityService API (Optional, User-Activated)",
                    body = "Why it is requested: To detect when an application you explicitly selected enters the foreground so $APP can display a calm pause screen before you proceed.\n\nMinimum data inspected: Only the foreground package identifier (event.packageName) during window state transitions.\n\nStrict technical limits: $APP sets canRetrieveWindowContent=\"false\" and canPerformGestures=\"false\". It cannot read screen text, form inputs, passwords, chats, notifications, or keystrokes, and does not perform automated clicks. You can disable this permission at any time in Android Accessibility Settings."
                ),
                LegalSection(
                    heading = "2. Launchable App Visibility (<queries>)",
                    body = "$APP declares a targeted launcher intent query (ACTION_MAIN / CATEGORY_LAUNCHER) instead of requesting broad QUERY_ALL_PACKAGES access. This lets you view and select installed apps you wish to pause. The list stays exclusively on your device."
                ),
                LegalSection(
                    heading = "3. Notifications (POST_NOTIFICATIONS on Android 13+)",
                    body = "Requested only when you enable reminders such as evening grocery planning or running-low item check-ins. Fully configurable with quiet hours and daily caps."
                ),
                LegalSection(
                    heading = "4. Boot Completed (RECEIVE_BOOT_COMPLETED)",
                    body = "Used solely to reschedule your configured local WorkManager reminders after your phone restarts."
                )
            )
        ),
        LegalDocument(
            id = "trademark_disclaimer",
            title = "Third-Party Trademark Disclaimer",
            subtitle = "Respect for external product names and ownership",
            lastUpdatedNote = "Draft v1.0",
            sections = listOf(
                LegalSection(
                    heading = "Nominative Reference Only",
                    body = "Names of third-party services such as Blinkit, Zepto, Swiggy, Zomato, Instamart, Amazon, Flipkart, Android, and Google Play are trademarks or registered trademarks of their respective owners. Their mention inside $APP is strictly descriptive and nominative to help users recognize common commerce categories or configure their own device preferences."
                ),
                LegalSection(
                    heading = "App Icons Displayed from Your Device",
                    body = "When you open the app selector, Android's local PackageManager renders icons already installed on your own device so you can identify your apps accurately. $APP bundles no third-party brand logos or proprietary artwork."
                )
            )
        ),
        LegalDocument(
            id = "data_rights",
            title = "Data Deletion & User Rights",
            subtitle = "How to inspect, export, reset, or erase everything locally",
            lastUpdatedNote = "Draft v1.0",
            sections = listOf(
                LegalSection(
                    heading = "1. Export Your Data",
                    body = "Use Settings > Privacy & Data > Export My Data to save a readable JSON copy of your local settings, restricted app list, grocery lists, pause history, and spending records to any folder you choose via Android's Storage Access Framework."
                ),
                LegalSection(
                    heading = "2. Reset Settings",
                    body = "Resets theme, cooldown duration, quiet hours, and notification preferences back to defaults while keeping your grocery lists and spending records intact."
                ),
                LegalSection(
                    heading = "3. Delete All Data",
                    body = "Immediately deletes all rows from the local database and clears local preferences. Uninstalling $APP from Android also permanently removes its local app storage."
                )
            )
        )
    )
}
