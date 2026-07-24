# Detailed Technical Analysis: Were-Race Model Rendering, Fallback Logic & Pehkui Scale Refresh

## Executive Summary
This report provides a comprehensive architectural investigation into the model rendering pipeline, transformation state checks, fallback model logic, and Pehkui dimension refresh triggers within the Custom Races Framework.

Key Findings:
1. **Default Player Model Retention**: Transformed players retain default player models because `PlayerRaceLayer` is registered merely as an additive `RenderLayer` on `PlayerRenderer` without hiding/canceling standard `PlayerModel` body parts (`getParentModel().head.visible = false`, etc.).
2. **Missing Custom / GeckoLib Model Renderer**: While `RaceData.java` defines Were-form model paths (`wereModelPath`, `wereTexturePath`, `wereAnimationPath`), no dedicated renderer (`WereModelRenderer` or `CustomRaceModelRenderer`) exists to load `.geo.json` models or swap player models during rendering.
3. **Unmapped Fallback Logic**: If `wereModelPath` is null, empty, or unmapped, rendering defaults to hardcoded cuboids (`renderWereBeastParts`) without validating resource locations or gracefully handling missing GeckoLib assets.
4. **Client-Side Bounding Box & Scale Desync**: On transformation toggle, `WereRaceTransformHandler` calls `PehkuiIntegration.applyRaceScales` on the server, but client packet receivers in `ModPackets.java` fail to trigger client-side scale updates or `player.refreshDimensions()`, leading to hitbox/camera desync on client entities.

---

## 1. Render Layer Architecture & Model Swapping Inspection

### Relevant Files & Line Numbers
- **`PlayerRaceLayer.java`**: `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java` (lines 22-256)
- **`CustomRacesFabric.java`**: `fabric/src/main/java/ddraig/net/customraces/fabric/CustomRacesFabric.java` (lines 21-25)
- **`CustomRacesForge.java`**: `forge/src/main/java/ddraig/net/customraces/forge/CustomRacesForge.java` (lines 24-31)

### Detailed Flow & State Inspection
1. **Registration**:
   - Fabric registers `PlayerRaceLayer` via `LivingEntityFeatureRendererRegistrationCallback` for `PlayerRenderer`.
   - Forge registers `PlayerRaceLayer` via `EntityRenderersEvent.AddLayers` for all player skin types (`default`, `slim`).
2. **Transformation Check in Render Loop**:
   - `PlayerRaceLayer.render(...)` (lines 28-94) checks transformation state:
     ```java
     boolean isWereTransformed = ddraig.net.customraces.client.ClientWereState.isTransformed(player.getUUID())
             || ddraig.net.customraces.event.WereRaceTransformHandler.isTransformed(player.getUUID());
     ```
   - If `isWereTransformed && race.enableWereRace`:
     - Applies scale transform to `PoseStack`: `poseStack.scale(wScale, hScale, wScale)` (line 46).
     - Renders procedural Were-form beast cuboids (ears, snout, crimson eye overlay) via `renderWereBeastParts(...)` (lines 49, 96-114).
     - Spawns client-side smoke and flame particles (lines 52-67).

### Current Limitations
- `PlayerRaceLayer` is executed *after* `PlayerRenderer` has already rendered the vanilla `PlayerModel` (Steve/Alex skin).
- There is no model swapping logic to suppress vanilla body parts when `isWereTransformed` is true.
- `WereModelRenderer` and `CustomRaceModelRenderer` classes mentioned in system design do not exist in the codebase.

---

## 2. Root Cause Analysis: Why Transformed Were-Race Players Retain Default Models

