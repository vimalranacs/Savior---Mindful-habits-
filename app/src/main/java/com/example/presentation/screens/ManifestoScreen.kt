package com.example.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.legal.EditorialExpandable
import com.example.legal.EvidenceBadge
import com.example.legal.ManifestoArticle
import com.example.legal.ManifestoContent
import com.example.presentation.components.CalmCard
import com.example.ui.theme.BrandConfig
import com.example.ui.theme.LocalSaviorColors

@Composable
fun ManifestoScreen() {
    val saviorColors = LocalSaviorColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("manifesto_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Editorial Masthead
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = BrandConfig.MANIFESTO_SECTION_TITLE,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Understand What Happens Behind Convenience",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Short editorial essays on digital consumption, behavioural design, and mobile privacy literacy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Featured Short Manifesto Card
        item {
            CalmCard(containerColor = MaterialTheme.colorScheme.surface) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "THE MANIFESTO • ${ManifestoContent.SHORT_MANIFESTO_TITLE.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                ManifestoContent.shortManifestoParagraphs.forEachIndexed { idx, paragraph ->
                    Text(
                        text = paragraph,
                        style = if (idx == 0 || idx == ManifestoContent.shortManifestoParagraphs.lastIndex) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Editorial Articles
        items(ManifestoContent.articles, key = { it.id }) { article ->
            ManifestoArticleCard(article = article)
        }

        // Verification Standard Footer
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, saviorColors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Editorial Integrity Note: ${BrandConfig.APP_NAME} never presents unverified commentary about named companies as established fact. Each technical topic above is labeled clearly to distinguish documented Android platform mechanics from third-party commentary.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ManifestoArticleCard(article: ManifestoArticle) {
    val context = LocalContext.current
    val saviorColors = LocalSaviorColors.current

    CalmCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = article.sectionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${article.readMinutes} min read",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Pull Quote
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, saviorColors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "“${article.pullQuote}”",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(14.dp)
            )
        }

        article.paragraphs.forEach { para ->
            Text(
                text = para,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (article.expandables.isNotEmpty()) {
            HorizontalDivider(color = saviorColors.borderSubtle)
            Text(
                text = "Deep Dives & Evidence Classification",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            article.expandables.forEach { item ->
                ExpandableTopicTile(item = item)
            }
        }

        if (article.references.isNotEmpty()) {
            HorizontalDivider(color = saviorColors.borderSubtle)
            Text(
                text = "Primary Technical Documentation & Sources",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            article.references.forEach { ref ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${ref.title} • ${ref.publisher}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = ref.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ref.url))
                                runCatching { context.startActivity(intent) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = ref.url,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableTopicTile(item: EditorialExpandable) {
    var expanded by remember { mutableStateOf(false) }
    val saviorColors = LocalSaviorColors.current

    val badgeContainer = when (item.badge) {
        EvidenceBadge.VERIFIED_CONCEPT -> saviorColors.positiveContainer
        EvidenceBadge.REPORTED_CLAIM -> saviorColors.warmContainer
        EvidenceBadge.GENERAL_PRIVACY_RISK -> MaterialTheme.colorScheme.surfaceVariant
        EvidenceBadge.EDITORIAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeText = when (item.badge) {
        EvidenceBadge.VERIFIED_CONCEPT -> saviorColors.positive
        EvidenceBadge.REPORTED_CLAIM -> saviorColors.warmAccent
        EvidenceBadge.GENERAL_PRIVACY_RISK -> MaterialTheme.colorScheme.primary
        EvidenceBadge.EDITORIAL -> MaterialTheme.colorScheme.primary
    }

    Surface(
        onClick = { expanded = !expanded },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, saviorColors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = badgeContainer
                    ) {
                        Text(
                            text = item.badge.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse explanation" else "Expand explanation"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Verification context: ${item.verificationNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
