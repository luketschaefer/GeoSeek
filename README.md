# GeoSeek - Living Specification
**Team Name:** LSL Industries Inc.

**Team Members:** Luke Erdman, Sean Kaliel, Luke Schaefer

CURRENT STATE OF THE GAME
1. The screens won't update when game state changes. HuntRound and XPManager use plain vars, which Compose doesn't watch. When the timer ticks or the score changes, the UI won't redraw. The standard fix is a ViewModel per screen that holds state in mutableStateOf or a StateFlow. It adds one small dependency (lifecycle-viewmodel-compose) and no DI framework. The timer should also run in the ViewModel as a coroutine, which keeps HuntRound as plain logic you can unit-test with JUnit (your README mentions JUnit).

3. ObjectDetector.detect() has the wrong shape. It currently returns a result right away, but ML Kit labels images asynchronously and camera frames arrive continuously through CameraX. It should become a CameraX ImageAnalysis.Analyzer that reports matches through a callback, e.g. onObjectFound: (GameObject) -> Unit. Settle this early, since whoever leads the computer vision work will build on that signature.

3. Progress isn't saved. Nothing is persisted yet, so XP and cards will reset every time the app restarts. Once XP and cards exist, SharedPreferences or DataStore will be enough, and you won't need a database.                                                                                                                                     
  6. Navigation will outgrow the enum. The enum + when works for four tabs, but the system back button currently exits the app. Hunt will also turn into a flow (pick an environment → camera → results). If that becomes awkward, switching to navigation-compose is a contained change inside MainActivity.

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
We plan on using the Claude Pro coding agent to assist development throughout the course of our project. We may also employ various testing tools, such as JUnit or Stryker Mutator, and XML formatters/verifiers to aid development. Hopefully using this we can achieve as many of our should’s and could’s as possible.

## 2. Targeted Platform & Initial Programming Language / Libraries use
**Target Platform:** 
We plan to develop GeoSeek on Android Studio, and deploy it as an independent mobile app which can be played on any Android device. Development in phase two could include iOS support. 

**Programming Language(s):** 
The app, which will be developed through Android Studio, will employ two of the primary supported languages present on the software, Java and XML.

**Frameworks & Libraries:**
CameraX
ARCore SDK + Sceneview 
Google ML Kit (Object Detection) 
Google Cloud Vision API

**Testing Framework**
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
├── MainActivity.kt        App entry point and simple screen switching
├── screens/               One Compose screen per file: Home, Hunt, Collection, Profile
├── engine/HuntRound.kt    A single hunt round: timer, targets, score
├── managers/XPManager.kt  Player XP, level, and daily quest bonus
├── models/                Plain data: GameObject + Rarity, Card, ObjectList (example objects)
└── detection/             ObjectDetector (ML Kit Image Labeling goes here)
```

CameraX, ML Kit, and ARCore/SceneView are not added yet; see the TODO in `app/build.gradle.kts`.
