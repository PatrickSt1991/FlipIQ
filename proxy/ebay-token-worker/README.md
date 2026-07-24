# FlipIQ eBay token proxy (Cloudflare Worker)

Returns a short-lived eBay **application** token so the Android app never ships the eBay secret.

## Deploy
```bash
cd proxy/ebay-token-worker
npm i -g wrangler          # if needed
wrangler login
wrangler secret put EBAY_CLIENT_ID       # paste your eBay Client ID
wrangler secret put EBAY_CLIENT_SECRET   # paste your eBay Client Secret
wrangler secret put APP_KEY              # optional: any random string
wrangler deploy
```
`wrangler deploy` prints the Worker URL, e.g. `https://flipiq-ebay-token.<subdomain>.workers.dev`.

## Point the app at it
In the repo's `local.properties` (gitignored):
```
ebay.proxyUrl=https://flipiq-ebay-token.<subdomain>.workers.dev
ebay.proxyKey=<the APP_KEY you set, or leave blank>
```
Rebuild — eBay goes live. Leave `ebay.proxyUrl` blank and eBay simply shows a search link.

## Test
```bash
curl -H "X-App-Key: <APP_KEY>" https://flipiq-ebay-token.<subdomain>.workers.dev
# → {"access_token":"v^1.1#...","expires_in":7200}
```

The secret stays in the Worker (encrypted). Rotate anytime with `wrangler secret put`.
