# Technical Analysis Report: VIP / Permission-Locked Races (R2) & Configurable First-Join Selection GUI Toggle (R3)

**Author**: Explorer 2 (Milestone 1)  
**Target Module**: `common` (`ddraig.net.customraces`)  
**Date**: 2026-07-24  

---

## Executive Summary

This investigation covers Requirement R2 (VIP / Permission-Locked Races) and Requirement R3 (Configurable First-Join Selection GUI Toggle) in the Custom Races Framework codebase.

1. **Requirement R2 (VIP / Permission-Locked Races)**:
   - `RaceData.java` already defines `public String permissionLock = "";` (line 78), but NBT serialization (`toNBT` / `fromNBT`) omits `permissionLock`.
   - `RaceRegistry.java` lacks a permission validation method to check if a `ServerPlayer` holds the required permission node before allowing race selection.
   - `ModPackets.java` lacks server-side permission verification in the `SET_PLAYER_RACE_ID` packet handler.
   - `RaceSelectionScreen.java` does not currently check permission lock state, render the `🔒 VIP / LOCKED` badge, present the tooltip `§cRequires Permission: §e<permissionLock>`, or disable the confirm button for locked races.

2. **Requirement R3 (Configurable First-Join Selection GUI Toggle)**:
   - `RaceRegistry.java` defines static field `public static boolean autoOpenSelectionOnJoin = true;` (line 24).
   - `FirstJoinHandler.java` checks `else if (RaceRegistry.autoOpenSelectionOnJoin)` on player join (line 25).
   - However, `RaceRegistry.java` lacks disk persistence logic (`loadConfig()` and `saveConfig()`) to load and save `autoOpenSelectionOnJoin` from `config/custom_races/config.json`.

---

## Detailed Technical Findings

### 1. Requirement R2: VIP / Permission-Locked Races

#### 1.1 Data Structure & NBT/JSON Serialization (`RaceData.java`)
- **Field Location**: `RaceData.java:78`
  ```java
  public String permissionLock = "";
  ```
- **JSON Serialization**: Managed via GSON in `RaceRegistry.saveRaces()` and `loadRaces()`. Because `permissionLock` is a standard public field, GSON serializes and deserializes it automatically.
- **NBT Serialization Gap**:
  - `toNBT(CompoundTag tag)` (lines 359–414): Omit saving `permissionLock`.
  - `fromNBT(CompoundTag tag)` (lines 417–473): Omit reading `permissionLock`.
  - `initDefaults()` (lines 258–285): Omit initializing `permissionLock = ""` if null.

**Proposed Code Edit (`RaceData.java`)**:
```java
// In initDefaults():
if (permissionLock == null) permissionLock = "";

// In toNBT(CompoundTag tag):
tag.putString("permissionLock", permissionLock != null ? permissionLock : "");

// In fromNBT(CompoundTag tag):
if (tag.contains("permissionLock")) this.permissionLock = tag.getString("permissionLock");
```

---

#### 1.2 Server-Side Permission Checking (`RaceRegistry.java` & `ModPackets.java`)

- **Validation Helper (`RaceRegistry.java`)**:
  A static helper method `canPlayerSelectRace(ServerPlayer player, RaceData race)` is needed to evaluate whether a player may select a race:
  - If `race == null`, return `false`.
  - If `race.permissionLock` is null or empty (`permissionLock.trim().isEmpty()`), return `true` (open to all).
  - If `player.hasPermissions(2)` (OP level 2+), return `true` (OP bypass).
  - If `permissionLock` is numeric (e.g. `"3"`), check `player.hasPermissions(numericLevel)`.
  - If a non-OP player attempts selection on a permission-locked race, return `false`.

