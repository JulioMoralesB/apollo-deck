# Apollo Deck

Android + Wear OS companion app for [**apollo-server-dashboard**](https://github.com/JulioMoralesB/apollo-server-dashboard) — home-screen widgets and Wear OS tiles to check service status and trigger actions without opening the web dashboard.

> **This app is a client, not a standalone product.** It has no functionality on its own — every screen, widget, and tile talks to a running instance of [apollo-server-dashboard](https://github.com/JulioMoralesB/apollo-server-dashboard)'s backend API (`/services`, `/auth/*`, action endpoints). You need a self-hosted apollo-server-dashboard instance reachable from your phone (LAN or via a tunnel) before this app does anything useful.

## Status

Early scaffold. No login, no networking, no widgets or tiles implemented yet — just a project that compiles and installs a placeholder screen on phone and watch.

## Project structure

```
.
├── app/    # Phone app + home-screen widget (Jetpack Compose + Glance)
├── wear/   # Wear OS app + tile (Wear Compose)
└── gradle/libs.versions.toml   # Centralized dependency versions
```

## Requirements

- Android Studio (latest stable)
- JDK 17
- A running apollo-server-dashboard instance to point the app at (see its [self-hosting docs](https://github.com/JulioMoralesB/apollo-server-dashboard#self-hosting))

## Building

Open the project root in Android Studio. On first open, if it reports a missing Gradle wrapper, accept the "Create Gradle Wrapper" prompt — this repo intentionally doesn't commit the wrapper's binary jar. Alternatively, with Gradle installed locally:

```bash
gradle wrapper --gradle-version 8.7
./gradlew build
```

## Auth

Uses the same username/password + JWT access/refresh flow as the web dashboard (see apollo-server-dashboard's `POST /auth/login` and `POST /auth/refresh`). A refresh token is what lets a home-screen widget or Wear tile silently renew its session in the background — neither can show an interactive login prompt when a token expires.
