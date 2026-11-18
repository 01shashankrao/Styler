package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.ai.StylerGeminiService
import com.example.data.ai.VisionTagResult
import com.example.data.local.OutfitComboEntity
import com.example.data.local.StylerConstants
import com.example.data.local.UserProfileEntity
import com.example.data.local.WardrobeItemEntity
import com.example.data.repository.StylerRepository
import com.example.util.ImageCompressor
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "StylerViewModel"
private const val STOP_TIMEOUT_MILLIS = 5_000L

enum class StylerTab(val route: String, val label: String) {
    DASHBOARD("dashboard", "Dashboard"),
    GALLERY("gallery", "Gallery"),
    STYLE_AI("style_ai", "Style AI"),
    PROFILE("profile", "Profile")
}

sealed interface VisionUiState {
    data object Idle : VisionUiState
    data object Analyzing : VisionUiState
    data class Success(val result: VisionTagResult) : VisionUiState
    data class Error(val message: String) : VisionUiState
}

sealed interface NanoBananaImageUiState {
    data object Idle : NanoBananaImageUiState
    data class Loading(val promptDescription: String) : NanoBananaImageUiState
    data class Success(val bitmap: Bitmap, val base64Data: String, val infoText: String) : NanoBananaImageUiState
    data class Error(val message: String) : NanoBananaImageUiState
}

sealed interface OutfitEngineUiState {
    data object Idle : OutfitEngineUiState
    data class Generating(val occasion: String, val weatherVibe: String) : OutfitEngineUiState
    data class Success(val count: Int, val occasion: String) : OutfitEngineUiState
    data class Error(val message: String) : OutfitEngineUiState
}

