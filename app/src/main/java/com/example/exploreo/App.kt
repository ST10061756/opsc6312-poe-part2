package com.example.exploreo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.exploreo.data.PreferencesRepository
import com.example.exploreo.screens.HomeNavShell
import com.example.exploreo.screens.LoginScreen
import com.example.exploreo.screens.RegisterScreen
import com.example.exploreo.screens.SettingsScreen
import com.example.exploreo.ui.ExploreoTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

// App-level routes I use for navigation across the app
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val SETTINGS = "settings"
}

// Root composable that wires the theme, preferences, auth state and navigation graph
@Composable
fun ExploreoApp() {
    val context = LocalContext.current
    val prefs = remember { PreferencesRepository(context) }
    val darkMode by prefs.darkMode.collectAsState(initial = true)
    val language by prefs.language.collectAsState(initial = "English")
    val scope = rememberCoroutineScope()
    val mapType by prefs.mapType.collectAsState(initial = "Normal")
    val traffic by prefs.trafficEnabled.collectAsState(initial = true)
    val imageQuality by prefs.imageQuality.collectAsState(initial = "High")
    // Apply my Material theme, respecting the dark mode preference
    ExploreoTheme(useDarkTheme = darkMode) {
        val navController = rememberNavController()
        val start = if (FirebaseAuth.getInstance().currentUser != null) Routes.HOME else Routes.LOGIN
        // Main navigation host for the app (login/register/home flows)
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.fillMaxSize()
        ) {
            // Login screen and a way to jump to registration
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                    onRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
            // Registration screen and a way to go back or proceed to home
            composable(Routes.REGISTER) {
                RegisterScreen(onRegistered = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }, onBack = { navController.popBackStack() })
            }
            // Home shell that contains the bottom navigation and all tabs
            composable(Routes.HOME) {
                HomeNavShell(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    darkMode = darkMode,
                    onToggleDark = { enabled -> scope.launch { prefs.setDarkMode(enabled) } },
                    language = language,
                    onLanguageChange = { value -> scope.launch { prefs.setLanguage(value) } },
                    mapType = mapType,
                    onMapTypeChange = { value -> scope.launch { prefs.setMapType(value) } },
                    trafficEnabled = traffic,
                    onToggleTraffic = { enabled -> scope.launch { prefs.setTrafficEnabled(enabled) } },
                    imageQuality = imageQuality,
                    onImageQualityChange = { value -> scope.launch { prefs.setImageQuality(value) } },
                )
            }
        }
    }
}


