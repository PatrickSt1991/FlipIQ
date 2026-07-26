package nl.madebypatrick.flipiq.data.source.engine

import android.util.Base64

/**
 * Identifies a video game from a photo of its cover/case (or a screenshot). A vision model reads the
 * whole cover — art, characters, stylised logos — so it succeeds where on-device OCR fails on
 * distressed titles like FIFA Street. The heavy lifting (and the API key) live in the engine Worker;
 * the app just uploads the JPEG.
 */
interface GameIdentifier {
    /** Returns a game title, or null if it can't be identified (never throws). */
    suspend fun identify(jpeg: ByteArray): String?
}

/** No-op: used when the engine isn't configured (identification unavailable). */
class NoopGameIdentifier : GameIdentifier {
    override suspend fun identify(jpeg: ByteArray): String? = null
}

/** Posts the JPEG (base64) to the engine's `/identify` and returns the recognised title. */
class EngineGameIdentifier(
    private val api: EngineApi,
    private val engineUrl: String,
    private val appKey: String,
) : GameIdentifier {
    override suspend fun identify(jpeg: ByteArray): String? {
        val b64 = Base64.encodeToString(jpeg, Base64.NO_WRAP)
        val endpoint = engineUrl.trimEnd('/') + "/identify"
        return runCatching { api.identify(endpoint, appKey, IdentifyRequest(image = b64)).title }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
