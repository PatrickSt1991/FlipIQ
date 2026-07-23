# FlipIQ

> 📦 Free and open-source barcode scanner and buying assistant for resellers.
>
> Instantly estimate the value of games, consoles, DVDs, Blu-rays, LEGO, books, collectibles and electronics by comparing multiple marketplaces.

![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple)
![Status](https://img.shields.io/badge/status-Planning-orange)

---

# Why?

There is currently no free Android application that provides fast, reliable and reseller-focused price estimates for second-hand items.

FlipIQ aims to become the open-source alternative to apps like CeX Scanner by combining multiple marketplaces into a single, lightning-fast interface.

Instead of only showing prices, FlipIQ helps users answer the only question that really matters:

> **"Should I buy this?"**

---

# Features

## 📷 Barcode Scanning

- UPC / EAN / ISBN support
- QR Code support
- Google ML Kit
- Extremely fast scanning
- Offline barcode recognition

## 💰 Price Comparison

Compare prices from multiple sources simultaneously.

- eBay Sold Listings
- CeX
- PriceCharting
- Vinted
- Marktplaats
- Amazon (optional)
- More sources coming soon

## 📊 Smart Pricing

Powered by the **FlipIQ Engine**.

The FlipIQ Engine automatically calculates:

- Average sold price
- Median sold price
- Lowest sale
- Highest sale
- Estimated resale value
- Maximum buying price
- Expected profit
- ROI %

---

# 💡 Buy Assistant

FlipIQ isn't just a price checker.

It's your personal buying assistant.

Every scan instantly tells you whether an item is worth buying.

---

# 🧠 FlipIQ Engine

The **FlipIQ Engine** is the intelligent pricing engine behind every recommendation.

Unlike traditional price checkers, it doesn't simply show prices.

It analyses market data and tells you whether an item is actually worth buying.

### The FlipIQ Engine analyses

- Average sold price
- Median sold price
- Number of recent sales
- Sales velocity
- Current market trend
- Marketplace fees
- Shipping costs
- User-defined profit goals
- Desired ROI
- Item condition
- Complete vs Loose
- Market confidence

Then calculates:

- ✅ Recommended Buy Price
- ✅ Estimated Resale Price
- ✅ Expected Profit
- ✅ ROI
- ✅ Confidence Score
- ✅ Deal Score
- ✅ Sell Speed

---

# 🎯 Deal Score

Every item receives a **Deal Score (0–100)**.

| Score | Recommendation |
|-------:|----------------|
| 🟢 90–100 | Buy Immediately |
| 🟢 75–89 | Great Deal |
| 🟡 50–74 | Fair Price |
| 🟠 25–49 | Low Profit |
| 🔴 0–24 | Skip |

---

# ⚡ Sell Speed

Knowing **how fast** something sells is just as important as knowing **how much** it sells for.

Every item receives a Sell Speed prediction.

| Speed | Meaning |
|--------|---------|
| ⚡ Very Fast | Usually sells within days |
| 🚀 Fast | Usually sells within 1–2 weeks |
| ⏳ Medium | Usually sells within 2–8 weeks |
| 🐢 Slow | May take several months |

---

# ⭐ Profit Mode

Configure FlipIQ once.

The app adapts every recommendation to your personal flipping strategy.

Example settings:

- Minimum Profit: **€5**
- Minimum ROI: **30%**
- Ignore items below **€10**
- Ignore incomplete items
- Ignore damaged items
- Ignore items with fewer than 5 sales
- Prefer fast-selling items
- Include shipping costs
- Include marketplace fees

The FlipIQ Engine automatically recalculates:

- Maximum buying price
- Expected profit
- ROI
- Deal Score
- Sell Speed

No manual calculations required.

---

# Example

```text
🎮 LEGO Jurassic World (PS4)

Average Sold
€11.95

Median Sold
€11.50

CeX Buy
€7.00

────────────────────────

🟢 Deal Score
91 / 100

Confidence
92%

Sell Speed
🚀 Fast

────────────────────────

🟢 Excellent Deal
Buy below €5.00

🟢 Good Deal
Buy below €7.00

🟡 Fair Deal
Buy below €9.00

🔴 Skip
Above €10.00

────────────────────────

Expected Profit
€5.45

Estimated ROI
46%

Open:
[eBay Sold]
[Vinted]
[Marktplaats]
[CeX]
```

---

# 📦 Categories

- Video Games
- Consoles
- Controllers
- Gaming Accessories
- LEGO
- Books
- DVDs
- Blu-rays
- Electronics
- Toys
- Collectibles
- Trading Cards

---

# 📚 Collection

- Scan History
- Purchase History
- Inventory
- Profit Tracker
- Favorites
- Wishlist

---

# 🔗 Marketplace Shortcuts

Open any marketplace with a single tap.

- eBay Sold
- Vinted
- Marktplaats
- CeX
- PriceCharting

---

# 🚀 Roadmap

## MVP

- Barcode Scanner
- eBay Sold Lookup
- PriceCharting Lookup
- Marketplace Shortcuts
- FlipIQ Engine
- Deal Score

## Version 1.0

- Profit Mode
- Sell Speed
- Purchase History
- Inventory
- Dark Mode
- Export CSV

## Version 2.0

- OCR Support
- AI Image Recognition
- Price Alerts
- Statistics Dashboard
- Wishlist
- Local Database Cache

## Future Ideas

- Cloud Sync
- Shared Collections
- Android Auto
- Wear OS
- Desktop Companion
- Browser Extension
- AI-powered Price Prediction

---

# 🛠 Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Google ML Kit
- Room
- Retrofit
- Coroutines
- Hilt
- Coil

---

# 🤝 Contributing

Contributions are welcome!

Ideas, bug reports, feature requests and pull requests are greatly appreciated.

If you have an idea that can improve FlipIQ or the FlipIQ Engine, feel free to open an issue or submit a PR.

---

# 📄 License

MIT

---

# ⚠ Disclaimer

FlipIQ is an independent open-source project.

It is **not affiliated with or endorsed by** eBay, CeX, Amazon, PriceCharting, Vinted, Marktplaats or any other marketplace.

All trademarks belong to their respective owners.