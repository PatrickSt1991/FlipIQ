/**
 * FlipIQ eBay token proxy (Cloudflare Worker).
 *
 * Holds the eBay client id/secret as encrypted secrets and returns a short-lived *application*
 * token (client-credentials grant) so the Android app never ships the secret.
 *
 * Secrets (set with `wrangler secret put ...`):
 *   EBAY_CLIENT_ID       your eBay app's Client ID
 *   EBAY_CLIENT_SECRET   your eBay app's Client Secret
 *   APP_KEY   (optional) shared key the app must send as X-App-Key; soft abuse gate
 *
 * The app calls this Worker's URL and expects: { "access_token": "...", "expires_in": 7200 }
 */
export default {
  async fetch(request, env) {
    // Optional soft gate so random callers can't drain your eBay quota.
    if (env.APP_KEY && request.headers.get("X-App-Key") !== env.APP_KEY) {
      return json({ error: "forbidden" }, 403);
    }
    if (!env.EBAY_CLIENT_ID || !env.EBAY_CLIENT_SECRET) {
      return json({ error: "proxy not configured" }, 500);
    }

    const basic = btoa(`${env.EBAY_CLIENT_ID}:${env.EBAY_CLIENT_SECRET}`);
    const resp = await fetch("https://api.ebay.com/identity/v1/oauth2/token", {
      method: "POST",
      headers: {
        Authorization: `Basic ${basic}`,
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body:
        "grant_type=client_credentials" +
        "&scope=" + encodeURIComponent("https://api.ebay.com/oauth/api_scope"),
    });

    const data = await resp.json().catch(() => ({}));
    if (!resp.ok || !data.access_token) {
      return json({ error: "token request failed", detail: data }, 502);
    }
    return json({ access_token: data.access_token, expires_in: data.expires_in });
  },
};

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
