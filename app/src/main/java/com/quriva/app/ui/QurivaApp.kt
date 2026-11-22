package com.quriva.app.ui

import android.app.Activity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quriva.app.data.AppPreferences
import com.quriva.app.data.AppUpdateInfo
import com.quriva.app.data.SurahAudioPlayer
import com.quriva.app.data.UpdateCheckResult
import com.quriva.app.data.UpdateRepository
import com.quriva.app.ui.screens.BookmarksScreen
import com.quriva.app.ui.screens.HomeScreen
import com.quriva.app.ui.screens.JuzScreen
import com.quriva.app.ui.screens.NotesScreen
import com.quriva.app.ui.screens.PageDetailScreen
import com.quriva.app.ui.screens.PagesScreen
import com.quriva.app.ui.screens.ProgressScreen
import com.quriva.app.ui.screens.ReaderScreen
import com.quriva.app.ui.screens.SearchScreen
import com.quriva.app.ui.screens.MemorizationScreen
import com.quriva.app.ui.screens.IslamicContentScreen
import com.quriva.app.ui.screens.QuranNavigationScreen
import com.quriva.app.ui.screens.TajweedGuideScreen
import com.quriva.app.ui.screens.AudioDownloadsScreen
import com.quriva.app.ui.screens.IslamicCalendarScreen
import com.quriva.app.ui.screens.FridayScreen
import com.quriva.app.ui.theme.QurivaTheme

