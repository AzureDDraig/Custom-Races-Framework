# Adversarial Challenge Report — Milestone 3 (R2 & R3)

**Agent**: Challenger 2 (`teamwork_preview_challenger_m3_fu_2`)  
**Milestone**: Milestone 3 (Requirement R2: Permission Locks & Requirement R3: First-Join Toggle & Config Persistence)  
**Scope**: Network Security Validation (`ModPackets.java`) & GUI State Isolation (`RaceSelectionScreen.java`)  
**Verdict**: **PASS / APPROVED with WARNING** (Risk Level: MEDIUM)

---

## Challenge Summary

**Overall risk assessment**: **MEDIUM**

Server-side network security validation in `ModPackets.java` is robust against client packet forgery. Non-OP clients cannot create, delete, or select permission-locked races via forged C2S network packets. Skill triggers and transformation toggles are properly bounded and rate-limited. GUI selection buttons and tooltips properly respond to permission locks.

However, an empirical state leakage vulnerability was discovered in `RaceSelectionScreen.onClose()` regarding Were-form visual preview state isolation.

---

## Challenges

### [Medium] Challenge 1: Were-Form Preview GUI State Leakage in `onClose()`
- **Assumption challenged**: Toggling Were-form preview mode in `RaceSelectionScreen` is temporary and resets cleanly to the player's true server-side transformation state upon closing the GUI.
- **Attack scenario**: 
  1. A player (who is NOT transformed on the server, `TRANSFORMED_PLAYERS` map = false) opens `RaceSelectionScreen`.
  2. The player clicks the Were-form toggle button (`wereToggleBtn`) to preview their Were-form model.
  3. `ClientWereState.setTransformed(playerUuid, true)` is set for previewing.
  4. The player closes the GUI screen (`onClose()`).
  5. `onClose()` calls `boolean serverState = WereRaceTransformHandler.isTransformed(playerUuid)`.
  6. `WereRaceTransformHandler.isTransformed(uuid)` checks `TRANSFORMED_PLAYERS.getOrDefault(uuid, false)` (false) and falls back to `return ClientWereState.isTransformed(uuid)` (which is `true`!).
  7. `serverState` evaluates to `true`, and `ClientWereState.setTransformed(playerUuid, true)` persists!
- **Blast radius**: The client player remains visually transformed into their Were-form model/texture in the world even though the server state is false.
- **Mitigation**: Update `RaceSelectionScreen.onClose()` or `WereRaceTransformHandler` to check server state without falling back to `ClientWereState`. E.g., add `isServerTransformed(UUID uuid)` returning `TRANSFORMED_PLAYERS.getOrDefault(uuid, false)`.

---

## Stress Test Results

| Scenario | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :--- |
| **Non-OP forged `SAVE_RACE_ID` packet** | Server rejects non-OP save attempt before modifying `loadedRaces` or file | `player.hasPermissions(2)` checked before queue/save; non-OP blocked | **PASS** |
| **Non-OP forged `DELETE_RACE_ID` packet** | Server rejects non-OP delete attempt before modifying `loadedRaces` or file | `player.hasPermissions(2)` checked before queue/delete; non-OP blocked | **PASS** |
| **Non-OP forged `SET_PLAYER_RACE_ID` for VIP race (`permissionLock = "2"`)** | Server checks permission server-side, blocks assignment, and notifies player | `canPlayerSelectRace` returns `false`, sends system warning message, returns without updating map | **PASS** |
| **Forged `TRIGGER_ABILITY_ID` with invalid slots (`-1`, `0`, `6`, `999`)** | Out-of-bounds slot numbers rejected without crash or out-of-bounds exception | `slot < 1 \|\| slot > 5` check returns early cleanly | **PASS** |
| **Non-Were race player forged `TOGGLE_WERE_FORM_ID` packet** | Rejects transformation request with notice | `race == null \|\| !race.enableWereRace` check sends notice and returns | **PASS** |
| **Rapid `TOGGLE_WERE_FORM_ID` packet spam** | Rate-limits rapid consecutive packets | Enforces 1000ms cooldown per player UUID | **PASS** |
| **GUI Confirm Button for locked race** | Confirm button disabled (`active = false`) with permission tooltip | `confirmButton.active = false`, tooltip shows `§cRequires Permission: §e<lock>` | **PASS** |
| **GUI Lock Badge rendering** | Displays `🔒 VIP / LOCKED` badge for locked races | Badge rendered at `topY + 5` in center panel and `§c🔒` prefix in list | **PASS** |
| **GUI Were-form Preview State Isolation** | Closing GUI resets client Were state back to server state | `isTransformed()` circular fallback retains preview state (`true`) upon GUI close | **WARNING / FINDING** |
| **First-Join toggle (`autoOpenSelectionOnJoin`) persistence** | Toggle persists across save/load cycles and controls `openRaceSelection` | Config saved to `config.json` and accurately toggles join prompt | **PASS** |

---

## Unchallenged Areas

- **Full rendering pipeline integration with real GPU shaders**: Tested under Minecraft headless unit execution harness with full class loading and Unsafe instance allocation. Real GPU rendering pipeline was not tested with active hardware displays (out of scope for unit test suite).

---

## Test Execution Summary

- **Gradle Command**: `./gradlew test`
- **Executed Test Suites**: 7 (`runM3Tests`, `runM2Tests`, `runWereTextureEdgeCaseTests`, `runWereTextureAdversarialTests`, `runM3VIPAndConfigTests`, `runM3AdversarialR2R3Tests`, `runM3NetworkAndGUITests`)
- **Total Test Cases Executed**: 42
- **Pass Rate**: 100% (0 Failures)
