# Apollo Deck

Android + Wear OS companion app for [**apollo-server-dashboard**](https://github.com/JulioMoralesB/apollo-server-dashboard) — home-screen widgets and Wear OS tiles to check service status and trigger actions without opening the web dashboard.

> **This app is a client, not a standalone product.** It has no functionality on its own — every screen, widget, and tile talks to a running instance of [apollo-server-dashboard](https://github.com/JulioMoralesB/apollo-server-dashboard)'s backend API (`/services`, `/auth/*`, action endpoints). You need a self-hosted apollo-server-dashboard instance reachable from your phone (LAN or via a tunnel) before this app does anything useful.

## Status

Phone app is functional: login, dashboard, settings, and a home-screen action widget are implemented. Wear OS is still a placeholder screen — no login or tiles yet.

- **Phone app** — username/password login (with Cloudflare Access support), a Stream Deck-style dashboard (service grid → action grid, in-place navigation), a settings screen, and a home-screen widget that pins a single action for one-tap execution.
- **Wear OS app** — scaffolded module, placeholder screen only. Planned: standalone login (independent of the phone, since the Wear app runs with `standalone=true`) and single-action / multi-action grid tiles.

## Project structure

```
.
├── core/   # Shared networking, auth, and secure storage (used by app + wear)
├── app/    # Phone app: login, dashboard, settings, home-screen widget (Jetpack Compose + Glance)
├── wear/   # Wear OS app + tile (Wear Compose) — placeholder for now
└── gradle/libs.versions.toml   # Centralized dependency versions
```

### `core/`

Shared by `app` (and eventually `wear`, once it gets its own auth flow):

- `net/ApiClient.kt`, `ApolloDeckApi.kt` — Retrofit + OkHttp client for the dashboard's REST API
- `net/TokenAuthenticator.kt` — OkHttp `Authenticator` that transparently refreshes an expired access token and retries the request once, mirroring the web dashboard's `authFetch` behavior
- `net/Interceptors.kt` — attaches the bearer token and, when configured, Cloudflare Access service-token headers
- `net/ActionExecutor.kt` — calls an action's endpoint and returns an `ActionResult`
- `store/TokenStore.kt`, `ServerConfigStore.kt`, `CloudflareAccessStore.kt` — `EncryptedSharedPreferences` (Android Keystore-backed) wrappers for tokens, the configured server URL, and optional Cloudflare Access credentials
- `auth/AuthRepository.kt` — login/refresh calls against `/auth/*`

### `app/`

- `ui/login/` — username/password + server URL fields, optional Cloudflare Access Service Token fields, autofill support (Bitwarden, etc.)
- `ui/dashboard/` — service grid (2-column adaptive) → tapping a service opens its action grid in place (no modal); pull-to-refresh; long-press a service for a one-tap shortcut when it has exactly one action
- `ui/settings/` — edit server URL and Cloudflare Access credentials; saving clears the stored session and returns to login
- `ui/icons/IconMapping.kt` — maps the dashboard's Lucide icon-name strings (from `services.yaml`) to Material Icons Extended equivalents, since Lucide has no Compose port
- `widget/` — the home-screen action widget (see below)

### Home-screen action widget

Built with Jetpack Glance. Pin a single dashboard action to the home screen and run it with one tap, no need to open the app.

- Placing the widget launches `ActionWidgetConfigureActivity`, which lists your dashboard's actions (grouped by service) and lets you pick one. Actions with `method: "href"` are excluded — those open an external link rather than calling the backend, so they don't fit an execute-on-tap widget.
- The widget reuses the phone app's stored session — no separate widget login. An expired access token is refreshed transparently through the same `TokenAuthenticator` the app itself uses, so a widget can keep working in the background indefinitely.
- Tapping the widget executes the action directly, with no confirmation dialog — a Glance widget can't show one without launching an Activity, and a deliberate tap is the point. Actions marked `confirm: true` instead get a persistent amber "⚠" treatment, both in the picker and on the placed widget, as an ongoing visual reminder.
- Text-only for now; no per-action icon on the widget itself yet.

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

Uses the same username/password + JWT access/refresh flow as the web dashboard (see apollo-server-dashboard's `POST /auth/login` and `POST /auth/refresh`). A refresh token is what lets the home-screen widget silently renew its session in the background — it can't show an interactive login prompt when a token expires. `core/net/TokenAuthenticator.kt` handles this for every request made through `ApiClient`, whether from the app UI or the widget.

Optionally, a Cloudflare Access Service Token (Client ID + Secret) can be configured in login or settings, sent as `CF-Access-Client-Id`/`CF-Access-Client-Secret` headers — useful when the dashboard is fronted by Cloudflare Access.
