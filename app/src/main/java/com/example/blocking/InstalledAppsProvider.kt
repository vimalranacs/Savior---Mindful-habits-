package com.example.blocking

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.data.local.AppCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val packageName: String,
    val displayName: String,
    val suggestedCategory: String,
    val icon: Drawable?
)

/**
 * Discovers user-launchable installed applications via Play-compliant ACTION_MAIN / CATEGORY_LAUNCHER
 * queries—no QUERY_ALL_PACKAGES required and never transmitted off-device.
 */
class InstalledAppsProvider(private val context: Context) {

    suspend fun loadLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPackage = context.packageName
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val seenPackages = HashSet<String>()
        val results = mutableListOf<InstalledAppInfo>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg == selfPackage || !seenPackages.add(pkg)) continue
            val label = runCatching { info.loadLabel(pm)?.toString() }.getOrNull()
                ?.takeIf { it.isNotBlank() } ?: pkg
            val icon = runCatching { info.loadIcon(pm) }.getOrNull()
            results.add(
                InstalledAppInfo(
                    packageName = pkg,
                    displayName = label,
                    suggestedCategory = categorize(pkg, label),
                    icon = icon
                )
            )
        }

        // Surface quick-commerce and food-delivery apps first, then alphabetically
        results.sortedWith(
            compareBy<InstalledAppInfo> { categorySortOrder(it.suggestedCategory) }
                .thenBy { it.displayName.lowercase() }
        )
    }

    companion object {
        fun categorize(packageName: String, displayName: String): String {
            val lowerPkg = packageName.lowercase()
            val lowerName = displayName.lowercase()
            return when {
                lowerPkg.contains("grofers") || lowerPkg.contains("blinkit") ||
                    lowerPkg.contains("zepto") || lowerPkg.contains("bigbasket") ||
                    lowerPkg.contains("dunzo") || lowerPkg.contains("instamart") ||
                    lowerName.contains("blinkit") || lowerName.contains("zepto") ||
                    lowerName.contains("instamart") || lowerName.contains("bigbasket") ->
                    AppCategory.QUICK_COMMERCE

                lowerPkg.contains("swiggy") || lowerPkg.contains("zomato") ||
                    lowerPkg.contains("eats") || lowerPkg.contains("doordash") ||
                    lowerPkg.contains("grubhub") || lowerPkg.contains("deliveroo") ||
                    lowerPkg.contains("dominos") || lowerPkg.contains("mcdonalds") ||
                    lowerName.contains("swiggy") || lowerName.contains("zomato") ||
                    lowerName.contains("uber eats") ->
                    AppCategory.FOOD_DELIVERY

                lowerPkg.contains("amazon") || lowerPkg.contains("flipkart") ||
                    lowerPkg.contains("myntra") || lowerPkg.contains("ajio") ||
                    lowerPkg.contains("nykaa") || lowerPkg.contains("meesho") ||
                    lowerPkg.contains("ebay") || lowerPkg.contains("etsy") ||
                    lowerPkg.contains("shein") || lowerPkg.contains("walmart") ||
                    lowerName.contains("amazon") || lowerName.contains("flipkart") ||
                    lowerName.contains("shop") || lowerName.contains("store") ->
                    AppCategory.SHOPPING

                lowerPkg.contains("youtube") || lowerPkg.contains("instagram") ||
                    lowerPkg.contains("netflix") || lowerPkg.contains("hotstar") ||
                    lowerPkg.contains("spotify") || lowerPkg.contains("reddit") ||
                    lowerPkg.contains("tiktok") || lowerPkg.contains("primevideo") ||
                    lowerName.contains("youtube") || lowerName.contains("instagram") ||
                    lowerName.contains("netflix") ->
                    AppCategory.ENTERTAINMENT

                else -> AppCategory.OTHER
            }
        }

        private fun categorySortOrder(category: String): Int = when (category) {
            AppCategory.QUICK_COMMERCE -> 0
            AppCategory.FOOD_DELIVERY -> 1
            AppCategory.SHOPPING -> 2
            AppCategory.ENTERTAINMENT -> 3
            else -> 4
        }
    }
}
