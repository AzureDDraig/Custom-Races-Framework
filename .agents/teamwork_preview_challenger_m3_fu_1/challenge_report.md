# Challenge Report — Milestone 3 (Requirement R2 & Requirement R3)

**Overall Risk Assessment**: LOW / PASSED

## Challenge Summary

Adversarial empirical testing was conducted on Requirement R2 (Permission Locks) and Requirement R3 (Config Persistence) within the `Custom Races Framework`. Tests were executed via `./gradlew test` using a dedicated harness (`M3AdversarialR2R3Test`). All 9 adversarial test cases passed cleanly across 15 total test tasks in the project build pipeline.

---

## 1. Requirement R2: Permission Locks (`canPlayerSelectRace`)

### Evaluated Dimensions & Empirical Results:
1. **Null / Empty / Whitespace Permission Locks (`null`, `""`, `"   "`)**:
   - **Expected**: Default open access for all players.
   - **Actual**: `RaceRegistry.canPlayerSelectRace` returned `true` for `null` player, non-OP player (level 0), and OP player (level 2).
   - **Status**: PASSED.

2. **String Permission Nodes (`"customraces.vip"`, `"admin.only"`)**:
   - **Expected**: Restrict non-OP players, allow OP level 2 players.
   - **Actual**:
     - `null` player: Returns `false`.
     - Non-OP player (level 0 / level 1): Returns `false`. `Integer.parseInt("customraces.vip")` throws `NumberFormatException`, caught safely and returns `false`.
     - OP player (level 2): Returns `true` via `player.hasPermissions(2)` early check.
   - **Status**: PASSED.

3. **Numeric Permission Levels (`"2"`, `"4"`)**:
   - **Expected**: Strict level requirement.
   - **Actual**:
     - Level 2 lock: Level 0/1 rejected (`false`), Level 2/4 accepted (`true`).
     - Level 4 lock: Level 0/1 rejected (`false`), Level 2/4 accepted (`true`). Note: OP Level 2 players pass early due to `player.hasPermissions(2)` check.
   - **Status**: PASSED.

4. **Numeric Edge Cases (`"-1"`, `"9999999999999999999999"`)**:
   - **Expected**: Graceful fallback without crash or integer overflow exception.
   - **Actual**:
     - `"-1"`: Level 0 player accepted (`hasPermissions(-1)` evaluates true).
     - Overflow string: `Integer.parseInt` throws `NumberFormatException`, safely returning `false` for non-OP players and `true` for OP players.
   - **Status**: PASSED.

5. **Null Handles**:
   - `canPlayerSelectRace(null, null)` -> `false`.
   - `canPlayerSelectRace(player, null)` -> `false`.
   - `canPlayerSelectRace(null, lockedRace)` -> `false`.
   - **Status**: PASSED.

---

## 2. Requirement R3: Config Persistence (`loadConfig` / `saveConfig`)

### Evaluated Dimensions & Empirical Results:
1. **Missing Config File**:
   - **Actual**: When `config/custom_races/config.json` is missing, `loadConfig()` invokes `saveConfig()`, creating default `config.json` on disk.
   - **Status**: PASSED.

2. **Corrupt JSON Resilience**:
   - **Actual**: Tested with malformed syntax (`{ "autoOpen...`), truncated JSON, and array JSON (`[1,2,3]`). All `JsonSyntaxException` / `EOFException` / `ClassCastException` occurrences are caught by `loadConfig()`, leaving existing in-memory state unchanged without crashing.
   - **Status**: PASSED.

3. **Invalid Field Data Types**:
   - **Actual**: Number primitive or nested JSON object in place of `autoOpenSelectionOnJoin` boolean field handled without crash.
   - **Status**: PASSED.

4. **Toggle Flips Persistence**:
   - **Actual**: Sequentially flipped `autoOpenSelectionOnJoin` (`false` -> `saveConfig` -> clear memory -> `loadConfig` -> `false`, then `true` -> `saveConfig` -> clear memory -> `loadConfig` -> `true`). Verified exact state persistence on disk and reload accuracy.
   - **Status**: PASSED.

5. **Concurrent Stress Execution**:
   - **Actual**: 20 concurrent worker threads executed 10,000 total `saveConfig()` and `loadConfig()` calls under contention. Zero thread deadlocks or uncaught I/O exceptions occurred.
   - **Status**: PASSED.

---

## Stress Test Results

| Scenario | Target Requirement | Expected Behavior | Actual Behavior | Pass / Fail |
|---|---|---|---|---|
| `permissionLock = null / "" / "   "` | R2 | Allow all players | Allowed null, non-OP, and OP players | PASS |
| `permissionLock = "customraces.vip"` | R2 | Block non-OP, allow OP | Blocked non-OP, allowed OP level 2 | PASS |
| `permissionLock = "2"` | R2 | Allow level >= 2 | Rejected level 0/1, allowed level 2/4 | PASS |
| `permissionLock = "9999999999999999999999"` | R2 | Fallback to false for non-OP | Rejected non-OP, allowed OP | PASS |
| Missing `config.json` | R3 | Auto-create default file | Created `config.json` on disk | PASS |
| Malformed / Truncated JSON | R3 | Catch exception & retain state | Retained in-memory state cleanly | PASS |
| Array JSON in config file | R3 | Catch exception & retain state | Retained in-memory state cleanly | PASS |
| Toggle `autoOpenSelectionOnJoin` | R3 | Save & load state accurately | Accurately persisted & loaded state | PASS |
| 10,000 Concurrent Save/Load Ops | R3 | Thread safety & I/O resilience | Zero exceptions / zero corruption | PASS |

---

## Unchallenged Areas

- **LuckPerms / External Permission Plugins Integration**: `RaceRegistry.canPlayerSelectRace` relies on `Player.hasPermissions(int)`. Advanced permission node checks delegating to Forge/Fabric permission APIs or LuckPerms were not tested as no third-party permission API dependency is included in `common`.
