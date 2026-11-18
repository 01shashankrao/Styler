package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.StylerConstants
import com.example.data.local.WardrobeItemEntity
import com.example.ui.components.Base64Image
import com.example.ui.components.ColorSwatchDot
import com.example.ui.theme.StylerAccentOrange
import com.example.ui.theme.StylerCardSurface
import com.example.ui.theme.StylerDarkCharcoal
import com.example.ui.theme.StylerDeepGreen
import com.example.ui.theme.StylerErrorContainer
import com.example.ui.theme.StylerErrorRed
import com.example.ui.theme.StylerLightSurface
import com.example.ui.theme.StylerMutedCharcoal
import com.example.ui.theme.StylerPureWhite
import com.example.ui.theme.StylerSageContainer
import com.example.ui.theme.StylerSubtleBorder

@Composable
fun GalleryScreen(
    allItems: List<WardrobeItemEntity>,
    filteredItems: List<WardrobeItemEntity>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onOpenUploadModal: () -> Unit,
    onDeleteItem: (WardrobeItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryCounts = remember(allItems) {
        allItems.groupingBy { it.category }.eachCount()
    }
    val filterCategories = remember {
        listOf("All") + StylerConstants.WARDROBE_CATEGORIES
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier
            .fillMaxSize()
            .background(StylerLightSurface)
            .testTag("gallery_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wardrobe & Fragrance Gallery",
                            style = MaterialTheme.typography.headlineLarge,
                            color = StylerDarkCharcoal
                        )
                        Text(
                            text = "${allItems.size} items synced in Cloud Firestore • Shirts, Jackets, Hoodies, Pullovers, Sweatshirts, Perfumes & more",
                            style = MaterialTheme.typography.bodySmall,
                            color = StylerMutedCharcoal
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenUploadModal,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StylerAccentOrange,
                            contentColor = StylerPureWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("gallery_upload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Add Wearable or Perfume",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Piece",
                            style = MaterialTheme.typography.labelLarge,
                            color = StylerPureWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    filterCategories.forEach { category ->
                        val count = if (category == "All") allItems.size else (categoryCounts[category] ?: 0)
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCategory(category) },
                            label = {
                                Text(
                                    text = "$category ($count)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StylerDeepGreen,
                                selectedLabelColor = StylerPureWhite,
                                containerColor = StylerCardSurface,
                                labelColor = StylerDarkCharcoal
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = StylerSubtleBorder,
                                selectedBorderColor = StylerDeepGreen
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StylerCardSurface),
                    border = BorderStroke(1.dp, StylerSubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checkroom,
                            contentDescription = null,
                            tint = StylerDeepGreen,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedCategory == "All") {
                                "Your Wardrobe Starts Fresh & Empty"
                            } else {
                                "No items in \"$selectedCategory\" yet"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = StylerDarkCharcoal,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No default images are pre-loaded. Tap 'Add Piece' or the orange Camera button to upload your own shirts, jackets, hoodies, pullovers, sweatshirts, pants, shoes, or perfumes — or generate & edit them with Gemini Nano Banana 2.1!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StylerMutedCharcoal,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenUploadModal,
                            colors = ButtonDefaults.buttonColors(containerColor = StylerAccentOrange)
                        ) {
                            Text("Upload or AI-Create First Item", color = StylerPureWhite)
                        }
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                WardrobeItemCard(
                    item = item,
                    onDelete = { onDeleteItem(item) }
                )
            }
        }
    }
}

@Composable
private fun WardrobeItemCard(
    item: WardrobeItemEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StylerCardSurface),
        border = BorderStroke(1.dp, StylerSubtleBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("wardrobe_item_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Base64Image(
                    base64Data = item.imageBase64,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = StylerDeepGreen.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = StylerPureWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = StylerSageContainer.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${item.undertone} Tone",
                        style = MaterialTheme.typography.labelSmall,
                        color = StylerDeepGreen,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = StylerDarkCharcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorSwatchDot(hexString = item.colorHex)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.dominantColor,
                    style = MaterialTheme.typography.labelMedium,
                    color = StylerMutedCharcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.styleNotes,
                style = MaterialTheme.typography.bodySmall,
                color = StylerMutedCharcoal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(32.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = StylerErrorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDelete() }
                    .testTag("delete_item_${item.id}")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Item",
                        tint = StylerErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Delete Item",
                        style = MaterialTheme.typography.labelLarge,
                        color = StylerErrorRed
                    )
                }
            }
        }
    }
}
