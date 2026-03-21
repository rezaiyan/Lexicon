package presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.rememberNavController
import domain.auth.session.ISessionManager
import domain.onboarding.usecase.ImportSuggestedVocabularyUseCase
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import expects.LocalSystemBarsController
import expects.SetSystemBarsColor
import expects.SystemBarsController
import expects.SystemBarsState
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
import feature.onboarding.model.OnboardingEffect
import feature.onboarding.model.VocabularyPreviewEffect
import feature.auth.AuthPhase
import presentation.model.AppUiState
import presentation.model.TabDestination
import overlay.OverlayHostContainer
import feature.auth.ui.AuthExperienceScreen
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
    val themeMode by settingsRepository.getThemeMode().collectAsState(ThemeMode.AUTO)
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
        val defaultNavBarColor = if (appUiState is AppUiState.Ready && isBottomNavLayout) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.background
        }
        val controller = remember {
            SystemBarsController(
                SystemBarsState(
                    statusBarColor = defaultNavBarColor,
                    navigationBarColor = defaultNavBarColor,
                    darkIcons = !darkMode
                )
            )
        }
        val defaultStatusBarColor = MaterialTheme.colorScheme.background
        SideEffect {
            controller.defaultState = SystemBarsState(
                statusBarColor = defaultStatusBarColor,
                navigationBarColor = defaultNavBarColor,
                darkIcons = !darkMode
            )
        }

        val bars = controller.currentState
        SetSystemBarsColor(bars.statusBarColor, bars.navigationBarColor, bars.darkIcons)

        Surface(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalSystemBarsController provides controller,
            LocalSnackbarHostState provides snackbarHostState
        ) {
            HandleVocabularyEffects(
                vocabularyViewModel = vocabularyViewModel,
            )

            OverlayHostContainer {
                AnimatedContent(
                    targetState = appUiState,
                    modifier = Modifier.fillMaxSize(),
                    // Auth(Verifying) and Auth(LoginRequired) share the same content key so the
                    // composable — and its single LottieGradientBackground — stays alive across
                    // the phase transition without restarting the animation.
                    contentKey = { if (it is AppUiState.Auth) AppUiState.Auth::class else it },
                    transitionSpec = {
                        val initial = initialState
                        val target = targetState
                        val toPreview = target is AppUiState.VocabularyPreview && initial is AppUiState.Onboarding
                        val fromPreview = initial is AppUiState.VocabularyPreview && target is AppUiState.Auth
                        val isLogout = initial is AppUiState.Ready && target is AppUiState.Auth
                        val isAuthToReady = initial is AppUiState.Auth && target is AppUiState.Ready
                        val slideForward = toPreview || fromPreview
                        when {
                            isLogout -> ContentTransform(
                                targetContentEnter = fadeIn(animationSpec = tween(400)),
                                initialContentExit = fadeOut(animationSpec = tween(300))
                            )
                            isAuthToReady -> ContentTransform(
                                targetContentEnter = EnterTransition.None,
                                initialContentExit = fadeOut(animationSpec = tween(300)),
                                targetContentZIndex = 1f
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
                    is AppUiState.Auth -> {
                        val pendingVocabulary = state.pendingVocabulary
                        val needsOnboardingCheck = state.needsOnboardingCheck
                        val importUseCase: ImportSuggestedVocabularyUseCase = koinInject()

                        AuthExperienceScreen(
                            phase = state.phase,
                            onVerifySession = { onComplete ->
                                authViewModel.verifyAndRestoreSession(onComplete = onComplete)
                            },
                            onSessionVerified = {
                                appNavigationViewModel.onSessionVerified(authState.isAuthenticated)
                            },
                            onLoginWithGoogle = { idToken ->
                                authViewModel.loginWithGoogle(idToken)
                            },
                            onLoginWithApple = { idToken, fullName, appleUserId ->
                                authViewModel.loginWithApple(idToken, fullName, appleUserId)
                            },
                            isLoading = authState.isLoading,
                            error = authState.error,
                        )

                        if (state.phase == AuthPhase.LoginRequired) {
                            LaunchedEffect(authState.isAuthenticated, authState.isLoading) {
                                if (authState.isAuthenticated && !authState.isLoading) {
                                    if (needsOnboardingCheck) {
                                        appNavigationViewModel.onAuthCompleteCheckingData()
                                    } else {
                                        if (pendingVocabulary.isNotEmpty()) {
                                            importUseCase(pendingVocabulary)
                                        }
                                        appNavigationViewModel.onAuthComplete()
                                    }
                                }
                            }
                        }
                    }

                    is AppUiState.Onboarding -> {
                        val onboardingViewModel: OnboardingViewModel = koinViewModel()
                        val onboardingState by onboardingViewModel.state()

                        LaunchedEffect(Unit) {
                            onboardingViewModel.effects.collect { event ->
                                when (event) {
                                    is OnboardingEffect.NavigateToPreview -> {
                                        appNavigationViewModel.onNavigateToVocabularyPreview(
                                            event.response.suggestedVocabulary
                                        )
                                    }
                                    is OnboardingEffect.NavigateToMain -> {
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
                                    is VocabularyPreviewEffect.ProceedWithSelection -> {
                                        appNavigationViewModel.onNavigateToAuthGate(event.words)
                                    }
                                    is VocabularyPreviewEffect.SkipVocabulary -> {
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
