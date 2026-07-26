package nl.madebypatrick.flipiq.ui.result

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import nl.madebypatrick.flipiq.R
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
    @Composable get() = when (this) {
        DealTier.BUY_IMMEDIATELY -> stringResource(R.string.tier_buy_immediately)
        DealTier.GREAT_DEAL -> stringResource(R.string.tier_great_deal)
        DealTier.FAIR_PRICE -> stringResource(R.string.tier_fair_price)
        DealTier.LOW_PROFIT -> stringResource(R.string.tier_low_profit)
        DealTier.SKIP -> stringResource(R.string.tier_skip)
    }

val DealTier.color: Color
    get() = when (this) {
        DealTier.BUY_IMMEDIATELY -> ScoreGreen
        DealTier.GREAT_DEAL -> ScoreLime
        DealTier.FAIR_PRICE -> ScoreAmber
        DealTier.LOW_PROFIT -> ScoreOrange
        DealTier.SKIP -> ScoreRed
    }

/** Friendly, human headline for the verdict card. */
val DealTier.headline: String
    @Composable get() = when (this) {
        DealTier.BUY_IMMEDIATELY -> stringResource(R.string.tier_headline_buy_immediately)
        DealTier.GREAT_DEAL -> stringResource(R.string.tier_headline_great_deal)
        DealTier.FAIR_PRICE -> stringResource(R.string.tier_headline_fair_price)
        DealTier.LOW_PROFIT -> stringResource(R.string.tier_headline_low_profit)
        DealTier.SKIP -> stringResource(R.string.tier_headline_skip)
    }

val DealTier.emoji: String
    get() = when (this) {
        DealTier.BUY_IMMEDIATELY -> "🤩"
        DealTier.GREAT_DEAL -> "😄"
        DealTier.FAIR_PRICE -> "🙂"
        DealTier.LOW_PROFIT -> "😐"
        DealTier.SKIP -> "🙅"
    }

val SellSpeed.label: String
    @Composable get() = when (this) {
        SellSpeed.VERY_FAST -> stringResource(R.string.sell_speed_very_fast)
        SellSpeed.FAST -> stringResource(R.string.sell_speed_fast)
        SellSpeed.MEDIUM -> stringResource(R.string.sell_speed_medium)
        SellSpeed.SLOW -> stringResource(R.string.sell_speed_slow)
    }

val SellSpeed.emoji: String
    get() = when (this) {
        SellSpeed.VERY_FAST -> "⚡"
        SellSpeed.FAST -> "🚀"
        SellSpeed.MEDIUM -> "⏳"
        SellSpeed.SLOW -> "🐢"
    }

val SellSpeed.hint: String
    @Composable get() = when (this) {
        SellSpeed.VERY_FAST -> stringResource(R.string.sell_speed_hint_very_fast)
        SellSpeed.FAST -> stringResource(R.string.sell_speed_hint_fast)
        SellSpeed.MEDIUM -> stringResource(R.string.sell_speed_hint_medium)
        SellSpeed.SLOW -> stringResource(R.string.sell_speed_hint_slow)
    }

val MarketTrend.label: String
    @Composable get() = when (this) {
        MarketTrend.RISING -> stringResource(R.string.trend_rising)
        MarketTrend.STABLE -> stringResource(R.string.trend_stable)
        MarketTrend.FALLING -> stringResource(R.string.trend_falling)
    }

val BuyTierLevel.label: String
    @Composable get() = when (this) {
        BuyTierLevel.EXCELLENT -> stringResource(R.string.buy_tier_excellent)
        BuyTierLevel.GOOD -> stringResource(R.string.buy_tier_good)
        BuyTierLevel.FAIR -> stringResource(R.string.buy_tier_fair)
        BuyTierLevel.SKIP -> stringResource(R.string.buy_tier_skip_above)
    }

val BuyTierLevel.color: Color
    get() = when (this) {
        BuyTierLevel.EXCELLENT -> ScoreGreen
        BuyTierLevel.GOOD -> ScoreLime
        BuyTierLevel.FAIR -> ScoreAmber
        BuyTierLevel.SKIP -> ScoreRed
    }

val Condition.label: String
    @Composable get() = when (this) {
        Condition.SEALED -> stringResource(R.string.condition_sealed)
        Condition.MINT -> stringResource(R.string.condition_mint)
        Condition.GOOD -> stringResource(R.string.condition_good)
        Condition.ACCEPTABLE -> stringResource(R.string.condition_acceptable)
        Condition.POOR -> stringResource(R.string.condition_poor)
    }

val Completeness.label: String
    @Composable get() = when (this) {
        Completeness.SEALED -> stringResource(R.string.completeness_sealed)
        Completeness.COMPLETE -> stringResource(R.string.completeness_complete)
        Completeness.LOOSE -> stringResource(R.string.completeness_loose)
    }
