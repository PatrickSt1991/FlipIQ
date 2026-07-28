package nl.madebypatrick.flipiq.ui.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * App-level language override (System / en / nl).
 *
 * Stored in SharedPreferences — not DataStore — so it can be read **synchronously** in
 * `Activity.attachBaseContext`, which runs before Hilt/DataStore are available. Applied by wrapping
 * the base context's configuration; changing it calls `Activity.recreate()`, which re-runs
 * `attachBaseContext` with the new locale.
 */
object AppLocale {
    private const val PREFS = "flipiq_locale"
    private const val KEY = "lang" // "" = follow the system, else a BCP-47 tag ("en" / "nl")

    fun getTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()

    fun setTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
    }

    /** Wrap [base] so its resources resolve in the chosen language; a no-op when following system. */
    fun wrap(base: Context): Context {
        val tag = getTag(base)
        if (tag.isBlank()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
