package com.example.legal

enum class EvidenceBadge(val label: String) {
    VERIFIED_CONCEPT("Verified Technical Concept"),
    REPORTED_CLAIM("Reported Claim • Unverified"),
    GENERAL_PRIVACY_RISK("General Privacy Risk"),
    EDITORIAL("Editorial Philosophy")
}

data class EditorialExpandable(
    val title: String,
    val explanation: String,
    val badge: EvidenceBadge,
    val verificationNote: String
)

data class ReferenceSource(
    val title: String,
    val publisher: String,
    val note: String,
    val url: String
)

data class ManifestoArticle(
    val id: String,
    val sectionLabel: String,
    val title: String,
    val readMinutes: Int,
    val pullQuote: String,
    val paragraphs: List<String>,
    val expandables: List<EditorialExpandable> = emptyList(),
    val references: List<ReferenceSource> = emptyList()
)

/**
 * Editorial content for BEYOND CONVENIENCE.
 * Follows strict digital-literacy and verification standards:
 * - Clearly separates established platform facts, reported third-party claims, and general privacy risks.
 * - Never presents unverified allegations about named companies as established fact.
 * - Never invents citations, statistics, or studies.
 */
object ManifestoContent {

    const val SHORT_MANIFESTO_TITLE = "Leave Room for Intention"

    val shortManifestoParagraphs = listOf(
        "We built technology to make life easier. Somewhere along the way, ease began to replace intention.",
        "Every unnecessary order may seem insignificant on its own. Every impulsive purchase may feel harmless in isolation. But repeated decisions eventually shape our habits, our spending and the way we interact with the world.",
        "This application is not against convenience, technology or the companies that provide these services.",
        "It is about remembering that convenience should remain a choice, not become an automatic response.",
        "Buy what you need. Order when it makes sense. Use technology when it serves you.",
        "But leave room for intention."
    )

