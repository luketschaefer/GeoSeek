# GeoSeek

GeoSeek is a scavenger hunt game that utilizes the camera and AR to challenge the player to explore the world around them. It will have at least two modes, Hunt mode and Collector mode. Hunt mode is a round based gamemode in which you can choose an environment for the hunt and it generates items to look for in a time limit. Different items are worth different amounts of points. There is also a daily quest for more XP. Collector mode adds to your profile where you can find objects and keep them as trading cards which you could potentially swap with other people. Cards are also worth XP which goes into your profile the first time you acquire them. Having a social network would be very valuable to this game so that players can trade and compare profiles.

**Status:** skeleton. Every screen exists and is reachable; the Hunt screen is a wiring proof (camera →
ML Kit → target matcher → debug overlay). See [ROADMAP.md](ROADMAP.md) and [CLAUDE.md](CLAUDE.md).

## Requirements

- **Android Studio 2026.1.4** or newer. AGP 9.4 needs a recent Studio; if Studio reports the AGP
  version as unsupported, update Studio rather than downgrading AGP.
- **JDK 17+** for Gradle. Studio's bundled JBR works; CI uses Temurin 21.
- Android SDK Platform **37** (Studio/Gradle installs it on first sync if licenses are accepted).
- A physical Android device with Android 8.0+ (API 26) for real detection. AR additionally needs an
  [ARCore-supported device](https://developers.google.com/ar/devices); others get a 2D reveal.

## Setup

```bash
git clone <repo-url> && cd GeoSeek
./gradlew build          # first run downloads Gradle 9.8 and dependencies
```

Or open the folder in Android Studio and let it sync. No API keys or `google-services.json` are needed.

## Run on a device

1. On the phone: Settings → About phone → tap *Build number* 7× → Developer options → enable **USB debugging**.
2. Connect via USB (or Wi-Fi pairing in Studio) and accept the debugging prompt.
3. In Studio, pick the device and press **Run**, or run `./gradlew installDebug`.
4. Start a hunt → pick an environment → allow camera. Point the camera at a **cup**: the overlay shows
   live labels, the streak goes 1/3 → 3/3 and switches to **MATCHED**; **Reveal** opens the AR (or 2D) reveal.

The emulator works too (its virtual camera scene is enough to see labels flow), but ARCore isn't available there.

## Checks

```bash
./gradlew build                            # everything CI runs: lint, ktlint, detekt, unit tests
./gradlew spotlessApply                    # auto-format
./gradlew :app:connectedDebugAndroidTest   # instrumented Room tests (device/emulator)
```

## Version compatibility

| Tool | Version |
|---|---|
| Gradle | 9.8.0 |
| Android Gradle Plugin | 9.4.1 (built-in Kotlin) |
| Kotlin / Compose compiler plugin | 2.4.20 |
| KSP | 2.3.12 |
| Hilt | 2.60.1 |
| Compose BOM | 2026.09.00 |
| CameraX | 1.6.2 |
| ML Kit Image Labeling (bundled) | 17.0.9 |
| ARCore / SceneView | 1.56.0 / 4.48.0 |
| Room | 2.8.5 |
| detekt | 2.0.0-alpha.6 (only line supporting Kotlin 2.4) |
| ktlint (via Spotless 8.10.3) | 1.8.0 + Compose rules 0.6.7 |

All versions live in `gradle/libs.versions.toml`. Bump the Kotlin/KSP/AGP/Hilt group together and run `./gradlew build`.
