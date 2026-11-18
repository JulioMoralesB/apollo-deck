# Apollo Deck

Android + Wear OS app for a homelab, backed by **Home Assistant** — home-screen widgets and Wear OS tiles to see at-a-glance summaries and trigger actions.

> **This app is a client, not a standalone product.** Every screen, widget, and tile talks to a Home Assistant instance reachable from the phone (here, through a Cloudflare tunnel), authenticated with a long-lived access token. It started as a companion for [apollo-server-dashboard](https://github.com/JulioMoralesB/apollo-server-dashboard); once the homelab host began running only ~1 h/day, Home Assistant — always on — took over as the backend. HA polls each service's `/api/summary` while the host is up and keeps the last good response in a `sensor.*_summary` template sensor (a `summary` attribute holding the raw JSON); the app reads those.

## Status

Both the phone and Wear OS apps are functional.

- **Phone app** — Home Assistant URL + long-lived token login, a Stream Deck-style dashboard (service grid → action grid, in-place navigation) with a summary panel for any service that exposes one, a settings screen (including icon overrides and watch-tile configuration), and three home-screen widget types: pin a single action, pin one service's summary, or a combined summary of every service that has one.
- **Wear OS app** — its own standalone login (independent of the phone, though "Sign In from Phone" syncs the phone's session automatically when available), and two tile types: one pinned action (tap-to-run), and a phone-configured multi-action grid.

## Project structure

```
.
├── core/   # Shared networking, auth, secure storage, and the summary feature's shared models (used by app + wear)
├── app/    # Phone app: login, dashboard, settings, home-screen widgets (Jetpack Compose; widgets render via plain RemoteViews)
├── wear/   # Wear OS app: standalone login, action tile, multi-action grid tile (Wear Compose + protolayout)
└── gradle/libs.versions.toml   # Centralized dependency versions
```

### `core/`

Shared by `app` and `wear`:

- `net/ApiClient.kt`, `HomeAssistantApi.kt` — Retrofit + OkHttp client for Home Assistant's REST API; `ApolloDeckClient.getServices()` turns every `sensor.*` with a `summary` attribute into a service, `fetchSummary()` returns that attribute's raw JSON
- `net/Interceptors.kt` — attaches the long-lived token as `Authorization: Bearer`
- `net/ActionExecutor.kt` — calls an action's method + endpoint against the HA base URL and returns the result
- `model/SummaryModels.kt`, `SummaryFormat.kt` — parses a service's summary response into a typed `ServiceSummary` by dispatching on response shape, and formats dates by local calendar day
- `store/TokenStore.kt`, `ServerConfigStore.kt` — `EncryptedSharedPreferences` (Android Keystore-backed) wrappers for the HA token and URL
- `sync/` — shared wire formats for phone↔watch Data Layer sync (session transfer, tile grid selection, icon overrides)
- `auth/HomeAssistantAuth.kt` — connect (save URL + token, verify with `GET /api/`) / disconnect

### `app/`

- `ui/login/` — Home Assistant URL + long-lived access token
- `ui/dashboard/` — service grid (adaptive columns) → tapping a service opens its action grid in place (no modal), with that service's summary shown above the actions when it has one; pull-to-refresh; long-press a service for a one-tap shortcut when it has exactly one action
- `ui/settings/` — edit the Home Assistant URL, configure the watch's multi-action tile, and set icon overrides
- `ui/icons/` — maps the dashboard's Lucide icon-name strings (from `services.yaml`) to Material Icons Extended equivalents (Lucide has no Compose port), plus a per-name override screen for anything the mapping misses
- `wearsync/` — publishes the phone's session and watch-tile/icon-override selections to the paired watch over the Wear Data Layer
- `widget/` — three home-screen widget types (see below)

### Home-screen widgets

All three render via plain `RemoteViews`/`AppWidgetProvider`, not Jetpack Glance — Glance's session/recomposition layer proved unreliable for background-triggered updates (tap feedback, WorkManager-driven status revert) on real hardware.

- **Action widget** — pin a single dashboard action, run it with one tap. Placing it launches a picker (grouped by service; `href` actions are excluded since those open a link rather than call the backend). Reuses the phone app's stored token — no separate widget login, no interactive re-auth possible. Runs on tap with no confirmation dialog (a widget can't show one without launching an Activity); a `confirm: true` action instead gets a persistent amber "⚠" as an ongoing visual reminder, both in the picker and on the placed widget.
- **Summary widget** — pin one service's summary (up to three condensed lines — e.g. Free Games Notifier's soonest-ending promo, or CaduTrack's expiring-soon count and next item due). Only services with a summary sensor show up in its picker.
- **Combined summary widget** — no configuration; automatically shows one row per service that has a summary, refreshed in the background every 15 minutes. Tapping a row opens the app directly into that service's own panel.

## Wear OS

Standalone (`standalone=true`, doesn't require the phone nearby), but the login screen leads with "Sign In from Phone" when a paired phone has a synced session — typing a Home Assistant URL and access token on a watch keyboard is painful, so this is the easy path; manual entry is the fallback.

- **Action tile** — the watch equivalent of the phone's action widget: one pinned action, a big circular tap-to-run button.
- **Multi-action grid tile** — a grid of icon-only buttons. Which actions appear, and in what order, is configured from the phone app (Settings → "Configure watch tile"), not on the watch itself. A `confirm: true` action needs two taps (arm, then run within a few seconds), since a tile can't distinguish a long-press from a tap.

Both tiles render entirely from a local cache — the system aggressively times out a tile that does network I/O while rendering, so a background `WorkManager` job keeps that cache warm instead.

## Requirements

- Android Studio (latest stable)
- JDK 17
- A Home Assistant instance reachable from the phone, and a long-lived access token for it (HA → Profile → Security)

## Building

Open the project root in Android Studio and sync — the Gradle wrapper is committed, so no extra setup is needed. Alternatively, from the command line:

```bash
./gradlew build
```

## Auth

A Home Assistant long-lived access token, sent as `Authorization: Bearer` on every request. It doesn't expire, so there's no refresh step — which is what lets a home-screen widget or Wear tile keep working in the background, where no interactive login prompt is possible. The login screen verifies URL + token with `GET /api/` before saving.
