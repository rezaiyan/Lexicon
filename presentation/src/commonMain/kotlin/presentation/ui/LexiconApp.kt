@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)

package presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import domain.auth.session.ISessionManager
import domain.onboarding.usecase.ImportSuggestedVocabularyUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import expects.SetSystemBarsColor
import expects.isSystemInDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.kodein.emoji.compose.EmojiService
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import feature.auth.AuthViewModel
import feature.onboarding.OnboardingViewModel
import feature.onboarding.VocabularyPreviewViewModel
import presentation.model.AppUiState
import presentation.model.TabDestination
import overlay.OverlayHostContainer
import feature.auth.ui.AuthGateScreen
import feature.auth.ui.SplashHost
import feature.onboarding.ui.OnboardingScreen
import feature.onboarding.ui.VocabularyPreviewScreen
import presentation.viewmodel.AppNavigationViewModel
import feature.words.VocabularyViewModel
import theme.LexiconTheme

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@Composable
fun LexiconApp() {
    val appNavigationViewModel = koinViewModel<AppNavigationViewModel>()
    val vocabularyViewModel = koinViewModel<VocabularyViewModel>()
    val authViewModel = koinViewModel<AuthViewModel>()
    val settingsRepository = koinInject<ISettingsRepository>()

    val appUiState by appNavigationViewModel.state()
    val authState by authViewModel.state()

    val systemInDarkTheme = isSystemInDarkTheme()
    val themeMode by settingsRepository.getThemeMode().collectAsStateWithLifecycle(ThemeMode.AUTO)
    val darkMode = when (themeMode) {
        ThemeMode.AUTO -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    remember { EmojiService.initialize() }
    val isBottomNavLayout = currentNavigationSuiteType() == NavigationSuiteType.NavigationBar

    val sessionManager = koinInject<ISessionManager>()
    LaunchedEffect(Unit) {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        sessionManager.initialize(scope)
    }

    // Handle logout: navigate to AuthGate when user becomes unauthenticated
    LaunchedEffect(authState.isAuthenticated, appUiState) {
        if (!authState.isAuthenticated && appUiState is AppUiState.Ready) {
            // Reset tab back stack to start destination so the next login lands on Study
            navController.navigate(TabDestination.Study) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            appNavigationViewModel.onLogout()
        }
    }

    LexiconTheme(darkTheme = darkMode) {
        Surface(modifier = Modifier.fillMaxSize()) {

            if (appUiState !is AppUiState.Ready) {
                SetSystemBarsColor(
                    statusBarColor = MaterialTheme.colorScheme.background,
                    navigationBarColor = MaterialTheme.colorScheme.background,
                    darkIcons = !darkMode
                )
            } else {
                SetSystemBarsColor(
                    statusBarColor = MaterialTheme.colorScheme.background,
                    navigationBarColor = if (isBottomNavLayout) {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else {
                        MaterialTheme.colorScheme.background
                    },
                    darkIcons = !darkMode
                )
            }

        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            HandleVocabularyEffects(
                vocabularyViewModel = vocabularyViewModel,
            )

            OverlayHostContainer {
                AnimatedContent(
                    targetState = appUiState,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val initial = initialState
                        val target = targetState
                        val toPreview = target is AppUiState.VocabularyPreview && initial is AppUiState.Onboarding
                        val fromPreview = initial is AppUiState.VocabularyPreview && target is AppUiState.AuthGate
                        val isLogout = initial is AppUiState.Ready && target is AppUiState.AuthGate
                        val slideForward = toPreview || fromPreview
                        when {
                            isLogout -> ContentTransform(
                                targetContentEnter = fadeIn(animationSpec = tween(400)),
                                initialContentExit = fadeOut(animationSpec = tween(300))
                            )
                            else -> ContentTransform(
                                targetContentEnter = slideInHorizontally(
                                    animationSpec = tween(350),
                                    initialOffsetX = { if (slideForward) it else -it }
                                ) + fadeIn(animationSpec = tween(350)),
                                initialContentExit = slideOutHorizontally(
                                    animationSpec = tween(350),
                                    targetOffsetX = { if (slideForward) -it else it }
                                ) + fadeOut(animationSpec = tween(350))
                            )
                        }
                    },
                    label = "onboarding_flow"
                ) { state ->
                when (state) {
                    is AppUiState.Splash -> {
                        SplashHost(onEnd = {
                            appNavigationViewModel.onSplashComplete(authState.isAuthenticated)
                        })
                    }

                    is AppUiState.Onboarding -> {
                        val onboardingViewModel: OnboardingViewModel = koinViewModel()
                        val onboardingState by onboardingViewModel.state()

                        LaunchedEffect(Unit) {
                            onboardingViewModel.effects.collect { event ->
                                when (event) {
                                    is OnboardingViewModel.Event.NavigateToPreview -> {
                                        appNavigationViewModel.onNavigateToVocabularyPreview(
                                            event.response.suggestedVocabulary
                                        )
                                    }
                                    is OnboardingViewModel.Event.NavigateToMain -> {
                                        appNavigationViewModel.onNavigateToAuthGate()
                                    }
                                }
                            }
                        }

                        OnboardingScreen(
                            state = onboardingState,
                            onTargetLanguageSelected = onboardingViewModel::selectTargetLanguage,
                            onNativeLanguageSelected = onboardingViewModel::selectNativeLanguage,
                            onLevelSelected = onboardingViewModel::selectLevel,
                            onNextStep = onboardingViewModel::nextStep,
                            onPreviousStep = onboardingViewModel::previousStep,
                            onSubmit = onboardingViewModel::submit,
                            onSkip = onboardingViewModel::skip
                        )
                    }

                    is AppUiState.VocabularyPreview -> {
                        val vocabularyPreviewViewModel: VocabularyPreviewViewModel = koinViewModel()
                        val previewWords = state.words
                        LaunchedEffect(previewWords) {
                            vocabularyPreviewViewModel.setWords(previewWords)
                        }
                        LaunchedEffect(Unit) {
                            vocabularyPreviewViewModel.effects.collect { event ->
                                when (event) {
                                    is VocabularyPreviewViewModel.Event.ProceedWithSelection -> {
                                        appNavigationViewModel.onNavigateToAuthGate(event.words)
                                    }
                                    is VocabularyPreviewViewModel.Event.SkipVocabulary -> {
                                        appNavigationViewModel.onNavigateToAuthGate()
                                    }
                                }
                            }
                        }
                        val previewState by vocabularyPreviewViewModel.state()
                        VocabularyPreviewScreen(
                            state = previewState,
                            onAccept = vocabularyPreviewViewModel::proceedWithSelected,
                            onDeny = vocabularyPreviewViewModel::skip
                        )
                    }

                    is AppUiState.AuthGate -> {
                        val pendingVocabulary = state.pendingVocabulary
                        val importUseCase: ImportSuggestedVocabularyUseCase = koinInject()

                        AuthGateScreen(
                            onLoginWithGoogle = { idToken ->
                                authViewModel.loginWithGoogle(idToken)
                            },
                            onLoginWithApple = { idToken, fullName, appleUserId ->
                                authViewModel.loginWithApple(idToken, fullName, appleUserId)
                            },
                            isLoading = authState.isLoading,
                            error = authState.error
                        )

                        // When auth succeeds, import pending vocabulary and navigate to main
                        LaunchedEffect(authState.isAuthenticated) {
                            if (authState.isAuthenticated) {
                                if (pendingVocabulary.isNotEmpty()) {
                                    importUseCase(pendingVocabulary)
                                }
                                appNavigationViewModel.onAuthComplete()
                            }
                        }
                    }

                    is AppUiState.Ready -> {
                        AppContent(navController = navController)
                    }
                }
                }
            }
        }
        }
    }
}
