package com.example.data.local

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.pow
import kotlin.math.roundToInt

object StylerConstants {
    val WARDROBE_CATEGORIES = listOf(
        "Shirt / Top",
        "Jacket",
        "Hoodie",
        "Pullover",
        "Sweatshirt",
        "Pant / Bottom",
        "Shoe / Footwear",
        "Perfume",
        "Belt",
        "Watch",
        "Shades (Sunglasses)",
        "Bracelet",
        "V-Chain (Necklace)",
        "Ring",
        "Cap / Hat"
    )

    val OUTERWEAR_LAYER_CATEGORIES = setOf(
        "Jacket",
        "Hoodie",
        "Pullover",
        "Sweatshirt"
    )

    val UPPER_BODY_CATEGORIES = setOf(
        "Shirt / Top",
        "Jacket",
        "Hoodie",
        "Pullover",
        "Sweatshirt"
    )

    val UNDERTONE_OPTIONS = listOf(
        "Warm",
        "Cool",
        "Neutral"
    )

    val PROFILE_WEATHER_CONDITIONS = listOf(
        "Sunny",
        "Humid",
        "Rainy",
        "Pleasant",
        "Chilly"
    )

    val QUESTIONNAIRE_OCCASIONS = listOf(
        "Gym",
        "Casual Date",
        "Lunch",
        "Dinner",
        "Morning Jog",
        "Night Out",
        "Formal Event",
        "Festival",
        "Temple"
    )

    val QUESTIONNAIRE_WEATHER_VIBES = listOf(
        "Hot",
        "Humid",
        "Rainy",
        "Pleasant",
        "Cold"
    )

    fun normalizeCategory(rawCategory: String): String {
        val trimmed = rawCategory.trim()
        if (trimmed in WARDROBE_CATEGORIES) return trimmed
        return when {
            trimmed.contains("Jacket", ignoreCase = true) || trimmed.contains("Blazer", ignoreCase = true) || trimmed.contains("Coat", ignoreCase = true) -> "Jacket"
            trimmed.contains("Hoodie", ignoreCase = true) || trimmed.contains("Hoddy", ignoreCase = true) -> "Hoodie"
            trimmed.contains("Pullover", ignoreCase = true) || trimmed.contains("Sweater", ignoreCase = true) || trimmed.contains("Knit", ignoreCase = true) -> "Pullover"
            trimmed.contains("Sweatshirt", ignoreCase = true) || trimmed.contains("Crewneck", ignoreCase = true) -> "Sweatshirt"
            trimmed.contains("Perfume", ignoreCase = true) || trimmed.contains("Fragrance", ignoreCase = true) || trimmed.contains("Cologne", ignoreCase = true) -> "Perfume"
            trimmed.contains("Shirt", ignoreCase = true) || trimmed.contains("Top", ignoreCase = true) || trimmed.contains("Tee", ignoreCase = true) -> "Shirt / Top"
            trimmed.contains("Pant", ignoreCase = true) || trimmed.contains("Bottom", ignoreCase = true) || trimmed.contains("Jean", ignoreCase = true) || trimmed.contains("Trouser", ignoreCase = true) -> "Pant / Bottom"
            trimmed.contains("Shoe", ignoreCase = true) || trimmed.contains("Footwear", ignoreCase = true) || trimmed.contains("Sneaker", ignoreCase = true) || trimmed.contains("Boot", ignoreCase = true) -> "Shoe / Footwear"
            trimmed.contains("Belt", ignoreCase = true) -> "Belt"
            trimmed.contains("Watch", ignoreCase = true) -> "Watch"
            trimmed.contains("Shade", ignoreCase = true) || trimmed.contains("Sunglass", ignoreCase = true) -> "Shades (Sunglasses)"
            trimmed.contains("Bracelet", ignoreCase = true) -> "Bracelet"
            trimmed.contains("Chain", ignoreCase = true) || trimmed.contains("Necklace", ignoreCase = true) -> "V-Chain (Necklace)"
            trimmed.contains("Ring", ignoreCase = true) -> "Ring"
            trimmed.contains("Cap", ignoreCase = true) || trimmed.contains("Hat", ignoreCase = true) -> "Cap / Hat"
            else -> WARDROBE_CATEGORIES.first()
        }
    }