| Root Cause ID | Severity | Description & Code Context |
|---|---|---|
| **RC-01** | High | **No Vanilla Body Part Visibility Cancellation**: `PlayerRenderer` renders base player mesh before `PlayerRaceLayer` runs. `PlayerRaceLayer` does not hide `getParentModel().head`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`. Result: The human player skin renders beneath/inside the beast features. |
| **RC-02** | Critical | **GeckoLib Were Model Path Ignored**: `RaceData.wereModelPath`, `wereTexturePath`, `wereAnimationPath` (lines 94-103) are stored in data and GUI, but no renderer loads `.geo.json` geometry or plays GeckoLib animations for the player. |
| **RC-03** | Medium | **Client-Side Transformation State Desync**: `ClientWereState.isTransformed` relies on receiving `SYNC_WERE_STATE_ID` from `ModPackets.java`. If packet delivery is delayed or missed by tracking clients, `isWereTransformed` returns `false`, reverting tracking clients to default player rendering. |
| **RC-04** | Low | **Reversion State Leak**: Reverting from Were-form does not reset model part visibilities if they were modified, risking invisible or corrupt player models on return to human form. |

---

## 3. Fallback Logic Analysis & Graceful Asset Resolution Design

### Current State
- `PlayerRaceLayer.java` lines 44-45 defaults scales to `1.3f` if `wereHeightScale` or `wereWidthScale` is <= 0.
- If `wereModelPath` is empty or unmapped, `PlayerRaceLayer` renders hardcoded cuboids (`renderWereBeastParts`) on top of the player.

### Fallback Hierarchy & Graceful Default Design

```
                     [Were Transformation Active]
                                  │
                   Is race.wereModelPath valid?
                   (non-null, non-empty, valid RL)
                                 ╱ ╲
                               YES  NO
                               ╱     ╲
        Load GeckoLib Asset File     Fall back to Procedural Beast Overlay
        (customraces:models/were/...)  (renderWereBeastParts + player model scaling)
               │                                      │
        Does asset load succeed?                      │
             ╱ ╲                                      │
           YES  NO (file missing/corrupt)             │
           ╱     ╲                                    │
 Hide Base Player   Log warning & fallback ───────────┘
 Model & Render      to Procedural Beast Overlay
 GeckoLib Model
