package nl.madebypatrick.flipiq.data.settings

/**
 * Where eBay listings may be located, sent to the engine as the `loc` query param and mapped there
 * to an eBay Browse filter. The Browse API has no EU/region grouping or all-items radius filter, so
 * these three are the honest, API-backed equivalents of eBay's website location picker.
 */
enum class EbayLocation(val param: String) {
    /** Item physically in the Netherlands (`itemLocationCountry:NL`). The default. */
    NETHERLANDS("nl"),

    /** Item that ships to the Netherlands (`deliveryCountry:NL`) — the practical "reachable" set. */
    DELIVERS_TO_NL("eu"),

    /** No location constraint. */
    WORLDWIDE("world");

    companion object {
        val DEFAULT = NETHERLANDS
        fun fromParam(p: String?): EbayLocation = entries.firstOrNull { it.param == p } ?: DEFAULT
    }
}
