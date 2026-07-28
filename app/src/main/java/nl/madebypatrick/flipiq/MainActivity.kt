package nl.madebypatrick.flipiq

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import nl.madebypatrick.flipiq.domain.model.ThemeMode
import nl.madebypatrick.flipiq.ui.FlipIQApp
import nl.madebypatrick.flipiq.ui.share.SharedItem
import nl.madebypatrick.flipiq.ui.share.toSharedItem
import nl.madebypatrick.flipiq.ui.util.AppLocale
import nl.madebypatrick.flipiq.ui.theme.FlipIQTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Apply the chosen app language before anything is inflated; recreate() re-runs this.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    /** What (if anything) another app shared into FlipIQ; drives share-target navigation. */
    private var sharedItem by mutableStateOf<SharedItem?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedItem = intent?.toSharedItem()
        setContent {
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            val darkTheme = when (theme.mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            FlipIQTheme(darkTheme = darkTheme, dynamicColor = theme.dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    FlipIQApp(shared = sharedItem)
                }
            }
        }
    }

    // Inert under the default launch mode (a new share starts a fresh instance), but correct if the
    // launch mode ever changes to singleTop/singleTask.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedItem = intent.toSharedItem()
    }
}
