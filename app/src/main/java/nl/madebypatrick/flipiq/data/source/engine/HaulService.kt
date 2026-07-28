package nl.madebypatrick.flipiq.data.source.engine

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.madebypatrick.flipiq.domain.model.Money

/** One item detected in a haul photo, with its (optional) quick resale value. */
data class HaulItem(val title: String, val value: Money?, val imageUrl: String?)

/** Sends a photo of many items to the engine's `/haul` and returns the detected items + values. */
interface HaulService {
    suspend fun scan(jpeg: ByteArray, rotationDegrees: Int, maxDim: Int = PHOTO_MAX_DIM): List<HaulItem>
}

/** No-op when the engine isn't configured. */
class NoopHaulService : HaulService {
    override suspend fun scan(jpeg: ByteArray, rotationDegrees: Int, maxDim: Int): List<HaulItem> = emptyList()
}

class EngineHaulService(
    private val api: EngineApi,
    private val engineUrl: String,
    private val appKey: String,
) : HaulService {
    override suspend fun scan(jpeg: ByteArray, rotationDegrees: Int, maxDim: Int): List<HaulItem> =
        withContext(Dispatchers.Default) {
            val prepared = prepareForUpload(jpeg, rotationDegrees, maxDim)
            val b64 = Base64.encodeToString(prepared, Base64.NO_WRAP)
            val endpoint = engineUrl.trimEnd('/') + "/haul"
            runCatching { api.haul(endpoint, appKey, IdentifyRequest(image = b64)).items }
                .getOrDefault(emptyList())
                .map { HaulItem(it.title, it.valueCents?.let(Money::ofCents), it.image) }
        }
}
