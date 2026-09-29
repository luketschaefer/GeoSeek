# GeoSeek roadmap (MoSCoW)

Each item names where it will be implemented. ✅ = done in the skeleton, 🟡 = interface/engine exists, needs wiring.

## Must (v1: playable Hunt + Collector, offline)

| Item | Where | Status |
|---|---|---|
| Live camera + on-device detection | `detection/camera/DetectionCamera`, `detection/MlKitLabelDetector` | ✅ |
| Debounced target matching | `domain/detection/TargetMatcher` | ✅ |
| Bundled, validated catalog | `assets/catalog.json`, `domain/catalog/CatalogParser`, `catalog/AssetCatalogRepository` | ✅ |
| Camera permission flow (rationale, permanently denied) | `core/permissions/CameraPermission.kt` | ✅ |
| Environment picker | `hunt/picker/` | ✅ |
| Timed hunt round: generate targets, countdown, find, win/time-up/abandon | `domain/hunt/RoundGenerator`, `RoundReducer`, `RoundTimer`; wire in `hunt/HuntViewModel` (replace debug target) | 🟡 |
| Scoring + results screen with real data | `domain/hunt/Scoring`, `hunt/results/`, save via `RoundHistoryRepository.record` | 🟡 |
| Found object → card in collection, first-find XP | `domain/collector/AcquireCardUseCase`, `collector/data/RoomCollectionRepository` | 🟡 (call from hunt on `newlyConfirmed`) |
| XP / level display | `domain/profile/LevelCurve`, `home/`, `profile/` | ✅ |
| Daily quest (same for everyone per date) + bonus XP on completion | `domain/quests/DailyQuestSelector`, `quests/TodaysQuestProvider`; completion tracking needs a `quest_completions` table | 🟡 |
| Collection grid + card detail | `collector/` | ✅ |
| Reveal after a find: AR on supported devices, 2D fallback | `ar/ArRevealScreen`, `ar/ArAvailabilityChecker` | 🟡 (AR scene shows planes; place a 3D card model) |
| Local persistence with migrations | `core/database/`, `app/schemas/` | ✅ |

## Should (v1.x)

| Item | Where |
|---|---|
| Accounts (Firebase Auth) | New `FirebaseProfileRemoteRepository` implementing `domain/social/ProfileRemoteRepository`; bind in `social/SocialModule` |
| Public profiles + friends | `ProfileRemoteRepository.observeProfile/observeFriends`, Firestore `profiles/{uid}` |
| Trading between friends | `domain/social/TradeRepository` → `FirestoreTradeRepository`; acceptance as a server-side transaction (Cloud Function) that moves cards; UI in `social/` |
| Cloud sync/backup of collection | New `SyncRepository` in domain; Room stays source of truth offline |
| Real 3D card model in AR reveal | `ar/ArRevealScreen` (SceneView `ModelNode`, glTF in assets) |
| Sound + haptics on find | `core/` feedback helper; toggles in `settings/` |
| Anti-cheat v1: reject frames that look like a screen | Heuristic on ML Kit labels `Screenshot`, `Web page`, `Television`, `Computer`, `Mobile phone` in a new `domain/detection/ScreenGuard`, applied before `TargetMatcher` |
| Localization (DE/EN) | `res/values-de/strings.xml` |

## Could (later)

| Item | Where |
|---|---|
| Server-side detection / bigger model | `detection/CloudVisionDetector` → calls **our** backend (never an embedded Cloud key) |
| Rare variants (supercar vs minivan) | Needs a custom TFLite model (ML Kit custom labeler) behind `ObjectDetector`; base model only knows `Car`/`Van`/`Vehicle` |
| Objects the base model can't label (bottle, book, tree, traffic light, laptop, backpack, …) | Same custom model; add catalog entries only once labels exist |
| Card battles | New `battle/` feature + `domain/battle/` engine |
| Personalized suggestions | `domain/quests/` suggestion engine from `RoundHistoryRepository` + collection gaps |
| Anti-cheat v2 (screen/moiré detection model, server attestation) | `domain/detection/ScreenGuard` + Play Integrity on the backend |
| Seasonal/limited catalog objects | Remote catalog download validated by the same `CatalogParser` |
| Server-driven daily quests (identical across app versions) | Backend + `quests/`; today quests depend on the bundled catalog version |

## Won't (this project)

- iOS app. The domain layer is pure Kotlin, so Kotlin Multiplatform stays possible later.
- Embedding Google Cloud API keys in the app.
- Location tracking / maps (GeoSeek recognizes objects, not GPS places).
- Real-money purchases or card marketplace.
