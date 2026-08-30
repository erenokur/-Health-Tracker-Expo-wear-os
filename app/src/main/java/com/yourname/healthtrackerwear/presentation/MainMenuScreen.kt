package com.yourname.healthtrackerwear.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.yourname.healthtrackerwear.data.LanguagePrefs

val LocalLanguage = compositionLocalOf { "tr" }
val LocalOnToggleLanguage = compositionLocalOf<() -> Unit> { {} }

@Composable
fun HealthTrackerWearApp() {
    val context = LocalContext.current
    var language by remember { mutableStateOf(LanguagePrefs.get(context)) }

    fun toggleLanguage() {
        language = if (language == "tr") "en" else "tr"
        LanguagePrefs.set(context, language)
    }

    CompositionLocalProvider(
        LocalLanguage provides language,
        LocalOnToggleLanguage provides ::toggleLanguage,
    ) {
        val navController = rememberSwipeDismissableNavController()

        MaterialTheme {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "menu",
            ) {
                composable("menu") {
                    MainMenuScreen(
                        onBpClick = { navController.navigate("bp") },
                        onMedClick = { navController.navigate("med") },
                    )
                }
                composable("bp") {
                    BpScreen(onSaved = { navController.popBackStack() })
                }
                composable("med") {
                    MedScreen(onSaved = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun MainMenuScreen(onBpClick: () -> Unit, onMedClick: () -> Unit) {
    val lang = LocalLanguage.current
    val toggleLanguage = LocalOnToggleLanguage.current

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = Strings.get("appTitle", lang),
                style = MaterialTheme.typography.title3,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        item {
            Chip(
                label = { Text(Strings.get("addBp", lang)) },
                onClick = onBpClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
            )
        }
        item {
            Chip(
                label = { Text(Strings.get("medTaken", lang)) },
                onClick = onMedClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
            )
        }
        item {
            Chip(
                label = { Text(Strings.get("languageToggle", lang)) },
                onClick = { toggleLanguage() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
            )
        }
    }
}

