package fumi.day.literalmemo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import fumi.day.literalmemo.data.prefs.UserPreferences
import fumi.day.literalmemo.data.prefs.UserPrefs
import fumi.day.literalmemo.ui.navigation.NavGraph
import fumi.day.literalmemo.ui.theme.LiteralMemoTheme

@Composable
fun App(
    userPreferences: UserPreferences
) {
    val userPrefs by userPreferences.userPrefs.collectAsState(initial = UserPrefs())
    
    LiteralMemoTheme(userPrefs = userPrefs) {
        val navController = rememberNavController()
        NavGraph(navController = navController)
    }
}