    val articles: List<ManifestoArticle> = listOf(
        ManifestoArticle(
            id = "economics_of_convenience",
            sectionLabel = "PART I • BEHAVIOURAL DESIGN",
            title = "The Economics of Convenience",
            readMinutes = 3,
            pullQuote = "Convenience is genuinely helpful when chosen deliberately. It becomes costly when friction disappears completely.",
            paragraphs = listOf(
                "Modern quick-commerce and food-delivery interfaces are engineered to remove hesitation. Saved payment methods, one-tap reorders, personalised carousels, countdown banners, and rapid doorstep delivery compress the gap between a passing thought and a completed purchase into a few seconds.",
                "None of this makes convenience apps inherently bad. When you are unwell, hosting guests unexpectedly, or short on time after a long workday, fast delivery can be a genuine relief.",
                "The quiet shift happens when low friction turns occasional convenience into a default reflex—opening an app out of boredom, tiredness, or a small kitchen gap that a five-minute evening check could have covered."
            ),
            expandables = listOf(
                EditorialExpandable(
                    title = "Four Kinds of Consumption",
                    explanation = "• Genuine necessity: Items you truly need promptly (medicine, essential ingredients for today's meal).\n\n• Planned consumption: Groceries and household staples reviewed in advance and bought on a calm schedule.\n\n• Convenience-driven consumption: Paying extra or ordering urgently to save time when you consciously choose the trade-off.\n\n• Impulsive consumption: Opening an app automatically without a clear need, then adding items triggered by prompts, bundles, or cravings.",
                    badge = EvidenceBadge.EDITORIAL,
                    verificationNote = "A practical self-reflection framework—not a moral judgment."
                ),
                EditorialExpandable(
                    title = "Why a 15-Second Pause Helps",
                    explanation = "Introducing even a brief, calm moment between tapping an icon and browsing a storefront restores deliberate decision-making. You can still choose to proceed—the difference is that the decision belongs to you rather than habit.",
                    badge = EvidenceBadge.EDITORIAL,
                    verificationNote = "Designed around autonomy rather than guilt or coercion."
                )
            )
        ),
        ManifestoArticle(
            id = "your_digital_footprint",
            sectionLabel = "PART II • DIGITAL LITERACY",
            title = "Your Digital Footprint: Five Signals Worth Understanding",
            readMinutes = 5,
            pullQuote = "Digital literacy is about understanding what mobile platforms can expose—without falling into fearmongering or unverified rumour.",
            paragraphs = listOf(
                "Online commentary frequently raises questions about how mobile apps interact with device settings, network details, and local storage. Because commentary is not the same as independently audited technical evidence, it helps to separate verified Android platform mechanics from unverified claims.",
                "Below are five privacy and security concepts often discussed in consumer-tech commentary. Each expandable note clearly distinguishes what Android technically permits, what remains an unverified claim, and how to think calmly about your own privacy."
            ),
            expandables = listOf(
                EditorialExpandable(
                    title = "1. Installed Application Information",
                    explanation = "Some online commentary alleges that certain consumer apps check whether roughly 21 other apps are installed on a device. Depending on Android version, manifest <queries> declarations, and permissions, knowing which apps exist on a phone can potentially reveal lifestyle habits, payment tools, or competing services.\n\nSince Android 11, Google introduced Package Visibility restrictions requiring apps to explicitly declare queries or justify broad visibility on Google Play.",
                    badge = EvidenceBadge.REPORTED_CLAIM,
                    verificationNote = "Any specific claim about a named app checking 21 apps is an unverified third-party claim unless independently verified via primary technical analysis. The general privacy principle around installed-app signals is a documented Android platform concept."
                ),
                EditorialExpandable(
                    title = "2. Accessibility and Display Settings",
                    explanation = "Commentary sometimes notes that apps can read display configuration such as system font scale, display zoom, or whether assistive services are enabled. While apps legitimately inspect display density and font scale to render readable layouts, combining accessibility signals with analytics could theoretically reveal how someone uses their phone.\n\nImportant: Display zoom or larger font sizes do NOT reliably determine a person's age—people of all ages adjust text size for comfort, eye strain, or personal preference.",
                    badge = EvidenceBadge.GENERAL_PRIVACY_RISK,
                    verificationNote = "Reading font scale for UI layout is standard Android behaviour. Drawing demographic or age conclusions from these signals is unreliable and speculative."
                ),
                EditorialExpandable(
                    title = "3. Account, Session & Backup Files",
                    explanation = "Public commentary occasionally alleges that specific backup files contain authentication or login details. Regardless of any unverified claim about a particular company, security engineering establishes a clear principle: storing sensitive session tokens or credentials in unencrypted files or automatic cloud backups increases risk if backup storage or a device is compromised.\n\nWell-engineered Android apps exclude sensitive databases from cloud backup and rely on Android Keystore encryption for sensitive local data.",
                    badge = EvidenceBadge.VERIFIED_CONCEPT,
                    verificationNote = "General security risk verified in Android security documentation. Do not treat unverified commentary about any specific company's backup implementation as established fact."
                ),
                EditorialExpandable(
                    title = "4. Wi-Fi Identifiers Combined with Location",
                    explanation = "Commentary often highlights collecting Wi-Fi network names (SSIDs) alongside approximate or precise location. Even when individual data points seem routine, combining persistent network identifiers with location history can become far more revealing—potentially indicating home routines, workplaces, or shared households.\n\nModern Android requires explicit location or nearby-device permissions before apps can inspect Wi-Fi network identifiers.",
                    badge = EvidenceBadge.GENERAL_PRIVACY_RISK,
                    verificationNote = "Data-combination privacy risk is well understood in mobile privacy engineering. Always review an app's runtime location permissions in Android Settings."
                ),
                EditorialExpandable(
                    title = "5. Battery Level & Contextual Behavioural Signals",
                    explanation = "Commentators sometimes cite discussion around reading device battery levels and suggest low battery could influence hurried consumer choices. While Android allows apps to read battery state without special permissions (often legitimately used to reduce background sync or animations on low power), contextual signals in general can be used in personalisation or A/B experimentation.\n\nCrucially, there is no basis here to assert that any specific named company manipulates prices based on battery percentage without rigorous, independently verified primary evidence.",
                    badge = EvidenceBadge.REPORTED_CLAIM,
                    verificationNote = "Price-manipulation claims tied to battery level are unverified unless supported by peer-reviewed or primary technical evidence. Battery APIs also serve legitimate power-saving purposes."
                )
            ),
            references = listOf(
                ReferenceSource(
                    title = "Package visibility filtering on Android",
                    publisher = "Android Developers (Official Documentation)",
                    note = "Explains how Android 11+ restricts installed-app discovery and why <queries> declarations exist.",
                    url = "https://developer.android.com/training/package-visibility"
                ),
                ReferenceSource(
                    title = "Back up user data with Auto Backup",
                    publisher = "Android Developers (Official Documentation)",
                    note = "Documents backup rules and how apps exclude sensitive local databases and credentials from cloud backups.",
                    url = "https://developer.android.com/identity/data/autobackup"
                ),
                ReferenceSource(
                    title = "Android Keystore System",
                    publisher = "Android Developers (Official Documentation)",
                    note = "Official overview of hardware-backed cryptographic key storage on Android devices.",
                    url = "https://developer.android.com/privacy-and-security/keystore"
                ),
                ReferenceSource(
                    title = "Wi-Fi scanning & location permission overview",
                    publisher = "Android Developers (Official Documentation)",
                    note = "Explains why Android guards Wi-Fi scan and SSID metadata behind location / nearby permissions.",
                    url = "https://developer.android.com/develop/connectivity/wifi/wifi-scan"
                )
            )
        ),
        ManifestoArticle(
            id = "editorial_standards",
            sectionLabel = "PART III • VERIFICATION STANDARDS",
            title = "How We Separate Facts, Claims & Privacy Risks",
            readMinutes = 2,
            pullQuote = "The goal of digital literacy is calm clarity—never defamation, outrage, or unverified accusation.",
            paragraphs = listOf(
                "Whenever technology commentary circulates on social media or video transcripts, technical nuance often gets compressed into dramatic headlines. Savior commits to four strict editorial distinctions:",
                "1. Independently verified facts: Documented Android platform APIs, official security guidelines, or verifiable primary technical disclosures.\n2. Reported third-party claims: Statements made by commentators that have not been independently reproduced or verified in primary sources.\n3. Plausible technical risks: Security or privacy weaknesses that can occur in mobile software in general, without accusing a specific brand.\n4. Editorial interpretation: Thoughtful perspectives on habit formation and intentional living.",
                "If credible primary evidence emerges that clarifies or contradicts any general note in this section, the editorial text should be updated transparently."
            )
        )
    )
}
