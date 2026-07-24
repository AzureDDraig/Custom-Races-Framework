# Were-Race Transformation State & Networking Analysis Report

## Executive Summary
This investigation analyzes the client-side transformation state (`ClientWereState`), server transformation tracking (`WereRaceTransformHandler`), network packet handlers (`ModPackets`), and tracking client synchronization across the `customraces` framework. 

The primary bug causing transformed players to render as default player models on tracking clients is the **absence of a server entity-tracking listener (`PlayerEvent.PLAYER_START_TRACKING`)**. When a player transforms, the state is broadcast to currently online players. However, when other players move into rendering/chunk distance, change dimensions, or log in after transformation, the server does not transmit the transformation state to the newly tracking clients. Furthermore, client-side packet reception lacks bounding box dimension refreshing (`player.refreshDimensions()`) and scale updates.

---

## 1. Codebase Transformation State Architecture & Flow

### 1.1 Server State Storage & Logic
- **File**: `common/src/main/java/ddraig/net/customraces/event/WereRaceTransformHandler.java`
- **State Map**: `private static final Map<UUID, Boolean> TRANSFORMED_PLAYERS = new ConcurrentHashMap<>();` (Line 26)
- **State Query Method**: 
  ```java
  public static boolean isTransformed(UUID uuid) {
      if (uuid == null) return false;
      if (TRANSFORMED_PLAYERS.getOrDefault(uuid, false)) return true;
      return ddraig.net.customraces.client.ClientWereState.isTransformed(uuid);
  }
  ```
  *(Lines 53–57)*
- **Transformation Trigger**: `transformIntoWereForm(ServerPlayer player, RaceData race)` (Lines 141–187):
  - Line 142: `TRANSFORMED_PLAYERS.put(player.getUUID(), true);`
  - Line 143: `ModPackets.syncWereStateToAll(player.getServer(), player.getUUID(), true);`
  - Line 183: `PehkuiIntegration.applyRaceScales(player, race);`
- **Reversion Trigger**: `revertWereForm(ServerPlayer player, RaceData race)` (Lines 189–204):
  - Line 190: `TRANSFORMED_PLAYERS.remove(player.getUUID());`
  - Line 191: `ModPackets.syncWereStateToAll(player.getServer(), player.getUUID(), false);`
  - Line 200: `PehkuiIntegration.applyRaceScales(player, race);`

### 1.2 Client State Storage & Logic
- **File**: `common/src/main/java/ddraig/net/customraces/client/ClientWereState.java`
- **State Map**: `public static final Map<UUID, Boolean> TRANSFORMED_PLAYERS = new ConcurrentHashMap<>();` (Line 11)
- **State Mutator**: `setTransformed(UUID uuid, boolean transformed)` (Lines 17–24):
  - If `transformed == true`, inserts `(uuid, true)`.
  - If `transformed == false`, removes `uuid` key.

### 1.3 Client Render Layer Inspection
- **File**: `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`
- **Render Check**:
  ```java
  boolean isWereTransformed = ddraig.net.customraces.client.ClientWereState.isTransformed(player.getUUID())
          || ddraig.net.customraces.event.WereRaceTransformHandler.isTransformed(player.getUUID());
  ```
  *(Lines 39–40)*
- If `isWereTransformed` is `true` and `race.enableWereRace` is `true`:
  - Applies werewolf model scaling: `poseStack.scale(wScale, hScale, wScale)` (Line 46).
  - Renders werewolf beast features: ears, snout, glowing crimson eyes (Line 49).
- If `isWereTransformed` is `false`:
  - Falls back to rendering base race preset parts (wings, tail, horns, halo) over the default player model (Line 70).

---

## 2. Root Cause Analysis: Tracking Client Synchronization Failure

### 2.1 Flaw 1 — Missing `PlayerEvent.PLAYER_START_TRACKING` Event Listener
- **Current Behavior**: State sync packets are sent ONLY when:
  1. A player actively triggers transform/revert (`syncWereStateToAll` in `WereRaceTransformHandler.java:143, 191`).
  2. A player joins the server (`FirstJoinHandler.java:19`).
- **Defect**: When Player A is transformed and Player B (already on the server) walks into entity tracking distance, teleports near Player A, or changes dimensions into Player A's world, Minecraft's tracking system starts tracking Player A for Player B.
- **Impact**: Because no listener is registered for Architectury's `PlayerEvent.PLAYER_START_TRACKING`, Player B's client NEVER receives a `SYNC_WERE_STATE_ID` packet for Player A. On Player B's client, `ClientWereState.isTransformed(Player A.getUUID())` evaluates to `false`. Player B renders Player A as a normal player entity.

### 2.2 Flaw 2 — Missing Client Bounding Box & Scale Refresh on Packet Receipt
- **Current Behavior**: In `ModPackets.java` (lines 41–47), receiving `SYNC_WERE_STATE_ID` updates `ClientWereState.setTransformed(pUuid, isTransformed)` on the client context queue.
- **Defect**: The packet handler does NOT invoke `clientPlayer.refreshDimensions()` or `PehkuiIntegration.applyRaceScales(clientPlayer, race)` for the target player on the client main thread.
- **Impact**: The client player entity retains default bounding box dimensions and scale, causing visual desynchronization between hitbox height, eye height, and rendered model scale.

### 2.3 Flaw 3 — Incomplete `PLAYER_RESPAWN` Sync
- **Current Behavior**: `FirstJoinHandler.java` registers `PLAYER_RESPAWN` (lines 30–37) to re-apply Pehkui scales, but does not re-sync active transformation states to the respawned player or update tracking clients of the respawned player entity.

