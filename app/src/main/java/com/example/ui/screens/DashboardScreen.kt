package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OutfitComboEntity
import com.example.data.local.StylerConstants
import com.example.data.local.UserProfileEntity
import com.example.data.local.WardrobeItemEntity
import com.example.ui.components.Base64Image
import com.example.ui.components.OutfitComboCard
import com.example.ui.components.WeatherBadgePill
import com.example.ui.theme.StylerAccentOrange
import com.example.ui.theme.StylerCardSurface
import com.example.ui.theme.StylerDarkCharcoal
import com.example.ui.theme.StylerDeepGreen
import com.example.ui.theme.StylerDeepGreenContainer
import com.example.ui.theme.StylerLightSurface
import com.example.ui.theme.StylerMutedCharcoal
import com.example.ui.theme.StylerOrangeContainer
import com.example.ui.theme.StylerPureWhite
import com.example.ui.theme.StylerSageContainer
import com.example.ui.theme.StylerSageGreen
import com.example.ui.theme.StylerSubtleBorder

@Composable
fun DashboardScreen(
    userProfile: UserProfileEntity,
    wardrobeItems: List<WardrobeItemEntity>,
    quickOutfits: List<OutfitComboEntity>,
    todayDate: String,
    onOpenQuestionnaire: () -> Unit,
    onOpenUploadModal: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onTogglePin: (OutfitComboEntity) -> Unit,
    onMarkWornToday: (OutfitComboEntity) -> Unit,
    onSwapOutfit: (OutfitComboEntity) -> Unit,
    onRegenerateOutfit: (OutfitComboEntity) -> Unit,
    onDeleteOutfit: (OutfitComboEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val bodyFrame = remember(userProfile.heightCm, userProfile.weightKg) {
        StylerConstants.calculateBodyFrame(userProfile.heightCm, userProfile.weightKg)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StylerLightSurface)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Summary Header + Current Weather Badge
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = StylerDeepGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToProfile() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(StylerSageGreen)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userProfile.profileImageBase64.isNotBlank()) {
                                    Base64Image(
                                        base64Data = userProfile.profileImageBase64,
                                        contentDescription = userProfile.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = StylerPureWhite,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Welcome back,",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StylerLightSurface.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = userProfile.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = StylerPureWhite
                                )
                                Text(
                                    text = "${userProfile.heightCm} cm • ${userProfile.weightKg} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StylerLightSurface.copy(alpha = 0.85f)
                                )
                            }
                        }

                        WeatherBadgePill(
                            temperatureC = userProfile.temperatureC,
                            condition = userProfile.weatherCondition,
                            onClick = onNavigateToProfile
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Body Frame & Fit Recommendation Tag Banner
                    Surface(
                        color = StylerSageGreen.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StylerPureWhite.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = StylerAccentOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = bodyFrame.tag,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StylerPureWhite
                                )
                                Text(
                                    text = bodyFrame.silhouetteRecommendation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StylerLightSurface.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary "Style" Action Button + Quick Camera Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenQuestionnaire,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StylerAccentOrange,
                                contentColor = StylerPureWhite
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("dashboard_style_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Style 4 AI Outfits",
                                style = MaterialTheme.typography.titleSmall,
                                color = StylerPureWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenUploadModal,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = StylerPureWhite
                            ),
                            border = BorderStroke(1.dp, StylerPureWhite.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_add_item_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = StylerAccentOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Piece (${wardrobeItems.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = StylerPureWhite
                            )
                        }
                    }
                }
            }
        }

        // 2. Section Header: "Best Quick Outfit Choices (4 Outfits)"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Best Quick Outfit Choices (4 Outfits)",
                        style = MaterialTheme.typography.headlineMedium,
                        color = StylerDarkCharcoal
                    )
                    Text(
                        text = "Curated & pinned daily combinations • Swap, regenerate, mark worn, or delete anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StylerMutedCharcoal
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = StylerDeepGreenContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${quickOutfits.size} / 4 Ready",
                        style = MaterialTheme.typography.labelLarge,
                        color = StylerDeepGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 3. The 4 Curated/Saved Top Outfits Grid / List
        if (quickOutfits.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StylerCardSurface),
                    border = BorderStroke(1.dp, StylerSubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StylerAccentOrange,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Quick Outfits Saved Yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = StylerDarkCharcoal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Style 4 AI Outfits' to answer a 2-step occasion & weather questionnaire and generate 4 personalized combinations from your wardrobe.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StylerMutedCharcoal
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenQuestionnaire,
                            colors = ButtonDefaults.buttonColors(containerColor = StylerAccentOrange)
                        ) {
                            Text("Launch Style Questionnaire", color = StylerPureWhite)
                        }
                    }
                }
            }
        } else {
            items(quickOutfits, key = { it.id }) { outfit ->
                OutfitComboCard(
                    outfit = outfit,
                    allWardrobeItems = wardrobeItems,
                    todayDate = todayDate,
                    isDashboardMode = true,
                    onTogglePin = onTogglePin,
                    onMarkWornToday = onMarkWornToday,
                    onDelete = onDeleteOutfit,
                    onSwapDashboard = onSwapOutfit,
                    onRegenerateDashboard = onRegenerateOutfit
                )
            }
        }

        // 4. Missing Piece Suggestion AI Tip Banner
        if (userProfile.lastMissingPieceTip.isNotBlank()) {
            item {
                Surface(
                    color = StylerOrangeContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, StylerAccentOrange.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Missing Piece Suggestion",
                            tint = StylerAccentOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = userProfile.lastMissingPieceTip,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = StylerDarkCharcoal
                        )
                    }
                }
            }
        }
    }
}
