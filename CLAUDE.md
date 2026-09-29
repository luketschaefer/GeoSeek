# GeoSeek — working guide

Android scavenger-hunt game: the camera recognizes real-world objects (ML Kit Image Labeling),
players hunt timed targets per environment and collect trading cards. This file is the source of
truth for how code is organized. Read it before changing anything; update it when conventions change.

## Checks (run before every push)

```bash
./gradlew build                    # assemble + Android Lint + unit tests + spotlessCheck (ktlint) + detekt
./gradlew spotlessApply            # auto-format; run this first when spotlessCheck fails
./gradlew :domain:test             # fast: pure-JVM domain tests only
./gradlew :app:testDebugUnitTest   # app JVM tests
./gradlew :app:connectedDebugAndroidTest   # instrumented (Room) tests; needs a device/emulator
```

CI (`.github/workflows/ci.yml`) runs `./gradlew build :app:assembleDebugAndroidTest` on every push/PR.
Kotlin warnings and Lint warnings are **errors**. Don't suppress; fix. If a suppression is unavoidable,
add `@Suppress("Rule")` on the smallest scope with a comment saying why.

## Architecture

Two Gradle modules:

| Module | What | Rule |
|---|---|---|
| `:domain` | Pure Kotlin/JVM: models, engines, repository **interfaces** | No Android, no AndroidX. The compiler enforces it (no Android SDK on the classpath). |
| `:app` | Everything Android, package-by-feature | Implements domain interfaces; UI; DI |

MVVM with unidirectional data flow: `Screen` (stateful, gets ViewModel) → `Content` (stateless,
takes `UiState` + callbacks, has `@Preview`) ← ViewModel exposes one `StateFlow<XxxUiState>`.

```
domain/src/main/kotlin/com/geoseek/domain/
  catalog/    CatalogObject, Rarity, Environment, Catalog, CatalogParser, CatalogRepository
  hunt/       Round, RoundConfig, RoundStatus, RoundEvent, RoundReducer, RoundGenerator, RoundTimer, Scoring
  detection/  Detection, TargetMatcher
  collector/  Card, CollectionRepository, AcquireCardUseCase
  profile/    Profile, LevelCurve, ProfileRepository
  quests/     DailyQuest, DailyQuestSelector
  history/    RoundRecord, RoundHistoryRepository
  social/     UserId, TradeOffer, TradeRepository, RemoteProfile, ProfileRemoteRepository
  util/       SeededRandom (SplitMix64)

app/src/main/java/com/geoseek/
  core/       di/ (Clock, dispatchers, app scope), designsystem/ (theme, RarityColors, GeoCard, RarityBadge),
              ui/ (GeoScaffold, LoadingView, MessageView), navigation/ (GeoSeekNavHost, HomeRoute),
              database/ (GeoSeekDatabase, DatabaseModule), permissions/ (camera permission state)
  home/       Home screen
  hunt/       picker/, HuntScreen (camera + debug overlay), results/, data/ (round history Room), navigation/
  detection/  ObjectDetector, MlKitLabelDetector, FakeDetector, CloudVisionDetector (stub), camera/DetectionCamera
  ar/         ArRevealScreen, ArAvailabilityChecker, 2D fallback, navigation/
  catalog/    AssetCatalogRepository (assets/catalog.json), CatalogModule
  collector/  Collection + detail/ screens, data/ (cards Room), navigation/
  profile/    Profile screen, data/ (profile Room), navigation/
  quests/     TodaysQuestProvider
  social/     Trading screen (stub), data/ (in-memory repos), navigation/
  settings/   Settings screen (stub)
```

## Package ownership

Each feature package has one owner who reviews every PR touching it. Fill in names:

| Owner | Packages |
|---|---|
| A: _name_ | `hunt/`, `quests/`, `detection/`; domain `hunt/`, `detection/`, `quests/`, `history/` |
| B: _name_ | `collector/`, `catalog/`, `ar/`; domain `collector/`, `catalog/`; `assets/catalog.json` |
| C: _name_ | `profile/`, `social/`, `settings/`, `core/designsystem/`, `core/ui/`; domain `profile/`, `social/` |

