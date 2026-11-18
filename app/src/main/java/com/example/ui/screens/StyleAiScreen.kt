package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.OutfitComboEntity
import com.example.data.local.StylerConstants
import com.example.data.local.UserProfileEntity
import com.example.data.local.WardrobeItemEntity
import com.example.ui.components.OutfitComboCard
import com.example.ui.theme.StylerAccentOrange
import com.example.ui.theme.StylerCardSurface
import com.example.ui.theme.StylerDarkCharcoal
import com.example.ui.theme.StylerDeepGreen
import com.example.ui.theme.StylerErrorContainer
import com.example.ui.theme.StylerErrorRed
import com.example.ui.theme.StylerLightSurface
import com.example.ui.theme.StylerMutedCharcoal
import com.example.ui.theme.StylerOrangeContainer
import com.example.ui.theme.StylerPureWhite
import com.example.ui.theme.StylerSageContainer
import com.example.ui.theme.StylerSubtleBorder
import com.example.ui.viewmodel.OutfitEngineUiState

@Composable
fun StyleAiScreen(
    userProfile: UserProfileEntity,
    wardrobeItems: List<WardrobeItemEntity>,
    allOutfits: List<OutfitComboEntity>,
    outfitEngineState: OutfitEngineUiState,
    todayDate: String,
    onOpenQuestionnaire: () -> Unit,
    onTogglePin: (OutfitComboEntity) -> Unit,
    onMarkWornToday: (OutfitComboEntity) -> Unit,
    onDeleteOutfit: (OutfitComboEntity) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bodyFrame = remember(userProfile.heightCm, userProfile.weightKg) {
        StylerConstants.calculateBodyFrame(userProfile.heightCm, userProfile.weightKg)
    }

    // Show the 4 most recently generated outfits at the top, plus any additional saved outfits
    val primaryFourCombos = remember(allOutfits) {
        allOutfits.sortedByDescending { it.createdAt?.seconds ?: 0L }.take(4)
    }
    val latestMissingPieceTip = remember(primaryFourCombos, userProfile.lastMissingPieceTip) {
        primaryFourCombos.firstOrNull { it.missingPieceSuggestion.isNotBlank() }?.missingPieceSuggestion
            ?: userProfile.lastMissingPieceTip
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StylerLightSurface)
            .testTag("style_ai_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner + "Style" Questionnaire Button
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = StylerDeepGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = StylerAccentOrange,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "GEMINI AI OUTFIT ENGINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StylerPureWhite,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "4 AI Outfit Combos",
                                style = MaterialTheme.typography.headlineLarge,
                                color = StylerPureWhite
                            )
                            Text(
                                text = "Tailored to ${userProfile.heightCm}cm • ${userProfile.weightKg}kg (${bodyFrame.shortLabel}) & ${wardrobeItems.size} wardrobe pieces",
                                style = MaterialTheme.typography.bodySmall,
                                color = StylerLightSurface.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onOpenQuestionnaire,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StylerAccentOrange,
                            contentColor = StylerPureWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("open_questionnaire_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Style New Occasion & Weather (4 Combos)",
                            style = MaterialTheme.typography.titleSmall,
                            color = StylerPureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Loading or Error Banner for AI Outfit Engine
        when (val state = outfitEngineState) {
            is OutfitEngineUiState.Generating -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = StylerSageContainer),
                        border = BorderStroke(1.5.dp, StylerDeepGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_generating_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = StylerDeepGreen,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Curating 4 Combos for ${state.occasion} (${state.weatherVibe})...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StylerDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Gemini is pairing your uploaded wardrobe items, applying Smart Occasion Rules, and calculating colour harmony scores.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StylerDarkCharcoal
                                )
                            }
                        }
                    }
                }
            }
            is OutfitEngineUiState.Error -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = StylerErrorContainer),
                        border = BorderStroke(1.dp, StylerErrorRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_error_banner")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = StylerErrorRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Styling Issue",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StylerErrorRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StylerDarkCharcoal
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onOpenQuestionnaire,
                                    colors = ButtonDefaults.buttonColors(containerColor = StylerErrorRed)
                                ) {
                                    Text("Try Again", color = StylerPureWhite)
                                }
                                Button(
                                    onClick = onDismissError,
                                    colors = ButtonDefaults.buttonColors(containerColor = StylerDarkCharcoal)
                                ) {
                                    Text("Dismiss", color = StylerPureWhite)
                                }
                            }
                        }
                    }
                }
            }
            else -> Unit
        }

        // 3. The 4 AI Outfit Combo Cards
        if (primaryFourCombos.isEmpty()) {
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
                            text = "Ready to Generate 4 AI Outfit Combos",
                            style = MaterialTheme.typography.titleLarge,
                            color = StylerDarkCharcoal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the button above to select your occasion (Gym, Casual Date, Lunch, Dinner, Morning Jog, Night Out, Formal Event, Festival, Temple) and current weather.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StylerMutedCharcoal
                        )
                    }
                }
            }
        } else {
            items(primaryFourCombos, key = { it.id }) { outfit ->
                OutfitComboCard(
                    outfit = outfit,
                    allWardrobeItems = wardrobeItems,
                    todayDate = todayDate,
                    isDashboardMode = false,
                    onTogglePin = onTogglePin,
                    onMarkWornToday = onMarkWornToday,
                    onDelete = onDeleteOutfit
                )
            }
        }

        // 4. Single-Line AI Missing Piece Suggestion at the bottom of the 4 combos
        if (latestMissingPieceTip.isNotBlank()) {
            item {
                Surface(
                    color = StylerOrangeContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, StylerAccentOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("missing_piece_tip_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Missing Piece Suggestion",
                            tint = StylerAccentOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MISSING PIECE SUGGESTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = StylerAccentOrange,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = latestMissingPieceTip,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = StylerDarkCharcoal
                            )
                        }
                    }
                }
            }
        }
    }
}
