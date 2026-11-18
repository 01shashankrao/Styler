package com.example.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.StyleQuestionnaireModal
import com.example.ui.components.UploadItemModal
import com.example.ui.theme.StylerAccentOrange
import com.example.ui.theme.StylerCardSurface
import com.example.ui.theme.StylerDarkCharcoal
import com.example.ui.theme.StylerDeepGreen
import com.example.ui.theme.StylerDeepGreenContainer
import com.example.ui.theme.StylerLightSurface
import com.example.ui.theme.StylerMutedCharcoal
import com.example.ui.theme.StylerPureWhite
import com.example.ui.viewmodel.StylerTab
import com.example.ui.viewmodel.StylerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

@Composable
fun StylerAppNavigation() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var showSplash by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        // Key composition and ViewModel by uid so signing out or switching accounts disposes old listeners cleanly
        key(uid) {
            val stylerViewModel: StylerViewModel = viewModel(
                key = uid,
                factory = StylerViewModel.provideFactory(
                    currentUserId = uid,
                    defaultDisplayName = user.displayName
                )
            )
            if (showSplash) {
                val wardrobeItems by stylerViewModel.wardrobeItems.collectAsStateWithLifecycle()
                val styleAiCombos by stylerViewModel.styleAiCombos.collectAsStateWithLifecycle()
                SplashScreen(
                    wardrobeCount = wardrobeItems.size,
                    savedOutfitCount = styleAiCombos.size,
                    onEnterDashboard = { showSplash = false }
                )
            } else {
                StylerAuthenticatedShell(viewModel = stylerViewModel)
            }
        }
    }
}

