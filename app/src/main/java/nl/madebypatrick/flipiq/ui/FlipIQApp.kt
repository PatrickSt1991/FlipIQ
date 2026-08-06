package nl.madebypatrick.flipiq.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nl.madebypatrick.flipiq.ui.collection.CollectionScreen
import nl.madebypatrick.flipiq.ui.diagnostics.DiagnosticsScreen
import nl.madebypatrick.flipiq.ui.discover.DiscoverScreen
import nl.madebypatrick.flipiq.ui.haul.HaulScreen
import nl.madebypatrick.flipiq.ui.result.ResultScreen
import nl.madebypatrick.flipiq.ui.scan.ScanScreen
import nl.madebypatrick.flipiq.ui.scan.TextScanScreen
import nl.madebypatrick.flipiq.ui.settings.SettingsScreen
import nl.madebypatrick.flipiq.ui.share.SharedImageScreen
import nl.madebypatrick.flipiq.ui.share.SharedItem
import nl.madebypatrick.flipiq.ui.stats.StatsScreen

object Routes {
    const val SCAN = "scan"
    const val RESULT = "result"
    const val SEARCH = "search"
    const val TEXT_SCAN = "textscan"
    const val COLLECTION = "collection"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
    const val STATS = "stats"
    const val DISCOVER = "discover"
    const val HAUL = "haul"
    const val SHARED_IMAGE = "shared-image"
    const val ARG_BARCODE = "barcode"
    const val ARG_TITLE = "title"
    fun result(barcode: String) = "$RESULT/$barcode"
    fun search(title: String) = "$SEARCH/${Uri.encode(title)}"
}

@Composable
fun FlipIQApp(shared: SharedItem? = null) {
    val navController = rememberNavController()

    // Passed as a parameter, not a route argument: a content URI would have to survive string
    // encode/decode and the read grant is tied to this activity anyway.
    var sharedImageUri by remember { mutableStateOf<Uri?>(null) }
    // Deduplicate on the item's string so rotating on the result screen doesn't re-navigate, but a
    // genuinely new share does. rememberSaveable so it survives config changes.
    var lastHandledShare by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(shared) {
        if (shared == null) return@LaunchedEffect
        val key = shared.toString()
        if (key == lastHandledShare) return@LaunchedEffect
        lastHandledShare = key
        when (shared) {
            is SharedItem.Title -> navController.navigate(Routes.search(shared.value))
            is SharedItem.Barcode -> navController.navigate(Routes.result(shared.value))
            is SharedItem.Image -> {
                sharedImageUri = shared.uri
                navController.navigate(Routes.SHARED_IMAGE)
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SCAN) {
        composable(Routes.SCAN) {
            ScanScreen(
                onBarcodeScanned = { barcode -> navController.navigate(Routes.result(barcode)) },
                onSearchTitle = { title -> navController.navigate(Routes.search(title)) },
                onOpenCollection = { navController.navigate(Routes.COLLECTION) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenDiscover = { navController.navigate(Routes.DISCOVER) },
                onOpenHaul = { navController.navigate(Routes.HAUL) },
            )
        }
        composable(Routes.COLLECTION) {
            CollectionScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
            )
        }
        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DISCOVER) {
            DiscoverScreen(
                onBack = { navController.popBackStack() },
                onOpenGame = { title -> navController.navigate(Routes.search(title)) },
            )
        }
        composable(Routes.HAUL) {
            HaulScreen(
                onBack = { navController.popBackStack() },
                onOpenGame = { title -> navController.navigate(Routes.search(title)) },
            )
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
        composable(Routes.SHARED_IMAGE) {
            SharedImageScreen(
                uri = sharedImageUri,
                onBack = { navController.popBackStack() },
                onTitle = { title ->
                    navController.navigate(Routes.search(title)) {
                        popUpTo(Routes.SHARED_IMAGE) { inclusive = true }
                    }
                },
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
