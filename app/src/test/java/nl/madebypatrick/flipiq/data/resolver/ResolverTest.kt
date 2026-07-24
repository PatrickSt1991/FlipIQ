package nl.madebypatrick.flipiq.data.resolver

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResolverTest {

    // --- EAN-Search mapping ------------------------------------------------------------------

    @Test
    fun `eanSearch firstTitle returns the first non-error name`() {
        val items = listOf(
            EanSearchItem(ean = "5051888210987", name = "LEGO Jurassic World, Xbox One"),
        )
        assertThat(items.firstTitle()).isEqualTo("LEGO Jurassic World, Xbox One")
    }

    @Test
    fun `eanSearch firstTitle skips error entries and blanks`() {
        assertThat(listOf(EanSearchItem(error = "Invalid token")).firstTitle()).isNull()
        assertThat(listOf(EanSearchItem(name = "")).firstTitle()).isNull()
        assertThat(emptyList<EanSearchItem>().firstTitle()).isNull()
    }

    // --- Composite ordering ------------------------------------------------------------------

    private fun resolver(result: String?) = object : BarcodeResolver {
        override suspend fun resolveTitle(barcode: String) = result
    }

    @Test
    fun `composite returns the first non-blank title`() = runTest {
        val composite = CompositeBarcodeResolver(
            listOf(resolver(null), resolver(""), resolver("Found It"), resolver("Later")),
        )
        assertThat(composite.resolveTitle("x")).isEqualTo("Found It")
    }

    @Test
    fun `composite returns null when every resolver misses`() = runTest {
        val composite = CompositeBarcodeResolver(listOf(resolver(null), resolver("")))
        assertThat(composite.resolveTitle("x")).isNull()
    }

    @Test
    fun `composite keeps going when a resolver throws`() = runTest {
        val throwing = object : BarcodeResolver {
            override suspend fun resolveTitle(barcode: String): String = error("boom")
        }
        val composite = CompositeBarcodeResolver(listOf(throwing, resolver("Recovered")))
        assertThat(composite.resolveTitle("x")).isEqualTo("Recovered")
    }
}
