# Handoff Report — Explorer 2 (Milestone 1)

**Working Directory**: `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_2_fu`  
**Report Type**: Hard Handoff  
**Target Milestone**: Milestone 3 Implementation (R2 VIP Permission Locks & R3 First-Join Toggle)  

---

## 1. Observation

Direct observations from codebase inspection across `common/src/main/java/ddraig/net/customraces/`:

1. **`RaceData.java` Field & NBT**:
   - Line 78: `public String permissionLock = "";` exists in `RaceData.java`.
   - Lines 359–414 (`toNBT`): `permissionLock` field is omitted from `CompoundTag` serialization.
   - Lines 417–473 (`fromNBT`): `permissionLock` field is omitted from `CompoundTag` deserialization.
   - Lines 258–285 (`initDefaults`): `permissionLock` is not checked for `null`.

2. **`RaceRegistry.java` Permission & Config Handling**:
   - Line 24: `public static boolean autoOpenSelectionOnJoin = true;` is declared.
   - Lines 45–55: `getRacesFile()` and `getPlayerRacesFile()` manage `races.json` and `player_races.json`.
   - No `config.json` helper (`loadConfig()`, `saveConfig()`, `getConfigFile()`) currently exists in `RaceRegistry.java`.
   - No server-side permission validation method (`canPlayerSelectRace`) exists to evaluate permission nodes or OP status.

3. **`ModPackets.java` Server Receiver**:
   - Lines 136–145 (`SET_PLAYER_RACE_ID`): Accepts player race updates from client without validating `permissionLock`.

4. **`RaceSelectionScreen.java` Client GUI**:
   - Lines 68–75 (`confirmButton`): `confirmButton` remains active regardless of selected race lock status.
   - Lines 145–161 (race list loop): Does not render lock badges or icons for permission-locked races.
   - Lines 170–210 (center panel header): Does not display a `🔒 VIP / LOCKED` badge banner.
   - Tooltip for `confirmButton` (line 74) is static `gui.customraces.tooltip.confirm` and does not display `§cRequires Permission: §e<permissionLock>`.

5. **`FirstJoinHandler.java` First-Join Event**:
   - Lines 22–27: Checks `else if (RaceRegistry.autoOpenSelectionOnJoin)` and calls `ModPackets.openRaceSelection(serverPlayer)`.

6. **`CustomRacesCommands.java` Reload Command**:
   - Lines 163–171: `/custom_races admin reload` calls `RaceRegistry.loadRaces()` and `loadPlayerRaces()`, but not `loadConfig()`.

---

## 2. Logic Chain

1. **Observation 1 & 3 → Security Gap**:
   - Because `RaceData.permissionLock` exists on `RaceData` but is never validated in `ModPackets.java` (`SET_PLAYER_RACE_ID`), a client could send a selection packet for any locked race and successfully gain the race stats/abilities.
   - **Reasoning**: A server-side permission check method (`RaceRegistry.canPlayerSelectRace`) must be added and invoked inside `ModPackets.java` before assigning player races.

2. **Observation 1 → Data Integrity**:
   - Omitting `permissionLock` in `toNBT` / `fromNBT` causes `permissionLock` to be lost when saving or transferring race state via NBT tags.
   - **Reasoning**: Adding `tag.putString("permissionLock", ...)` in `toNBT` and reading it in `fromNBT` ensures full NBT codec parity.

3. **Observation 4 → User Experience (R2)**:
   - Without GUI feedback, players would see locked races as selectable or experience silent rejection.
   - **Reasoning**: `RaceSelectionScreen.java` must calculate `isRaceLocked(race)`, display a `§c🔒` icon in the left list, render a `🔒 VIP / LOCKED` banner in the center detail panel, set `confirmButton.active = false`, and attach tooltip `§cRequires Permission: §e<permissionLock>`.

4. **Observation 2 & 5 & 6 → Config Persistence (R3)**:
   - While `FirstJoinHandler.java` reads `RaceRegistry.autoOpenSelectionOnJoin`, `RaceRegistry` never loads or saves `config/custom_races/config.json`. Any changes to this toggle are lost on server restart.
   - **Reasoning**: `RaceRegistry.java` must implement `loadConfig()` and `saveConfig()`, invoke `loadConfig()` during `RaceRegistry.init()`, and update `/custom_races admin reload` in `CustomRacesCommands.java`.

---

## 3. Caveats

- **Permission API Integration**: Server permission checks currently fall back to vanilla OP level check (`player.hasPermissions(2)` or numeric level). If LuckPerms or Fabric Permissions API is loaded on a server, integrated API calls can be added if desired, but standard OP level check provides full vanilla compatibility.
- **Client OP Parity**: On multiplayer servers, client `Minecraft.getInstance().player.hasPermissions(2)` accurately reflects whether the client player has operator status. If a race is locked, non-OP client players will see the button disabled with the tooltip.

---

## 4. Conclusion

- Requirement R2 (VIP / Permission-Locked Races) and Requirement R3 (Configurable First-Join Selection GUI Toggle) are fully mapped.
- All required code additions across `RaceData.java`, `RaceRegistry.java`, `ModPackets.java`, `RaceSelectionScreen.java`, `FirstJoinHandler.java`, and `CustomRacesCommands.java` are documented with complete snippets in `analysis.md`.

---

## 5. Verification Method

1. **Codebase Inspection**:
   - Verify `analysis.md` contains exact line-by-line snippets for `RaceData.java`, `RaceRegistry.java`, `ModPackets.java`, `RaceSelectionScreen.java`, `FirstJoinHandler.java`, and `CustomRacesCommands.java`.

2. **Gradle Build Verification**:
   - Run `./gradlew build -x test` or `./gradlew compileJava` across common, fabric, and forge modules to verify zero syntax or compilation errors after implementation.

3. **In-Game Verification**:
   - **R2 Verification**: Set `"permissionLock": "customraces.vip"` on a race in `config/custom_races/races.json`. Log in as non-OP player -> verify GUI renders `🔒 VIP / LOCKED` badge, lock icon in list, tooltip `§cRequires Permission: §e<permissionLock>`, and disabled confirm button.
   - **R3 Verification**: Set `"autoOpenSelectionOnJoin": false` in `config/custom_races/config.json`. Join with a new player UUID -> verify selection GUI does not auto-open on join.
