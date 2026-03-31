package fumi.day.literalmemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import fumi.day.literalmemo.ui.edit.MemoEditScreen
import fumi.day.literalmemo.ui.list.MemoListScreen
import fumi.day.literalmemo.ui.settings.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val MEMO_LIST = "memos/list"
    const val MEMO_EDIT = "memos/edit"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {}
) {
    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            val encoded = URLEncoder.encode(sharedText, "UTF-8")
            navController.navigate("${Routes.MEMO_EDIT}?initialContent=$encoded")
            onSharedTextConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.MEMO_LIST
    ) {
        composable(Routes.MEMO_LIST) {
            MemoListScreen(
                onNavigateToEdit = { fileName ->
                    if (fileName != null) {
                        navController.navigate("${Routes.MEMO_EDIT}?fileName=$fileName")
                    } else {
                        navController.navigate(Routes.MEMO_EDIT)
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = "${Routes.MEMO_EDIT}?fileName={fileName}&initialContent={initialContent}",
            arguments = listOf(
                navArgument("fileName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("initialContent") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialContent = backStackEntry.arguments?.getString("initialContent")?.let {
                URLDecoder.decode(it, "UTF-8")
            }
            MemoEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                initialContent = initialContent
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
