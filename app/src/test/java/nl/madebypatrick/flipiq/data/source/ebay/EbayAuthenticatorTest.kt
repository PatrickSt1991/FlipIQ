package nl.madebypatrick.flipiq.data.source.ebay

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EbayAuthenticatorTest {

    private class FakeApi(var expiresIn: Long = 7200) : EbayApi {
        var tokenCalls = 0
        override suspend fun proxyToken(proxyUrl: String, appKey: String): EbayTokenResponse {
            tokenCalls++
            return EbayTokenResponse(accessToken = "tok$tokenCalls", expiresIn = expiresIn)
        }
        override suspend fun search(bearerAuth: String, query: String, limit: Int, marketplace: String) =
            EbaySearchResponse()
    }

    @Test
    fun `not configured when proxy url is blank`() = runTest {
        val auth = EbayAuthenticator(FakeApi(), proxyUrl = "", appKey = "")
        assertThat(auth.isConfigured).isFalse()
        assertThat(auth.bearer()).isNull()
    }

    @Test
    fun `fetches a bearer token and caches it within its lifetime`() = runTest {
        val api = FakeApi(expiresIn = 7200)
        var now = 0L
        val auth = EbayAuthenticator(api, proxyUrl = "https://proxy", appKey = "k", now = { now })

        assertThat(auth.bearer()).isEqualTo("Bearer tok1")
        now = 60_000 // 1 min later — still cached
        assertThat(auth.bearer()).isEqualTo("Bearer tok1")
        assertThat(api.tokenCalls).isEqualTo(1)
    }

    @Test
    fun `refreshes after the token expires`() = runTest {
        val api = FakeApi(expiresIn = 100)
        var now = 0L
        val auth = EbayAuthenticator(api, proxyUrl = "https://proxy", appKey = "k", now = { now })

        assertThat(auth.bearer()).isEqualTo("Bearer tok1")
        now = 200_000 // well past the 100s lifetime
        assertThat(auth.bearer()).isEqualTo("Bearer tok2")
        assertThat(api.tokenCalls).isEqualTo(2)
    }
}
