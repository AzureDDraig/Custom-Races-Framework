## 2026-07-24T18:57:06Z
You are Worker M3 for Milestone 3 of the Custom Races Framework project.
Your working directory is: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m3_fu

Objective: Implement Requirement R2 (VIP / Permission-Locked Races) and Requirement R3 (Configurable First-Join Selection GUI Toggle).
Detailed Task Instructions:
1. Refer to M1 Explorer 2's report at `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_2_fu\handoff.md` and `analysis.md`.
2. Requirement R2 (VIP / Permission-Locked Races):
   - `RaceData.java`: Ensure `public String permissionLock = "";`, add NBT serialization (`tag.putString("permissionLock", permissionLock)` in `toNBT`, `permissionLock = tag.getString("permissionLock")` in `fromNBT`), and null check in `initDefaults()`.
   - `RaceRegistry.java`: Add `public static boolean canPlayerSelectRace(Player player, RaceData race)`. If `permissionLock` is non-empty, evaluate if player has permission or OP level 2.
   - `ModPackets.java`: In `SET_PLAYER_RACE_ID` server packet handler, validate `RaceRegistry.canPlayerSelectRace(serverPlayer, race)`. Reject race assignment if unauthorized.
   - `RaceSelectionScreen.java`:
     - Determine client lock state `isRaceLocked(RaceData race)`.
     - Render "🔒 VIP / LOCKED" badge banner in center detail panel for locked races.
     - Render `§c🔒` lock icon in race list next to locked races.
     - Attach tooltip "§cRequires Permission: §e" + race.permissionLock when hovering over locked race or confirm button.
     - Disable confirm button (`confirmButton.active = false`) when selected race is locked.
3. Requirement R3 (Configurable First-Join Selection GUI Toggle):
   - `RaceRegistry.java`: Add `getConfigFile()`, `loadConfig()`, `saveConfig()` reading/writing `config/custom_races/config.json` with field `autoOpenSelectionOnJoin` (boolean, default: true). Call `loadConfig()` in `RaceRegistry.init()`.
   - `FirstJoinHandler.java`: Check `RaceRegistry.autoOpenSelectionOnJoin` before opening selection screen on first join.
   - `CustomRacesCommands.java`: Call `RaceRegistry.loadConfig()` in `/custom_races admin reload`.
4. Run build verification: `./gradlew build -x test` and test suite `./gradlew test`. Document exact commands and outputs.
5. Write full handoff report (`handoff.md`) and summary of changes (`changes.md`) in your working directory.
6. Send a message to your parent with build results and path to your handoff report.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
