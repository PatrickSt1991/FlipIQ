package nl.madebypatrick.flipiq.ui.result

import androidx.compose.ui.graphics.Color
import nl.madebypatrick.flipiq.domain.model.BuyTierLevel
import nl.madebypatrick.flipiq.domain.model.Completeness
import nl.madebypatrick.flipiq.domain.model.Condition
import nl.madebypatrick.flipiq.domain.model.DealTier
import nl.madebypatrick.flipiq.domain.model.MarketTrend
import nl.madebypatrick.flipiq.domain.model.SellSpeed
import nl.madebypatrick.flipiq.ui.theme.ScoreAmber
import nl.madebypatrick.flipiq.ui.theme.ScoreGreen
import nl.madebypatrick.flipiq.ui.theme.ScoreLime
import nl.madebypatrick.flipiq.ui.theme.ScoreOrange
import nl.madebypatrick.flipiq.ui.theme.ScoreRed

/** Presentation-layer mapping of domain enums to labels, colours and emoji for the result screen. */

val DealTier.label: String
    get() = when (this) {
        DealTier.BUY_IMMEDIATELY -> "Buy Immediately"
        DealTier.GREAT_DEAL -> "Great Deal"
        DealTier.FAIR_PRICE -> "Fair Price"
        DealTier.LOW_PROFIT -> "Low Profit"
        DealTier.SKIP -> "Skip"
    }

val DealTier.color: Color
    get() = when (this) {
        DealTier.BUY_IMMEDIATELY -> ScoreGreen
        DealTier.GREAT_DEAL -> ScoreLime
        DealTier.FAIR_PRICE -> ScoreAmber
        DealTier.LOW_PROFIT -> ScoreOrange
        DealTier.SKIP -> ScoreRed
    }

val SellSpeed.label: String
    get() = when (this) {
        SellSpeed.VERY_FAST -> "Very Fast"
        SellSpeed.FAST -> "Fast"
        SellSpeed.MEDIUM -> "Medium"
        SellSpeed.SLOW -> "Slow"
    }

val SellSpeed.emoji: String
    get() = when (this) {
        SellSpeed.VERY_FAST -> "⚡"
        SellSpeed.FAST -> "🚀"
        SellSpeed.MEDIUM -> "⏳"
        SellSpeed.SLOW -> "🐢"
    }

val SellSpeed.hint: String
    get() = when (this) {
        SellSpeed.VERY_FAST -> "Usually sells within days"
        SellSpeed.FAST -> "Usually sells within 1–2 weeks"
        SellSpeed.MEDIUM -> "Usually sells within 2–8 weeks"
        SellSpeed.SLOW -> "May take several months"
    }

val MarketTrend.label: String
    get() = when (this) {
        MarketTrend.RISING -> "Rising ↗"
        MarketTrend.STABLE -> "Stable →"
        MarketTrend.FALLING -> "Falling ↘"
    }

val BuyTierLevel.label: String
    get() = when (this) {
        BuyTierLevel.EXCELLENT -> "Excellent"
        BuyTierLevel.GOOD -> "Good"
        BuyTierLevel.FAIR -> "Fair"
        BuyTierLevel.SKIP -> "Skip above"
    }

val BuyTierLevel.color: Color
    get() = when (this) {
        BuyTierLevel.EXCELLENT -> ScoreGreen
        BuyTierLevel.GOOD -> ScoreLime
        BuyTierLevel.FAIR -> ScoreAmber
        BuyTierLevel.SKIP -> ScoreRed
    }

val Condition.label: String
    get() = when (this) {
        Condition.SEALED -> "Sealed"
        Condition.MINT -> "Mint"
        Condition.GOOD -> "Good"
        Condition.ACCEPTABLE -> "Acceptable"
        Condition.POOR -> "Poor"
    }

val Completeness.label: String
    get() = when (this) {
        Completeness.SEALED -> "Sealed"
        Completeness.COMPLETE -> "Complete"
        Completeness.LOOSE -> "Loose"
    }