@Composable
fun QurivaApp(prefs: AppPreferences, updateRequestKey: Int = 0) {
    val context = LocalContext.current
    var pendingUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    LaunchedEffect(updateRequestKey) {
        when (val result = UpdateRepository.checkForUpdate(context)) {
            is UpdateCheckResult.Available -> {
                prefs.markUpdateChecked()
                if (result.info.tagName != prefs.dismissedUpdateTag) {
                    pendingUpdate = result.info
                }
            }
            is UpdateCheckResult.UpToDate -> prefs.markUpdateChecked()
            is UpdateCheckResult.Error -> Unit
        }
    }
    val audioPlayer = remember { SurahAudioPlayer(context) }
    DisposableEffect(audioPlayer, prefs) {
        audioPlayer.onAyahPlayed = { _, _ -> prefs.recordPlayActivity(1) }
        onDispose {
            audioPlayer.onAyahPlayed = null
            audioPlayer.release()
        }
    }
    LaunchedEffect(
        audioPlayer.isPlaying,
        audioPlayer.activeSurahNumber,
        audioPlayer.activeAyahNumber,
        audioPlayer.activeReciterId
    ) {
        while (audioPlayer.isPlaying &&
            audioPlayer.activeSurahNumber in 1..114 &&
            audioPlayer.activeReciterId > 0
        ) {
            audioPlayer.syncPosition()
            prefs.saveLastAudio(
                audioPlayer.activeSurahNumber,
                audioPlayer.activeAyahNumber.coerceAtLeast(1),
                audioPlayer.activeReciterId,
                audioPlayer.positionMs
            )
            kotlinx.coroutines.delay(3_000)
        }
        if (audioPlayer.activeSurahNumber in 1..114 && audioPlayer.activeReciterId > 0) {
            audioPlayer.syncPosition()
            prefs.saveLastAudio(
                audioPlayer.activeSurahNumber,
                audioPlayer.activeAyahNumber.coerceAtLeast(1),
                audioPlayer.activeReciterId,
                audioPlayer.positionMs
            )
        }
    }
    val darkTheme = when (prefs.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
            view.keepScreenOn = prefs.keepScreenOn
        }
    }

    QurivaTheme(darkTheme = darkTheme, highContrast = prefs.highContrast, largeText = prefs.largeControls) {
      CompositionLocalProvider(LocalReduceMotion provides prefs.reduceMotion) {
        var showSettings by rememberSaveable { mutableStateOf(false) }
        val navController = rememberNavController()

        val enter: EnterTransition = if (prefs.reduceMotion) EnterTransition.None else fadeIn(tween(200, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { it / 12 }
        val exit: ExitTransition = if (prefs.reduceMotion) ExitTransition.None else fadeOut(tween(140, easing = FastOutSlowInEasing)) +
            slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { -it / 22 }
        val popEnter: EnterTransition = if (prefs.reduceMotion) EnterTransition.None else fadeIn(tween(190, easing = FastOutSlowInEasing)) +
            slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { -it / 12 }
        val popExit: ExitTransition = if (prefs.reduceMotion) ExitTransition.None else fadeOut(tween(150, easing = FastOutSlowInEasing)) +
            slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it / 22 }

        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "home",
                enterTransition = { enter },
                exitTransition = { exit },
                popEnterTransition = { popEnter },
                popExitTransition = { popExit }
            ) {
                composable("home") {
                    HomeScreen(
                        prefs = prefs,
                        onOpenSurah = { number, ayah ->
                            navController.navigate("reader/$number?ayah=$ayah")
                        },
                        onOpenSearch = { navController.navigate("search") },
                        onOpenBookmarks = { navController.navigate("bookmarks") },
                        onOpenJuz = { navController.navigate("juz") },
                        onOpenProgress = { navController.navigate("progress") },
                        onOpenNotes = { navController.navigate("notes") },
                        onOpenPages = { navController.navigate("pages") },
                        onOpenMemorization = { navController.navigate("memorization") },
                        onOpenContent = { navController.navigate("islamic-content") },
                        onOpenNavigation = { navController.navigate("quran-navigation") },
                        onOpenTajweed = { navController.navigate("tajweed-guide") },
                        onOpenAudioDownloads = { navController.navigate("audio-downloads") },
                        onOpenCalendar = { navController.navigate("islamic-calendar") },
                        onOpenFriday = { navController.navigate("friday") },
                        onOpenSettings = { showSettings = true }
                    )
                }
                composable("search") {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAyah = { surah, ayah ->
                            navController.navigate("reader/$surah?ayah=$ayah")
                        }
                    )
                }
                composable("bookmarks") {
                    BookmarksScreen(
                        prefs = prefs,
                        onBack = { navController.popBackStack() },
                        onOpenAyah = { surah, ayah ->
                            navController.navigate("reader/$surah?ayah=$ayah")
                        }
                    )
                }
                composable("juz") {
                    JuzScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAyah = { surah, ayah ->
                            navController.navigate("reader/$surah?ayah=$ayah")
                        }
                    )
                }
                composable("progress") {
                    ProgressScreen(
                        prefs = prefs,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("notes") {
                    NotesScreen(
                        prefs = prefs,
                        onBack = { navController.popBackStack() },
                        onOpenAyah = { surah, ayah ->
                            navController.navigate("reader/$surah?ayah=$ayah")
                        }
                    )
                }
                composable("pages") {
                    PagesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPage = { page -> navController.navigate("page/$page") }
                    )
                }
                composable("memorization") { MemorizationScreen(prefs, audioPlayer) { navController.popBackStack() } }
                composable("islamic-content") { IslamicContentScreen(prefs) { navController.popBackStack() } }
                composable("quran-navigation") { QuranNavigationScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAyah = { s, a -> navController.navigate("reader/$s?ayah=$a") },
                    onOpenPages = { navController.navigate("pages") }
                ) }
                composable("tajweed-guide") { TajweedGuideScreen { navController.popBackStack() } }
                composable("audio-downloads") { AudioDownloadsScreen(prefs) { navController.popBackStack() } }
                composable("islamic-calendar") { IslamicCalendarScreen(prefs) { navController.popBackStack() } }
                composable("friday") { FridayScreen(prefs, { navController.popBackStack() }, { navController.navigate("reader/18?ayah=1") }) }
                composable(
                    route = "page/{page}",
                    arguments = listOf(navArgument("page") { type = NavType.IntType })
                ) { entry ->
                    val page = entry.arguments?.getInt("page") ?: 1
                    PageDetailScreen(
                        page = page,
                        prefs = prefs,
                        onBack = { navController.popBackStack() },
                        onOpenAyah = { surah, ayah -> navController.navigate("reader/$surah?ayah=$ayah") }
                    )
                }
                composable(
                    route = "reader/{number}?ayah={ayah}",
                    arguments = listOf(
                        navArgument("number") { type = NavType.IntType },
                        navArgument("ayah") {
                            type = NavType.IntType
                            defaultValue = 1
                        }
                    )
                ) { entry ->
                    val number = entry.arguments?.getInt("number") ?: 1
                    val ayah = entry.arguments?.getInt("ayah") ?: 1
                    ReaderScreen(
                        surahNumber = number,
                        initialAyah = ayah,
                        prefs = prefs,
                        audioPlayer = audioPlayer,
                        onBack = { navController.popBackStack() },
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }

        if (showSettings) {
            SettingsSheet(
                prefs = prefs,
                onUpdateFound = { info ->
                    pendingUpdate = info
                    showSettings = false
                },
                onDismiss = { showSettings = false }
            )
        }

        val update = pendingUpdate
        if (update != null) {
            UpdateAvailableDialog(
                info = update,
                currentVersion = UpdateRepository.currentVersionName(context),
                onDismiss = { pendingUpdate = null },
                onSkipVersion = {
                    prefs.dismissUpdateTag(update.tagName)
                    pendingUpdate = null
                },
                onOpenRelease = { UpdateRepository.openUrl(context, update.htmlUrl) }
            )
        }
      }
    }
}
