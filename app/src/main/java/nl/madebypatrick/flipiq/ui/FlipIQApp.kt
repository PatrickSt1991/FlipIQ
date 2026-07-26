package nl.madebypatrick.flipiq.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nl.madebypatrick.flipiq.ui.collection.CollectionScreen
import nl.madebypatrick.flipiq.ui.result.ResultScreen
import nl.madebypatrick.flipiq.ui.scan.ScanScreen
import nl.madebypatrick.flipiq.ui.scan.TextScanScreen
import nl.madebypatrick.flipiq.ui.settings.SettingsScreen
import nl.madebypatrick.flipiq.ui.stats.StatsScreen

object Routes {
    const val SCAN = "scan"
    const val RESULT = "result"
    const val SEARCH = "search"
    const val TEXT_SCAN = "textscan"
    const val COLLECTION = "collection"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val ARG_BARCODE = "barcode"
    const val ARG_TITLE = "title"
    fun result(barcode: String) = "$RESULT/$barcode"
    fun search(title: String) = "$SEARCH/${Uri.encode(title)}"
}

@Composable
fun FlipIQApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SCAN) {
        composable(Routes.SCAN) {
            ScanScreen(
                onBarcodeScanned = { barcode -> navController.navigate(Routes.result(barcode)) },
                onSearchTitle = { title -> navController.navigate(Routes.search(title)) },
                onOpenCollection = { navController.navigate(Routes.COLLECTION) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenStats = { navController.navigate(Routes.STATS) },
            )
        }
        composable(Routes.COLLECTION) {
            CollectionScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "${Routes.RESULT}/{${Routes.ARG_BARCODE}}",
            arguments = listOf(navArgument(Routes.ARG_BARCODE) { type = NavType.StringType }),
        ) {
            ResultScreen(
                onBack = { navController.popBackStack() },
                onScanFront = { navController.navigate(Routes.TEXT_SCAN) },
                onEditSearch = { edited -> navController.navigate(Routes.search(edited)) },
            )
        }
        composable(
            route = "${Routes.SEARCH}/{${Routes.ARG_TITLE}}",
            arguments = listOf(navArgument(Routes.ARG_TITLE) { type = NavType.StringType }),
        ) {
            ResultScreen(
                onBack = { navController.popBackStack() },
                onScanFront = { navController.navigate(Routes.TEXT_SCAN) },
                onEditSearch = { edited -> navController.navigate(Routes.search(edited)) },
            )
        }
        composable(Routes.TEXT_SCAN) {
            TextScanScreen(
                onBack = { navController.popBackStack() },
                onSearch = { title ->
                    navController.navigate(Routes.search(title)) {
                        popUpTo(Routes.TEXT_SCAN) { inclusive = true }
                    }
                },
            )
        }
    }
}
