# Summary of Changes — Worker M3 (Milestone 3)

## Objective
Implemented Requirement R2 (VIP / Permission-Locked Races) and Requirement R3 (Configurable First-Join Selection GUI Toggle).

---

## 1. `RaceData.java`
- **File Path**: `common/src/main/java/ddraig/net/customraces/data/RaceData.java`
- **Changes Made**:
  - `initDefaults()`: Added null check `if (permissionLock == null) permissionLock = "";`.
  - `toNBT(CompoundTag tag)`: Added NBT serialization for permission lock `tag.putString("permissionLock", permissionLock != null ? permissionLock : "");`.
  - `fromNBT(CompoundTag tag)`: Added NBT deserialization `if (tag.contains("permissionLock")) this.permissionLock = tag.getString("permissionLock");`.

---

## 2. `RaceRegistry.java`
- **File Path**: `common/src/main/java/ddraig/net/customraces/data/RaceRegistry.java`
- **Changes Made**:
  - Added `public static boolean canPlayerSelectRace(Player player, RaceData race)`: Checks if `permissionLock` is empty (unlocked), if player is OP (hasPermissions(2)), or checks numeric permission level.
  - Added configuration management methods:
    - `getConfigFile()`: Returns `File("config/custom_races/config.json")`.
    - `loadConfig()`: Deserializes `config.json` and updates `autoOpenSelectionOnJoin`.
    - `saveConfig()`: Serializes `autoOpenSelectionOnJoin` to `config.json`.
  - `init()`: Added call to `loadConfig()` upon initialization.

---

## 3. `ModPackets.java`
- **File Path**: `common/src/main/java/ddraig/net/customraces/network/ModPackets.java`
- **Changes Made**:
  - Imported `net.minecraft.network.chat.Component`.
  - `SET_PLAYER_RACE_ID` server packet handler: Added validation `if (race != null && !RaceRegistry.canPlayerSelectRace(player, race))`. Sends systemic error message and aborts race assignment if player is unauthorized.

---

## 4. `RaceSelectionScreen.java`
- **File Path**: `common/src/main/java/ddraig/net/customraces/client/gui/RaceSelectionScreen.java`
- **Changes Made**:
  - Added `public boolean isRaceLocked(RaceData race)` to evaluate client lock status.
  - Rendered `🔒 VIP / LOCKED` dark red badge banner in center detail panel header for locked races.
  - Rendered `§c🔒` icon and `§c[VIP]` tag in the left scrollable list for locked races.
  - Attached tooltip `§cRequires Permission: §e<permissionLock>` when hovering over locked races or disabled confirm button.
  - Set `confirmButton.active = !isSelectedLocked && selectedRace != null`.

---

## 5. `CustomRacesCommands.java`
- **File Path**: `common/src/main/java/ddraig/net/customraces/command/CustomRacesCommands.java`
- **Changes Made**:
  - In `/custom_races admin reload`: Added `RaceRegistry.loadConfig();` prior to reloading races.

---

## 6. Build & Test Infrastructure
- **File Path**: `common/src/test/java/ddraig/net/customraces/data/M3VIPAndConfigVerificationTest.java`
  - Created standalone verification test suite testing NBT serialization, null/empty checks, permission evaluation, and config load/save.
- **File Path**: `common/build.gradle`
  - Added `runM3VIPAndConfigTests` JavaExec task and wired `test.dependsOn` dependencies.
