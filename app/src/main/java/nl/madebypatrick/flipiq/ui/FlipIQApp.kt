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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import nl.madebypatrick.flipiq.R
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

object Routes {
    const val SCAN = "scan"
    const val RESULT = "result"
    const val SEARCH = "search"
    const val TEXT_SCAN = "textscan"
    const val COLLECTION = "collection"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
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

    // Home is a swipeable pager: Catalogus · Ontdekken · Partij (with a bottom nav). The scanner
    // opens from the catalog's + button (CLZ-style).
    NavHost(navController = navController, startDestination = Routes.COLLECTION) {
        composable(Routes.COLLECTION) {
            HomePager(
                onOpenScan = { navController.navigate(Routes.SCAN) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenGame = { title -> navController.navigate(Routes.search(title)) },
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onBarcodeScanned = { barcode -> navController.navigate(Routes.result(barcode)) },
                onSearchTitle = { title -> navController.navigate(Routes.search(title)) },
            )
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

/**
 * The home experience as a swipeable pager over the three browse destinations — Catalogus,
 * Ontdekken and Partij — with a bottom navigation bar. The catalog stays page 0 (the default), and
 * its own folder/detail drill-ins suspend the pager's swipe so they keep their horizontal gestures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomePager(
    onOpenScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGame: (String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var catalogDrilled by remember { mutableStateOf(false) }

    // System back from Ontdekken/Partij returns to the catalog rather than exiting the app.
    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Triple(Icons.Filled.Inventory2, R.string.nav_catalog, 0),
                    Triple(Icons.Filled.TravelExplore, R.string.discover_title, 1),
                    Triple(Icons.Filled.Collections, R.string.haul_title, 2),
                )
                items.forEach { (icon, labelRes, index) ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            // Don't steal horizontal drags from the catalog's folder/detail (its own detail pager).
            userScrollEnabled = pagerState.currentPage != 0 || !catalogDrilled,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) { page ->
            when (page) {
                0 -> CollectionScreen(
                    onOpenScan = onOpenScan,
                    onOpenSettings = onOpenSettings,
                    onDrilledChange = { catalogDrilled = it },
                )
                1 -> DiscoverScreen(
                    onBack = { scope.launch { pagerState.animateScrollToPage(0) } },
                    onOpenGame = onOpenGame,
                )
                else -> HaulScreen(
                    onBack = { scope.launch { pagerState.animateScrollToPage(0) } },
                    onOpenGame = onOpenGame,
                )
            }
        }
    }
}
