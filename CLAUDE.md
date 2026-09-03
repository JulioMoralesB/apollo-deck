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
- `wear/` — Wear OS app, currently a placeholder. Planned: standalone login (independent of phone — `standalone=true` in its manifest), single + multi-action tiles.

## Key Conventions

- Kotlin 2.0 Compose: apply `org.jetbrains.kotlin.plugin.compose` in every module with `buildFeatures.compose = true`.
- `kotlinx.serialization`: needs both the Gradle plugin per-module AND explicit imports `kotlinx.serialization.encodeToString`/`decodeFromString` (the reified extensions, not the base `Json` members).
- Lucide icon names (from the dashboard's `services.yaml`) have no Compose port — mapped manually to Material Icons Extended in `app/.../ui/icons/IconMapping.kt`. New service icons need an entry there.
- Widgets/tiles reuse the phone app's stored session (no separate login) and must tolerate silent background token refresh — no interactive re-auth possible.
- Widget config/status live in plain `SharedPreferences` (`apollo_deck_widgets`, keyed by `appWidgetId`), not DataStore — reads must work synchronously from `BroadcastReceiver.onReceive` and from a `CoroutineWorker`. A placed widget supports long-press → reconfigure (`widgetFeatures="reconfigurable"`).
- Match the web dashboard's action model: `method: "href"` = external link (skip in anything that "executes" an action), `confirm: true` = should get a visual warning where no confirm dialog is possible (e.g. widgets).

## Workflow

Always use PRs — never push to `main` directly. Verify Kotlin/Gradle changes with a real `./gradlew` build before calling work done, not just code review.
