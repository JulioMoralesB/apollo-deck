# Apollo Deck

Android + Wear OS companion app for [**apollo-server-dashboard**](https://github.com/JulioMoralesB/apollo-server-dashboard) — home-screen widgets and Wear OS tiles to check service status, see at-a-glance summaries, and trigger actions without opening the web dashboard.

> **This app is a client, not a standalone product.** It has no functionality on its own — every screen, widget, and tile talks to a running instance of [apollo-server-dashboard](https://github.com/JulioMoralesB/apollo-server-dashboard)'s backend API (`/services`, `/auth/*`, action and summary endpoints). You need a self-hosted apollo-server-dashboard instance reachable from your phone (LAN or via a tunnel) before this app does anything useful.

## Status

Both the phone and Wear OS apps are functional.

- **Phone app** — username/password login (with Cloudflare Access support), a Stream Deck-style dashboard (service grid → action grid, in-place navigation) with a summary panel for any service that exposes one, a settings screen (including icon overrides and watch-tile configuration), and three home-screen widget types: pin a single action, pin one service's summary, or a combined summary of every service that has one.
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

- `net/ApiClient.kt`, `ApolloDeckApi.kt` — Retrofit + OkHttp client for the dashboard's REST API
- `net/TokenAuthenticator.kt` — OkHttp `Authenticator` that transparently refreshes an expired access token and retries the request once, mirroring the web dashboard's `authFetch` behavior
- `net/Interceptors.kt` — attaches the bearer token and, when configured, Cloudflare Access service-token headers
- `net/ActionExecutor.kt` — calls an action's endpoint (or a service's summary endpoint) and returns the result
- `model/SummaryModels.kt`, `SummaryFormat.kt` — parses a service's summary response into a typed `ServiceSummary` by dispatching on response shape, and formats dates by local calendar day
- `store/TokenStore.kt`, `ServerConfigStore.kt`, `CloudflareAccessStore.kt` — `EncryptedSharedPreferences` (Android Keystore-backed) wrappers for tokens, the configured server URL, and optional Cloudflare Access credentials
- `sync/` — shared wire formats for phone↔watch Data Layer sync (session transfer, tile grid selection, icon overrides)
- `auth/AuthRepository.kt` — login/refresh calls against `/auth/*`

### `app/`

- `ui/login/` — username/password + server URL fields, optional Cloudflare Access Service Token fields, autofill support (Bitwarden, etc.)
- `ui/dashboard/` — service grid (adaptive columns) → tapping a service opens its action grid in place (no modal), with that service's summary shown above the actions when it has one; pull-to-refresh; long-press a service for a one-tap shortcut when it has exactly one action
- `ui/settings/` — edit server URL and Cloudflare Access credentials, configure the watch's multi-action tile, and set icon overrides
- `ui/icons/` — maps the dashboard's Lucide icon-name strings (from `services.yaml`) to Material Icons Extended equivalents (Lucide has no Compose port), plus a per-name override screen for anything the mapping misses
- `wearsync/` — publishes the phone's session and watch-tile/icon-override selections to the paired watch over the Wear Data Layer
- `widget/` — three home-screen widget types (see below)

### Home-screen widgets

All three render via plain `RemoteViews`/`AppWidgetProvider`, not Jetpack Glance — Glance's session/recomposition layer proved unreliable for background-triggered updates (tap feedback, WorkManager-driven status revert) on real hardware.

- **Action widget** — pin a single dashboard action, run it with one tap. Placing it launches a picker (grouped by service; `href` actions are excluded since those open a link rather than call the backend). Reuses the phone app's session, refreshed transparently in the background — no separate widget login, no interactive re-auth possible. Runs on tap with no confirmation dialog (a widget can't show one without launching an Activity); a `confirm: true` action instead gets a persistent amber "⚠" as an ongoing visual reminder, both in the picker and on the placed widget.
- **Summary widget** — pin one service's summary (up to three condensed lines — e.g. Free Games Notifier's soonest-ending promo, or CaduTrack's expiring-soon count and next item due). Only services with a summary endpoint show up in its picker.
- **Combined summary widget** — no configuration; automatically shows one row per service that has a summary, refreshed in the background every 15 minutes. Tapping a row opens the app directly into that service's own panel.

## Wear OS

Standalone (`standalone=true`, doesn't require the phone nearby), but the login screen leads with "Sign In from Phone" when a paired phone has a synced session — typing a server URL, username, and password on a watch keyboard is painful, so this is the easy path; manual entry is the fallback.

- **Action tile** — the watch equivalent of the phone's action widget: one pinned action, a big circular tap-to-run button.
- **Multi-action grid tile** — a grid of icon-only buttons. Which actions appear, and in what order, is configured from the phone app (Settings → "Configure watch tile"), not on the watch itself. A `confirm: true` action needs two taps (arm, then run within a few seconds), since a tile can't distinguish a long-press from a tap.

Both tiles render entirely from a local cache — the system aggressively times out a tile that does network I/O while rendering, so a background `WorkManager` job keeps that cache warm instead.

## Requirements

- Android Studio (latest stable)
- JDK 17
- A running apollo-server-dashboard instance to point the app at (see its [self-hosting docs](https://github.com/JulioMoralesB/apollo-server-dashboard#self-hosting))

## Building

Open the project root in Android Studio and sync — the Gradle wrapper is committed, so no extra setup is needed. Alternatively, from the command line:

```bash
./gradlew build
```

## Auth

Uses the same username/password + JWT access/refresh flow as the web dashboard (see apollo-server-dashboard's `POST /auth/login` and `POST /auth/refresh`). A refresh token is what lets a home-screen widget or Wear tile silently renew its session in the background — neither can show an interactive login prompt when a token expires. `core/net/TokenAuthenticator.kt` handles this for every request made through `ApiClient`, regardless of caller.

Optionally, a Cloudflare Access Service Token (Client ID + Secret) can be configured in login or settings, sent as `CF-Access-Client-Id`/`CF-Access-Client-Secret` headers — useful when the dashboard is fronted by Cloudflare Access.