class StylerViewModel(
    private val repository: StylerRepository,
    private val geminiService: StylerGeminiService,
    private val currentUserId: String,
    defaultDisplayName: String?
) : ViewModel() {

    private val _currentTab = MutableStateFlow(StylerTab.DASHBOARD)
    val currentTab: StateFlow<StylerTab> = _currentTab.asStateFlow()

    private val _showUploadModal = MutableStateFlow(false)
    val showUploadModal: StateFlow<Boolean> = _showUploadModal.asStateFlow()

    private val _showQuestionnaireModal = MutableStateFlow(false)
    val showQuestionnaireModal: StateFlow<Boolean> = _showQuestionnaireModal.asStateFlow()

    private val _selectedGalleryCategory = MutableStateFlow("All")
    val selectedGalleryCategory: StateFlow<String> = _selectedGalleryCategory.asStateFlow()

    private val _visionUiState = MutableStateFlow<VisionUiState>(VisionUiState.Idle)
    val visionUiState: StateFlow<VisionUiState> = _visionUiState.asStateFlow()

    private val _nanoBananaUiState = MutableStateFlow<NanoBananaImageUiState>(NanoBananaImageUiState.Idle)
    val nanoBananaUiState: StateFlow<NanoBananaImageUiState> = _nanoBananaUiState.asStateFlow()

    private val _outfitEngineUiState = MutableStateFlow<OutfitEngineUiState>(OutfitEngineUiState.Idle)
    val outfitEngineUiState: StateFlow<OutfitEngineUiState> = _outfitEngineUiState.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    // Two-tier Kotlin Flow error handling on Firestore streams
    val userProfile: StateFlow<UserProfileEntity> = repository.observeUserProfile(currentUserId)
        .map { it }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", rootError)
            _statusBannerMessage.value = rootError.message ?: "Failed to load user profile."
            emit(UserProfileEntity(id = currentUserId, userId = currentUserId))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UserProfileEntity(id = currentUserId, userId = currentUserId)
        )

    val wardrobeItems: StateFlow<List<WardrobeItemEntity>> = repository.observeWardrobeItems(currentUserId)
        .map { it }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing wardrobe items", rootError)
            _statusBannerMessage.value = rootError.message ?: "Failed to observe wardrobe items."
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val filteredWardrobeItems: StateFlow<List<WardrobeItemEntity>> = combine(
        wardrobeItems,
        _selectedGalleryCategory
    ) { items, category ->
        if (category == "All") {
            items
        } else {
            val targetCat = StylerConstants.normalizeCategory(category)
            items.filter { StylerConstants.normalizeCategory(it.category).equals(targetCat, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val allOutfits: StateFlow<List<OutfitComboEntity>> = repository.observeOutfits(currentUserId)
        .map { it }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing outfits", rootError)
            _statusBannerMessage.value = rootError.message ?: "Failed to observe outfits."
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val dashboardQuickOutfits: StateFlow<List<OutfitComboEntity>> = allOutfits
        .map { outfits ->
            val pinned = outfits.filter { it.pinnedToDashboard }
            val unpinned = outfits.filter { !it.pinnedToDashboard }
            (pinned + unpinned).take(4)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val styleAiCombos: StateFlow<List<OutfitComboEntity>> = allOutfits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfileInitialized(currentUserId, defaultDisplayName)
        }
    }

    fun selectTab(tab: StylerTab) {
        _currentTab.value = tab
    }

    fun selectGalleryCategory(category: String) {
        _selectedGalleryCategory.value = category
    }

    fun openUploadModal() {
        _visionUiState.value = VisionUiState.Idle
        _nanoBananaUiState.value = NanoBananaImageUiState.Idle
        _showUploadModal.value = true
    }

    fun closeUploadModal() {
        _showUploadModal.value = false
        _visionUiState.value = VisionUiState.Idle
        _nanoBananaUiState.value = NanoBananaImageUiState.Idle
    }

    fun openQuestionnaireModal() {
        _showQuestionnaireModal.value = true
    }

    fun closeQuestionnaireModal() {
        _showQuestionnaireModal.value = false
    }

    fun clearBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun clearOutfitEngineError() {
        if (_outfitEngineUiState.value is OutfitEngineUiState.Error) {
            _outfitEngineUiState.value = OutfitEngineUiState.Idle
        }
    }

    fun saveUserProfile(
        name: String,
        profileImageBase64: String,
        heightCm: Int,
        weightKg: Int,
        temperatureC: Int,
        weatherCondition: String
    ) {
        viewModelScope.launch {
            val result = repository.updateProfile(
                userId = currentUserId,
                name = name,
                profileImageBase64 = profileImageBase64,
                heightCm = heightCm,
                weightKg = weightKg,
                temperatureC = temperatureC,
                weatherCondition = weatherCondition
            )
            result.onSuccess {
                _statusBannerMessage.value = "Profile & body frame metrics synced to Firestore."
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Failed to save profile."
            }
        }
    }

    /**
     * Generates a wearable or perfume image from a text prompt using gemini-nano-banana-2.1,
     * then automatically runs Gemini Vision to understand its dress style & color theory!
     */
    fun generateImageWithNanoBanana(promptText: String) {
        if (promptText.isBlank()) {
            _nanoBananaUiState.value = NanoBananaImageUiState.Error("Please enter a text prompt describing the wearable or perfume.")
            return
        }
        viewModelScope.launch {
            _nanoBananaUiState.value = NanoBananaImageUiState.Loading("Generating image with gemini-nano-banana-2.1 (may take 30-60s)...")
            try {
                val generatedBitmap = geminiService.generateWearableImageFromPrompt(promptText)
                val compressed = ImageCompressor.compressBitmap(generatedBitmap)
                _nanoBananaUiState.value = NanoBananaImageUiState.Success(
                    bitmap = compressed.bitmap,
                    base64Data = compressed.base64Data,
                    infoText = "Generated via gemini-nano-banana-2.1 (${compressed.width}x${compressed.height}px • ${compressed.sizeKb} KB)"
                )
                // Automatically understand the dress & color theory with Gemini Vision
                analyzeWardrobeImageWithVision(compressed.bitmap)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "gemini-nano-banana-2.1 generation timed out", e)
                _nanoBananaUiState.value = NanoBananaImageUiState.Error(
                    "Image generation timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "gemini-nano-banana-2.1 generation failed", e)
                _nanoBananaUiState.value = NanoBananaImageUiState.Error(
                    e.localizedMessage ?: "Image generation failed."
                )
            }
        }
    }

    /**
     * Edits the current wearable/perfume image using text instructions via gemini-nano-banana-2.1,
     * then re-analyzes its color theory with Gemini Vision!
     */
    fun editImageWithNanoBanana(inputBitmap: Bitmap, editInstruction: String) {
        if (editInstruction.isBlank()) {
            _nanoBananaUiState.value = NanoBananaImageUiState.Error("Please enter an edit instruction (e.g., 'Change colour to sage green').")
            return
        }
        viewModelScope.launch {
            _nanoBananaUiState.value = NanoBananaImageUiState.Loading("Editing image with gemini-nano-banana-2.1 (may take 30-60s)...")
            try {
                val editedBitmap = geminiService.editWearableImageWithPrompt(inputBitmap, editInstruction)
                val compressed = ImageCompressor.compressBitmap(editedBitmap)
                _nanoBananaUiState.value = NanoBananaImageUiState.Success(
                    bitmap = compressed.bitmap,
                    base64Data = compressed.base64Data,
                    infoText = "Edited via gemini-nano-banana-2.1 (${compressed.width}x${compressed.height}px • ${compressed.sizeKb} KB)"
                )
                analyzeWardrobeImageWithVision(compressed.bitmap)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "gemini-nano-banana-2.1 image edit timed out", e)
                _nanoBananaUiState.value = NanoBananaImageUiState.Error(
                    "Image editing timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "gemini-nano-banana-2.1 image edit failed", e)
                _nanoBananaUiState.value = NanoBananaImageUiState.Error(
                    e.localizedMessage ?: "Image editing failed."
                )
            }
        }
    }

    fun analyzeWardrobeImageWithVision(bitmap: Bitmap) {
        viewModelScope.launch {
            _visionUiState.value = VisionUiState.Analyzing
            try {
                val result = geminiService.detectWardrobeItemWithVision(bitmap)
                _visionUiState.value = VisionUiState.Success(result)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini Vision auto-detection timed out", e)
                _visionUiState.value = VisionUiState.Error(
                    "Gemini Vision analysis timed out after 60 seconds. You can retry or select the category manually."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Vision auto-detection failed", e)
                _visionUiState.value = VisionUiState.Error(
                    e.localizedMessage ?: "Gemini Vision could not analyze the image. You can still tag it manually."
                )
            }
        }
    }

    fun addWardrobeItem(
        name: String,
        category: String,
        dominantColor: String,
        colorHex: String,
        undertone: String,
        styleNotes: String,
        imageBase64: String
    ) {
        if (imageBase64.isBlank()) {
            _statusBannerMessage.value = "Please capture, upload, or AI-generate an image for this item before saving."
            return
        }
        viewModelScope.launch {
            val canonicalCategory = StylerConstants.normalizeCategory(category)
            val result = repository.addWardrobeItem(
                userId = currentUserId,
                name = name,
                category = canonicalCategory,
                dominantColor = dominantColor,
                colorHex = colorHex,
                undertone = undertone,
                styleNotes = styleNotes,
                imageBase64 = imageBase64
            )
            result.onSuccess {
                _showUploadModal.value = false
                _visionUiState.value = VisionUiState.Idle
                _nanoBananaUiState.value = NanoBananaImageUiState.Idle
                _statusBannerMessage.value = "Saved \"$name\" ($canonicalCategory) to your cloud wardrobe."
            }.onFailure { err ->
                _statusBannerMessage.value = err.localizedMessage ?: "Failed to save item."
            }
        }
    }

    fun deleteWardrobeItem(item: WardrobeItemEntity) {
        viewModelScope.launch {
            repository.deleteWardrobeItem(currentUserId, item.id)
                .onSuccess {
                    _statusBannerMessage.value = "Deleted \"${item.name}\" from wardrobe."
                }
                .onFailure { err ->
                    _statusBannerMessage.value = err.localizedMessage ?: "Failed to delete item."
                }
        }
    }

    fun generateFourAiOutfits(
        occasion: String,
        weatherVibe: String,
        temperatureC: Int
    ) {
        viewModelScope.launch {
            _showQuestionnaireModal.value = false
            _currentTab.value = StylerTab.STYLE_AI
            _outfitEngineUiState.value = OutfitEngineUiState.Generating(occasion, weatherVibe)
            try {
                val batch = geminiService.generateFourOutfits(
                    userId = currentUserId,
                    profile = userProfile.value,
                    wardrobeItems = wardrobeItems.value,
                    occasion = occasion,
                    weatherVibe = weatherVibe,
                    temperatureC = temperatureC
                )
                repository.saveGeneratedOutfits(currentUserId, batch.outfits, batch.missingPieceTip)
                    .onSuccess {
                        _outfitEngineUiState.value = OutfitEngineUiState.Success(
                            count = batch.outfits.size,
                            occasion = occasion
                        )
                        _statusBannerMessage.value = "Created 4 Color Theory outfits for $occasion ($weatherVibe)."
                    }
                    .onFailure { err ->
                        _outfitEngineUiState.value = OutfitEngineUiState.Error(
                            err.localizedMessage ?: "Failed to save generated outfits."
                        )
                    }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Outfit generation timed out", e)
                _outfitEngineUiState.value = OutfitEngineUiState.Error(
                    "AI Outfit generation timed out after 60 seconds. Please tap Retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Outfit generation failed", e)
                _outfitEngineUiState.value = OutfitEngineUiState.Error(
                    e.localizedMessage ?: "Failed to generate AI outfits. Please try again."
                )
            }
        }
    }

    fun regenerateOutfitFromDashboard(outfit: OutfitComboEntity) {
        generateFourAiOutfits(
            occasion = outfit.occasion,
            weatherVibe = outfit.weatherVibe,
            temperatureC = outfit.temperatureC
        )
    }

    fun togglePinToDashboard(outfit: OutfitComboEntity) {
        viewModelScope.launch {
            repository.togglePinOutfit(currentUserId, outfit)
                .onSuccess { newPinned ->
                    _statusBannerMessage.value = if (newPinned) {
                        "Pinned \"${outfit.title}\" to Dashboard Quick Choices."
                    } else {
                        "Unpinned \"${outfit.title}\" from Dashboard."
                    }
                }
        }
    }

    fun swapDashboardOutfit(outfit: OutfitComboEntity) {
        viewModelScope.launch {
            repository.swapDashboardOutfit(currentUserId, outfit, allOutfits.value)
                .onSuccess {
                    _statusBannerMessage.value = "Swapped \"${outfit.title}\" with next saved outfit."
                }
        }
    }

    fun markOutfitWornToday(outfit: OutfitComboEntity) {
        viewModelScope.launch {
            repository.markOutfitWornToday(currentUserId, outfit)
                .onSuccess { newDate ->
                    _statusBannerMessage.value = if (newDate.isNotEmpty()) {
                        "Marked \"${outfit.title}\" as Worn Today ($newDate)."
                    } else {
                        "Cleared Worn Today status for \"${outfit.title}\"."
                    }
                }
        }
    }

    fun deleteOutfit(outfit: OutfitComboEntity) {
        viewModelScope.launch {
            repository.deleteOutfit(currentUserId, outfit.id)
                .onSuccess {
                    _statusBannerMessage.value = "Deleted outfit \"${outfit.title}\"."
                }
        }
    }

    fun getTodayDateString(): String = repository.getTodayDateString()

    companion object {
        fun provideFactory(
            currentUserId: String,
            defaultDisplayName: String?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repository = StylerRepository(db)
                val geminiService = StylerGeminiService()
                StylerViewModel(
                    repository = repository,
                    geminiService = geminiService,
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName
                )
            }
        }
    }
}