### 2.4 Flaw 4 — Stale Client State Lifecycle Cleanup
- **Current Behavior**: `ClientWereState.clear()` exists in `ClientWereState.java:26`, but is not hooked to client disconnection or world unload events.
- **Impact**: Rejoining a singleplayer world or switching servers may retain stale transformation states in `ClientWereState.TRANSFORMED_PLAYERS`.

---

## 3. Networking Packet & Listener Inventory

### 3.1 Existing Packets & Handlers
| Packet ResourceLocation | Direction | Primary Purpose | File & Location |
|---|---|---|---|
| `customraces:sync_were_state` | S2C | Broadcasts transformation state (`UUID`, `boolean`) | `ModPackets.java:36, 41-47, 215-223` |
| `customraces:toggle_were_form` | C2S | Client keybind request to toggle Were-form | `ModPackets.java:37, 146-151, 200-203` |
| `customraces:sync_races` | S2C | Syncs loaded race definitions & player assignments | `ModPackets.java:29, 49-81, 154-174` |

### 3.2 Required Server Tracking Listener Registration
- **Event**: `dev.architectury.event.events.common.PlayerEvent.PLAYER_START_TRACKING`
- **Trigger**: Fired by Architectury whenever a player starts tracking an entity (e.g. player enters render distance or dimension).
- **Target Logic**: When `trackedEntity instanceof ServerPlayer targetPlayer`, check `WereRaceTransformHandler.isTransformed(targetPlayer.getUUID())`. If `true`, send `SYNC_WERE_STATE_ID` for `targetPlayer` to `trackingPlayer`.

---

## 4. Precise Required Code Changes

### Change 1: `common/src/main/java/ddraig/net/customraces/network/ModPackets.java`
1. **Add `sendWereStateToPlayer` helper method**:
   ```java
   public static void sendWereStateToPlayer(ServerPlayer recipient, UUID playerUuid, boolean isTransformed) {
       if (recipient == null || playerUuid == null) return;
       FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
       buf.writeUUID(playerUuid);
       buf.writeBoolean(isTransformed);
       NetworkManager.sendToPlayer(recipient, SYNC_WERE_STATE_ID, buf);
   }
   ```
2. **Refactor `syncWereStateToAll`** (Lines 215–223):
   Replace internal loop byte-buf creation with calls to `sendWereStateToPlayer(p, playerUuid, isTransformed)`.
3. **Enhance `SYNC_WERE_STATE_ID` Client Receiver** (Lines 41–47):
   ```java
   NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_WERE_STATE_ID, (buf, context) -> {
       UUID pUuid = buf.readUUID();
       boolean isTransformed = buf.readBoolean();
       context.queue(() -> {
           ddraig.net.customraces.client.ClientWereState.setTransformed(pUuid, isTransformed);
           net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
           if (mc.level != null) {
               net.minecraft.world.entity.player.Player target = mc.level.getPlayerByUUID(pUuid);
               if (target != null) {
                   target.refreshDimensions();
                   RaceData race = RaceRegistry.getPlayerRace(pUuid);
                   if (race != null) {
                       PehkuiIntegration.applyRaceScales(target, race);
                   }
               }
           }
       });
   });
   ```

### Change 2: `common/src/main/java/ddraig/net/customraces/event/WereRaceTransformHandler.java`
1. **Register `PLAYER_START_TRACKING` in `init()`** (Lines 31–39):
   ```java
   public static void init() {
       TickEvent.PLAYER_POST.register(player -> {
           if (player instanceof ServerPlayer serverPlayer) {
               if (serverPlayer.tickCount % 40 == 0) {
                   checkTransformation(serverPlayer);
               }
           }
       });

       dev.architectury.event.events.common.PlayerEvent.PLAYER_START_TRACKING.register((trackedEntity, trackingPlayer) -> {
           if (trackedEntity instanceof ServerPlayer targetPlayer) {
               if (isTransformed(targetPlayer.getUUID())) {
                   ddraig.net.customraces.network.ModPackets.sendWereStateToPlayer(trackingPlayer, targetPlayer.getUUID(), true);
               }
           }
       });
   }
   ```
2. **Refactor `syncAllWereStatesTo`** (Lines 41–51):
   Use `ModPackets.sendWereStateToPlayer(targetPlayer, entry.getKey(), true)`.

### Change 3: `common/src/main/java/ddraig/net/customraces/event/FirstJoinHandler.java`
1. **Update `PLAYER_RESPAWN` listener** (Lines 30–37):
   ```java
   PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
       if (newPlayer != null) {
           RaceData race = RaceRegistry.getPlayerRace(newPlayer.getUUID());
           if (race != null) {
               PehkuiIntegration.applyRaceScales(newPlayer, race);
           }
           if (newPlayer instanceof ServerPlayer serverPlayer) {
               WereRaceTransformHandler.syncAllWereStatesTo(serverPlayer);
               if (WereRaceTransformHandler.isTransformed(serverPlayer.getUUID())) {
                   ModPackets.syncWereStateToAll(serverPlayer.getServer(), serverPlayer.getUUID(), true);
               }
           }
       }
   });
   ```

---

## 5. Verification Plan

1. **Multiplayer Tracking Verification**:
   - Spawn two players (Player A and Player B) on a dedicated server.
   - Have Player A transform into Were-form while Player B is out of tracking range (e.g. 200 blocks away).
   - Have Player B walk toward Player A until Player A enters tracking range.
   - Confirm Player B immediately renders Player A's custom Were-form model without needing to re-join or toggle state.
2. **Dimension Change Verification**:
   - Player A transforms in the Overworld. Player B enters the Nether and returns to Overworld near Player A.
   - Confirm `PLAYER_START_TRACKING` re-syncs state and renders Were-form.
3. **Bounding Box & Scale Verification**:
   - Toggle transformation state on local client and remote client.
   - Verify `refreshDimensions()` updates bounding box dimensions without visual stutter or offset.
