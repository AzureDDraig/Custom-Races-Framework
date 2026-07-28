# R2 Focus Analysis: Base Human Player Model Suppression Guardrails & Fallback Mechanisms

**Explorer 2 (M1)** — Custom Race GeckoLib Player Model Overhaul  
**Target Workspace**: `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework`  
**Date**: 2026-07-28  

---

## 1. Executive Summary & Core Findings

This analysis investigates **R2: Base Human Player Model Suppression Guardrails & Fallback Mechanisms** for the Custom Races Framework. The goal is to ensure that when a player transforms into a race with an active GeckoLib custom 3D model, the standard human player cuboid mesh is completely suppressed without clipping or skin bleeding, while guaranteeing through fail-safe guardrails that players **NEVER become invisible** if model assets fail to load, are invalid, or are missing.

### Key Discoveries:
1. **Suppression Execution Point**: Base human player mesh parts are suppressed via `LivingEntityRendererMixin.java` injecting at `@At("HEAD")` of `LivingEntityRenderer.render()`. It invokes `WereModelRenderer.setBaseModelVisible(playerModel, false)`, which sets `.visible = false` on 12 default `PlayerModel` cuboid fields (`head`, `hat`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`, `jacket`, `rightSleeve`, `leftSleeve`, `rightPants`, `leftPants`).
2. **Fallback Safety Guardrails**: In `WereModelRenderer.renderWereForm()` and `LivingEntityRendererMixin`, suppression is **strictly conditional** on `isWereForm(player, race) && isModelAvailable(race)`. If a GeckoLib model fails to parse, bake, or render, `renderWereForm()` immediately catches the failure, calls `setBaseModelVisible(parentModel, true)`, and falls back to procedural beast features (`renderWereBeastParts()`) on top of the default human player mesh.
3. **Identified Deficiencies & Edge Cases**:
   - **Missing Overlay Part Suppression**: `setBaseModelVisible()` currently omits `PlayerModel.cloak` (Cape) and `PlayerModel.ear` (Deadmau5 ears). If a player wearing a cape transforms, the cape mesh continues to render suspended in mid-air.
   - **Pehkui Double-Scaling Defect**: `PehkuiIntegration.java` sets `BASE`, `HEIGHT`, and `WIDTH` scales on Pehkui's entity `ScaleData`. However, `PlayerRaceLayer.java` independently applies `poseStack.scale(wScale, hScale, wScale)` during transformation. When Pehkui is active, scaling occurs twice ($1.3 \times 1.3 = 1.69\times$), resulting in oversized models.
   - **Invisibility & Spectator Handling**: `PlayerRaceLayer` currently lacks checks for `player.isInvisible()` or `player.isInvisibleTo(...)`. Active GeckoLib custom models render as fully opaque 3D models even when the player is in spectator mode or has an active Invisibility effect.
   - **1st-Person Hand Rendering**: In 1st-person view, vanilla `PlayerRenderer.renderRightHand()` / `renderLeftHand()` forcefully sets `rightArm.visible = true` locally before drawing. While this prevents complete arm disappearance in 1st person, it draws the human skin arm instead of a transformed GeckoLib arm.

---

## 2. Architecture of Player Model Rendering Pipeline

In Minecraft 1.20.1 (Fabric and Forge):

```
LivingEntityRenderer.render(AbstractClientPlayer)
  │
  ├──► [MIXIN] LivingEntityRendererMixin.onRenderLivingHead (@At("HEAD"))
  │      ├─► Checks WereModelRenderer.isWereForm(player, race)
  │      └─► Checks WereModelRenderer.isModelAvailable(race)
  │             ├── TRUE  => WereModelRenderer.setBaseModelVisible(playerModel, false)
  │             └── FALSE => WereModelRenderer.setBaseModelVisible(playerModel, true)
  │
  ├──► PlayerModel.setupAnim(...)  [Calculates limb rotations / head angles]
  │
  ├──► PlayerModel.renderToBuffer(...)  [Renders primary player cuboid mesh]
  │      └─► Skips cuboid parts where part.visible == false
  │
  └──► RenderLayer Iteration:
         ├── HumanoidArmorLayer  [Skips armor pieces where parentModel part.visible == false]
         ├── ItemInHandLayer     [Renders mainhand / offhand items attached to arm pose]
         ├── CustomHeadLayer     [Renders skull / pumpkin if worn]
         ├── ElytraLayer         [Renders elytra wings]
         └── PlayerRaceLayer.render(...)
                ├── isWereTransformed == true:
                │      ├── WereModelRenderer.renderWereForm(...)
                │      │      ├── GeckoLibWereRenderer.renderGeckoModel(...)
                │      │      └── [FAIL] => setBaseModelVisible(true) -> renderWereBeastParts(...)
                │      └── Real-time Were Smoke Particles
                └── isWereTransformed == false:
                       ├── setBaseModelVisible(true)
                       └── renderPresetParts(...) [Ears, Wings, Tail, Horns, Halo, Legs]
```

---

## 3. Detailed Base Player Mesh Suppression Analysis

### Current Implementation (`LivingEntityRendererMixin.java` & `WereModelRenderer.java`)

```java
// LivingEntityRendererMixin.java (Lines 21-37)
@Inject(method = "render", at = @At("HEAD"))
private void onRenderLivingHead(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
    if (entity instanceof AbstractClientPlayer player) {
        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        LivingEntityRenderer<T, M> renderer = (LivingEntityRenderer<T, M>) (Object) this;
        M model = renderer.getModel();

        if (model instanceof PlayerModel<?> playerModel) {
            if (WereModelRenderer.isWereForm(player, race) && WereModelRenderer.isModelAvailable(race)) {
                WereModelRenderer.setBaseModelVisible(playerModel, false);
            } else {
                WereModelRenderer.setBaseModelVisible(playerModel, true);
            }
        }
    }
}
```

```java
// WereModelRenderer.java (Lines 234-248)
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

### Suppression Mechanics & Strengths:
1. **Zero Matrix Stack Contamination**: Setting `.visible = false` on `ModelPart` instances natively bypasses vertex generation in Minecraft's `ModelPart.compile()` without mutating matrix transforms or corrupting downstream rendering layers.
2. **Synchronized Layer Hiding**: Vanilla `HumanoidArmorLayer` checks `parentModel.head.visible`, `parentModel.body.visible`, etc., before rendering armor cuboids. Consequently, setting `setBaseModelVisible(false)` automatically suppresses vanilla helmet, chestplate, leggings, and boots rendering, eliminating clipping between Steve/Alex human armor meshes and custom 3D GeckoLib entity models.

### Recommended Fix for Missing Part Suppression:
`PlayerModel` contains two additional overlay fields: `cloak` and `ear`. `setBaseModelVisible()` must be updated as follows:

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
    model.cloak.visible = visible; // Prevent floating cape mesh
    model.ear.visible = visible;   // Prevent floating Deadmau5 ears
}
```

---

## 4. Fail-Safe Guardrails & Fallback Matrix ("Never Invisible" Verification)

To guarantee players are **NEVER INVISIBLE** under any race configuration or asset state, we audited 5 primary failure scenarios:

| Failure Scenario | Trigger Condition | System Behavior & Fallback Mechanism | Visibility Outcome |
| :--- | :--- | :--- | :--- |
| **1. Unassigned Model** | `wereModelPath` is `null`, `""`, or `"none"` | `hasCustomModel(race)` returns `false`. `isModelAvailable(race)` returns `false`. `setBaseModelVisible(true)` is executed. `PlayerRaceLayer` renders `renderWereBeastParts()` (procedural ears/snout/glowing eyes). | **VISIBLE** (Base Human + Procedural Beast Overlay) |
| **2. Invalid / Corrupted Model File** | `wereModelPath` points to non-existent file or corrupted `.geo.json` | `GeckoLibWereRenderer.isModelPresent()` returns `false` or `bakeModelFromFile()` catches `Throwable` and logs warning. `LivingEntityRendererMixin` keeps base model visible (`setBaseModelVisible(true)`). | **VISIBLE** (Base Human + Procedural Beast Overlay) |
| **3. Render-Time Exception in GeckoLib** | Exception during reflection bone rendering (`renderGeckoModel`) | `WereModelRenderer.renderWereForm()` catches `false` return from renderer, immediately invokes `setBaseModelVisible(parentModel, true)`, and falls back to `renderWereBeastParts()`. | **VISIBLE** (Base Human + Procedural Beast Overlay) |
| **4. Missing Texture Asset** | `wereTexturePath` file missing or syntax invalid | `WereModelRenderer.getValidWereTextureLocation()` executes a 5-step fallback ladder: Custom Disk Path $\rightarrow$ Mod Resource Path $\rightarrow$ Dynamic Cache $\rightarrow$ `DEFAULT_WERE_TEXTURE` $\rightarrow$ `player.getSkinTextureLocation()`. | **VISIBLE** (Clean Texture, No Purple/Black Checkerboard) |
| **5. GeckoLib Mod Missing / Reflection Failure** | GeckoLib library absent from runtime environment | Reflection calls catch `ClassNotFoundException` / `NoClassDefFoundError` safely in `GeckoLibWereRenderer`. System gracefully degrades to base model + procedural features. | **VISIBLE** (Base Human + Procedural Beast Overlay) |

---

## 5. Edge Case Analysis & Technical Risks

### 1. Invisibility Effects & Spectator Mode
- **Current Behavior**: `PlayerRaceLayer.java` and `GeckoLibWereRenderer.java` render GeckoLib models using `RenderType.entityCutoutNoCull(textureLoc)` regardless of player visibility state.
- **Risk**: Invisible players or players in spectator mode render as solid, opaque 3D beasts.
- **Recommendation**: In `PlayerRaceLayer.render()`, check `player.isInvisible()` and `player.isSpectator()`. If `player.isInvisible()`, use `RenderType.entityTranslucent(textureLoc)` or skip rendering when completely hidden to non-team members (`player.isInvisibleTo(mc.player)`).

### 2. Pehkui Double-Scaling Conflict
- **Current Behavior**:
  - `PehkuiIntegration.applyRaceScales()` sets Pehkui's `BASE`, `HEIGHT`, and `WIDTH` scale data on the player entity.
  - `PlayerRaceLayer.java` (Lines 46-48) ALSO calls `poseStack.scale(wScale, hScale, wScale)`.
- **Impact**: When Pehkui is installed, transformed players experience exponential scaling ($1.3 \times 1.3 = 1.69\times$).
- **Recommendation**: Guard `PlayerRaceLayer` scaling with `if (!PehkuiIntegration.isPehkuiLoaded())`:
  ```java
  if (isWereTransformed) {
      if (!PehkuiIntegration.isPehkuiLoaded()) {
          float hScale = race.wereHeightScale > 0 ? race.wereHeightScale : 1.3f;
          float wScale = race.wereWidthScale > 0 ? race.wereWidthScale : 1.3f;
          poseStack.scale(wScale, hScale, wScale);
      }
      ...
  }
  ```

### 3. First-Person Hand Rendering (`ItemInHandRenderer`)
- **Current Behavior**: In 1st-person view, Minecraft calls `PlayerRenderer.renderRightHand()`. Vanilla explicitly sets `model.rightArm.visible = true` right before rendering.
- **Impact**: 1st-person view shows the player's default human skin arm rather than a transformed beast claw/arm.
- **Recommendation for M3/M4**: In M3/M4, implement a hand rendering hook or mixin into `PlayerRenderer.renderRightHand` / `renderLeftHand` to render the transformed GeckoLib arm/claw bone in 1st person when transformed.

### 4. Held Items & Equipment Layer Alignment
- **Current Behavior**: Vanilla `ItemInHandLayer` renders items relative to `parentModel.rightArm` / `leftArm`.
- **Impact**: When `setBaseModelVisible(false)` is active, `rightArm` limb rotations are still updated by `PlayerModel.setupAnim()`, so held swords and items render in the correct world space relative to the player's pose.
- **Verification**: Held items remain visible and animated with player movement.

---

## 6. Implementation Recommendations Summary for M2 / M3

1. **Update `setBaseModelVisible`**: Add `model.cloak.visible = visible` and `model.ear.visible = visible` to `WereModelRenderer.java`.
2. **Fix Pehkui Scaling**: Wrap `poseStack.scale(wScale, hScale, wScale)` in `PlayerRaceLayer.java` with `!PehkuiIntegration.isPehkuiLoaded()`.
3. **Add Invisibility Awareness**: Check `player.isInvisible()` in `GeckoLibWereRenderer.java` and select translucent render types or respect invisibility rules.
4. **Maintain Fail-Safe Paradigm**: Preserve the strict fallback chain in `LivingEntityRendererMixin` and `WereModelRenderer.renderWereForm()` where any rendering error restores base model visibility and applies procedural beast parts.
