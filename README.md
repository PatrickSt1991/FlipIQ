<p align="center">
  <img src="art/FlipIQ-logo.svg" alt="FlipIQ" width="380">
</p>

<p align="center">
  📦 Free, open-source barcode scanner &amp; buying assistant for resellers.<br>
  Scan a game, console, DVD, LEGO set or book and instantly see what it's worth on the Dutch second-hand market — and whether it's worth buying.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License">
  <img src="https://img.shields.io/badge/platform-Android-brightgreen" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-100%25-purple" alt="Kotlin">
  <img src="https://img.shields.io/badge/status-alpha-orange" alt="Status">
</p>

---

## Why?

There's no free Android app that gives fast, reseller-focused price estimates for second-hand items. FlipIQ is the open-source alternative to apps like the CeX scanner — but built for the **Dutch market**, and it answers the only question that matters:

> **"Should I buy this?"**

## How it works

1. **Scan** a barcode, snap the cover, or photograph a whole pile (haul mode).
2. FlipIQ pulls live Dutch prices and the **FlipIQ Engine** scores the item.
3. You get a **Deal Score**, a max buy price, and expected profit — in one glance.

## Features

| | |
|---|---|
| 📷 **Scanning** | UPC/EAN/ISBN + QR via ML Kit, offline & fast. Cover-photo (AI) and OCR fallback. |
| 🧺 **Haul mode** | One photo of a pile → every title priced and ranked. |
| 🧠 **FlipIQ Engine** | Turns raw prices into a buy/skip verdict (see below). |
| 🎯 **Deal Score** | 0–100 score + max buy price per item. |
| ⚡ **Sell Speed** | How fast it typically sells. |
| ⭐ **Profit Mode** | Every recommendation adapts to your margin/ROI rules. |
| 📚 **Collection** | Inventory, profit tracker, purchase history, favorites, wishlist, manual add, CSV export. |
| 🔔 **Price alerts** | Get notified when a watched item drops to your target. |
| 🌍 **EN / NL** | Full Dutch and English UI. |

## Data sources

FlipIQ is focused on the **Dutch second-hand market**, so every price is an NL/EUR figure:

- **eBay.nl** — active listings (official Browse API) + sold comps.
- **Marktplaats** — active Dutch asking prices.

Prices are aggregated server-side by the [FlipIQ Engine](#the-flipiq-engine) (a Cloudflare Worker) and cached to keep request volume low. One-tap "open on marketplace" shortcuts are provided for eBay and Marktplaats.

## The FlipIQ Engine

The engine doesn't just show prices — it analyses the market and decides whether an item is worth buying.

**Looks at:** sold average/median, number of recent sales, sales velocity, market trend, marketplace fees, shipping, your profit goals & ROI, item condition, complete-vs-loose, and market confidence.

**Returns:** recommended buy price · estimated resale · expected profit · ROI · confidence · Deal Score · Sell Speed.

### Deal Score

| Score | Verdict | | Sell Speed | Typical time |
|------:|---------|---|---|---|
| 🟢 90–100 | Buy Immediately | | ⚡ Very Fast | within days |
| 🟢 75–89 | Great Deal | | 🚀 Fast | 1–2 weeks |
| 🟡 50–74 | Fair Price | | ⏳ Medium | 2–8 weeks |
| 🟠 25–49 | Low Profit | | 🐢 Slow | months |
| 🔴 0–24 | Skip | | | |

### Profit Mode

Configure it once (min profit, min ROI, price floor, ignore incomplete/damaged, min sales, prefer fast sellers, include fees & shipping) and the engine recalculates the max buy price, expected profit, ROI, Deal Score and Sell Speed on every scan.

## Example

```text
🎮 LEGO Jurassic World (PS4)

Median sold      €11.50        🟢 Deal Score  91/100
Est. resale      €11.95        Confidence     92%
                               Sell Speed     🚀 Fast
Buy ladder
  🟢 Excellent   ≤ €5.00       Expected profit  €5.45
  🟢 Good        ≤ €7.00       Estimated ROI    46%
  🟡 Fair        ≤ €9.00
  🔴 Skip        > €10.00      Open: [eBay.nl] [Marktplaats]
```

## Categories

Video games · consoles · controllers & accessories · LEGO · books · DVDs · Blu-rays · electronics · toys · collectibles · trading cards.

## Roadmap

- **Shipped** — barcode/cover/OCR scanning, haul mode, FlipIQ Engine, Deal Score, Sell Speed, Profit Mode, collection & profit tracker, price alerts, stats, dark mode, CSV export, EN/NL.
- **Planned** — local database cache, richer statistics, more open/official data sources.
- **Ideas** — cloud sync, shared collections, Wear OS, desktop companion, browser extension.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · ML Kit · Room · Retrofit · Coroutines · Hilt · Coil · Cloudflare Workers (engine).

## Contributing

Ideas, bug reports and pull requests are very welcome — open an issue or a PR.

## License

MIT.

---

### Disclaimer

FlipIQ is an independent open-source project, **not affiliated with or endorsed by** eBay, Marktplaats or any other marketplace. All trademarks belong to their respective owners.

**A note on Reway:** FlipIQ does **not** use Reway (reway.nl / rewayverkopen.nl). An earlier prototype experimented with their prices, but Reway asked us not to use or scrape their data and their policy doesn't permit it. We respect that — all Reway integration has been removed. For reference data, use official, openly-licensed databases that permit commercial use.