```java
public static boolean canPlayerSelectRace(net.minecraft.server.level.ServerPlayer player, RaceData race) {
    if (race == null) return false;
    if (race.permissionLock == null || race.permissionLock.trim().isEmpty()) {
        return true;
    }
    if (player == null) return false;
    if (player.hasPermissions(2)) {
        return true;
    }
    try {
        int level = Integer.parseInt(race.permissionLock.trim());
        return player.hasPermissions(level);
    } catch (NumberFormatException ignored) {}
    return false;
}
```

- **Network Validation (`ModPackets.java`)**:
  In `SET_PLAYER_RACE_ID` server packet handler (lines 136–145):
  ```java
  NetworkManager.registerReceiver(NetworkManager.Side.C2S, SET_PLAYER_RACE_ID, (buf, context) -> {
      String raceId = buf.readUtf(256);
      ServerPlayer player = (ServerPlayer) context.getPlayer();
      context.queue(() -> {
          RaceData race = RaceRegistry.getRace(raceId);
          if (race != null && !RaceRegistry.canPlayerSelectRace(player, race)) {
              player.sendSystemMessage(Component.literal("§cYou do not have permission to select the " + race.name + " race! (§e" + race.permissionLock + "§c)"));
              return;
          }
          RaceRegistry.setPlayerRace(player.getUUID(), raceId);
          if (race != null) PehkuiIntegration.applyRaceScales(player, race);
          syncRacesToAll(player.getServer());
      });
  });
  ```

---

#### 1.3 Client GUI Rendering (`RaceSelectionScreen.java`)

- **Lock Detection Helper**:
  ```java
  public boolean isRaceLocked(RaceData race) {
      if (race == null) return false;
      if (race.permissionLock == null || race.permissionLock.trim().isEmpty()) {
          return false;
      }
      if (this.minecraft != null && this.minecraft.player != null) {
          if (this.minecraft.player.hasPermissions(2)) {
              return false;
          }
          try {
              int level = Integer.parseInt(race.permissionLock.trim());
              return !this.minecraft.player.hasPermissions(level);
          } catch (NumberFormatException ignored) {}
      }
      return true;
  }
  ```

- **Scrollable List Item Rendering (Left Panel)**:
  In `render(...)` (lines 145–161):
  ```java
  boolean isLocked = isRaceLocked(race);
  String prefix = isLocked ? "§c🔒 " : (isSelected ? "§e§l❖ " : "§7• ");
  String displayName = prefix + race.name + (isLocked ? " §c[VIP]" : "");
  guiGraphics.drawString(this.font, displayName, 16, itemY + 6, 0xFFFFFF);
  ```

- **Center Panel Detail Rendering & Badge**:
  In `render(...)` center panel header (lines 175–195):
  ```java
  boolean isSelectedLocked = isRaceLocked(selectedRace);
  if (isSelectedLocked) {
      int badgeX = centerLeft + centerWidth - 110;
      guiGraphics.fill(badgeX, topY + 5, centerLeft + centerWidth - 10, topY + 23, 0xFF8B0000);
      guiGraphics.fill(badgeX, topY + 5, centerLeft + centerWidth - 10, topY + 6, 0xFFFF5555);
      guiGraphics.drawCenteredString(this.font, "🔒 VIP / LOCKED", badgeX + 50, topY + 9, 0xFFFFFF);
  }
  ```

- **Confirm Choice Button Handling**:
  In `render(...)` or `init()` state update:
  ```java
  boolean isLocked = isRaceLocked(selectedRace);
  if (confirmButton != null) {
      confirmButton.active = !isLocked && selectedRace != null;
      if (isLocked && selectedRace != null) {
          confirmButton.setTooltip(Tooltip.create(Component.literal("§cRequires Permission: §e" + selectedRace.permissionLock)));
      } else {
          confirmButton.setTooltip(Tooltip.create(Component.translatable("gui.customraces.tooltip.confirm")));
      }
  }
  ```

---

### 2. Requirement R3: Configurable First-Join Selection GUI Toggle

#### 2.1 Configuration File Persistence (`RaceRegistry.java`)

