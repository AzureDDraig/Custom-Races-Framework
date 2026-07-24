# Handoff Report — Forensic Audit M3 (R2: VIP Permission Locks & R3: Selection GUI Toggle)

## 1. Observation

- **Target Files & Locations**:
  1. `common/src/main/java/ddraig/net/customraces/data/RaceData.java`
     - Line 78: `public String permissionLock = "";`
     - Lines 60–61: `public int particleCount = 5; public int wereParticleCount = 10;`
     - Lines 281–283: `initDefaults()` fallback handling for `permissionLock`, `particleCount`, `wereParticleCount`.
     - Lines 376–377, 415: `toNBT()` serializes `particleCount`, `wereParticleCount`, and `permissionLock`.
     - Lines 435–436, 474: `fromNBT()` deserializes `particleCount`, `wereParticleCount`, and `permissionLock`.
  2. `common/src/main/java/ddraig/net/customraces/data/RaceRegistry.java`
     - Line 25: `public static boolean autoOpenSelectionOnJoin = true;`
     - Lines 64–89: `loadConfig()` and `saveConfig()` reading/writing `config/custom_races/config.json`.
     - Lines 91–105: `canPlayerSelectRace(Player player, RaceData race)` handling null/empty locks, OP level 2 override, and numeric level parsing.
  3. `common/src/main/java/ddraig/net/customraces/network/ModPackets.java`
     - Lines 137–150: Server C2S handler for `SET_PLAYER_RACE_ID` validates `RaceRegistry.canPlayerSelectRace(player, race)` and sends system message on failure.
  4. `common/src/main/java/ddraig/net/customraces/client/gui/RaceSelectionScreen.java`
     - Lines 105–120: `isRaceLocked(RaceData race)` helper logic.
     - Lines 176–181: Renders `"🔒 VIP"` badge for locked races in list.
     - Lines 196–203: Disables `confirmButton.active` when selected race is locked and sets red warning tooltip.
     - Lines 231–235: Renders `"🔒 VIP / LOCKED"` banner on center panel for locked races.
  5. `common/src/main/java/ddraig/net/customraces/event/FirstJoinHandler.java`
     - Lines 25–27: Checks `RaceRegistry.autoOpenSelectionOnJoin` before sending `ModPackets.openRaceSelection(serverPlayer)`.
  6. `common/src/main/java/ddraig/net/customraces/command/CustomRacesCommands.java`
     - Lines 163–172: Admin `/custom_races admin reload` reloads `config.json` via `RaceRegistry.loadConfig()`.

- **Empirical Execution Commands & Output**:
  - Command: `.\gradlew build -x test`
    - Output: `BUILD SUCCESSFUL in 13s` (31 actionable tasks: 20 executed, 11 up-to-date).
  - Command: `.\gradlew :common:runM3VIPAndConfigTests`
    - Output: `RESULTS: 5 Passed, 0 Failed`
  - Command: `.\gradlew :common:runM3AdversarialR2R3Tests`
    - Output: `SUMMARY: 9 PASSED, 0 FAILED`

## 2. Logic Chain

1. **Source Code Integrity**: Line-by-line inspection of `RaceData.java`, `RaceRegistry.java`, `ModPackets.java`, `RaceSelectionScreen.java`, `FirstJoinHandler.java`, and `CustomRacesCommands.java` proves that all target features implement genuine logic rather than hardcoded returns or dummy facades.
2. **Server-Side Security**: In `ModPackets.java`, `canPlayerSelectRace` is invoked on the server context when receiving `SET_PLAYER_RACE_ID`. This prevents client-side packet spoofing or GUI modification from bypassing permission locks.
3. **Configuration Persistence**: `RaceRegistry.loadConfig()` and `saveConfig()` properly utilize Gson serialization for `autoOpenSelectionOnJoin` in `config/custom_races/config.json`, which is checked by `FirstJoinHandler` on player join.
4. **Empirical Verification**: Multi-module Gradle build succeeds across Fabric and Forge target modules (`.\gradlew build -x test`), and both unit and multi-threaded adversarial stress tests pass without errors.

## 3. Caveats

No caveats. All files, methods, network handlers, GUI screens, configuration loaders, and build artifacts were directly verified.

## 4. Conclusion

Verdict: **CLEAN**. Worker M3's implementation of Requirement R2 (VIP Permission Locks) and Requirement R3 (First-Join Selection GUI Toggle) contains zero integrity violations, passes all empirical tests, and builds cleanly.

## 5. Verification Method

To independently verify this audit:
1. Run multi-platform Gradle build:
   ```powershell
   .\gradlew build -x test
   ```
   *Expected result*: `BUILD SUCCESSFUL`.

2. Run VIP lock and config test suite:
   ```powershell
   .\gradlew :common:runM3VIPAndConfigTests
   ```
   *Expected result*: `RESULTS: 5 Passed, 0 Failed`.

3. Run adversarial permission and config stress test suite:
   ```powershell
   .\gradlew :common:runM3AdversarialR2R3Tests
   ```
   *Expected result*: `SUMMARY: 9 PASSED, 0 FAILED`.

4. Inspect files:
   - `common/src/main/java/ddraig/net/customraces/data/RaceData.java`
   - `common/src/main/java/ddraig/net/customraces/data/RaceRegistry.java`
   - `common/src/main/java/ddraig/net/customraces/network/ModPackets.java`
   - `common/src/main/java/ddraig/net/customraces/client/gui/RaceSelectionScreen.java`
   - `common/src/main/java/ddraig/net/customraces/event/FirstJoinHandler.java`
   - `common/src/main/java/ddraig/net/customraces/command/CustomRacesCommands.java`
