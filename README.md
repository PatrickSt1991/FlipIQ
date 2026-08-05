<p align="center">
  <img src="art/Valoo-logo.svg" alt="Valoo" width="380">
</p>

<p align="center">
  📦 Gratis, open-source barcodescanner &amp; inkoopassistent voor resellers.<br>
  Scan een game, console, dvd, LEGO-set of boek en zie meteen wat het waard is op de Nederlandse tweedehandsmarkt — en of het slim is om te kopen.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android-brightgreen" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-100%25-purple" alt="Kotlin">
  <img src="https://img.shields.io/badge/status-alpha-orange" alt="Status">
</p>

---

## Waarom?

Er is geen gratis Android-app die snelle, betrouwbare prijsinschattingen geeft voor tweedehands spullen, gemaakt voor de reseller. Valoo is het open-source alternatief voor apps zoals de CeX-scanner — maar dan voor de **Nederlandse markt**. Het beantwoordt de enige vraag die telt:

> **"Moet ik dit kopen?"**

## Hoe het werkt

1. **Scan** een barcode, maak een foto van de voorkant, of fotografeer een hele stapel (haul-modus).
2. Valoo haalt live Nederlandse prijzen op en de **Valoo Engine** beoordeelt het item.
3. Je ziet in één oogopslag een **Dealscore**, een maximale inkoopprijs en de verwachte winst.

## Functies

| | |
|---|---|
| 📷 **Scannen** | UPC/EAN/ISBN + QR via ML Kit, snel & offline. Foto van de voorkant (AI) en OCR als terugval. |
| 🧺 **Haul-modus** | Eén foto van een stapel → elke titel geprijsd en gerangschikt. |
| 🧠 **Valoo Engine** | Zet ruwe prijzen om in een koop-/niet-kopen-oordeel (zie onder). |
| 🎯 **Dealscore** | Score 0–100 + maximale inkoopprijs per item. |
| ⚡ **Verkoopsnelheid** | Hoe snel iets doorgaans verkoopt. |
| ⭐ **Winstmodus** | Elke aanbeveling past zich aan jouw marge-/ROI-regels aan. |
| 📚 **Collectie** | Voorraad, winsttracker, inkoophistorie, favorieten, verlanglijst, handmatig toevoegen, CSV-export. |
| 🔔 **Prijsmeldingen** | Krijg bericht als een item zakt naar jouw streefprijs. |
| 🌍 **NL / EN** | Volledig Nederlandse en Engelse interface. |

## Prijsbronnen

Valoo richt zich op de **Nederlandse tweedehandsmarkt**, dus elke prijs is een NL/EUR-bedrag:

- **eBay.nl** — actieve advertenties (officiële Browse API) + verkochte prijzen.
- **Marktplaats** — actuele Nederlandse vraagprijzen.

Prijzen worden serverkant samengevoegd door de Valoo Engine (een Cloudflare Worker) en gecachet om het aantal verzoeken laag te houden. Er zijn "open op marktplaats"-snelkoppelingen voor eBay en Marktplaats.

## De Valoo Engine

De engine toont niet alleen prijzen — hij analyseert de markt en bepaalt of een item de moeite waard is om te kopen.

**Kijkt naar:** gemiddelde/mediaan verkoopprijs, aantal recente verkopen, verkoopsnelheid, markttrend, marktplaatskosten, verzendkosten, jouw winstdoelen & ROI, staat van het item, compleet-vs-los, en marktvertrouwen.

**Geeft terug:** aanbevolen inkoopprijs · geschatte verkoop · verwachte winst · ROI · vertrouwen · Dealscore · Verkoopsnelheid.

### Dealscore & verkoopsnelheid

| Score | Oordeel | | Snelheid | Doorlooptijd |
|------:|---------|---|---|---|
| 🟢 90–100 | Direct kopen | | ⚡ Heel snel | binnen enkele dagen |
| 🟢 75–89 | Topdeal | | 🚀 Snel | 1–2 weken |
| 🟡 50–74 | Redelijke prijs | | ⏳ Gemiddeld | 2–8 weken |
| 🟠 25–49 | Weinig winst | | 🐢 Langzaam | maanden |
| 🔴 0–24 | Overslaan | | | |

### Winstmodus

Stel het één keer in (minimale winst, minimale ROI, prijsdrempel, incompleet/beschadigd negeren, minimaal aantal verkopen, snelle verkopers voortrekken, kosten & verzending meerekenen) en de engine herberekent bij elke scan de maximale inkoopprijs, verwachte winst, ROI, Dealscore en verkoopsnelheid.

## Voorbeeld

```text
🎮 LEGO Jurassic World (PS4)

Mediaan verkocht  €11,50        🟢 Dealscore   91/100
Gesch. verkoop    €11,95        Vertrouwen     92%
                                Verkoopsnelheid 🚀 Snel
Inkoopladder
  🟢 Uitstekend   ≤ €5,00       Verwachte winst  €5,45
  🟢 Goed         ≤ €7,00       Geschatte ROI    46%
  🟡 Redelijk     ≤ €9,00
  🔴 Overslaan    > €10,00      Open: [eBay.nl] [Marktplaats]
```

## Categorieën

Games · consoles · controllers & accessoires · LEGO · boeken · dvd's · Blu-rays · elektronica · speelgoed · verzamelobjecten · verzamelkaarten.

## Over Reway

Valoo maakt **geen** gebruik van Reway (reway.nl / rewayverkopen.nl). Een eerdere versie experimenteerde met het tonen van Reway's inkoop- en verkoopprijzen, maar Reway heeft ons gevraagd hun data niet te gebruiken of te scrapen, en hun beleid staat dat niet toe. Wij respecteren die keuze volledig: alle Reway-integratie is verwijderd en de app vraagt, toont of linkt op geen enkele manier naar hun data.

## Techniek

Kotlin · Jetpack Compose · Material 3 · ML Kit · Room · Retrofit · Coroutines · Hilt · Coil · Cloudflare Workers (engine).

## Bijdragen

Ideeën, bugmeldingen en pull requests zijn welkom — open een issue of PR.

---

Valoo is een onafhankelijk open-source project en is **niet gelieerd aan of goedgekeurd door** eBay, Marktplaats of enige andere marktplaats. Alle merknamen zijn eigendom van hun respectievelijke eigenaren.