@Composable
fun StylerAuthenticatedShell(
    viewModel: StylerViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val showUploadModal by viewModel.showUploadModal.collectAsStateWithLifecycle()
    val showQuestionnaireModal by viewModel.showQuestionnaireModal.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val wardrobeItems by viewModel.wardrobeItems.collectAsStateWithLifecycle()
    val filteredWardrobeItems by viewModel.filteredWardrobeItems.collectAsStateWithLifecycle()
    val selectedGalleryCategory by viewModel.selectedGalleryCategory.collectAsStateWithLifecycle()
    val dashboardQuickOutfits by viewModel.dashboardQuickOutfits.collectAsStateWithLifecycle()
    val styleAiCombos by viewModel.styleAiCombos.collectAsStateWithLifecycle()
    val visionUiState by viewModel.visionUiState.collectAsStateWithLifecycle()
    val nanoBananaUiState by viewModel.nanoBananaUiState.collectAsStateWithLifecycle()
    val outfitEngineUiState by viewModel.outfitEngineUiState.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    BackHandler(enabled = currentTab != StylerTab.DASHBOARD) {
        viewModel.selectTab(StylerTab.DASHBOARD)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = StylerLightSurface,
        topBar = {
            StylerTopBar(
                currentTab = currentTab,
                onOpenStyleQuestionnaire = { viewModel.openQuestionnaireModal() },
                onSignOut = {
                    signOutStylerUser(credentialManager, scope)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openUploadModal() },
                containerColor = StylerAccentOrange,
                contentColor = StylerPureWhite,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier.testTag("fab_camera_upload")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Camera, Upload, or AI Create Wearable",
                        tint = StylerPureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Camera / AI",
                        style = MaterialTheme.typography.labelLarge,
                        color = StylerPureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        bottomBar = {
            StylerBottomNavBar(
                currentTab = currentTab,
                onSelectTab = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                StylerTab.DASHBOARD -> {
                    DashboardScreen(
                        userProfile = userProfile,
                        wardrobeItems = wardrobeItems,
                        quickOutfits = dashboardQuickOutfits,
                        todayDate = viewModel.getTodayDateString(),
                        onOpenQuestionnaire = { viewModel.openQuestionnaireModal() },
                        onOpenUploadModal = { viewModel.openUploadModal() },
                        onNavigateToProfile = { viewModel.selectTab(StylerTab.PROFILE) },
                        onTogglePin = { viewModel.togglePinToDashboard(it) },
                        onMarkWornToday = { viewModel.markOutfitWornToday(it) },
                        onSwapOutfit = { viewModel.swapDashboardOutfit(it) },
                        onRegenerateOutfit = { viewModel.regenerateOutfitFromDashboard(it) },
                        onDeleteOutfit = { viewModel.deleteOutfit(it) }
                    )
                }
                StylerTab.GALLERY -> {
                    GalleryScreen(
                        allItems = wardrobeItems,
                        filteredItems = filteredWardrobeItems,
                        selectedCategory = selectedGalleryCategory,
                        onSelectCategory = { viewModel.selectGalleryCategory(it) },
                        onOpenUploadModal = { viewModel.openUploadModal() },
                        onDeleteItem = { viewModel.deleteWardrobeItem(it) }
                    )
                }
                StylerTab.STYLE_AI -> {
                    StyleAiScreen(
                        userProfile = userProfile,
                        wardrobeItems = wardrobeItems,
                        allOutfits = styleAiCombos,
                        outfitEngineState = outfitEngineUiState,
                        todayDate = viewModel.getTodayDateString(),
                        onOpenQuestionnaire = { viewModel.openQuestionnaireModal() },
                        onTogglePin = { viewModel.togglePinToDashboard(it) },
                        onMarkWornToday = { viewModel.markOutfitWornToday(it) },
                        onDeleteOutfit = { viewModel.deleteOutfit(it) },
                        onDismissError = { viewModel.clearOutfitEngineError() }
                    )
                }
                StylerTab.PROFILE -> {
                    ProfileScreen(
                        userProfile = userProfile,
                        onSaveProfile = { name, img, h, w, temp, cond ->
                            viewModel.saveUserProfile(name, img, h, w, temp, cond)
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = statusBannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    color = StylerDarkCharcoal,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusBannerMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = StylerPureWhite,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearBannerMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = StylerPureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUploadModal) {
        UploadItemModal(
            visionUiState = visionUiState,
            nanoBananaUiState = nanoBananaUiState,
            onAnalyzeWithVision = { bmp -> viewModel.analyzeWardrobeImageWithVision(bmp) },
            onGenerateWithNanoBanana = { prompt -> viewModel.generateImageWithNanoBanana(prompt) },
            onEditWithNanoBanana = { bmp, instruction -> viewModel.editImageWithNanoBanana(bmp, instruction) },
            onSaveItem = { name, cat, color, hex, undertone, notes, base64 ->
                viewModel.addWardrobeItem(name, cat, color, hex, undertone, notes, base64)
            },
            onDismiss = { viewModel.closeUploadModal() }
        )
    }

    if (showQuestionnaireModal) {
        StyleQuestionnaireModal(
            userProfile = userProfile,
            onSubmitQuestionnaire = { occasion, vibe, tempC ->
                viewModel.generateFourAiOutfits(occasion, vibe, tempC)
            },
            onDismiss = { viewModel.closeQuestionnaireModal() }
        )
    }
}

@Composable
private fun StylerTopBar(
    currentTab: StylerTab,
    onOpenStyleQuestionnaire: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        color = StylerCardSurface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.styler_brand_logo),
                    contentDescription = "Styler Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Styler",
                    style = MaterialTheme.typography.headlineMedium,
                    color = StylerDeepGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = StylerDeepGreenContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = currentTab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = StylerDeepGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = StylerAccentOrange,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clickable { onOpenStyleQuestionnaire() }
                        .testTag("topbar_style_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Open Style Questionnaire",
                            tint = StylerPureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Style",
                            style = MaterialTheme.typography.labelLarge,
                            color = StylerPureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = StylerDarkCharcoal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StylerBottomNavBar(
    currentTab: StylerTab,
    onSelectTab: (StylerTab) -> Unit
) {
    NavigationBar(
        containerColor = StylerCardSurface,
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("bottom_navigation_bar")
    ) {
        StylerTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    val icon = when (tab) {
                        StylerTab.DASHBOARD -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
                        StylerTab.GALLERY -> if (selected) Icons.Filled.Checkroom else Icons.Outlined.Checkroom
                        StylerTab.STYLE_AI -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
                        StylerTab.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = StylerPureWhite,
                    selectedTextColor = StylerDeepGreen,
                    indicatorColor = StylerDeepGreen,
                    unselectedIconColor = StylerMutedCharcoal,
                    unselectedTextColor = StylerMutedCharcoal
                ),
                modifier = Modifier.testTag("nav_tab_${tab.route}")
            )
        }
    }
}
