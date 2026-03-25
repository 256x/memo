package fumi.day.literalmemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import fumi.day.literalmemo.ui.edit.MemoEditScreen
import fumi.day.literalmemo.ui.list.MemoListScreen
import fumi.day.literalmemo.ui.settings.SettingsScreen

object Routes {
    const val MEMO_LIST = "memos/list"
    const val MEMO_EDIT = "memos/edit"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(
    navController: NavHostController
) {
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
            route = "${Routes.MEMO_EDIT}?fileName={fileName}",
            arguments = listOf(
                navArgument("fileName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            MemoEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
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
