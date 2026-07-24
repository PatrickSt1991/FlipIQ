package nl.madebypatrick.flipiq.domain.model

import kotlin.math.roundToLong

/**
 * A monetary amount stored as whole cents to avoid binary floating-point drift in pricing math.
 *
 * All engine arithmetic runs in cents; conversion to/from euros happens only at the edges
 * (parsing marketplace data in, formatting for the UI out).
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    val euros: Double get() = cents / 100.0

    operator fun plus(other: Money) = Money(cents + other.cents)
    operator fun minus(other: Money) = Money(cents - other.cents)
    operator fun times(factor: Double) = Money((cents * factor).roundToLong())
    operator fun div(divisor: Double) = Money((cents / divisor).roundToLong())

    /** Never let a computed price go negative; a "buy at less than nothing" is just zero. */
    fun coerceAtLeastZero() = if (cents < 0) ZERO else this

    override fun compareTo(other: Money) = cents.compareTo(other.cents)

    override fun toString() = "€%.2f".format(euros)

    companion object {
        val ZERO = Money(0)

        fun ofEuros(euros: Double) = Money((euros * 100).roundToLong())
        fun ofCents(cents: Long) = Money(cents)
    }
}
