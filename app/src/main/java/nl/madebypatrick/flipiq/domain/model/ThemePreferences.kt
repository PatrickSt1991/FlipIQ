package nl.madebypatrick.flipiq.domain.model

/** How the app chooses light vs dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User appearance preferences, applied by the Compose theme. */
data class ThemePreferences(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You dynamic colour (Android 12+). Off by default so the friendly brand palette shows. */
    val dynamicColor: Boolean = false,
) {
    companion object {
        val DEFAULT = ThemePreferences()
    }
}
