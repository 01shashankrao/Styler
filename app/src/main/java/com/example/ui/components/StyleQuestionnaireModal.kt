package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.StylerConstants
import com.example.data.local.UserProfileEntity
import com.example.ui.theme.StylerAccentOrange
import com.example.ui.theme.StylerCardSurface
import com.example.ui.theme.StylerDarkCharcoal
import com.example.ui.theme.StylerDeepGreen
import com.example.ui.theme.StylerLightSurface
import com.example.ui.theme.StylerMutedCharcoal
import com.example.ui.theme.StylerOrangeContainer
import com.example.ui.theme.StylerPureWhite
import com.example.ui.theme.StylerSageContainer
import com.example.ui.theme.StylerSubtleBorder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StyleQuestionnaireModal(
    userProfile: UserProfileEntity,
    onSubmitQuestionnaire: (occasion: String, weatherVibe: String, temperatureC: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedOccasion by remember { mutableStateOf("Casual Date") }
    var selectedWeatherVibe by remember(userProfile.weatherCondition, userProfile.temperatureC) {
        mutableStateOf(
            StylerConstants.mapProfileWeatherToQuestionnaireVibe(
                userProfile.weatherCondition,
                userProfile.temperatureC
            )
        )
    }
    var temperatureInput by remember(userProfile.temperatureC) {
        mutableStateOf(userProfile.temperatureC.toString())
    }

    val bodyFrame = remember(userProfile.heightCm, userProfile.weightKg) {
        StylerConstants.calculateBodyFrame(userProfile.heightCm, userProfile.weightKg)
    }

    val isAthleticOccasion = selectedOccasion == "Gym" || selectedOccasion == "Morning Jog"
    val isCulturalOccasion = selectedOccasion == "Temple" || selectedOccasion == "Festival"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StylerCardSurface,
        modifier = Modifier.testTag("style_questionnaire_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Style Questionnaire",
                        style = MaterialTheme.typography.headlineMedium,
                        color = StylerDarkCharcoal
                    )
                    Text(
                        text = "Tailored for ${userProfile.heightCm}cm • ${userProfile.weightKg}kg (${bodyFrame.shortLabel})",
                        style = MaterialTheme.typography.bodySmall,
                        color = StylerMutedCharcoal
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Questionnaire",
                        tint = StylerDarkCharcoal
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step 1: What is the occasion?
            Text(
                text = "1. What is the occasion?",
                style = MaterialTheme.typography.titleMedium,
                color = StylerDarkCharcoal,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StylerConstants.QUESTIONNAIRE_OCCASIONS.forEach { occasion ->
                    val isSelected = selectedOccasion == occasion
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedOccasion = occasion },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = { Text(occasion) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StylerDeepGreen,
                            selectedLabelColor = StylerPureWhite,
                            selectedLeadingIconColor = StylerPureWhite,
                            containerColor = StylerLightSurface,
                            labelColor = StylerDarkCharcoal
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = StylerSubtleBorder,
                            selectedBorderColor = StylerDeepGreen
                        ),
                        modifier = Modifier.testTag("occasion_option_${occasion.lowercase().replace(" ", "_")}")
                    )
                }
            }

            // Smart Occasion Rule Live Notice
            if (isAthleticOccasion || isCulturalOccasion) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = if (isAthleticOccasion) StylerSageContainer else StylerOrangeContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isAthleticOccasion) StylerDeepGreen else StylerAccentOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isAthleticOccasion) {
                            "Smart Occasion Rule Active: Formal belts, rings, and heavy V-chains will be automatically stripped for $selectedOccasion."
                        } else {
                            "Smart Occasion Rule Active: Prioritizing ethnic, modest, and warm harmonious colour palettes for $selectedOccasion."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = StylerDarkCharcoal,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step 2: Current Weather / Vibe (Pre-filled from Profile/Dashboard)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = StylerAccentOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "2. Current Weather / Vibe (Pre-filled from Profile)",
                    style = MaterialTheme.typography.titleMedium,
                    color = StylerDarkCharcoal,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StylerConstants.QUESTIONNAIRE_WEATHER_VIBES.forEach { vibe ->
                    val isSelected = selectedWeatherVibe == vibe
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedWeatherVibe = vibe },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = { Text(vibe) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StylerAccentOrange,
                            selectedLabelColor = StylerPureWhite,
                            selectedLeadingIconColor = StylerPureWhite,
                            containerColor = StylerLightSurface,
                            labelColor = StylerDarkCharcoal
                        ),
                        modifier = Modifier.testTag("weather_vibe_option_${vibe.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = temperatureInput,
                onValueChange = { value ->
                    if (value.isEmpty() || value == "-" || value.toIntOrNull() != null) {
                        if (value.length <= 3) temperatureInput = value
                    }
                },
                label = { Text("Temperature (°C)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StylerDeepGreen,
                    focusedLabelColor = StylerDeepGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("questionnaire_temp_input")
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    val tempC = temperatureInput.toIntOrNull() ?: userProfile.temperatureC
                    onSubmitQuestionnaire(selectedOccasion, selectedWeatherVibe, tempC)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StylerAccentOrange,
                    contentColor = StylerPureWhite
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_questionnaire_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate 4 AI Outfit Combos",
                    style = MaterialTheme.typography.titleMedium,
                    color = StylerPureWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
