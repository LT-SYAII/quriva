package com.quriva.app.ui

import android.app.Activity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quriva.app.data.AppPreferences
import com.quriva.app.ui.screens.BookmarksScreen
import com.quriva.app.ui.screens.HomeScreen
import com.quriva.app.ui.screens.JuzScreen
import com.quriva.app.ui.screens.NotesScreen
import com.quriva.app.ui.screens.PageDetailScreen
import com.quriva.app.ui.screens.PagesScreen
import com.quriva.app.ui.screens.ProgressScreen
import com.quriva.app.ui.screens.ReaderScreen
import com.quriva.app.ui.screens.SearchScreen
import com.quriva.app.ui.theme.QurivaTheme

@Composable
fun QurivaApp(prefs: AppPreferences) {
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
        }
    }

    QurivaTheme(darkTheme = darkTheme) {
        var showSettings by rememberSaveable { mutableStateOf(false) }
        val navController = rememberNavController()

        val enter: EnterTransition = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
            scaleIn(initialScale = 0.985f, animationSpec = tween(220, easing = FastOutSlowInEasing))
        val exit: ExitTransition = fadeOut(tween(180, easing = FastOutSlowInEasing))
        val popEnter: EnterTransition = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
            scaleIn(initialScale = 0.992f, animationSpec = tween(200, easing = FastOutSlowInEasing))
        val popExit: ExitTransition = fadeOut(tween(160, easing = FastOutSlowInEasing)) +
            scaleOut(targetScale = 0.985f, animationSpec = tween(160, easing = FastOutSlowInEasing))

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
                composable(
                    route = "page/{page}",
                    arguments = listOf(navArgument("page") { type = NavType.IntType })
                ) { entry ->
                    val page = entry.arguments?.getInt("page") ?: 1
                    PageDetailScreen(
                        page = page,
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
                        onBack = { navController.popBackStack() },
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }

        if (showSettings) {
            SettingsSheet(
                prefs = prefs,
                onDismiss = { showSettings = false }
            )
        }
    }
}
