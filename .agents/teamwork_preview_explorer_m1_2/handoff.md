# Handoff Report: Were-Race Model Rendering & Pehkui Scale Refresh Exploration

## 1. Observation
- **`PlayerRaceLayer.java` (`common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`)**:
  - `render(...)` (lines 28-94): Extends `RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>`. Checks `ClientWereState.isTransformed(player.getUUID()) || WereRaceTransformHandler.isTransformed(player.getUUID())` (lines 39-40).
  - Lines 44-46: Applies `poseStack.scale(wScale, hScale, wScale)` for the render layer.
  - Lines 49, 96-114: `renderWereBeastParts(...)` renders hardcoded ears, snout, and crimson eye cuboids.
  - Does NOT hide base player model parts (`getParentModel().head.visible`, `body.visible`, etc.).
  - Does NOT instantiate or render GeckoLib `.geo.json` models specified in `RaceData.wereModelPath`.
- **`PehkuiIntegration.java` (`common/src/main/java/ddraig/net/customraces/integration/PehkuiIntegration.java`)**:
  - Lines 53-55: Calculates transformed height/width scale multipliers (`wereHeightScale`, `wereWidthScale`).
  - Lines 62-123: Sets Pehkui BASE, HEIGHT, WIDTH, REACH, STEP_HEIGHT scales via reflection.
  - Line 125 & Line 166: Calls `player.refreshDimensions()`.
- **`WereRaceTransformHandler.java` (`common/src/main/java/ddraig/net/customraces/event/WereRaceTransformHandler.java`)**:
  - Lines 141-187 (`transformIntoWereForm`) & Lines 189-204 (`revertWereForm`): Invokes `PehkuiIntegration.applyRaceScales(player, race)` on server side.
- **`ModPackets.java` (`common/src/main/java/ddraig/net/customraces/network/ModPackets.java`)**:
  - Lines 41-47: Receives `SYNC_WERE_STATE_ID` on client and updates `ClientWereState.setTransformed(pUuid, isTransformed)`, but does NOT call `PehkuiIntegration.applyRaceScales` or `target.refreshDimensions()` on client player entity.

## 2. Logic Chain
1. **Observation**: `PlayerRaceLayer` is registered as a feature renderer layer attached to `PlayerRenderer`. `PlayerRenderer` executes its standard `render` method (drawing vanilla Steve/Alex player mesh) before running attached layers.
   **Inference**: `PlayerRaceLayer` currently draws procedural beast features on top of the already-rendered vanilla player model. Without hiding `getParentModel()` body parts, transformed players retain their default player model underneath.
2. **Observation**: `RaceData` contains `wereModelPath`, `wereTexturePath`, `wereAnimationPath`, `wereIdleAnim`, but no renderer class (`WereModelRenderer` / `CustomRaceModelRenderer`) exists to parse or render `.geo.json` GeckoLib models for players.
   **Inference**: A custom model renderer layer must be implemented in M2 to handle GeckoLib models and replace the default player model mesh when a custom Were model is defined.
3. **Observation**: `WereRaceTransformHandler` calls `PehkuiIntegration.applyRaceScales` (which calls `player.refreshDimensions()`) on `ServerPlayer` during transformation, but `ModPackets.SYNC_WERE_STATE_ID` client packet receiver only sets `ClientWereState.setTransformed` without refreshing client entity scales or dimensions.
   **Inference**: Client-side bounding boxes, camera eye heights, and rendering hitboxes lag or desync until client scales are explicitly updated and `player.refreshDimensions()` is called on client player entities.

## 3. Caveats
- No live runtime Minecraft client environment was launched during this read-only static analysis phase (all findings are based on static code inspection across common, fabric, and forge modules).
- GeckoLib API version compatibility assumes standard GeckoLib 4.x `GeoModel` / `GeoEntityRenderer` patterns used in 1.20.1.

## 4. Conclusion
Transformed Were-race players retain default player models because `PlayerRaceLayer` is purely additive and never suppresses vanilla player mesh rendering. Furthermore, no GeckoLib model renderer is hooked into the player entity render pipeline. To fix this in M2:
1. Implement model visibility toggles (`getParentModel().head.visible = false`, etc.) during transformed rendering.
2. Implement custom GeckoLib model renderer integration with fallback to procedural beast features when `wereModelPath` is empty/unmapped.
3. Update `ModPackets.SYNC_WERE_STATE_ID` client receiver to trigger `PehkuiIntegration.applyRaceScales` and `player.refreshDimensions()` on client player entities.

## 5. Verification Method
- **Build Verification**: Run `./gradlew build -x test` from root directory to verify zero compilation errors.
- **Code Inspection Verification**:
  - Check `PlayerRaceLayer.java` lines 39-46 to verify transformation checks and model suppression.
  - Check `ModPackets.java` lines 41-47 to verify client packet handler triggers client scale updates.
  - Check `PehkuiIntegration.java` lines 46-127 to verify `player.refreshDimensions()` invocation.
