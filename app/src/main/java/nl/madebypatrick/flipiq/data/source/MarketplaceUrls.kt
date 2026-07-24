package nl.madebypatrick.flipiq.data.source

import java.net.URLEncoder

/** Builds the "open on marketplace with one tap" deep links from a product title. */
object MarketplaceUrls {

    private fun q(term: String) = URLEncoder.encode(term, "UTF-8")

    fun ebaySold(title: String) =
        "https://www.ebay.nl/sch/i.html?_nkw=${q(title)}&LH_Sold=1&LH_Complete=1"

    fun vinted(title: String) =
        "https://www.vinted.nl/catalog?search_text=${q(title)}"

    fun marktplaats(title: String) =
        "https://www.marktplaats.nl/q/${q(title)}/"

    fun cex(title: String) =
        "https://nl.webuy.com/search?stext=${q(title)}"

    fun priceCharting(title: String) =
        "https://www.pricecharting.com/search-products?q=${q(title)}&type=prices"

    fun tweakers(title: String) =
        "https://tweakers.net/pricewatch/zoeken/?keyword=${q(title)}"
}
