@file:OptIn(ExperimentalMaterial3Api::class)

package presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import domain.auth.session.ISessionManager
import domain.onboarding.usecase.ImportSuggestedVocabularyUseCase
import domain.settings.repository.ISettingsRepository
import events.OnEvents
import events.VocabularyEffect
import expects.SetSystemBarsColor
import expects.isSystemInDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.auth.AuthViewModel
import presentation.feature.onboarding.OnboardingViewModel
import presentation.feature.onboarding.VocabularyPreviewViewModel
import presentation.feature.subscription.SubscriptionViewModel
import presentation.model.AppUiState
import presentation.model.ReviewType
import presentation.model.TabDestination
import presentation.model.UiMessage
import presentation.model.UiState
import presentation.ui.components.AnimatedNavIcon
import presentation.ui.overlay.OverlayHostContainer
import presentation.ui.screens.AuthGateScreen
import presentation.ui.screens.CollectionsScreen
import presentation.ui.screens.OnboardingScreen
import presentation.ui.screens.ProfileScreen
import presentation.ui.screens.SettingsScreen
import presentation.ui.screens.SplashScreen
import presentation.ui.screens.StudyScreen
import presentation.ui.screens.SubscriptionScreen
import presentation.ui.screens.SubscriptionScreenActions
import presentation.ui.screens.VocabularyPreviewScreen
import presentation.ui.screens.settings.WordManagerScreen
import presentation.viewmodel.AppNavigationViewModel
import presentation.viewmodel.VocabularyViewModel
import theme.AppColors
import domain.settings.model.ThemeMode
import org.kodein.emoji.compose.EmojiService
import theme.LexiconTheme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.import_failed_generic
import vokab.resources.generated.resources.please_login_for_ai
import vokab.resources.generated.resources.profile
import vokab.resources.generated.resources.review_complete
import vokab.resources.generated.resources.review_complete_message
import vokab.resources.generated.resources.settings
import vokab.resources.generated.resources.study
import vokab.resources.generated.resources.success_imported_words
import vokab.resources.generated.resources.word_deleted

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@Composable
fun LexiconApp() {
    val appNavigationViewModel = koinViewModel<AppNavigationViewModel>()
    val vocabularyViewModel = koinViewModel<VocabularyViewModel>()
    val authViewModel = koinViewModel<AuthViewModel>()
    val settingsRepository = koinInject<ISettingsRepository>()

    val reviewScreenState by vocabularyViewModel.reviewScreenState.collectAsStateWithLifecycle()
    val appUiState by appNavigationViewModel.appUiState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

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

    val sessionManager = koinInject<ISessionManager>()
    LaunchedEffect(Unit) {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        sessionManager.initialize(scope)
    }

    val isPreviewModeOpen = reviewScreenState.reviewType == ReviewType.BROWSE &&
            reviewScreenState.wordListState is UiState.Loaded

    val effectiveDarkMode = isPreviewModeOpen || darkMode

    LexiconTheme(darkTheme = effectiveDarkMode) {
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            HandleVocabularyEffects(
                vocabularyViewModel = vocabularyViewModel,
            )

            HandleUiMessages(
                vocabularyViewModel = vocabularyViewModel
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
                        val slideForward = toPreview || fromPreview
                        ContentTransform(
                            targetContentEnter = slideInHorizontally(
                                animationSpec = tween(350),
                                initialOffsetX = { if (slideForward) it else -it }
                            ) + fadeIn(animationSpec = tween(350)),
                            initialContentExit = slideOutHorizontally(
                                animationSpec = tween(350),
                                targetOffsetX = { if (slideForward) -it else it }
                            ) + fadeOut(animationSpec = tween(350))
                        )
                    },
                    label = "onboarding_flow"
                ) { state ->
                when (state) {
                    is AppUiState.Splash -> {
                        SplashScreen(onEnd = {
                            appNavigationViewModel.onSplashComplete(authState.isAuthenticated)
                        })
                    }

                    is AppUiState.Onboarding -> {
                        val onboardingViewModel: OnboardingViewModel = koinViewModel()
                        val onboardingState by onboardingViewModel.state.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) {
                            onboardingViewModel.events.collect { event ->
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
                            vocabularyPreviewViewModel.events.collect { event ->
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
                        val previewState by vocabularyPreviewViewModel.state.collectAsStateWithLifecycle()
                        VocabularyPreviewScreen(
                            state = previewState,
                            onToggleWord = vocabularyPreviewViewModel::toggleWord,
                            onSelectAll = vocabularyPreviewViewModel::selectAll,
                            onDeselectAll = vocabularyPreviewViewModel::deselectAll,
                            onProceed = vocabularyPreviewViewModel::proceedWithSelected,
                            onSkip = vocabularyPreviewViewModel::skip
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
                            isLoading = authState.isLoading
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
                        AppContent(
                            navController = navController,
                            effectiveDarkMode = effectiveDarkMode,
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun AppContent(
    navController: NavHostController,
    effectiveDarkMode: Boolean,
) {
    val snackbarHostState = LocalSnackbarHostState.current

    // System Bars
    SetSystemBarsColor(
        statusBarColor = if (effectiveDarkMode) Color(0xFF1A1A2E) else AppColors.normalBackground,
        navigationBarColor = if (effectiveDarkMode) Color(0xFF1A1A2E) else AppColors.normalSurface,
        darkIcons = !effectiveDarkMode
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        containerColor = if (snackbarData.visuals.message.startsWith("✗"))
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (snackbarData.visuals.message.startsWith("✗"))
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            )
        }
    ) { innerPadding ->
        NavigationGraph(
            modifier = Modifier
                .consumeWindowInsets(WindowInsets.statusBars)
                .padding(innerPadding),
            navController = navController,
        )
    }
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route ?: return

    NavigationBar {
        NavigationBarItem(
            icon = {
                AnimatedNavIcon(
                    icon = Icons.Filled.Person,
                    contentDescription = stringResource(Res.string.profile),
                    selected = LexiconRoute.Profile.isEqualTo(currentDestination)
                )
            },
            label = { Text(stringResource(Res.string.profile)) },
            selected = LexiconRoute.Profile.isEqualTo(currentDestination),
            onClick = {
                navController.navigateToTab(TabDestination.Profile)
            }
        )
        NavigationBarItem(
            icon = {
                AnimatedNavIcon(
                    icon = Icons.Filled.Book,
                    contentDescription = stringResource(Res.string.study),
                    selected = LexiconRoute.Study.isEqualTo(currentDestination)
                )
            },
            label = { Text(stringResource(Res.string.study)) },
            selected = LexiconRoute.Study.isEqualTo(currentDestination),
            onClick = {
                navController.navigateToTab(TabDestination.Study)
            }
        )
        NavigationBarItem(
            icon = {
                AnimatedNavIcon(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(Res.string.settings),
                    selected = LexiconRoute.Settings.isEqualTo(currentDestination)
                )
            },
            label = { Text(stringResource(Res.string.settings)) },
            selected = LexiconRoute.Settings.isEqualTo(currentDestination),
            onClick = {
                navController.navigateToTab(TabDestination.Settings)
            }
        )
    }
}

@Composable
private fun NavigationGraph(
    modifier: Modifier,
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = TabDestination.Study,
        modifier = modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable<TabDestination.Profile> {
            ProfileScreen()
        }

        composable<TabDestination.Study> {
            StudyScreen()
        }

        composable<TabDestination.Settings> {
            SettingsScreen(
                onNavigateToWordManager = {
                    navController.navigate(TabDestination.WordManager)
                },
                onNavigateToCollection = {
                    navController.navigate(TabDestination.Collections)
                },
                onNavigateToSubscription = {
                    navController.navigate(TabDestination.Subscription)
                }
            )
        }

        composable<TabDestination.WordManager> {
            WordManagerScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable<TabDestination.Collections> {
            CollectionsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable<TabDestination.Subscription> {
            val subscriptionViewModel: SubscriptionViewModel = koinViewModel()
            val state by subscriptionViewModel.state.collectAsStateWithLifecycle()
            val subscriptionUiState by subscriptionViewModel.uiState.collectAsStateWithLifecycle()

            SubscriptionScreen(
                state = state,
                isPurchasing = subscriptionUiState.isPurchasing,
                errorMessage = subscriptionUiState.errorMessage,
                successMessage = subscriptionUiState.successMessage,
                actions = SubscriptionScreenActions(
                    onPurchaseClick = { pkg -> subscriptionViewModel.purchasePackage(pkg) },
                    onRestoreClick = { subscriptionViewModel.restorePurchases() },
                    onRetryClick = { subscriptionViewModel.retry() },
                    onDismissError = { subscriptionViewModel.clearError() },
                    onDismissSuccess = { subscriptionViewModel.clearSuccess() },
                    onManageSubscription = { subscriptionViewModel.manageSubscription() },
                    onCancelSubscription = { subscriptionViewModel.cancelSubscription() }
                ),
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}

@Composable
private fun HandleVocabularyEffects(
    vocabularyViewModel: VocabularyViewModel,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val importFailedGeneric = stringResource(Res.string.import_failed_generic)
    val pleaseLoginForAi = stringResource(Res.string.please_login_for_ai)
    val successImportedWordsFormat = stringResource(Res.string.success_imported_words)

    LaunchedEffect(Unit) {
        vocabularyViewModel.events.collect { event ->
            when (event) {
                is VocabularyEffect.ImportSuccess -> {
                    val pattern = "%1" + '$' + "d"
                    val message =
                        successImportedWordsFormat.replace(pattern, event.count.toString())
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }

                is VocabularyEffect.ImportError -> {
                    val message = if (event.message.isNotEmpty()) {
                        "✗ ${event.message}"
                    } else {
                        importFailedGeneric
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }

                is VocabularyEffect.ImageImportSuccess -> {

                }

                is VocabularyEffect.ImageImportError -> {
                    snackbarHostState.showSnackbar(
                        message = "Something wrong happened!",
                        duration = SnackbarDuration.Short
                    )
                }

                is VocabularyEffect.ImageImportRequiresLogin -> {
                    snackbarHostState.showSnackbar(
                        message = pleaseLoginForAi,
                        duration = SnackbarDuration.Short
                    )
                }

                is VocabularyEffect.ReviewSessionComplete -> {
                    // Handled by UiMessages
                }
            }
        }
    }
}

@Composable
private fun HandleUiMessages(
    vocabularyViewModel: VocabularyViewModel
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val reviewComplete = stringResource(Res.string.review_complete)
    val reviewCompleteMessage = stringResource(Res.string.review_complete_message)
    val wordDeleted = stringResource(Res.string.word_deleted)

    OnEvents(vocabularyViewModel.uiMessages) { message ->
        when (message) {
            is UiMessage.ReviewComplete -> {
                snackbarHostState.showSnackbar(
                    message = "$reviewComplete\n$reviewCompleteMessage",
                    duration = SnackbarDuration.Short
                )
            }

            is UiMessage.WordDeleted -> {
                snackbarHostState.showSnackbar(
                    message = wordDeleted,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(destination: TabDestination) {
    navigate(destination) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

enum class LexiconRoute(val route: String) {
    Profile("Profile"),
    Study("Study"),
    Settings("Settings");
}

fun LexiconRoute.isEqualTo(currentRoute: String) = currentRoute.contains(this.route)
