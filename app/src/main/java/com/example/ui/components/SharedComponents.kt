package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.OutfitComboEntity
import com.example.data.local.WardrobeItemEntity
import com.example.ui.theme.StylerAccentOrange
import com.example.ui.theme.StylerCardSurface
import com.example.ui.theme.StylerDarkCharcoal
import com.example.ui.theme.StylerDeepGreen
import com.example.ui.theme.StylerDeepGreenContainer
import com.example.ui.theme.StylerErrorContainer
import com.example.ui.theme.StylerErrorRed
import com.example.ui.theme.StylerLightSurface
import com.example.ui.theme.StylerMutedCharcoal
import com.example.ui.theme.StylerOrangeContainer
import com.example.ui.theme.StylerPureWhite
import com.example.ui.theme.StylerSageContainer
import com.example.ui.theme.StylerSageGreen
import com.example.ui.theme.StylerSubtleBorder
import com.example.util.ImageCompressor

@Composable
fun Base64Image(
    base64Data: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val imageBitmap = remember(base64Data) {
        ImageCompressor.decodeBase64ToBitmap(base64Data)?.asImageBitmap()
    }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(StylerSageContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Checkroom,
                contentDescription = contentDescription,
                tint = StylerDeepGreen,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun ColorSwatchDot(
    hexString: String,
    modifier: Modifier = Modifier
) {
    val parsedColor = remember(hexString) {
        try {
            Color(android.graphics.Color.parseColor(hexString))
        } catch (_: Exception) {
            StylerDeepGreen
        }
    }
    Box(
        modifier = modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(parsedColor)
            .border(1.dp, StylerSubtleBorder, CircleShape)
    )
}

@Composable
fun WeatherBadgePill(
    temperatureC: Int,
    condition: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StylerOrangeContainer,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, StylerAccentOrange.copy(alpha = 0.35f)),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = "Weather condition",
                tint = StylerAccentOrange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$temperatureC°C • $condition",
                style = MaterialTheme.typography.labelLarge,
                color = StylerDarkCharcoal
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutfitComboCard(
    outfit: OutfitComboEntity,
    allWardrobeItems: List<WardrobeItemEntity>,
    todayDate: String,
    isDashboardMode: Boolean,
    onTogglePin: (OutfitComboEntity) -> Unit,
    onMarkWornToday: (OutfitComboEntity) -> Unit,
    onDelete: (OutfitComboEntity) -> Unit,
    onSwapDashboard: ((OutfitComboEntity) -> Unit)? = null,
    onRegenerateDashboard: ((OutfitComboEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val itemsById = remember(allWardrobeItems) { allWardrobeItems.associateBy { it.id } }
    val selectedItems = remember(outfit.itemIdsCsv, itemsById) {
        outfit.parsedItemIds().mapNotNull { itemsById[it] }
    }
    val isWornToday = outfit.lastWornDate == todayDate && todayDate.isNotEmpty()
    val isAthleticOccasion = outfit.occasion.equals("Gym", ignoreCase = true) ||
        outfit.occasion.equals("Morning Jog", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StylerCardSurface),
        border = BorderStroke(
            width = if (outfit.pinnedToDashboard) 1.5.dp else 1.dp,
            color = if (outfit.pinnedToDashboard) StylerDeepGreen else StylerSubtleBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("outfit_card_${outfit.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Badges + Harmony Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = StylerDeepGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = outfit.occasion,
                            style = MaterialTheme.typography.labelMedium,
                            color = StylerPureWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = StylerSageContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${outfit.weatherVibe} • ${outfit.temperatureC}°C",
                            style = MaterialTheme.typography.labelMedium,
                            color = StylerDeepGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (isWornToday) {
                        Surface(
                            color = StylerAccentOrange,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Worn Today",
                                    tint = StylerPureWhite,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Worn Today (${outfit.lastWornDate})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StylerPureWhite
                                )
                            }
                        }
                    } else if (outfit.lastWornDate.isNotEmpty()) {
                        Surface(
                            color = StylerOrangeContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Last worn: ${outfit.lastWornDate}",
                                style = MaterialTheme.typography.labelMedium,
                                color = StylerDarkCharcoal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = StylerDeepGreenContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${outfit.colorHarmonyScore}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StylerDeepGreen
                        )
                        Text(
                            text = "Harmony",
                            style = MaterialTheme.typography.labelSmall,
                            color = StylerDeepGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = outfit.title,
                style = MaterialTheme.typography.headlineMedium,
                color = StylerDarkCharcoal
            )

            if (isAthleticOccasion) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Smart Occasion Rule: Formal belts, rings & heavy chains stripped for athletic mobility.",
                    style = MaterialTheme.typography.labelSmall,
                    color = StylerSageGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (outfit.colorHarmonyScore / 100f).coerceIn(0f, 1f) },
                color = StylerAccentOrange,
                trackColor = StylerSageContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Complete Outfit Pieces & Scent (${selectedItems.size})",
                style = MaterialTheme.typography.labelLarge,
                color = StylerMutedCharcoal
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedItems.isEmpty()) {
                Surface(
                    color = StylerLightSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pieces from this combination were removed from your gallery. Tap Regenerate for a fresh combo!",
                        style = MaterialTheme.typography.bodySmall,
                        color = StylerMutedCharcoal,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    selectedItems.forEach { item ->
                        Surface(
                            color = StylerLightSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StylerSubtleBorder),
                            modifier = Modifier.width(114.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    Base64Image(
                                        base64Data = item.imageBase64,
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = StylerSageContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.category.substringBefore(" ("),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StylerDeepGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    ColorSwatchDot(hexString = item.colorHex)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StylerDarkCharcoal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Styling & Fit Note Box
            Surface(
                color = StylerLightSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StylerSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Styling Note",
                        tint = StylerAccentOrange,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Dress, Layering & Silhouette Note",
                            style = MaterialTheme.typography.labelLarge,
                            color = StylerDeepGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = outfit.stylingNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StylerDarkCharcoal
                        )
                    }
                }
            }

            if (outfit.colorTheoryNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = StylerSageContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StylerSageGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Color Theory & Perfume Harmony",
                            tint = StylerDeepGreen,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Color Theory & Perfume Harmony",
                                style = MaterialTheme.typography.labelLarge,
                                color = StylerDeepGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = outfit.colorTheoryNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = StylerDarkCharcoal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row 1: Mark as Worn Today + Pin to Dashboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onMarkWornToday(outfit) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isWornToday) StylerOrangeContainer else Color.Transparent,
                        contentColor = if (isWornToday) StylerAccentOrange else StylerDarkCharcoal
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isWornToday) StylerAccentOrange else StylerSubtleBorder
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("worn_today_button_${outfit.id}")
                ) {
                    Icon(
                        imageVector = if (isWornToday) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Mark as Worn Today",
                        tint = if (isWornToday) StylerAccentOrange else StylerSageGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isWornToday) "Worn Today" else "Mark Worn Today",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = { onTogglePin(outfit) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (outfit.pinnedToDashboard) StylerDeepGreen else StylerSageGreen,
                        contentColor = StylerPureWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pin_outfit_button_${outfit.id}")
                ) {
                    Icon(
                        imageVector = if (outfit.pinnedToDashboard) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin to Dashboard",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (outfit.pinnedToDashboard) "Pinned" else "Pin to Dashboard",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row 2: Dashboard Swap / Regenerate + Delete Outfit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDashboardMode && onSwapDashboard != null && onRegenerateDashboard != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSwapDashboard(outfit) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StylerSubtleBorder),
                            modifier = Modifier.testTag("swap_outfit_button_${outfit.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap Outfit",
                                tint = StylerDeepGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Swap",
                                style = MaterialTheme.typography.labelLarge,
                                color = StylerDeepGreen
                            )
                        }

                        OutlinedButton(
                            onClick = { onRegenerateDashboard(outfit) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StylerAccentOrange.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("regenerate_outfit_button_${outfit.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate Outfit",
                                tint = StylerAccentOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Regenerate",
                                style = MaterialTheme.typography.labelLarge,
                                color = StylerAccentOrange
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Surface(
                    color = StylerErrorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clickable { onDelete(outfit) }
                        .testTag("delete_outfit_button_${outfit.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Outfit",
                            tint = StylerErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete Outfit",
                            style = MaterialTheme.typography.labelLarge,
                            color = StylerErrorRed
                        )
                    }
                }
            }
        }
    }
}
