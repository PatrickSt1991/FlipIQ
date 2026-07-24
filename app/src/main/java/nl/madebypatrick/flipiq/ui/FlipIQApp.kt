package nl.madebypatrick.flipiq.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nl.madebypatrick.flipiq.ui.collection.CollectionScreen
import nl.madebypatrick.flipiq.ui.result.ResultScreen
import nl.madebypatrick.flipiq.ui.scan.ScanScreen
import nl.madebypatrick.flipiq.ui.settings.SettingsScreen
import nl.madebypatrick.flipiq.ui.stats.StatsScreen

object Routes {
    const val SCAN = "scan"
    const val RESULT = "result"
    const val COLLECTION = "collection"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val ARG_BARCODE = "barcode"
    fun result(barcode: String) = "$RESULT/$barcode"
}

@Composable
fun FlipIQApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SCAN) {
        composable(Routes.SCAN) {
            ScanScreen(
                onBarcodeScanned = { barcode ->
                    navController.navigate(Routes.result(barcode))
                },
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
            ResultScreen(onBack = { navController.popBackStack() })
        }
    }
}
