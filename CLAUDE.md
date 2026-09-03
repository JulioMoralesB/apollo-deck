# CLAUDE.md

Guidance for Claude Code in this repo.

## What This Is

Android + Wear OS companion app for [apollo-server-dashboard](https://github.com/JulioMoralesB/apollo-server-dashboard). Pure client — no functionality without a running dashboard backend. Primary use case: pin individual dashboard actions as home-screen widgets / Wear tiles, not just a mini dashboard.

## Commands

```bash
./gradlew build              # full build
./gradlew :app:assembleDebug # phone app debug APK
./gradlew clean assembleDebug --console=plain 2>&1 | tail -60   # verify after changes
```

No emulator/device available in this environment — a successful `assembleDebug` is the verification bar, not a manual run.

## Modules

- `core/` — shared networking (Retrofit/OkHttp), auth (JWT access/refresh via `TokenAuthenticator`, transparent silent refresh), `EncryptedSharedPreferences` storage (`TokenStore`, `ServerConfigStore`, `CloudflareAccessStore`). `retrofit-core` is exposed as `api` (not `implementation`) so callers can catch `HttpException`.
- `app/` — phone app: login, dashboard (`ui/dashboard/`), settings, home-screen widget (`widget/`). Configure UI is Compose (`WidgetSetupScreen.kt`); the widget itself renders via plain `RemoteViews`/`AppWidgetProvider` built directly in `WidgetRemoteViews.kt`, not Jetpack Glance — Glance's session/recomposition layer was found unreliable for background-triggered updates (tap feedback, WorkManager-driven revert) on-device, so it was dropped in favor of pushing `RemoteViews` straight to `AppWidgetManager`.
- `wear/` — Wear OS app (`standalone=true`, no phone pairing required, but the login screen's primary path uses one when available — see below). Has its own login (`presentation/login/`, reuses core's `AuthRepository`/`ApiClient`) and an actions list (`presentation/actions/`) that fetches `/services` and executes actions — no `TextField` in Wear Compose Material, so input fields are hand-rolled from `BasicTextField`; `confirm: true` actions require a long-press instead of a dialog. No icons yet — `IconMapping` lives in `app/` (Compose Material Icons, not in `core`).
  - Tiles (`wear/.../tile/`): `ActionTileService` (one pinned action, mirrors the phone widget) and `MultiActionTileService` (top 4 actions across all services, no config). This module is `compileSdk = 35` — one higher than `app`/`core` — because `androidx.wear.tiles:tiles:1.6.0` requires it (needed AGP bumped to 8.6.1 too). A Tile's `Clickable` can only launch an Activity or fire a `LoadAction` (re-request the tile) — there's no PendingIntent-to-arbitrary-broadcast the way RemoteViews has — so "run the action" is: `Clickable` sets a `LoadAction`, `onTileRequest` reads the click back via `requestParams.currentState.lastClickableId`, and fires `TileActionReceiver` itself (execute + status + `WorkManager` revert, same shape as the widget's `ActionWidgetReceiver`/`RevertWidgetStatusWorker`) before rendering. `TileConfigActivity` (launched via `LaunchAction` from the unconfigured "Tap to configure" state) **must be `exported="true"`** — confirmed on real hardware: the tile renderer that fires the `LaunchAction` runs in a different process/UID (`com.samsung.android.wearable.sysui` on this device), and logs `Activity constraints not met. Not launching LaunchAction Activity` if it isn't. `ActionTileService`'s config is one global `SharedPreferences` entry, not keyed per tile instance (unlike the widget's per-`appWidgetId` config) — simpler, but means pinning the tile a second time (if the OS's add-tile picker even allows it — on this device it didn't reoffer an already-added tile type) would control the same single pinned action rather than a second independent one. Making that possible would mean keying config by `requestParams.tileId` instead, like the widget does by `appWidgetId`.
- Phone → watch session sync (`app/.../wearsync/`, `wear/.../sync/`): typing server URL + username + password on a watch keyboard is painful, so the watch's login screen leads with "Sign In from Phone" — the phone publishes its session as a synced Wear Data Layer `DataItem` (`PhoneSessionPublisher`, on login/logout/app-open) and the watch reads whatever's currently synced (`PhoneSessionSync`, a `DataClient.getDataItems()` pull, not a live round-trip). `core/.../sync/SessionTransfer.kt` is the shared wire format (server URL, tokens, optional Cloudflare Access creds) and the `DataItem` path. Manual entry on the watch stays as a fallback for a watch used without a phone nearby.
  - **`wear/`'s `applicationId` must exactly match `app/`'s (`com.apollox10.apollodeck`)** — Play Services' Wear Data Layer only routes `DataClient`/`MessageClient` traffic between a phone app and watch app it can verify are "the same app" (matching package name + matching signing certificate); a mismatched id makes every call between them silently fail. `namespace` can still differ (it only names the generated R/BuildConfig package) — only `applicationId` needs to match. Confirmed against real paired hardware: a `MessageClient` RPC attempt failed first (`Failed to deliver message to AppKey` in the phone's logcat) and `DataClient.getDataItems()` on the watch came back empty even minutes after a confirmed-successful `putDataItem` on the phone, both while the ids didn't match; both started working immediately once they did.

## Key Conventions

- Kotlin 2.0 Compose: apply `org.jetbrains.kotlin.plugin.compose` in every module with `buildFeatures.compose = true`.
- `kotlinx.serialization`: needs both the Gradle plugin per-module AND explicit imports `kotlinx.serialization.encodeToString`/`decodeFromString` (the reified extensions, not the base `Json` members).
- Lucide icon names (from the dashboard's `services.yaml`) have no Compose port — mapped manually to Material Icons Extended in `app/.../ui/icons/IconMapping.kt`. New service icons need an entry there.
- Widgets/tiles reuse the phone app's stored session (no separate login) and must tolerate silent background token refresh — no interactive re-auth possible.
- Widget config/status live in plain `SharedPreferences` (`apollo_deck_widgets`, keyed by `appWidgetId`), not DataStore — reads must work synchronously from `BroadcastReceiver.onReceive` and from a `CoroutineWorker`. A placed widget supports long-press → reconfigure (`widgetFeatures="reconfigurable"`).
- Match the web dashboard's action model: `method: "href"` = external link (skip in anything that "executes" an action), `confirm: true` = should get a visual warning where no confirm dialog is possible (e.g. widgets).

## Workflow

Always use PRs — never push to `main` directly. Verify Kotlin/Gradle changes with a real `./gradlew` build before calling work done, not just code review.