**Shared files** (merge-conflict hotspots; keep edits to one line and tell the team):
`core/navigation/GeoSeekNavHost.kt` (one `xxxGraph()` call per feature), `core/database/GeoSeekDatabase.kt`
(entities/DAOs list), `res/values/strings.xml` (keep strings grouped under your feature's comment),
`gradle/libs.versions.toml`.

## Conventions

**Where new code goes**
- Game rules, calculations, state machines → `:domain`, with unit tests. If you can write it without
  Android, it belongs there.
- A screen → its feature package (see "Adding a screen").
- Persistence → `feature/data/` (entity, DAO, `RoomXxxRepository`) implementing a domain interface.
- Cross-feature code → `core/`. A feature **never imports another feature's screens, ViewModels,
  routes or data classes**; wire cross-feature navigation via callbacks in `GeoSeekNavHost`.
  The service packages `detection/` (`ObjectDetector`, `DetectionCamera`), `quests/`
  (`TodaysQuestProvider`) and `catalog/` may be injected anywhere; keep their public API small.

**Naming**
- `XxxScreen` (stateful, `hiltViewModel()`), `XxxContent` (stateless + `@Preview`), `XxxViewModel`, `XxxUiState`.
- Routes: `@Serializable data object/class XxxRoute` in `feature/navigation/XxxNavigation.kt`, next to
  `fun NavGraphBuilder.xxxGraph(...)`. Route args are primitives/Strings (enum args need `@Keep`, which
  `:domain` can't use; see `HuntRoute.environmentName`).
- Repositories: interface `XxxRepository` in domain; implementations `RoomXxxRepository`, `InMemoryXxxRepository`.

**State**
- One immutable `data class XxxUiState` (or `sealed interface` for loading/empty/loaded) per screen,
  exposed as `StateFlow`. Update with `_uiState.update { it.copy(...) }`. No mutable collections in state.
- Streams from repositories: `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.
- One-shot navigation after async work: put a nullable field in state (e.g. `pendingRevealObjectId`),
  the screen handles it in `LaunchedEffect` and calls `onXxxHandled()`.
- Time: inject `java.time.Clock`; never call `Instant.now()` / `LocalDate.now()` without it.
- Dispatchers: inject `@IoDispatcher` / `@DefaultDispatcher`; never hard-code `Dispatchers.IO` in classes.

**Errors**
- Programmer errors / impossible states: `require` / `check` / `error` (crash in dev).
- Expected failures in domain APIs: sealed result types (see `TradeResult`), not exceptions.
- Detector backends wrap failures in `DetectionException`; the ViewModel shows them, never crashes.
- Bad bundled data (catalog) fails at app startup on purpose (`GeoSeekApplication`).

**Camera ownership**: ARCore and CameraX must never hold the camera at the same time. Before navigating
to any AR screen, `DetectionCamera.release()` (unbinds and waits for `CameraState.CLOSED`), then navigate.
See `HuntViewModel.onRevealClicked`.

**Secrets**: never put API keys in the app (BuildConfig, resources, `local.properties`). Anything that
needs a Google Cloud key goes through our backend. See `CloudVisionDetector` for the reasoning.

**Tests**: JUnit4 + Truth + Turbine + kotlinx-coroutines-test. Prefer hand-written fakes over mocks
(`app/src/test/.../testing/Fakes.kt`). Time-based code uses `delay` so `runTest` virtual time works
(see `RoundTimerTest`). Seeded algorithms have a "golden" test; changing its expected value is a
player-visible change (shared seeds, daily quests), so do it deliberately.

## Adding a catalog object

1. Pick labels from the ML Kit base model: `app/src/test/resources/mlkit-base-labels-17.0.9.txt`
   (447 labels, exact case). If nothing fits, **don't invent a label**. Leave the object out, or note it in
   ROADMAP ("custom model").
2. Add an entry to `app/src/main/assets/catalog.json`:
   ```json
   { "id": "kettle", "name": "Kettle", "rarity": "UNCOMMON", "points": 25, "xp": 15,
     "environments": ["KITCHEN"], "labels": ["Cookware and bakeware"], "minConfidence": 0.7 }
   ```
   Rules (enforced by `CatalogParser`): id `[a-z][a-z0-9_]*` and unique; points/xp > 0;
   minConfidence in (0, 1]; no unknown keys; each environment keeps ≥ 5 objects. By convention
   points/xp follow rarity: Common 10/5, Uncommon 25/15, Rare 50/40, Epic 100/100, Legendary 250/300
   (`BundledCatalogTest` checks points never decrease with rarity).
3. `./gradlew :app:testDebugUnitTest --tests '*BundledCatalogTest*'`.
4. Test on a device with the Hunt debug overlay: it shows raw labels and confidences, so you can tune
   `minConfidence`.

Adding an environment: add it to `domain/catalog/Environment.kt`, give it ≥ 5 catalog objects, add
`env_xxx` string + mapping in `core/designsystem/EnvironmentLabels.kt`.

Bumping ML Kit: `scripts/extract-mlkit-labels.py <version> > app/src/test/resources/mlkit-base-labels-<version>.txt`,
update the resource name in `BundledCatalogTest`, fix any catalog labels that disappeared.

## Adding a screen

1. `feature/XxxViewModel.kt`: `@HiltViewModel`, `XxxUiState`, `StateFlow`. Nav args via
   `savedStateHandle.toRoute<XxxRoute>()`.
2. `feature/XxxScreen.kt`: `XxxScreen(onBack, ..., viewModel = hiltViewModel())` → `XxxContent(state, ...)`
   wrapped in `GeoScaffold`, plus a `@Preview` of `XxxContent` inside `GeoSeekTheme`.
3. `feature/navigation/XxxNavigation.kt`: `@Serializable` route + `composable<XxxRoute> { ... }` inside the
   feature's `xxxGraph()`. New feature? Add one `xxxGraph(...)` line to `GeoSeekNavHost`.
4. Strings in `res/values/strings.xml` under your feature's comment block; use plurals for counts.
5. Unit-test any logic in the ViewModel with fakes (`MainDispatcherRule`), or better, move it to `:domain`.

## Database changes

`GeoSeekDatabase` has `exportSchema = true`; schemas live in `app/schemas/` and **must be committed**.
To change the schema: bump `version`, build (generates `N.json`), add `MIGRATION_(N-1)_N` to
`DatabaseModule.MIGRATIONS` (or an `@AutoMigration`), add a test in `GeoSeekDatabaseMigrationTest`, run
`connectedDebugAndroidTest`. Never use `fallbackToDestructiveMigration`: it would wipe player collections.
Enums are stored by name, so renaming an enum constant needs a migration.

## Key facts & gotchas

- ML Kit Image Labeling classifies the **whole frame** (447 labels); it doesn't locate objects. `TargetMatcher`
  requires K = 3 consecutive frames ≥ `minConfidence`. Do not use ML Kit Object Detection's built-in
  classifier for targets (only 5 coarse categories).
- Daily quest = `DailyQuestSelector.questFor(LocalDate.now(clock))`: device-local date, deterministic per
  catalog version (SplitMix64, not `kotlin.random.Random`, whose sequence isn't guaranteed across versions).
- The Hunt screen is currently a **wiring proof**: hard-coded target `cup`, no timer/scoring/saving.
  The engines exist in `:domain` (`RoundGenerator`, `RoundReducer`, `RoundTimer`, `Scoring`); wiring them is
  the next Must item.
- Debug builds show "View in AR" on every card so the reveal can be tested on emulators.
- Emulator camera: the virtual scene works for checking the pipeline; ARCore is unavailable, so you'll get the 2D reveal.
- detekt is `2.0.0-alpha.6` (the only line built for Kotlin 2.4 / AGP 9). Upgrade to 2.0.0 final when released.
- `compileSdk`/`targetSdk` 37 is required by current AndroidX (`core-ktx` 1.19).