```

1. **Primary Model Resolution**:
   - Check if `race.wereModelPath != null && !race.wereModelPath.trim().isEmpty()`.
   - Verify `ResourceLocation.isValidResourceLocation(race.wereModelPath)`.
2. **GeckoLib Asset Fallback**:
   - If `wereModelPath` is missing on disk or fails parsing, log a client warning once (`[CustomRaces] Failed to load Were model asset: ...`).
   - Gracefully default to the built-in procedural beast overlay (`renderWereBeastParts`) with scaled player model.
3. **Scale Fallback Defaults**:
   - `wereHeightScale`: default to `1.3f` if `<= 0.0f`.
   - `wereWidthScale`: default to `1.3f` if `<= 0.0f`.
   - `wereTransformSound`: default to `minecraft:entity.wolf.howl` if empty.

---

## 4. Pehkui Scale Refresh & `player.refreshDimensions()` Analysis

### Relevant Files & Line Numbers
- **`PehkuiIntegration.java`**: `common/src/main/java/ddraig/net/customraces/integration/PehkuiIntegration.java` (lines 46-127, 129-168)
- **`WereRaceTransformHandler.java`**: `common/src/main/java/ddraig/net/customraces/event/WereRaceTransformHandler.java` (lines 160-184, 189-201)
- **`ModPackets.java`**: `common/src/main/java/ddraig/net/customraces/network/ModPackets.java` (lines 41-47)

### Scale Mechanics Analysis
1. **Scale Multipliers**:
   ```java
   boolean isTransformed = WereRaceTransformHandler.isTransformed(player.getUUID());
   float heightMult = isTransformed && race.enableWereRace ? race.wereHeightScale : race.heightScale;
   float widthMult = isTransformed && race.enableWereRace ? race.wereWidthScale : race.widthScale;
   float hScale = heightMult * race.baseScale;
   float wScale = widthMult * race.baseScale;
   ```
2. **Pehkui Scale Application**:
   - Applies BASE, HEIGHT, WIDTH, REACH, STEP_HEIGHT scale data via reflection.
   - At end of `applyRaceScales` (line 125) and `resetPlayerScales` (line 166), calls `player.refreshDimensions()`.

### Why `player.refreshDimensions()` is Critical
- `player.refreshDimensions()` recalculates `EntityDimensions` (width, height, eyeHeight, bounding box).
- Without calling `refreshDimensions()`:
  - Transformed player bounding box remains 1.0x while visual scale is 1.3x/1.5x.
  - Camera height remains at normal player eye level.
  - Hitbox desync occurs on both server collisions and client rendering bounds.

### Identified Gap: Client-Side Dimension Refresh Missing
- `WereRaceTransformHandler.transformIntoWereForm(...)` runs on `ServerPlayer` and invokes `PehkuiIntegration.applyRaceScales(player, race)`, which updates server-side dimensions.
- On client side, when `ModPackets.java` receives `SYNC_WERE_STATE_ID` (lines 41-46):
  ```java
  NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_WERE_STATE_ID, (buf, context) -> {
      UUID pUuid = buf.readUUID();
      boolean isTransformed = buf.readBoolean();
      context.queue(() -> {
          ClientWereState.setTransformed(pUuid, isTransformed);
          // MISSING: Client player scale update & refreshDimensions() call!
      });
  });
  ```
- **Fix Required**: Client receiver for `SYNC_WERE_STATE_ID` must find the client entity for `pUuid` (e.g. `Minecraft.getInstance().level.getPlayerByUUID(pUuid)`) and execute `PehkuiIntegration.applyRaceScales(player, race)` and `player.refreshDimensions()`.

---

## 5. Proposed Modifications for Milestone 2

### 1. `PlayerRaceLayer.java`
- Modify `render(...)` to handle vanilla model visibility suppression when `isWereTransformed` is active:
  - If `isWereTransformed && race.enableWereRace`:
    - Check if custom GeckoLib model exists or fallback to procedural beast overlay.
    - Set `getParentModel().head.visible = false`, `body.visible = false`, `rightArm.visible = false`, `leftArm.visible = false`, `rightLeg.visible = false`, `leftLeg.visible = false` when custom model is rendered.
  - In `finally` block or when `!isWereTransformed`, restore visibility of all parent model parts to `true`.

### 2. Implementation of `WereModelRenderer.java` / GeckoLib Integration
- Create `WereModelRenderer` in `ddraig.net.customraces.client.render`:
  - Handle loading and caching of GeckoLib model files (`wereModelPath`), textures (`wereTexturePath`), and animation state controllers (`wereIdleAnim`, `wereWalkAnim`, `wereAttackAnim`, etc.).
  - Fall back gracefully to `PlayerRaceLayer.renderWereBeastParts(...)` if GeckoLib model path is empty or invalid.

### 3. `ModPackets.java` Client Receiver Fix
- Update `SYNC_WERE_STATE_ID` client handler:
  ```java
  NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_WERE_STATE_ID, (buf, context) -> {
      UUID pUuid = buf.readUUID();
      boolean isTransformed = buf.readBoolean();
      context.queue(() -> {
          ClientWereState.setTransformed(pUuid, isTransformed);
          if (Minecraft.getInstance().level != null) {
              Player target = Minecraft.getInstance().level.getPlayerByUUID(pUuid);
              if (target != null) {
                  RaceData race = RaceRegistry.getPlayerRace(pUuid);
                  PehkuiIntegration.applyRaceScales(target, race);
                  target.refreshDimensions();
              }
          }
      });
  });
  ```

---

## Verification Plan
1. Compile using `./gradlew build -x test`.
2. Inspect player model rendering during transformation toggle to ensure vanilla player mesh is hidden when custom model is active and restored on reversion.
3. Test empty `wereModelPath` fallback to ensure procedural beast features render without crash.
4. Verify Pehkui scale changes and `refreshDimensions()` update hitboxes and camera height on both server and client.
