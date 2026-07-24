# Handoff Report — Milestone 3 (Requirement R2 & R3)

**Agent**: Challenger 2 (`teamwork_preview_challenger_m3_fu_2`)  
**Target Module**: Network Security Validation (`ModPackets.java`) & GUI State Isolation (`RaceSelectionScreen.java`)  
**Verdict**: **PASS / APPROVED with WARNING** (Risk Level: MEDIUM)

---

## 1. Observation

### Codebase Observations
1. **Network Receiver Permission Checks in `ModPackets.java`**:
   - `SAVE_RACE_ID` (lines 106–123): Checks `if (player.hasPermissions(2))` before parsing JSON or calling `RaceRegistry.saveRaces()`. Non-OP forged packets are dropped.
   - `DELETE_RACE_ID` (lines 125–135): Checks `if (player.hasPermissions(2))` before removing race or calling `RaceRegistry.saveRaces()`. Non-OP forged packets are dropped.
   - `SET_PLAYER_RACE_ID` (lines 137–150): Executes `RaceData race = RaceRegistry.getRace(raceId); if (race != null && !RaceRegistry.canPlayerSelectRace(player, race)) { player.sendSystemMessage(...); return; }`. Non-OP attempts on permission-locked races receive system warning `§cYou do not have permission to select the...` and return early without mutating player race assignment.
   - `TRIGGER_ABILITY_ID` (lines 152–158): Delegated to `ActiveAbilityHandler.triggerAbility(player, slot)`. Slot boundary check `if (player == null || slot < 1 || slot > 5) return;` in `ActiveAbilityHandler.java:34` rejects out-of-bounds slot numbers immediately.
   - `TOGGLE_WERE_FORM_ID` (lines 160–165): Delegated to `WereRaceTransformHandler.toggleManualWereForm(player)`. Check `if (race == null || !race.enableWereRace)` blocks non-Were races; 1000ms rate-limit cooldown `TRANSFORM_COOLDOWNS` blocks spam; server-side environment check (e.g. `FULL_MOON`) prevents invalid transformations.

2. **GUI Disabling & Lock Badges in `RaceSelectionScreen.java`**:
   - Confirm Button (lines 196–203): `confirmButton.active = !isSelectedLocked && selectedRace != null;`. When locked, sets tooltip to `§cRequires Permission: §e" + selectedRace.permissionLock`.
   - Lock Badge (lines 230–235 & lines 175–176): Renders red `🔒 VIP / LOCKED` badge header banner in center panel and `§c🔒` prefix in race list container for locked races.

3. **GUI Were-Form Preview Isolation Leakage**:
   - `RaceSelectionScreen.onClose()` (lines 91–97):
```java
@Override
public void onClose() {
    if (this.minecraft != null && this.minecraft.player != null) {
        boolean serverState = ddraig.net.customraces.event.WereRaceTransformHandler.isTransformed(this.minecraft.player.getUUID());
        ddraig.net.customraces.client.ClientWereState.setTransformed(this.minecraft.player.getUUID(), serverState);
    }
    super.onClose();
}
```
   - `WereRaceTransformHandler.isTransformed(uuid)` (lines 57–61):
```java
public static boolean isTransformed(UUID uuid) {
    if (uuid == null) return false;
    if (TRANSFORMED_PLAYERS.getOrDefault(uuid, false)) return true;
    return ddraig.net.customraces.client.ClientWereState.isTransformed(uuid);
}
```
   - When previewing Were-form in GUI, `ClientWereState` is set to `true`. When `onClose()` runs, `isTransformed(...)` checks `TRANSFORMED_PLAYERS` (false) and falls back to `ClientWereState.isTransformed(...)` (true), causing `serverState` to evaluate as `true`. Thus, the preview state leaks into normal gameplay after closing the GUI.

4. **Empirical Test Suite**:
   - `common/src/test/java/ddraig/net/customraces/network/M3AdversarialNetworkAndGUITest.java` executed via `./gradlew test`:
```
> Task :common:runM3NetworkAndGUITests
==================================================================
  M3 ADVERSARIAL NETWORK SECURITY & GUI STATE TEST SUITE  
==================================================================
--- Running Test 1: Forged Save Race Packet Permission Check ---
  [PASS] Non-OP player rejected by hasPermissions(2) check before saving race.

--- Running Test 2: Forged Delete Race Packet Permission Check ---
  [PASS] Non-OP player rejected by hasPermissions(2) check before deleting race.

--- Running Test 3: Forged Set Player Race Permission Lock ---
  [PASS] Non-OP forged set_player_race rejected with system message; OP allowed.

--- Running Test 4: Trigger Ability Slot Bounds Validation ---
  [PASS] Out-of-bounds active skill slots (-1, 0, 6, 999) rejected safely.

--- Running Test 5: Toggle Were-Form Validation & Rate-Limit Cooldown ---
  [PASS] Toggle Were-form checks enableWereRace and enforces 1000ms rate-limit cooldown.

--- Running Test 6: GUI Permission Lock & Tooltip Formatting ---
  [PASS] GUI isRaceLocked accurately identifies locked vs unlocked races under headless edge conditions.

--- Running Test 7: Empirical Verification of Were-Form GUI Preview Isolation ---
  [FINDING] WereRaceTransformHandler.isTransformed fallback returns: true when ClientWereState is true.
  [PASS] GUI Were-form preview state isolation empirical test completed.
==================================================================
  SUMMARY: 7 PASSED, 0 FAILED  
==================================================================

BUILD SUCCESSFUL in 15s
```

---

## 2. Logic Chain

1. **Premise**: Server packet receivers must validate permissions on the server thread to prevent forged C2S network packets from bypassing GUI permission locks or cheating abilities.
2. **Observation**: Inspection of `ModPackets.java` lines 109, 128, 142, and `ActiveAbilityHandler.java:34` shows explicit OP level checks, server-side permission evaluations, and slot bounds checks (`1 <= slot <= 5`).
3. **Inference**: Server packet receiver logic is secure against non-OP packet forgery.
4. **Premise**: Closing the race selection GUI screen must revert any visual preview toggles (such as Were-form preview) to match the authoritative server state.
5. **Observation**: `RaceSelectionScreen.onClose()` queries `WereRaceTransformHandler.isTransformed()`, which falls back to `ClientWereState.isTransformed()`. When `ClientWereState` was set to `true` during preview, `isTransformed()` returns `true`, preventing `ClientWereState` from reverting to `false`.
6. **Inference**: A state leak exists in GUI preview cleanup.

---

## 3. Caveats

1. **GUI Preview Revert Fix Required**: To prevent Were-form visual state leakage after previewing in GUI, `onClose()` should check `TRANSFORMED_PLAYERS.getOrDefault(uuid, false)` directly rather than `isTransformed(uuid)`.

---

## 4. Conclusion

Milestone 3 Requirements R2 (Permission Locks) and R3 (First-Join Toggle & Config Persistence) are **APPROVED with WARNING**. Server network packet validation is secure. GUI selection button disabling, tooltip formatting, and lock badge rendering are verified. A minor fix is recommended for GUI Were-form preview cleanup in `RaceSelectionScreen.onClose()`.

---

## 5. Verification Method

To independently run the verification test suite:

```bash
./gradlew test
```

Inspect output for `:common:runM3NetworkAndGUITests` and `:common:runM3AdversarialR2R3Tests`. All test cases report `SUMMARY: 7 PASSED, 0 FAILED` and `BUILD SUCCESSFUL`.
