# Handoff Report: Base Human Player Model Suppression Guardrails & Fallback Mechanisms (R2)

**Agent**: Explorer 2 (M1)  
**Working Directory**: `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_2`  
**Date**: 2026-07-28  
**Handoff Type**: Hard Handoff (Task Complete)  

---

## 1. Observation

### 1.1 Base Player Mesh Suppression Mixin
- **File**: `common/src/main/java/ddraig/net/customraces/mixin/LivingEntityRendererMixin.java` (Lines 21–37)
- **Code**:
  ```java
  @Inject(method = "render", at = @At("HEAD"))
  private void onRenderLivingHead(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
      if (entity instanceof AbstractClientPlayer player) {
          RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
          LivingEntityRenderer<T, M> renderer = (LivingEntityRenderer<T, M>) (Object) this;
          M model = renderer.getModel();

          if (model instanceof PlayerModel<?> playerModel) {
              if (WereModelRenderer.isWereForm(player, race) && WereModelRenderer.isModelAvailable(race)) {
                  // Suppress base human player model ONLY if custom model is available and baked
                  WereModelRenderer.setBaseModelVisible(playerModel, false);
              } else {
                  WereModelRenderer.setBaseModelVisible(playerModel, true);
              }
          }
      }
  }
  ```

### 1.2 Model Part Visibility Helper
- **File**: `common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java` (Lines 234–248)
- **Code**:
  ```java
  public static void setBaseModelVisible(PlayerModel<?> model, boolean visible) {
      if (model == null) return;
      model.head.visible = visible;
      model.hat.visible = visible;
      model.body.visible = visible;
      model.rightArm.visible = visible;
      model.leftArm.visible = visible;
      model.rightLeg.visible = visible;
      model.leftLeg.visible = visible;
      model.jacket.visible = visible;
      model.rightSleeve.visible = visible;
      model.leftSleeve.visible = visible;
      model.rightPants.visible = visible;
      model.leftPants.visible = visible;
  }
  ```

### 1.3 Layer Fallback Execution & Procedural Overlay
- **File**: `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java` (Lines 44–54, 111–131)
- **Code**:
  ```java
  if (isWereTransformed) {
      float hScale = race.wereHeightScale > 0 ? race.wereHeightScale : 1.3f;
      float wScale = race.wereWidthScale > 0 ? race.wereWidthScale : 1.3f;
      poseStack.scale(wScale, hScale, wScale);

      boolean customRendered = WereModelRenderer.renderWereForm(poseStack, buffer, packedLight, player, this.getParentModel(), race, netHeadYaw, headPitch);
      if (!customRendered) {
          renderWereBeastParts(poseStack, buffer, packedLight, player, race, netHeadYaw, headPitch);
      }
      ...
  }
  ```

### 1.4 Pehkui Integration Scaling Logic
- **File**: `common/src/main/java/ddraig/net/customraces/integration/PehkuiIntegration.java` (Lines 68–130)
- **Observation**: Pehkui scale data sets `BASE`, `HEIGHT`, and `WIDTH` scales directly on Pehkui's API when installed.

---

## 2. Logic Chain

1. **Suppression Mechanism**: `LivingEntityRendererMixin` intercepts player rendering at `LivingEntityRenderer.render()` HEAD. If a player is in transformed Were-form AND `WereModelRenderer.isModelAvailable(race)` is `true`, it calls `setBaseModelVisible(playerModel, false)`.
2. **Execution Effect**: `PlayerModel.renderToBuffer()` checks `part.visible` for each of its 12 cuboid parts before drawing. Setting `.visible = false` prevents the base Steve/Alex mesh from rendering without altering matrix transforms or interfering with downstream render layers.
3. **Vanilla Armor Suppression**: `HumanoidArmorLayer` checks `parentModel.head.visible`, `parentModel.body.visible`, etc., before rendering armor pieces. Hiding `parentModel` parts cleanly suppresses human armor meshes from rendering over custom GeckoLib entity models, eliminating mesh clipping.
4. **Guaranteed Fallback ("Never Invisible")**: If a custom GeckoLib model fails to parse, bake, or render, `WereModelRenderer.renderWereForm()` returns `false` and executes `setBaseModelVisible(parentModel, true)`. `PlayerRaceLayer` then calls `renderWereBeastParts()`, rendering procedural wolf ears/snout/glowing eyes ON TOP of the base human player model. Player is ALWAYS visible under all failure modes.
5. **Deficiency 1 (Missing Overlay Part Suppression)**: `setBaseModelVisible()` does not hide `model.cloak` (Cape) or `model.ear` (Deadmau5 ears). A player wearing a cape would have a floating cape mesh attached to an invisible body.
6. **Deficiency 2 (Pehkui Double-Scaling)**: `PlayerRaceLayer` calls `poseStack.scale(wScale, hScale, wScale)` unconditionally. When Pehkui is loaded, Pehkui ALREADY scales the entity `PoseStack`. This causes double-scaling ($1.3 \times 1.3 = 1.69\times$).
7. **Deficiency 3 (Invisibility Effect Handling)**: `PlayerRaceLayer` and `GeckoLibWereRenderer` do not check `player.isInvisible()`, causing invisible/spectator players to render as fully opaque 3D models.

---

## 3. Caveats

- **1st-Person Hand Rendering**: In 1st-person view, vanilla `PlayerRenderer.renderRightHand()` explicitly sets `model.rightArm.visible = true` locally before drawing. While this ensures the player's hand is not invisible, it renders the human skin hand instead of a GeckoLib claw. Implementing custom 1st-person GeckoLib hand rendering is deferred to M3/M4.
- **Custom Modded Armor Layers**: Non-standard modded armor layers that do not check `parentModel` part visibility may still attempt to render armor over GeckoLib models.

---

## 4. Conclusion

The existing suppression guardrails and fallback mechanisms in `LivingEntityRendererMixin.java`, `WereModelRenderer.java`, and `PlayerRaceLayer.java` provide a robust foundation that successfully hides default human player mesh parts when a valid GeckoLib model is active and safely falls back to base human model + procedural beast features on any asset failure.

### Concrete Recommendations for Implementation (M2/M3):
1. **Extend `setBaseModelVisible()`**: Add `model.cloak.visible = visible` and `model.ear.visible = visible` in `WereModelRenderer.java`.
2. **Fix Pehkui Double Scaling**: Guard `poseStack.scale()` in `PlayerRaceLayer.java` with `if (!PehkuiIntegration.isPehkuiLoaded())`.
3. **Invisibility Effect Support**: Check `player.isInvisible()` in `GeckoLibWereRenderer.java` and use `RenderType.entityTranslucent()` or skip rendering when invisible.

---

## 5. Verification Method

### How to Independently Verify:
1. **Multi-Platform Build Verification**:
   Run `./gradlew build -x test` from workspace root to verify compilation across Fabric and Forge modules.
2. **Suppression Code Inspection**:
   Inspect `common/src/main/java/ddraig/net/customraces/mixin/LivingEntityRendererMixin.java` lines 21-37 and `common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java` lines 234-248 to verify `setBaseModelVisible` parameters.
3. **Fallback Guardrail Verification**:
   Set `wereModelPath` to `"none"` or an invalid location in `common/config/custom_races/player_races.json`. Verify in-game that player renders base human model with procedural beast features (`renderWereBeastParts`).
4. **Invalidation Conditions**:
   If a change to `LivingEntityRendererMixin` removes `isModelAvailable(race)` checks, players with missing GeckoLib models will render completely invisible (invalidation condition).
