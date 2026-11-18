package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.StylerConstants
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
import com.example.ui.theme.StylerSageGreen
import com.example.ui.theme.StylerSubtleBorder
import com.example.ui.viewmodel.NanoBananaImageUiState
import com.example.ui.viewmodel.VisionUiState
import com.example.util.ImageCompressor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UploadItemModal(
    visionUiState: VisionUiState,
    nanoBananaUiState: NanoBananaImageUiState,
    onAnalyzeWithVision: (Bitmap) -> Unit,
    onGenerateWithNanoBanana: (String) -> Unit,
    onEditWithNanoBanana: (Bitmap, String) -> Unit,
    onSaveItem: (
        name: String,
        category: String,
        dominantColor: String,
        colorHex: String,
        undertone: String,
        styleNotes: String,
        imageBase64: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCategory by remember { mutableStateOf(StylerConstants.WARDROBE_CATEGORIES.first()) }
    var itemName by remember { mutableStateOf("") }
    var dominantColor by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#264A31") }
    var selectedUndertone by remember { mutableStateOf("Neutral") }
    var styleNotes by remember { mutableStateOf("") }
    var compressionInfo by remember { mutableStateOf("No default image loaded • Upload, snap, or create with AI") }

    // Starts empty (no default image pre-loaded!)
    var compressedBase64 by remember { mutableStateOf("") }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // AI Create / Edit prompt inputs for gemini-nano-banana-2.1
    var aiPromptText by remember { mutableStateOf("") }
    var aiEditInstruction by remember { mutableStateOf("") }

    // Apply Gemini Nano Banana 2.1 generated or edited image when ready
    LaunchedEffect(nanoBananaUiState) {
        if (nanoBananaUiState is NanoBananaImageUiState.Success) {
            compressedBase64 = nanoBananaUiState.base64Data
            currentBitmap = nanoBananaUiState.bitmap
            compressionInfo = nanoBananaUiState.infoText
        }
    }

    // Apply Gemini Vision dress & color theory results when auto-detection finishes
    LaunchedEffect(visionUiState) {
        if (visionUiState is VisionUiState.Success) {
            val res = visionUiState.result
            selectedCategory = res.category
            itemName = res.name
            dominantColor = res.dominantColor
            colorHex = res.colorHex
            selectedUndertone = res.undertone
            styleNotes = res.styleNotes
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            ImageCompressor.compressFromUri(context, uri)?.let { compressed ->
                compressedBase64 = compressed.base64Data
                currentBitmap = compressed.bitmap
                compressionInfo = "Compressed: ${compressed.width}x${compressed.height}px • ${compressed.sizeKb} KB"
                onAnalyzeWithVision(compressed.bitmap)
            }
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val compressed = ImageCompressor.compressBitmap(bitmap)
            compressedBase64 = compressed.base64Data
            currentBitmap = compressed.bitmap
            compressionInfo = "Compressed: ${compressed.width}x${compressed.height}px • ${compressed.sizeKb} KB"
            onAnalyzeWithVision(compressed.bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPreviewLauncher.launch(null)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StylerCardSurface,
        modifier = Modifier.testTag("upload_item_modal")
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
                        text = "Add Wearable, Outerwear or Perfume",
                        style = MaterialTheme.typography.headlineMedium,
                        color = StylerDarkCharcoal
                    )
                    Text(
                        text = compressionInfo,
                        style = MaterialTheme.typography.labelMedium,
                        color = StylerSageGreen
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Upload Modal",
                        tint = StylerDarkCharcoal
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Image Preview + Camera / Gallery / Vision Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(StylerSageContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (compressedBase64.isNotBlank()) {
                        Base64Image(
                            base64Data = compressedBase64,
                            contentDescription = itemName.ifBlank { "Selected Wearable" },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = StylerDeepGreen,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Upload, Snap, or AI Create",
                                style = MaterialTheme.typography.labelSmall,
                                color = StylerDeepGreen,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                cameraPreviewLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StylerAccentOrange,
                            contentColor = StylerPureWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_camera_capture_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Camera", color = StylerPureWhite)
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        border = BorderStroke(1.dp, StylerDeepGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_gallery_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = StylerDeepGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick from Gallery", color = StylerDeepGreen)
                    }

                    OutlinedButton(
                        onClick = {
                            currentBitmap?.let { bmp -> onAnalyzeWithVision(bmp) }
                        },
                        enabled = currentBitmap != null,
                        border = BorderStroke(1.dp, StylerSageGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_vision_autodetect_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StylerAccentOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Vision Color Theory",
                            style = MaterialTheme.typography.labelLarge,
                            color = StylerDeepGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Gemini Nano Banana 2.1 AI Image Creation & Editing Studio
            Surface(
                color = StylerLightSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StylerSubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = null,
                            tint = StylerAccentOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Create & Edit Image (gemini-nano-banana-2.1)",
                            style = MaterialTheme.typography.titleSmall,
                            color = StylerDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = aiPromptText,
                        onValueChange = { aiPromptText = it },
                        label = { Text("Prompt to Create Item (e.g., Forest green hoodie, Oud perfume)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StylerDeepGreen,
                            focusedLabelColor = StylerDeepGreen
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nano_banana_create_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onGenerateWithNanoBanana(aiPromptText) },
                        enabled = nanoBananaUiState !is NanoBananaImageUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StylerDeepGreen,
                            contentColor = StylerPureWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nano_banana_create_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StylerAccentOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Image with Gemini Nano Banana 2.1", color = StylerPureWhite)
                    }

                    if (currentBitmap != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = aiEditInstruction,
                            onValueChange = { aiEditInstruction = it },
                            label = { Text("Edit Current Image (e.g., Change jacket color to charcoal)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StylerAccentOrange,
                                focusedLabelColor = StylerAccentOrange
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nano_banana_edit_prompt_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                currentBitmap?.let { bmp ->
                                    onEditWithNanoBanana(bmp, aiEditInstruction)
                                }
                            },
                            enabled = nanoBananaUiState !is NanoBananaImageUiState.Loading,
                            border = BorderStroke(1.dp, StylerAccentOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nano_banana_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = null,
                                tint = StylerAccentOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Current Image with AI", color = StylerAccentOrange)
                        }
                    }

                    when (nanoBananaUiState) {
                        is NanoBananaImageUiState.Loading -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = StylerAccentOrange,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = nanoBananaUiState.promptDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StylerDarkCharcoal
                                )
                            }
                        }
                        is NanoBananaImageUiState.Error -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = StylerErrorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = nanoBananaUiState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StylerErrorRed,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            }

            // 3. Gemini Vision Status Banner
            when (visionUiState) {
                is VisionUiState.Analyzing -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = StylerSageContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = StylerDeepGreen,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Gemini Vision is analyzing dress cut, undertone, and Color Theory pairings...",
                                style = MaterialTheme.typography.bodySmall,
                                color = StylerDeepGreen
                            )
                        }
                    }
                }
                is VisionUiState.Success -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = StylerOrangeContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI Dress & Color Analysis: ${visionUiState.result.category} • ${visionUiState.result.dominantColor} (${visionUiState.result.undertone} Undertone)",
                            style = MaterialTheme.typography.labelLarge,
                            color = StylerDarkCharcoal,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is VisionUiState.Error -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = StylerErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = visionUiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = StylerErrorRed,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                else -> Unit
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. All 15 Wearable, Outerwear & Perfume Categories
            Text(
                text = "Select Wearable or Perfume Category (15 Categories)",
                style = MaterialTheme.typography.titleSmall,
                color = StylerDarkCharcoal
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StylerConstants.WARDROBE_CATEGORIES.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else null,
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StylerDeepGreen,
                            selectedLabelColor = StylerPureWhite,
                            selectedLeadingIconColor = StylerPureWhite,
                            containerColor = StylerLightSurface,
                            labelColor = StylerDarkCharcoal
                        ),
                        modifier = Modifier.testTag("modal_category_${category.lowercase().replace(" ", "_")}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Color Wheel Undertone Selector
            Text(
                text = "Color Theory Undertone",
                style = MaterialTheme.typography.titleSmall,
                color = StylerDarkCharcoal
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StylerConstants.UNDERTONE_OPTIONS.forEach { tone ->
                    val isSelected = selectedUndertone == tone
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedUndertone = tone },
                        label = { Text("$tone Undertone") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StylerSageGreen,
                            selectedLabelColor = StylerPureWhite,
                            containerColor = StylerLightSurface,
                            labelColor = StylerDarkCharcoal
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Item Name (e.g., Olive Fleece Hoodie, Woody Amber Perfume)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StylerDeepGreen,
                    focusedLabelColor = StylerDeepGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_item_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dominantColor,
                    onValueChange = { dominantColor = it },
                    label = { Text("Dominant Colour / Scent") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StylerDeepGreen,
                        focusedLabelColor = StylerDeepGreen
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("upload_item_color_input")
                )

                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text("Hex (#RRGGBB)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StylerDeepGreen,
                        focusedLabelColor = StylerDeepGreen
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = styleNotes,
                onValueChange = { styleNotes = it },
                label = { Text("AI Dress Understanding & Color Theory Match Notes") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StylerDeepGreen,
                    focusedLabelColor = StylerDeepGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_item_notes_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    onSaveItem(
                        itemName.ifBlank { selectedCategory },
                        selectedCategory,
                        dominantColor.ifBlank { "Forest Green" },
                        colorHex.ifBlank { "#264A31" },
                        selectedUndertone,
                        styleNotes.ifBlank { "Versatile $selectedUndertone undertone piece." },
                        compressedBase64
                    )
                },
                enabled = compressedBase64.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StylerDeepGreen,
                    contentColor = StylerPureWhite
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_save_item_button")
            ) {
                Text(
                    text = if (compressedBase64.isNotBlank()) {
                        "Save to Cloud Wardrobe"
                    } else {
                        "Upload, Snap, or AI-Create an Image First"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = StylerPureWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
