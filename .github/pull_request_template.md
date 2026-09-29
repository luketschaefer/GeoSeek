## What & why
<!-- One or two sentences. Link the issue / ROADMAP item. -->

## Area
<!-- Which feature package(s)? Tag the owner from CLAUDE.md if you touch someone else's package. -->
- [ ] hunt / quests / detection
- [ ] collector / catalog / ar
- [ ] profile / social / settings / core

## How I tested
<!-- Unit tests added/changed? Tried on a real device (camera/AR changes need one)? -->

## Checklist
- [ ] `./gradlew build` passes locally (lint, detekt, ktlint, unit tests)
- [ ] New logic has unit tests (domain logic lives in `:domain`)
- [ ] No Android imports added to `:domain`
- [ ] DB schema changed? Version bumped, migration + migration test added, `app/schemas/` JSON committed
- [ ] Catalog changed? Labels exist in the ML Kit label map (BundledCatalogTest passes)
- [ ] No API keys or secrets committed
- [ ] Screenshots for UI changes
