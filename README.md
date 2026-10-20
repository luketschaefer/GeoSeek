# GeoSeek - Living Specification
**Team Name:** LSL Industries Inc.

**Team Members:** Luke Erdman, Sean Kaliel, Luke Schaefer

CURRENT STATE OF THE GAME
1. **Real-Time Reactive UI & State Management**: Built with Jetpack Compose featuring a custom pixel-art design system (`GeoTheme`, `GeoColors`, `GeoType`, pixel panels, and logo typography). Screen state updates reactively on countdown ticks, real-time object detections, score updates, quest claims, and level-up events.
2. **Asynchronous Real-Time Vision Pipeline**: Integrates CameraX (`LifecycleCameraController`, `ImageAnalysis`) with Google ML Kit Image Labeling (`MlKitAnalyzer`). Uses `ObjectDetector`, `ObjectLabelMatcher`, `LabelSanitizer`, and `TemporalFrameFilter` with expanded word-aware alias dictionaries (e.g. matching `flowerpot` and `houseplant` to `Plant`) and 2-frame temporal streak validation for smooth recognition.
3. **Engine Architecture & Game Modes**: Implements the Strategy Design Pattern (`GameMode` interface, `GameSessionState`, `GameRuleOutcome`, `GameScoreResult`). Features **Timed Hunt Mode** (racing the clock across Indoors, Park, and Streets environments with remaining time score bonuses) and **Free Look Mode** (`CollectionMode` for untimed card discovery and rarity XP).
4. **Explorer Accounts, Progression Persistence & Leveling**: On-device explorer profile system backed by `AuthStore`, `ProgressStore`, and local `SharedPreferences`. Features PBKDF2 password hashing, persistent level progress (`XPManager`), trading card binder, best scores, daily quest tracking, and animated `LevelUpOverlay` celebrations.
5. **Automated Testing Suite**: Tested with JUnit unit tests covering vision label matching, alias resolution, temporal frame streak filtering, `TimedHuntMode` scoring/timer, `CollectionMode` trading cards, and `XPManager` level progression.

-----------------------------------------------------------------------------------------------------------------------------------------------

## 1. Target Project & Scope
### Project Summary:
GeoSeek is a scavenger hunt game that utilizes the camera and AR to challenge the player to explore the world around them. It will have at least two modes, Hunt mode and Collector mode. Hunt mode is a round based gamemode in which you can choose an environment for the hunt and it generates items to look for in a time limit. Different items are worth different amounts of points. There is also a daily quest for more XP. Collector mode adds to your profile where you can find objects and keep them as trading cards which you could potentially swap with other people. Cards are also worth XP which goes into your profile the first time you acquire them. Having a social network would be very valuable to this game so that players can trade and compare profiles. 

### Problem Statement & Objectives: 
Exploration of common places is often lacklustre and boring without much incentive for people to pay attention to their surroundings. Traditional games and scavenger hunts can encourage exploration, but are often repetitive, manually organized, or limited to specific locations. This application aims to make exploring real world environments more engaging by making the user’s surroundings into an interactive game through the employment of modern technology such as computer vision and augmented reality.
This application will task the user, based on the mode selected, to complete tasks such as fulfilling quests for XP, or collecting objects like trading cards. Objects will have different rarity levels and point values, encouraging players to search more carefully and take on more difficult challenges. Combined with augmented reality to make objects come to life once they are found, this application will make exploration of modern spaces more engaging for any user type.

### Key Features & Functionality:
**Must** <ol><li>Detect objects successfully using the camera</li><li>Include multiple objects to look for (at least two)</li><li>Working timer</li></ol>

**Should** <ol><li>Include different gamemodes</li><li>Have a pool of at least 20 different objects to find</li><li>Implement a trading card system</li></ol>

**Could** <ol><li>Implement battles between trading cards</li><li>Enforce anti-cheating capabilities (detect images originating from sources such as screens)</li><li>Add multiplayer capabilities to the game</li><li>Implement advanced image recognition to allow finding rare versions of the same object possible (ex. Supercar vs a minivan)</li><li>Based on objects you have already found, give you suggestions of objects similar to those that you may be likely to find based on your previous experiences.</li><li>iOS support</li></ol>

**Won't** <ol><li>Live AI generated quests</li><li>Global card trading</li></ol>

### Tooling Strategy: 
We are using Claude Pro, Gemini Pro, and Codex Pro coding agents to assist development throughout the course of our project. We may also employ various testing tools, such as JUnit or Stryker Mutator, and XML formatters/verifiers to aid development. Hopefully using this we can achieve as many of our should’s and could’s as possible.

