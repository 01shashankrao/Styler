package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.StylerConstants
import com.example.data.local.UserProfileEntity
import com.example.ui.components.Base64Image
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
import com.example.util.ImageCompressor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfileEntity,
    onSaveProfile: (
        name: String,
        profileImageBase64: String,
        heightCm: Int,
        weightKg: Int,
        temperatureC: Int,
        weatherCondition: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var nameInput by rememberSaveable(userProfile.name) { mutableStateOf(userProfile.name) }
    var profileImageBase64 by rememberSaveable(userProfile.profileImageBase64) {
        mutableStateOf(userProfile.profileImageBase64)
    }
    var heightInput by rememberSaveable(userProfile.heightCm) {
        mutableStateOf(userProfile.heightCm.toString())
    }
    var weightInput by rememberSaveable(userProfile.weightKg) {
        mutableStateOf(userProfile.weightKg.toString())
    }
    var tempInput by rememberSaveable(userProfile.temperatureC) {
        mutableStateOf(userProfile.temperatureC.toString())
    }
    var selectedWeather by rememberSaveable(userProfile.weatherCondition) {
        mutableStateOf(userProfile.weatherCondition)
    }

    LaunchedEffect(userProfile) {
        nameInput = userProfile.name
        profileImageBase64 = userProfile.profileImageBase64
        heightInput = userProfile.heightCm.toString()
        weightInput = userProfile.weightKg.toString()
        tempInput = userProfile.temperatureC.toString()
        selectedWeather = userProfile.weatherCondition
    }

    val liveHeight = heightInput.toIntOrNull() ?: userProfile.heightCm
    val liveWeight = weightInput.toIntOrNull() ?: userProfile.weightKg
    val liveTemp = tempInput.toIntOrNull() ?: userProfile.temperatureC

    val bodyFrameAnalysis = remember(liveHeight, liveWeight) {
        StylerConstants.calculateBodyFrame(liveHeight, liveWeight)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            ImageCompressor.compressFromUri(context, uri)?.let { compressed ->
                profileImageBase64 = compressed.base64Data
                onSaveProfile(nameInput, compressed.base64Data, liveHeight, liveWeight, liveTemp, selectedWeather)
            }
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val compressed = ImageCompressor.compressBitmap(bitmap)
            profileImageBase64 = compressed.base64Data
            onSaveProfile(nameInput, compressed.base64Data, liveHeight, liveWeight, liveTemp, selectedWeather)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPreviewLauncher.launch(null)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StylerLightSurface)
            .testTag("profile_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Avatar Upload Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = StylerCardSurface),
                border = BorderStroke(1.dp, StylerSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Style Profile & Metrics",
                        style = MaterialTheme.typography.headlineLarge,
                        color = StylerDarkCharcoal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Persisted permanently on device • Drives AI fit & weather styling",
                        style = MaterialTheme.typography.bodySmall,
                        color = StylerMutedCharcoal
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(StylerDeepGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageBase64.isNotBlank()) {
                            Base64Image(
                                base64Data = profileImageBase64,
                                contentDescription = "User Profile Avatar",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Profile Avatar",
                                tint = StylerPureWhite,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StylerDeepGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = StylerDeepGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photo", color = StylerDeepGreen)
                        }

                        OutlinedButton(
                            onClick = {
                                val hasCamPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCamPerm) {
                                    cameraPreviewLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StylerAccentOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = StylerAccentOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera", color = StylerAccentOrange)
                        }
                    }
                }
            }
        }

        // 2. Auto-Calculated Body Frame / Fit Recommendation Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StylerDeepGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("body_frame_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = StylerAccentOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BODY FRAME & FIT RECOMMENDATION",
                                style = MaterialTheme.typography.labelMedium,
                                color = StylerAccentOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = StylerSageGreen,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "BMI ${bodyFrameAnalysis.bmi}",
                                style = MaterialTheme.typography.labelMedium,
                                color = StylerPureWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = bodyFrameAnalysis.tag,
                        style = MaterialTheme.typography.headlineMedium,
                        color = StylerPureWhite
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = bodyFrameAnalysis.silhouetteRecommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StylerLightSurface.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // 3. Editable Personal Metrics (Name, Height cm, Weight kg)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StylerCardSurface),
                border = BorderStroke(1.dp, StylerSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Personal Proportions",
                        style = MaterialTheme.typography.titleLarge,
                        color = StylerDarkCharcoal
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StylerDeepGreen,
                            focusedLabelColor = StylerDeepGreen
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() } && value.length <= 3) {
                                    heightInput = value
                                }
                            },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StylerDeepGreen,
                                focusedLabelColor = StylerDeepGreen
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_height_input")
                        )

                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() } && value.length <= 3) {
                                    weightInput = value
                                }
                            },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StylerDeepGreen,
                                focusedLabelColor = StylerDeepGreen
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_weight_input")
                        )
                    }
                }
            }
        }

        // 4. Default Local Weather (Temperature °C + Condition: Sunny, Humid, Rainy, Pleasant, Chilly)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StylerCardSurface),
                border = BorderStroke(1.dp, StylerSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = StylerAccentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Default Local Weather",
                            style = MaterialTheme.typography.titleLarge,
                            color = StylerDarkCharcoal
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = { value ->
                            if (value.isEmpty() || value == "-" || value.toIntOrNull() != null) {
                                if (value.length <= 3) tempInput = value
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
                            .testTag("profile_temp_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Weather Condition",
                        style = MaterialTheme.typography.labelLarge,
                        color = StylerMutedCharcoal
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StylerConstants.PROFILE_WEATHER_CONDITIONS.forEach { condition ->
                            val isSelected = selectedWeather == condition
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedWeather = condition },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                label = { Text(condition) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StylerDeepGreen,
                                    selectedLabelColor = StylerPureWhite,
                                    selectedLeadingIconColor = StylerPureWhite,
                                    containerColor = StylerLightSurface,
                                    labelColor = StylerDarkCharcoal
                                ),
                                modifier = Modifier.testTag("weather_condition_chip_${condition.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            onSaveProfile(
                                nameInput,
                                profileImageBase64,
                                liveHeight,
                                liveWeight,
                                liveTemp,
                                selectedWeather
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StylerAccentOrange,
                            contentColor = StylerPureWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Profile & Fit Metrics",
                            style = MaterialTheme.typography.titleSmall,
                            color = StylerPureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