- **Current Implementation**:
  - `RaceRegistry.java:24`: `public static boolean autoOpenSelectionOnJoin = true;`
  - `FirstJoinHandler.java:25`: `else if (RaceRegistry.autoOpenSelectionOnJoin)` calls `ModPackets.openRaceSelection(serverPlayer);`.

- **Missing Persistence Logic**:
  `config/custom_races/config.json` is not currently created, loaded, or saved.

**Proposed Code Additions (`RaceRegistry.java`)**:

```java
private static File getConfigFile() {
    File dir = new File("config/custom_races");
    if (!dir.exists()) dir.mkdirs();
    return new File(dir, "config.json");
}

public static void loadConfig() {
    File file = getConfigFile();
    if (file.exists()) {
        try (FileReader reader = new FileReader(file)) {
            com.google.gson.JsonObject json = GSON.fromJson(reader, com.google.gson.JsonObject.class);
            if (json != null && json.has("autoOpenSelectionOnJoin")) {
                autoOpenSelectionOnJoin = json.get("autoOpenSelectionOnJoin").getAsBoolean();
            }
        } catch (Exception e) {
            System.err.println("[CustomRaces] Error loading config.json: " + e.getMessage());
        }
    } else {
        saveConfig();
    }
}

public static void saveConfig() {
    File file = getConfigFile();
    try (FileWriter writer = new FileWriter(file)) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("autoOpenSelectionOnJoin", autoOpenSelectionOnJoin);
        GSON.toJson(json, writer);
    } catch (Exception e) {
        System.err.println("[CustomRaces] Error saving config.json: " + e.getMessage());
    }
}
```

- **Initialization Hook (`RaceRegistry.init()`)**:
  ```java
  public static void init() {
      initDirectories();
      loadConfig();
      loadRaces();
      loadPlayerRaces();
      rebuildSuggestionsCache();
  }
  ```

- **Command Reload Hook (`CustomRacesCommands.java`)**:
  In `/custom_races admin reload`:
  ```java
  .then(Commands.literal("reload")
      .executes(context -> {
          RaceRegistry.loadConfig();
          RaceRegistry.loadRaces();
          RaceRegistry.loadPlayerRaces();
          ModPackets.syncRacesToAll(context.getSource().getServer());
          context.getSource().sendSuccess(() -> Component.literal("Reloaded all race configurations!"), true);
          return 1;
      })
  )
  ```

---

## Architectural Parity & Cross-Platform Considerations

- **Fabric & Forge Compatibility**:
  - `dev.architectury.event.events.common.PlayerEvent.PLAYER_JOIN` handles player join uniformly on both platforms.
  - `ServerPlayer.hasPermissions(int level)` is standard Minecraft API available in both Fabric and Forge environments.
  - Packet handling via `dev.architectury.networking.NetworkManager` works identically across Fabric and Forge.

---

## Implementation Roadmap for Milestone 3

| Component | Target File | Action Required |
|-----------|-------------|-----------------|
| `RaceData` | `RaceData.java` | Add `permissionLock` to `toNBT`, `fromNBT`, and `initDefaults`. |
| `RaceRegistry` | `RaceRegistry.java` | Add `canPlayerSelectRace(...)`, `getConfigFile()`, `loadConfig()`, and `saveConfig()`. Call `loadConfig()` in `init()`. |
| `ModPackets` | `ModPackets.java` | Add `canPlayerSelectRace` permission check in `SET_PLAYER_RACE_ID` server receiver. |
| `RaceSelectionScreen` | `RaceSelectionScreen.java` | Add `isRaceLocked(...)`, badge rendering, lock icons, tooltip `§cRequires Permission: §e<permissionLock>`, and disable confirm button when locked. |
| `FirstJoinHandler` | `FirstJoinHandler.java` | Confirm check against `RaceRegistry.autoOpenSelectionOnJoin`. |
| `CustomRacesCommands` | `CustomRacesCommands.java` | Add `RaceRegistry.loadConfig()` to `/custom_races admin reload`. |
