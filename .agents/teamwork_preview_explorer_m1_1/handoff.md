# Handoff Report — Were-Race Transformation State & Networking Analysis

## 1. Observation
- **State Storage & Query**:
  - Server Map: `WereRaceTransformHandler.TRANSFORMED_PLAYERS` in `common/src/main/java/ddraig/net/customraces/event/WereRaceTransformHandler.java:26`.
  - Server Query: `WereRaceTransformHandler.isTransformed(UUID uuid)` at `WereRaceTransformHandler.java:53-57`.
  - Client Map: `ClientWereState.TRANSFORMED_PLAYERS` in `common/src/main/java/ddraig/net/customraces/client/ClientWereState.java:11`.
  - Client Query: `ClientWereState.isTransformed(UUID uuid)` at `ClientWereState.java:13-15`.
- **Packet Registrations & Sync Logic**:
  - Packet ResourceLocation: `SYNC_WERE_STATE_ID = new ResourceLocation("customraces", "sync_were_state")` in `common/src/main/java/ddraig/net/customraces/network/ModPackets.java:36`.
  - Client Packet Receiver: `ModPackets.java:41-47` registers `SYNC_WERE_STATE_ID` S2C receiver calling `ClientWereState.setTransformed(pUuid, isTransformed)`.
  - Global Broadcast: `ModPackets.syncWereStateToAll(...)` at `ModPackets.java:215-223`.
  - Initial Join Sync: `FirstJoinHandler.java:19` invokes `WereRaceTransformHandler.syncAllWereStatesTo(serverPlayer)` on `PLAYER_JOIN`.
- **Render Layer Check**:
  - `PlayerRaceLayer.java:39-40` evaluates `boolean isWereTransformed = ClientWereState.isTransformed(player.getUUID()) || WereRaceTransformHandler.isTransformed(player.getUUID())`.
  - Lines 42-67 render beast features and scale if `isWereTransformed == true`; lines 68-71 fall back to preset human model parts if `isWereTransformed == false`.
- **Missing Event & Sync Gaps**:
  - `PLAYER_START_TRACKING` is not registered anywhere in the project (`grep_search` returned 0 occurrences of entity tracking listeners).
  - `SYNC_WERE_STATE_ID` receiver (`ModPackets.java:41-47`) queues `ClientWereState.setTransformed` but does NOT call `targetPlayer.refreshDimensions()` or `PehkuiIntegration.applyRaceScales` on the client.

## 2. Logic Chain
1. *From Observation 1 & 3*: On multiplayer client rendering, `PlayerRaceLayer` inspects `ClientWereState.isTransformed(player.getUUID())` to determine whether to render Were-form beast features or normal human model parts.
2. *From Observation 2 & 4*: When Player A transforms into Were-form, `ModPackets.syncWereStateToAll` sends `SYNC_WERE_STATE_ID` to currently online players. However, if Player B enters tracking range of Player A later (or teleports/changes dimension near Player A), Minecraft's server entity tracking system starts tracking Player A for Player B.
3. *From Observation 4*: Because `PlayerEvent.PLAYER_START_TRACKING` is not registered, the server never sends Player A's transformation state to Player B upon start tracking.
4. *From Observation 1 & 3*: Consequently, Player B's client map `ClientWereState.TRANSFORMED_PLAYERS` does not contain Player A's transformed state. `ClientWereState.isTransformed(Player A)` returns `false` on Player B's client.
5. *From Observation 3*: `PlayerRaceLayer` on Player B's client evaluates `isWereTransformed` to `false`, causing Player A to retain their default player model on Player B's screen.
6. *From Observation 4*: In addition, when `SYNC_WERE_STATE_ID` is received by any client, the absence of `player.refreshDimensions()` prevents the client-side hitbox and Pehkui model scaling from updating immediately on the client main thread.

## 3. Caveats
- **Multi-Platform Architectury Behavior**: `PlayerEvent.PLAYER_START_TRACKING` is a standard Architectury common event supported on both Fabric and Forge targets. Testing on dedicated server environments with multiple client connections is required to verify network timing.
- **Pehkui Soft Dependency**: Pehkui scale updates on the client depend on `Platform.isModLoaded("pehkui")`. Fallback vanilla scaling and `player.refreshDimensions()` execute independently of Pehkui.

## 4. Conclusion
The issue where tracking clients do not render transformed Were-race players is caused by the lack of an entity-tracking listener (`PlayerEvent.PLAYER_START_TRACKING`) and missing client-side dimension refreshing in the S2C packet handler. Registering `PLAYER_START_TRACKING` in `WereRaceTransformHandler.java` and updating `ModPackets.java` with client dimension refreshing (`player.refreshDimensions()`) fully resolves the tracking synchronization defect.

## 5. Verification Method
1. **Compilation Check**:
   Run `./gradlew build -x test` from project root to ensure clean compilation across Fabric and Forge modules.
2. **File Inspection**:
   - Inspect `common/src/main/java/ddraig/net/customraces/network/ModPackets.java` for `sendWereStateToPlayer` helper method and client `refreshDimensions()` call in `SYNC_WERE_STATE_ID` handler.
   - Inspect `common/src/main/java/ddraig/net/customraces/event/WereRaceTransformHandler.java` for `PlayerEvent.PLAYER_START_TRACKING` registration.
   - Inspect `common/src/main/java/ddraig/net/customraces/event/FirstJoinHandler.java` for `PLAYER_RESPAWN` transformation state re-sync.
3. **Invalidation Conditions**:
   If a newly joined player or a player entering tracking range still renders a transformed player as human, `PLAYER_START_TRACKING` packet delivery or client-side `ClientWereState` update failed.
