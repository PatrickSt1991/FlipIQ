package nl.madebypatrick.flipiq.data.source.reway

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared rate limiter for **both** Reway stores, so a 40-item Haul can't fire 80 requests at once
 * (§6). One instance is injected into both [RewaySource]s:
 *
 *  - concurrency is capped (a small [Semaphore]) so lookups queue instead of stampeding;
 *  - a 429 sets a circuit breaker via [blockFor], honouring `Retry-After`, and [isBlocked] then
 *    short-circuits every further Reway call for the rest of that window — "stop querying Reway".
 *
 * The breaker is set by an OkHttp interceptor (see NetworkModule) that inspects the response code,
 * since Retrofit turns a 429 into an exception before the source sees the headers.
 */
@Singleton
class RewayThrottle @Inject constructor() {

    private val semaphore = Semaphore(MAX_CONCURRENCY)
    private val blockedUntilMs = AtomicLong(0)

    fun isBlocked(nowMs: Long = System.currentTimeMillis()): Boolean = nowMs < blockedUntilMs.get()

    /** Back off for [seconds] (a 429's `Retry-After`); never shortens an existing, longer window. */
    fun blockFor(seconds: Long, nowMs: Long = System.currentTimeMillis()) {
        val until = nowMs + seconds.coerceAtLeast(1) * 1_000
        blockedUntilMs.updateAndGet { existing -> maxOf(existing, until) }
    }

    /** Run [block] under the concurrency cap. */
    suspend fun <T> withPermit(block: suspend () -> T): T = semaphore.withPermit { block() }

    companion object {
        const val MAX_CONCURRENCY = 2
        /** Fallback back-off when a 429 carries no parseable `Retry-After`. */
        const val DEFAULT_BACKOFF_SECONDS = 60L
    }
}