## 2. Targeted Platform & Initial Programming Language / Libraries use
**Target Platform:** 
We are developing GeoSeek on Android Studio, and deploy it as an independent mobile app which can be played on any Android device. Development in phase two could include iOS support. 

**Programming Language(s):** 
The app, which is being developed through Android Studio, employs Kotlin and XML.

**Frameworks & Libraries:**
CameraX
ARCore SDK + Sceneview 
Google ML Kit (Object Detection) 
Google Cloud Vision API

**Testing Framework:**
JUnit

## 3. Project Roadmap & Timeline.
### Phase 1 (Proposal to Oct 25): 
**September 25, 2026**
Familiarize ourselves with image processing tools

**October 12, 2026**
Implement working image detection with small object catalogue

**October 20, 2026**
Have working game loop (minimum 1 game mode)

**October 23, 2026**
Increase object catalogue size (minimum 10)

**October 25, 2026**
Present prototype


### Phase 2 (Post-Midterm to Term End): 
**November 9, 2026**
Implement second game mode, including trading card system

**November 16, 2026**
Finish object catalogue (minimum 20 objects, aim for much larger)

**November 20, 2026**
Polish added features, complete UI and graphical elements

**November 28, 2026**
Finish comprehensive testing

**November 30, 2026**
Finish application for presentation



### Team Roles & Initial Task Allocation:
We will all likely collaborate on all aspects of the project, but these are the areas that we have chosen to each lead development on:

**Sean** - Computer vision / AR 

**Luke Erdman** - Game / Backend logic

**Luke Schaefer** - UI / Application user experience


## 4. Project Layout
Single-module Android app (Kotlin + Jetpack Compose). Build with `./gradlew assembleDebug`.

```
app/src/main/java/com/example/geoseek/
├── MainActivity.kt               App entry point setting up GeoSeekRoot UI
├── data/                         On-device persistence (SharedPreferences)
│   ├── AuthStore.kt              Accounts, PBKDF2 password hashing, and the remembered session
│   ├── Player.kt                 Player profile model and level info
│   ├── PlayerPrefs.kt            Shared preference keys and player loading
│   └── ProgressStore.kt          XP, cards, hunt stats, and daily quest progress
├── detection/                    Real-time computer vision pipeline
│   ├── LabelSanitizer.kt         Filters non-object ML Kit structural tags
│   ├── ObjectDetector.kt         Orchestrates CameraX frame analysis & ML Kit labeler
│   ├── ObjectLabelMatcher.kt     Maps vision labels to targets using alias dictionaries
│   └── TemporalFrameFilter.kt    Enforces multi-frame streak rules for smooth detection
├── engine/                       Domain game engine & rules strategies
│   ├── GameMode.kt               Strategy interface, session states, and outcome results
│   ├── DailyQuests.kt            Daily quest rotation and goal types
│   ├── HuntRound.kt              Active hunt session orchestrator & countdown timer
│   ├── HuntTargets.kt            Picks hunt targets for an environment's habitat
│   └── modes/
│       ├── CollectionMode.kt     Untimed free-look discovery & card collecting strategy
│       └── TimedHuntMode.kt      Timed hunt strategy with speed bonus scoring
├── managers/
│   └── XPManager.kt              Player experience points & level curve calculations
├── models/                       Domain data models
│   ├── Card.kt                   Collectible trading card model
│   ├── GameObject.kt             Findable real-world target model, Rarity & Habitat enums
│   └── ObjectList.kt             Global catalog of 20+ findable targets
└── screens/                      Jetpack Compose UI & navigation
    ├── AuthScreen.kt             Title screen with Sign In & Sign Up forms
    ├── CameraScan.kt             CameraX preview view, scan line, & reticle overlays
    ├── CollectionScreen.kt       Card binder view, rarity filters, & card inspector
    ├── Components.kt             Pixel art design system components & LevelUpOverlay
    ├── ExploreScreen.kt          Free Look screen powered by CollectionMode
    ├── GeoIcons.kt               Custom pixel-art icon and object sprite renderers
    ├── GeoSeekRoot.kt            App shell, stage state, and navigation routing
    ├── GeoTheme.kt               Pixel art colors, typography, & scene themes
    ├── HomeScreen.kt             Main menu, player badge, and daily quest panel
    ├── HuntScreen.kt             Timed hunt setup, active HUD, and results breakdown
    ├── ProfileScreen.kt          Explorer stats, level progress, and account settings
    └── WorldBackground.kt        Animated parallax biome background
```
