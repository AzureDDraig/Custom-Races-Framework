# Forensic Audit Report — Milestone 3 (R2 & R3 Verification)

**Work Product**: Milestone 3 (Requirement R2: VIP Permission Locks & Requirement R3: First-Join Selection GUI Toggle)
**Profile**: General Project
**Verdict**: CLEAN

---

### Executive Summary

A comprehensive forensic audit was conducted on Worker M3's implementation of Requirements R2 and R3. All six target source files (`RaceData.java`, `RaceRegistry.java`, `ModPackets.java`, `RaceSelectionScreen.java`, `FirstJoinHandler.java`, and `CustomRacesCommands.java`) were inspected for authentic logic. No hardcoded test outcomes, facade implementations, bypassed validation, or fake attestations were detected. Multi-platform compilation (`./gradlew build -x test`) and empirical unit test suites passed cleanly.

---

### Phase Results

| Phase / Check | Description | Status | Evidence / Details |
|---|---|:---:|---|
| **Phase 1: Source Analysis** | Hardcoded test outcome check | **PASS** | Source code contains dynamic logic, string parsing, and NBT/Gson serialization without pre-baked pass values. |
| **Phase 1: Source Analysis** | Facade implementation check | **PASS** | `RaceRegistry.canPlayerSelectRace`, `ModPackets.register`, and `FirstJoinHandler.init` execute genuine validation logic. |
| **Phase 1: Source Analysis** | Pre-populated artifact check | **PASS** | No pre-baked log or attestation files exist in the repository pre-audit. |
| **Phase 2: Behavioral Check** | Gradle Build Verification | **PASS** | `./gradlew build -x test` succeeded in 13s across Fabric & Forge modules. |
| **Phase 2: Behavioral Check** | Unit & Stress Test Suites | **PASS** | `runM3VIPAndConfigTests` (5/5 PASSED) and `runM3AdversarialR2R3Tests` (9/9 PASSED) completed with zero errors. |
| **Phase 2: Behavioral Check** | Server-Side Validation Check | **PASS** | `ModPackets.java` enforces `canPlayerSelectRace` on server C2S packet handler, preventing permission bypass. |

---

### File Inspection Details

1. **`RaceData.java`**:
   - Declares `public String permissionLock = "";` (line 78), `public int particleCount = 5;`, `public int wereParticleCount = 10;`.
   - Default initialization (`initDefaults()`) sets fallback for null/whitespace strings and invalid particle counts.
   - Serializes and deserializes `permissionLock`, `particleCount`, and `wereParticleCount` via `toNBT` (lines 376–377, 415) and `fromNBT` (lines 435–436, 474).

2. **`RaceRegistry.java`**:
   - Declares `public static boolean autoOpenSelectionOnJoin = true;` (line 25).
   - Implements persistent JSON configuration via `loadConfig()` and `saveConfig()` targeting `config/custom_races/config.json`.
   - Implements `canPlayerSelectRace(Player player, RaceData race)`:
     - Null/empty/whitespace check returns `true` (unlocked).
     - OP level 2 check (`player.hasPermissions(2)`) returns `true`.
     - Numeric level parsing checks `player.hasPermissions(level)`.
     - Non-numeric permission node strings safely default to restricted for non-OP players.

3. **`ModPackets.java`**:
   - Implements server-side packet handler for `SET_PLAYER_RACE_ID` (lines 137–150).
   - Evaluates `RaceRegistry.canPlayerSelectRace(player, race)` on the server side prior to assigning player race. Sends a refusal system message if permission is lacking.

4. **`RaceSelectionScreen.java`**:
   - Implements client-side `isRaceLocked(RaceData race)` helper matching server rules.
   - Displays `"🔒 VIP / LOCKED"` badge and `"§cRequires Permission: §e" + permissionLock` tooltip.
   - Sets `confirmButton.active = !isSelectedLocked && selectedRace != null;` to disable confirmation for locked races.
   - Displays `"🔒 VIP"` tags in scrollable race list.

5. **`FirstJoinHandler.java`**:
   - Registers `PLAYER_JOIN` listener checking `RaceRegistry.autoOpenSelectionOnJoin` before sending `OPEN_SELECTION_ID` packet to unassigned players.

6. **`CustomRacesCommands.java`**:
   - Implements admin reload (`/custom_races admin reload`) which reloads `config.json` via `RaceRegistry.loadConfig()`.

---

### Forensic Evidence & Commands Executed

1. **Gradle Build Verification**:
   ```powershell
   .\gradlew build -x test
   ```
   *Result*: `BUILD SUCCESSFUL in 13s` (31 actionable tasks: 20 executed, 11 up-to-date).

2. **VIP Lock & Config Verification Test Suite**:
   ```powershell
   .\gradlew :common:runM3VIPAndConfigTests
   ```
   *Result*: `RESULTS: 5 Passed, 0 Failed` (PermissionLock NBT, Defaults, Null/Empty checks, Locked Null Player, Config Persistence).

3. **Adversarial VIP Lock & Config Stress Test Suite**:
   ```powershell
   .\gradlew :common:runM3AdversarialR2R3Tests
   ```
   *Result*: `SUMMARY: 9 PASSED, 0 FAILED` (Null/Empty across player types, String nodes, Numeric levels, Edge cases -1/overflow, Missing file auto-creation, Corrupt JSON syntax resilience, Invalid data types, Toggle flips, 20-thread 10,000 op concurrency).

---

### Final Verdict

**`CLEAN`** — Milestone 3 (Requirements R2 & R3) is fully authentic, robustly implemented, and verified empirically.