    fun mapProfileWeatherToQuestionnaireVibe(condition: String, temperatureC: Int): String {
        return when (condition) {
            "Sunny" -> if (temperatureC >= 28) "Hot" else "Pleasant"
            "Humid" -> "Humid"
            "Rainy" -> "Rainy"
            "Chilly" -> "Cold"
            "Pleasant" -> "Pleasant"
            else -> if (condition in QUESTIONNAIRE_WEATHER_VIBES) condition else "Pleasant"
        }
    }

    data class BodyFrameAnalysis(
        val tag: String,
        val shortLabel: String,
        val bmi: Double,
        val silhouetteRecommendation: String
    )

    fun calculateBodyFrame(heightCm: Int, weightKg: Int): BodyFrameAnalysis {
        val safeHeightM = (heightCm.coerceIn(100, 250)) / 100.0
        val safeWeight = weightKg.coerceIn(25, 250)
        val bmi = (safeWeight / safeHeightM.pow(2.0) * 10.0).roundToInt() / 10.0

        val heightNote = when {
            heightCm < 168 -> "Monochromatic vertical lines & cropped jackets/hoodies elongate your frame."
            heightCm > 183 -> "Layered textures (pullovers, sweatshirts, jackets), contrast belts, and cuffed trousers balance your tall stature."
            else -> "Mid-rise tapered trousers, structured jackets, and clean pullovers highlight balanced proportions."
        }

        return when {
            bmi < 19.5 -> BodyFrameAnalysis(
                tag = "Lean Frame • Layered Structured Fit",
                shortLabel = "Lean",
                bmi = bmi,
                silhouetteRecommendation = "Opt for heavyweight hoodies, textured pullovers, jackets, and structured collars to add dimension. $heightNote"
            )
            bmi < 24.2 -> BodyFrameAnalysis(
                tag = "Athletic Frame • Tailored Tapered Fit",
                shortLabel = "Athletic",
                bmi = bmi,
                silhouetteRecommendation = "Fitted shirts, sweatshirts, and tailored jackets through the chest with tapered chinos emphasize an athletic V-taper. $heightNote"
            )
            bmi < 27.5 -> BodyFrameAnalysis(
                tag = "Balanced Frame • Classic Regular Fit",
                shortLabel = "Balanced",
                bmi = bmi,
                silhouetteRecommendation = "Clean straight-cut trousers, structured jackets, and breathable pullovers offer effortless versatility. $heightNote"
            )
            else -> BodyFrameAnalysis(
                tag = "Broad Frame • Relaxed Vertical Fit",
                shortLabel = "Broad",
                bmi = bmi,
                silhouetteRecommendation = "Unstructured open jackets, matte dark-toned pullovers, and vertical plackets create a sleek silhouette. $heightNote"
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfileEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Styler Member",
    val profileImageBase64: String = "",
    val heightCm: Int = 178,
    val weightKg: Int = 74,
    val temperatureC: Int = 24,
    val weatherCondition: String = "Pleasant",
    val lastMissingPieceTip: String = "Tip: Upload your shirts, hoodies, jackets, pullovers, sweatshirts, pants, shoes, and perfumes so Gemini can craft color-theory outfits.",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WardrobeItemEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Shirt / Top",
    val dominantColor: String = "",
    val colorHex: String = "#264A31",
    val undertone: String = "Neutral",
    val styleNotes: String = "",
    val imageBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OutfitComboEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val occasion: String = "Casual Date",
    val weatherVibe: String = "Pleasant",
    val temperatureC: Int = 24,
    /** Comma-separated list of WardrobeItemEntity Firestore document IDs */
    val itemIdsCsv: String = "",
    val stylingNote: String = "",
    val colorTheoryNote: String = "",
    val colorHarmonyScore: Int = 90,
    val pinnedToDashboard: Boolean = false,
    /** Format: YYYY-MM-DD when marked as worn today, or empty string */
    val lastWornDate: String = "",
    val missingPieceSuggestion: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedItemIds(): List<String> {
        if (itemIdsCsv.isBlank()) return emptyList()
        return itemIdsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
