package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.OutfitComboEntity
import com.example.data.local.StylerConstants
import com.example.data.local.UserProfileEntity
import com.example.data.local.WardrobeItemEntity
import com.example.util.ImageCompressor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "StylerGeminiService"

data class VisionTagResult(
    val name: String,
    val category: String,
    val dominantColor: String,
    val colorHex: String,
    val undertone: String,
    val styleNotes: String
)

data class GeneratedOutfitsBatch(
    val outfits: List<OutfitComboEntity>,
    val missingPieceTip: String
)

class StylerGeminiService {

    private val visionSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Descriptive name of the wearable or perfume (e.g., Olive Fleece Hoodie, Cable-Knit Pullover, Woody Amber Eau de Parfum)"),
            "category" to Schema.enumeration(
                values = StylerConstants.WARDROBE_CATEGORIES,
                description = "Exact wearable or fragrance category from the 15 allowed categories"
            ),
            "dominantColor" to Schema.string(description = "Human-readable dominant color or scent tone (e.g., Deep Forest Green, Charcoal Heather, Warm Amber)"),
            "colorHex" to Schema.string(description = "6-digit hex color code starting with # representing the dominant color (e.g., #264A31)"),
            "undertone" to Schema.enumeration(
                values = StylerConstants.UNDERTONE_OPTIONS,
                description = "Color wheel undertone classification: Warm, Cool, or Neutral"
            ),
            "styleNotes" to Schema.string(description = "Dress/garment structure analysis, fabric/scent profile, and Color Theory pairing advice (Complementary, Analogous, Monochromatic matches)")
        )
    )

    private val outfitComboItemSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Distinctive editorial title for this complete outfit combination"),
            "selectedItemIds" to Schema.array(
                items = Schema.string(description = "Exact document ID string of a wardrobe item from the user's catalog"),
                description = "Array of wardrobe item IDs forming a complete outfit (Top + Jacket/Hoodie/Pullover/Sweatshirt layer + Pant + Shoe + Perfume + Accessories)"
            ),
            "stylingNote" to Schema.string(description = "AI Styling Note explaining why these silhouettes, layers, and fits flatter the user's height, weight, body frame, weather, and occasion"),
            "colorTheoryNote" to Schema.string(description = "Color Theory breakdown (Complementary, Analogous, Triadic, or Monochromatic palette harmony + how the Perfume / Fragrance complements the mood)"),
            "colorHarmonyScore" to Schema.integer(description = "Color wheel harmony score between 80 and 99")
        )
    )

    private val outfitResponseSchema = Schema.obj(
        properties = mapOf(
            "combos" to Schema.array(
                items = outfitComboItemSchema,
                description = "Exactly 4 distinct complete outfit combinations using the user's uploaded wardrobe items"
            ),
            "missingPieceTip" to Schema.string(
                description = "Single-line AI tip starting with 'Tip:' suggesting one missing wearable, outerwear layer, or perfume that would unlock more combinations"
            )
        )
    )

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visionSchema
            }
        )
    }

    private val outfitModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = outfitResponseSchema
            }
        )
    }

    // Minimal, safe zero-configuration baseline for gemini-nano-banana-2.1 image generation & editing
    private val nanoBananaImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    /**
     * Generates a wearable, garment, perfume, or outfit image from a text prompt using gemini-nano-banana-2.1.
     */
    suspend fun generateWearableImageFromPrompt(promptText: String): Bitmap {
        val enhancedPrompt = """
            Studio flat-lay fashion product photography on a clean warm cream surface (#F7F8F6):
            $promptText.
            Crisp natural lighting, high-detail textile and material texture, centered composition.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            nanoBananaImageModel.generateContent(enhancedPrompt)
        }
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
        return imagePart?.image ?: throw IllegalStateException("No image was returned by gemini-nano-banana-2.1.")
    }

    /**
     * Edits an existing wearable or outfit Bitmap using text instructions via gemini-nano-banana-2.1.
     */
    suspend fun editWearableImageWithPrompt(
        inputBitmap: Bitmap,
        editInstruction: String
    ): Bitmap {
        val response = withTimeout(60_000L) {
            nanoBananaImageModel.generateContent(
                content {
                    image(inputBitmap)
                    text(editInstruction)
                }
            )
        }
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
        return imagePart?.image ?: throw IllegalStateException("No edited image was returned by gemini-nano-banana-2.1.")
    }

    /**
     * Understands the uploaded or generated dress/wearable/perfume image using Gemini Vision and Color Theory.
     */
    suspend fun detectWardrobeItemWithVision(bitmap: Bitmap): VisionTagResult {
        val response = withTimeout(60_000L) {
            visionModel.generateContent(
                content {
                    image(bitmap)
                    text(
                        """
                        Analyze this wearable, outerwear piece, accessory, or perfume bottle image.
                        1. Classify it into one of these exact 15 categories:
                           ${StylerConstants.WARDROBE_CATEGORIES.joinToString(", ")}.
                        2. Identify its descriptive item title, dominant color name, accurate #RRGGBB hex code, and color wheel undertone (Warm, Cool, Neutral).
                        3. In 'styleNotes', explain the garment's cut/fabric (or fragrance vibe if Perfume) AND how to pair it using Color Theory (Complementary, Analogous, or Monochromatic matches).
                        """.trimIndent()
                    )
                }
            )
        }

        val jsonText = response.text ?: throw IllegalStateException("Gemini Vision returned an empty response.")
        val json = JSONObject(jsonText)
        val rawCategory = json.optString("category", "Shirt / Top")
        val validCategory = StylerConstants.normalizeCategory(rawCategory)
        val rawHex = json.optString("colorHex", "#264A31").trim()
        val validHex = if (rawHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) rawHex else "#264A31"
        val rawUndertone = json.optString("undertone", "Neutral")
        val validUndertone = if (rawUndertone in StylerConstants.UNDERTONE_OPTIONS) rawUndertone else "Neutral"

        return VisionTagResult(
            name = json.optString("name", "Curated $validCategory").take(110).ifBlank { "Curated $validCategory" },
            category = validCategory,
            dominantColor = json.optString("dominantColor", "Forest Green").take(70).ifBlank { "Forest Green" },
            colorHex = validHex,
            undertone = validUndertone,
            styleNotes = json.optString(
                "styleNotes",
                "Balanced $validUndertone undertone piece; pairs harmoniously with earth neutrals and analogous tones."
            ).take(550)
        )
    }

    /**
     * Generates 4 Complete Outfit Combinations using Color Theory, Layering (Jacket/Hoodie/Pullover/Sweatshirt),
     * Perfume pairing, and Smart Occasion Rules.
     */
    suspend fun generateFourOutfits(
        userId: String,
        profile: UserProfileEntity,
        wardrobeItems: List<WardrobeItemEntity>,
        occasion: String,
        weatherVibe: String,
        temperatureC: Int
    ): GeneratedOutfitsBatch {
        if (wardrobeItems.isEmpty()) {
            throw IllegalStateException("Your wardrobe gallery is empty. Tap the orange Camera/AI button to upload or generate your clothes and perfumes first!")
        }

        val bodyFrame = StylerConstants.calculateBodyFrame(profile.heightCm, profile.weightKg)
        val isAthleticOccasion = occasion.equals("Gym", ignoreCase = true) || occasion.equals("Morning Jog", ignoreCase = true)
        val isTraditionalOccasion = occasion.equals("Temple", ignoreCase = true) || occasion.equals("Festival", ignoreCase = true)

        val catalogDescription = wardrobeItems.joinToString("\n") { item ->
            val canonicalCategory = StylerConstants.normalizeCategory(item.category)
            "- ID=\"${item.id}\" | Category=\"$canonicalCategory\" | Name=\"${item.name}\" | Color=\"${item.dominantColor}\" (${item.colorHex}, Undertone=${item.undertone}) | Dress/Color Notes=\"${item.styleNotes}\""
        }

        val sampleBitmaps = wardrobeItems.take(6).mapNotNull { item ->
            ImageCompressor.decodeBase64ToBitmap(item.imageBase64)?.let { bmp -> item to bmp }
        }

        val promptText = """
            You are Styler, an elite AI fashion director, master of Color Theory, and fragrance curator.
            Create 4 distinct, complete Outfit Combinations using the user's actual uploaded wardrobe items below.

            USER METRICS & SILHOUETTE PROFILE:
            - Name: ${profile.name}
            - Height: ${profile.heightCm} cm
            - Weight: ${profile.weightKg} kg
            - Calculated Body Frame & Fit Tag: ${bodyFrame.tag} (BMI ${bodyFrame.bmi})
            - Silhouette Guidance: ${bodyFrame.silhouetteRecommendation}

            TARGET OCCASION & WEATHER:
            - Occasion: $occasion
            - Current Weather / Vibe: $weatherVibe ($temperatureC°C)

            COLOR THEORY & COMPLETE OUTFIT RULES (CRITICAL):
            1. Build a COMPLETE outfit from the user's wardrobe: pair Tops (Shirt / Top) + Outerwear/Mid-Layers (Jacket, Hoodie, Pullover, Sweatshirt — especially in Cold, Pleasant, Rainy, or Evening occasions) + Bottoms (Pant / Bottom) + Footwear (Shoe / Footwear) + Signature Scent (Perfume, if uploaded) + Accessories (Belt, Watch, Shades (Sunglasses), Bracelet, V-Chain (Necklace), Ring, Cap / Hat).
            2. Apply strict Color Theory to every outfit: balance Warm/Cool/Neutral undertones using Complementary contrast, Analogous earth/botanical harmony, Monochromatic vertical lengthening, or Triadic balance. Explain this explicitly in 'colorTheoryNote' (and include how the selected Perfume complements the color mood and occasion).
            3. ${if (isAthleticOccasion) "SMART OCCASION RULE (ATHLETIC): Since the occasion is '$occasion', automatically OMIT formal belts ('Belt'), rings ('Ring'), and heavy chains ('V-Chain (Necklace)'). Prioritize breathable tops, hoodies/sweatshirts, athletic pants, sneakers, and clean/fresh perfumes." else "Include refined layering pieces, accessories, and signature perfumes that elevate '$occasion'."}
            4. ${if (isTraditionalOccasion) "SMART OCCASION RULE (CULTURAL/MODEST): Since the occasion is '$occasion', prioritize ethnic, modest, warm, and harmonious colour palettes." else "Match formality and fabric weight to '$occasion' and '$weatherVibe' ($temperatureC°C)."}
            5. Provide a single-line 'missingPieceTip' starting with 'Tip: ' suggesting 1 specific wearable, jacket/hoodie/pullover/sweatshirt, or perfume that would unlock even more '$occasion' combinations.

            AVAILABLE WARDROBE ITEMS (USE ONLY THESE EXACT STRING IDs in selectedItemIds):
            $catalogDescription
        """.trimIndent()

        val response = withTimeout(60_000L) {
            outfitModel.generateContent(
                content {
                    sampleBitmaps.forEach { (item, bmp) ->
                        image(bmp)
                        text("Wardrobe Item ID=\"${item.id}\": ${item.name} (${StylerConstants.normalizeCategory(item.category)}, ${item.dominantColor}, ${item.undertone} undertone)")
                    }
                    text(promptText)
                }
            )
        }

        val jsonText = response.text ?: throw IllegalStateException("Gemini AI Outfit Engine returned an empty response.")
        Log.d(TAG, "Received structured Color Theory outfit JSON from Gemini")
        val json = JSONObject(jsonText)
        val combosArray = json.getJSONArray("combos")
        val missingPieceTip = json.optString(
            "missingPieceTip",
            "Tip: Adding a versatile neutral jacket or woody amber perfume would unlock 3 more $occasion combinations."
        ).let { tip ->
            if (tip.startsWith("Tip:", ignoreCase = true)) tip else "Tip: $tip"
        }.take(480)

        val itemsById = wardrobeItems.associateBy { it.id }
        val forbiddenAthleticCategories = setOf("Belt", "Ring", "V-Chain (Necklace)")

        val generatedList = mutableListOf<OutfitComboEntity>()
        val count = combosArray.length().coerceAtMost(4)

        for (i in 0 until count) {
            val comboObj = combosArray.getJSONObject(i)
            val title = comboObj.optString("title", "$occasion Look #${i + 1}").take(130).ifBlank { "$occasion Look #${i + 1}" }
            val stylingNote = comboObj.optString(
                "stylingNote",
                "Tailored proportions for ${profile.heightCm}cm / ${profile.weightKg}kg (${bodyFrame.shortLabel}) in $weatherVibe weather."
            ).take(1100).ifBlank { "Tailored silhouette for $occasion." }
            val colorTheoryNote = comboObj.optString(
                "colorTheoryNote",
                "Harmonious undertone pairing balancing dominant and accent wardrobe hues with occasion-matched fragrance."
            ).take(750)
            val score = comboObj.optInt("colorHarmonyScore", 92 - i * 2).coerceIn(70, 99)

            val rawIdsArray = comboObj.optJSONArray("selectedItemIds")
            val validSelectedIds = mutableListOf<String>()
            if (rawIdsArray != null) {
                for (j in 0 until rawIdsArray.length()) {
                    val id = rawIdsArray.optString(j, "").trim()
                    val item = itemsById[id]
                    if (item != null) {
                        val normalizedCat = StylerConstants.normalizeCategory(item.category)
                        if (isAthleticOccasion && normalizedCat in forbiddenAthleticCategories) {
                            continue
                        }
                        if (id !in validSelectedIds) {
                            validSelectedIds.add(id)
                        }
                    }
                }
            }

            val finalIds = ensureCompleteOutfitSelection(
                selectedIds = validSelectedIds,
                allItems = wardrobeItems,
                comboIndex = i,
                isAthleticOccasion = isAthleticOccasion
            )

            generatedList.add(
                OutfitComboEntity(
                    userId = userId,
                    title = title,
                    occasion = occasion,
                    weatherVibe = weatherVibe,
                    temperatureC = temperatureC,
                    itemIdsCsv = finalIds.joinToString(","),
                    stylingNote = stylingNote,
                    colorTheoryNote = colorTheoryNote,
                    colorHarmonyScore = score,
                    pinnedToDashboard = i < 2,
                    lastWornDate = "",
                    missingPieceSuggestion = missingPieceTip
                )
            )
        }

        return GeneratedOutfitsBatch(
            outfits = generatedList,
            missingPieceTip = missingPieceTip
        )
    }

    private fun ensureCompleteOutfitSelection(
        selectedIds: List<String>,
        allItems: List<WardrobeItemEntity>,
        comboIndex: Int,
        isAthleticOccasion: Boolean
    ): List<String> {
        val result = selectedIds.toMutableList()
        val itemsById = allItems.associateBy { it.id }
        val selectedCategories = result
            .mapNotNull { itemsById[it]?.category?.let(StylerConstants::normalizeCategory) }
            .toMutableSet()

        // Core upper wear (Shirt / Top or Jacket / Hoodie / Pullover / Sweatshirt)
        if (selectedCategories.intersect(StylerConstants.UPPER_BODY_CATEGORIES).isEmpty()) {
            val uppers = allItems.filter { StylerConstants.normalizeCategory(it.category) in StylerConstants.UPPER_BODY_CATEGORIES }
            if (uppers.isNotEmpty()) {
                val pick = uppers[comboIndex % uppers.size]
                result.add(pick.id)
                selectedCategories.add(StylerConstants.normalizeCategory(pick.category))
            }
        }

        // Core lower & footwear + Perfume if available in wardrobe
        listOf("Pant / Bottom", "Shoe / Footwear", "Perfume").forEach { category ->
            if (category !in selectedCategories) {
                val candidates = allItems.filter { StylerConstants.normalizeCategory(it.category) == category }
                if (candidates.isNotEmpty()) {
                    val pick = candidates[comboIndex % candidates.size]
                    result.add(pick.id)
                    selectedCategories.add(category)
                }
            }
        }

        // Add outerwear layer if user has Jacket/Hoodie/Pullover/Sweatshirt and only a Shirt was picked
        if (selectedCategories.intersect(StylerConstants.OUTERWEAR_LAYER_CATEGORIES).isEmpty()) {
            val layers = allItems.filter { StylerConstants.normalizeCategory(it.category) in StylerConstants.OUTERWEAR_LAYER_CATEGORIES }
            if (layers.isNotEmpty()) {
                val pick = layers[comboIndex % layers.size]
                if (pick.id !in result) {
                    result.add(pick.id)
                    selectedCategories.add(StylerConstants.normalizeCategory(pick.category))
                }
            }
        }

        if (result.isEmpty() && allItems.isNotEmpty()) {
            result.addAll(allItems.take(4).map { it.id })
        }

        return result.distinct()
    }
}
