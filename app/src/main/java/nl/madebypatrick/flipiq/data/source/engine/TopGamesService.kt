package nl.madebypatrick.flipiq.data.source.engine

import nl.madebypatrick.flipiq.domain.model.Money

/** A game in a console's "most valuable" list, with its guide value in EUR. */
data class ConsoleGame(val title: String, val value: Money)

/** Fetches the "most valuable games" list for a console from the engine's `/top` endpoint. */
interface TopGamesService {
    suspend fun top(consoleSlug: String): List<ConsoleGame>
}

/** No-op when the engine isn't configured. */
class NoopTopGamesService : TopGamesService {
    override suspend fun top(consoleSlug: String): List<ConsoleGame> = emptyList()
}

class EngineTopGamesService(
    private val api: EngineApi,
    private val engineUrl: String,
    private val appKey: String,
) : TopGamesService {
    override suspend fun top(consoleSlug: String): List<ConsoleGame> {
        val endpoint = engineUrl.trimEnd('/') + "/top"
        return runCatching { api.top(endpoint, appKey, consoleSlug).games }
            .getOrDefault(emptyList())
            .mapNotNull { g -> g.priceCents?.let { ConsoleGame(g.title, Money.ofCents(it)) } }
    }
}
