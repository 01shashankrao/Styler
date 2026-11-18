package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.OutfitComboEntity
import com.example.data.local.StylerConstants
import com.example.data.local.UserProfileEntity
import com.example.data.local.WardrobeItemEntity
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StylerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfileEntity> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserProfileEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfileEntity(
                    id = userId,
                    userId = userId,
                    name = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Styler Member"
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeWardrobeItems(userId: String): Flow<List<WardrobeItemEntity>> {
        val path = "users/$userId/wardrobe_items"
        return db.collection("users").document(userId).collection("wardrobe_items")
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(
                    WardrobeItemEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeOutfits(userId: String): Flow<List<OutfitComboEntity>> {
        val path = "users/$userId/outfits"
        return db.collection("users").document(userId).collection("outfits")
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(
                    OutfitComboEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                list.sortedWith(
                    compareByDescending<OutfitComboEntity> { it.pinnedToDashboard }
                        .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    /**
     * Initializes the user's profile document in Firestore on first sign-in without adding any
     * default wardrobe images (per user instruction: "dont add default images I'll add afterd downloding tthe app").
     */
    suspend fun ensureUserProfileInitialized(userId: String, defaultDisplayName: String?): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(userId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = (defaultDisplayName?.trim()?.takeIf { it.isNotEmpty() } ?: "Styler Member").take(100)
                val createPayload = mapOf(
                    "userId" to userId,
                    "name" to safeName,
                    "heightCm" to 178L,
                    "weightKg" to 74L,
                    "temperatureC" to 24L,
                    "weatherCondition" to "Pleasant",
                    "lastMissingPieceTip" to "Tip: Upload your shirts, hoodies, jackets, pullovers, pants, shoes, and perfumes to generate color-theory outfits.",
                    "profileImageBase64" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        name: String,
        profileImageBase64: String,
        heightCm: Int,
        weightKg: Int,
        temperatureC: Int,
        weatherCondition: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val safeName = name.trim().ifBlank { "Styler Member" }.take(100)
            val safeHeight = heightCm.coerceIn(100, 250).toLong()
            val safeWeight = weightKg.coerceIn(25, 250).toLong()
            val safeTemp = temperatureC.coerceIn(-30, 60).toLong()
            val safeWeather = if (weatherCondition in StylerConstants.PROFILE_WEATHER_CONDITIONS) {
                weatherCondition
            } else {
                "Pleasant"
            }
            val safeImg = profileImageBase64.take(340_000)

            val docRef = db.collection("users").document(userId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "name" to safeName,
                        "profileImageBase64" to safeImg,
                        "heightCm" to safeHeight,
                        "weightKg" to safeWeight,
                        "temperatureC" to safeTemp,
                        "weatherCondition" to safeWeather,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "name" to safeName,
                        "profileImageBase64" to safeImg,
                        "heightCm" to safeHeight,
                        "weightKg" to safeWeight,
                        "temperatureC" to safeTemp,
                        "weatherCondition" to safeWeather,
                        "lastMissingPieceTip" to "Tip: Upload your clothes and perfumes to unlock AI color-theory outfits.",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addWardrobeItem(
        userId: String,
        name: String,
        category: String,
        dominantColor: String,
        colorHex: String,
        undertone: String,
        styleNotes: String,
        imageBase64: String
    ): Result<String> {
        var docPath = "users/$userId/wardrobe_items"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val safeCategory = if (category in StylerConstants.WARDROBE_CATEGORIES) category else "Shirt / Top"
            val safeName = name.trim().ifBlank { safeCategory }.take(120)
            val safeColor = dominantColor.trim().ifBlank { "Forest Green" }.take(80)
            val safeHex = colorHex.trim().takeIf { it.length in 4..16 } ?: "#264A31"
            val safeUndertone = if (undertone in StylerConstants.UNDERTONE_OPTIONS) undertone else "Neutral"
            val safeNotes = styleNotes.trim().take(600)
            val safeImage = imageBase64.take(340_000)

            val docRef = db.collection("users").document(userId).collection("wardrobe_items").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to userId,
                "name" to safeName,
                "category" to safeCategory,
                "dominantColor" to safeColor,
                "colorHex" to safeHex,
                "undertone" to safeUndertone,
                "styleNotes" to safeNotes,
                "imageBase64" to safeImage,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteWardrobeItem(userId: String, itemId: String): Result<Unit> {
        val docPath = "users/$userId/wardrobe_items/$itemId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(itemId.isNotBlank()) { "Item ID must not be blank" }
            db.collection("users").document(userId)
                .collection("wardrobe_items").document(itemId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveGeneratedOutfits(
        userId: String,
        outfits: List<OutfitComboEntity>,
        missingPieceTip: String
    ): Result<Unit> {
        var currentPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            // Update missing piece tip on UserProfile if it exists
            val userDocRef = db.collection("users").document(userId)
            val userSnap = userDocRef.get().await()
            if (userSnap.exists()) {
                userDocRef.update(
                    mapOf(
                        "lastMissingPieceTip" to missingPieceTip.take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Save each generated outfit combo document
            for (outfit in outfits) {
                val docRef = db.collection("users").document(userId).collection("outfits").document()
                currentPath = docRef.path
                val payload = mapOf(
                    "userId" to userId,
                    "title" to outfit.title.trim().ifBlank { "Styled Look" }.take(140),
                    "occasion" to outfit.occasion.trim().ifBlank { "Casual Date" }.take(60),
                    "weatherVibe" to outfit.weatherVibe.trim().ifBlank { "Pleasant" }.take(60),
                    "temperatureC" to outfit.temperatureC.coerceIn(-30, 60).toLong(),
                    "itemIdsCsv" to outfit.itemIdsCsv.take(2000),
                    "stylingNote" to outfit.stylingNote.trim().ifBlank { "Tailored silhouette." }.take(1200),
                    "colorTheoryNote" to outfit.colorTheoryNote.trim().take(800),
                    "colorHarmonyScore" to outfit.colorHarmonyScore.coerceIn(0, 100).toLong(),
                    "pinnedToDashboard" to outfit.pinnedToDashboard,
                    "lastWornDate" to outfit.lastWornDate.take(30),
                    "missingPieceSuggestion" to missingPieceTip.take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun togglePinOutfit(userId: String, outfit: OutfitComboEntity): Result<Boolean> {
        val docPath = "users/$userId/outfits/${outfit.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(outfit.id.isNotBlank()) { "Outfit ID must not be blank" }
            val newPinned = !outfit.pinnedToDashboard
            db.collection("users").document(userId)
                .collection("outfits").document(outfit.id)
                .update(
                    mapOf(
                        "pinnedToDashboard" to newPinned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(newPinned)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markOutfitWornToday(userId: String, outfit: OutfitComboEntity): Result<String> {
        val docPath = "users/$userId/outfits/${outfit.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(outfit.id.isNotBlank()) { "Outfit ID must not be blank" }
            val todayStr = getTodayDateString()
            val updatedDate = if (outfit.lastWornDate == todayStr) "" else todayStr
            db.collection("users").document(userId)
                .collection("outfits").document(outfit.id)
                .update(
                    mapOf(
                        "lastWornDate" to updatedDate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(updatedDate)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun swapDashboardOutfit(
        userId: String,
        targetOutfit: OutfitComboEntity,
        allOutfits: List<OutfitComboEntity>
    ): Result<Unit> {
        var docPath = "users/$userId/outfits/${targetOutfit.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(targetOutfit.id.isNotBlank()) { "Outfit ID must not be blank" }

            val unpinnedCandidate = allOutfits.firstOrNull { !it.pinnedToDashboard && it.id != targetOutfit.id }
            db.collection("users").document(userId)
                .collection("outfits").document(targetOutfit.id)
                .update(
                    mapOf(
                        "pinnedToDashboard" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            if (unpinnedCandidate != null && unpinnedCandidate.id.isNotBlank()) {
                docPath = "users/$userId/outfits/${unpinnedCandidate.id}"
                db.collection("users").document(userId)
                    .collection("outfits").document(unpinnedCandidate.id)
                    .update(
                        mapOf(
                            "pinnedToDashboard" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteOutfit(userId: String, outfitId: String): Result<Unit> {
        val docPath = "users/$userId/outfits/$outfitId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(outfitId.isNotBlank()) { "Outfit ID must not be blank" }
            db.collection("users").document(userId)
                .collection("outfits").document(outfitId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
