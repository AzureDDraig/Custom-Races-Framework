# Comprehensive Architecture Analysis: GeckoLib Player Model Override & Asset Resolution (R1)

**Author:** Explorer 1 (M1)  
**Date:** 2026-07-28  
**Focus Area:** R1 - GeckoLib Player Model Override & Asset Resolution  
**Target Milestone:** M1 (Exploration & Architecture Analysis) -> M2 Implementation  

---

## 1. Executive Summary

This document provides a deep, read-only architectural analysis of the GeckoLib player model override system, dual-path asset resolution (disk config vs mod resource pack), model alignment/positioning/scaling, and texture/model binding mechanics in the Custom Races Framework.

Key findings indicate that while reflection-based GeckoLib model rendering (`GeckoLibWereRenderer.java`) and basic model suppression (`WereModelRenderer.java`, `LivingEntityRendererMixin.java`) are functional in simple cases, there are critical gaps and architectural bugs:
1. **Asset Resolution Deficiencies**: Path normalization is inconsistent across models, textures, and animations. `wereModelPath` and `wereAnimationPath` lack namespace defaults, subfolder prefixing (`geo/`, `animations/`), and extension fallbacks when resolving via `ResourceManager` or disk config.
2. **Missing Head Rotation & Camera Pitch Alignment**: `GeckoLibWereRenderer` ignores `netHeadYaw` and `headPitch` parameters during bone rendering. Transformed GeckoLib models remain completely rigid when players look up/down or turn their heads.
3. **Double Scaling Bug with Pehkui Integration**: Both `PehkuiIntegration.applyRaceScales` and `PlayerRaceLayer.render` apply height/width scaling independently, resulting in quadratic scale multiplication (`hScale^2`, `wScale^2`) when Pehkui is active.
4. **Player Invisibility Edge Case**: If model baking succeeds in `isModelAvailable` but vertex rendering or top-level bone retrieval fails inside `renderGeckoModel`, `LivingEntityRendererMixin` has already suppressed the base player mesh, causing the player entity to become completely invisible.
5. **Missing Animation Engine**: Keyframe animation files (`.animation.json`) are parsed into memory, but there is no animation controller or keyframe tick updater to animate bones over time for idle, walking, attacking, or hurt states.

---

## 2. Current Codebase Render Pipeline & Architecture

### 2.1 Class Structure & Interaction Chain
The player rendering pipeline for transformed races relies on the following component chain:

```
[LivingEntityRenderer.render()]
      │
      ├──> [LivingEntityRendererMixin @ HEAD]
      │        Checks WereModelRenderer.isWereForm() && isModelAvailable()
      │        If true: calls setBaseModelVisible(playerModel, false) to hide human cuboids.
      │
      ├──> [PlayerModel.renderToBuffer()] (Suppressed if setBaseModelVisible(false))
      │
      └──> [PlayerRaceLayer.render()] (RenderLayer attached to PlayerRenderer)
               │
               ├──> Checks isWereTransformed
               ├──> Applies PoseStack scale (wScale, hScale, wScale)
               └──> Calls WereModelRenderer.renderWereForm()
                        │
                        └──> Calls GeckoLibWereRenderer.renderGeckoModel()
                                 │
                                 ├──> Resolves/bakes bakedModel via reflection
                                 ├──> Calls topLevelBones()
                                 └──> Recursively calls renderBoneReflect() -> renderCubeReflect()
```

### 2.2 Detailed Class Analysis

1. **`WereModelRenderer.java`** (`common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java`):
   - Defines default fallback resource locations:
     - `DEFAULT_WERE_MODEL = customraces:models/were/default_werewolf.geo.json`
     - `DEFAULT_WERE_TEXTURE = customraces:textures/were/default_werewolf.png`
     - `DEFAULT_WERE_ANIMATION = customraces:animations/were/default_werewolf.animation.json`
   - Handles `isWereForm(player, race)` checks combining `race.enableWereRace` and `ClientWereState` / `WereRaceTransformHandler` transformed state.
   - Provides `setBaseModelVisible(model, visible)` to toggle visibility of standard player cuboids (`head`, `hat`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`, `jacket`, `rightSleeve`, `leftSleeve`, `rightPants`, `leftPants`).
   - Contains `getValidWereTextureLocation(player, race)` with dynamic texture loading from `config/custom_races/textures/`.

2. **`GeckoLibWereRenderer.java`** (`common/src/main/java/ddraig/net/customraces/client/render/GeckoLibWereRenderer.java`):
   - Uses reflection to interface with GeckoLib 4 runtime classes (`software.bernie.geckolib.cache.GeckoLibCache`, `software.bernie.geckolib.util.JsonUtil`, `software.bernie.geckolib.loading.object.BakedModelFactory`, etc.).
   - Parses `.geo.json` model files from either disk paths (`config/custom_races/models/`) or Minecraft `ResourceManager` into `BakedModel` objects and registers them in `GeckoLibCache`.
   - Iterates through `topLevelBones()` and renders bone hierarchies using `renderBoneReflect()` and `renderCubeReflect()`.

3. **`PlayerRaceLayer.java`** (`common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`):
   - Extends `RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>`.
   - Intercepts player rendering to scale `PoseStack` by `wereHeightScale` and `wereWidthScale`, invokes `WereModelRenderer.renderWereForm`, spawns real-time ambient smoke particles, and handles fallback procedural beast parts if custom model rendering returns `false`.

4. **`LivingEntityRendererMixin.java`** (`common/src/main/java/ddraig/net/customraces/mixin/LivingEntityRendererMixin.java`):
   - Injects at `@At("HEAD")` of `LivingEntityRenderer.render()`.
   - Pre-emptively suppresses base human player model mesh parts when `isWereForm` and `isModelAvailable` are `true`.

---

## 3. Dual-Path Asset Resolution Analysis (Disk Config vs Mod Resource Pack)

### 3.1 Resolution Mechanism Breakdown

Custom Race models, textures, and animations can be stored in two distinct locations:
- **Disk Config Directory**: `config/custom_races/models/`, `config/custom_races/textures/`, `config/custom_races/animations/`
- **Mod Resource Pack Directory**: `assets/customraces/geo/`, `assets/customraces/textures/`, `assets/customraces/animations/` (or `assets/customraces/models/were/`)

#### Current Asset Resolution Paths:

| Asset Type | Config Input Example | Disk Lookup Path | Resource Pack Lookup Path | Bugs / Gaps Identified |
|------------|----------------------|------------------|---------------------------|------------------------|
| **Texture** | `werewolf.png` or `were/werewolf` | `config/custom_races/textures/<cleanPath>` | `assets/customraces/textures/<path>.png` | Clean path stripping loses subfolder structure; keywords (`skin`, `player`) supported. |
| **Model** | `werewolf.geo.json` or `customraces:werewolf` | `config/custom_races/models/<cleanPath>` | `assets/minecraft/werewolf.geo.json` (if no namespace) | **CRITICAL**: Missing namespace fallback; no `geo/` subfolder prefix normalization; `ResourceLocation.tryParse` defaults to `minecraft:` namespace if un-prefixed. |
| **Animation** | `werewolf.animation.json` | `config/custom_races/animations/<cleanPath>` | `assets/minecraft/werewolf.animation.json` | **CRITICAL**: Missing namespace fallback; no `.animation.json` extension normalization; no keyframe playback engine. |

### 3.2 Key Asset Resolution Flaws

1. **Namespace & Path Prefix Asymmetry**:
   - `WereModelRenderer.getValidWereTextureLocation` performs namespace normalization (defaulting to `"customraces"`), prefix normalization (adding `"textures/"`), and suffix normalization (adding `".png"`).
   - In contrast, `getValidWereModelLocation` and `getValidWereAnimationLocation` directly call `ResourceLocation.tryParse(path)`. If a user inputs `"werewolf.geo.json"`, `tryParse` returns `ResourceLocation("minecraft", "werewolf.geo.json")`.
   - When Minecraft's `ResourceManager` attempts to load `minecraft:werewolf.geo.json`, it looks under `assets/minecraft/werewolf.geo.json` instead of `assets/customraces/geo/werewolf.geo.json`, causing resource pack loading to fail.

2. **Resource Pack Path Convention Differences**:
   - GeckoLib standard convention for model files in resource packs is `assets/<namespace>/geo/<filename>.geo.json`.
   - Existing code checks `models/were/` and `models/`, but does not attempt fallback candidate lookups (`geo/<filename>.geo.json`, `models/were/<filename>.geo.json`, `models/<filename>.geo.json`).

3. **Dynamic Disk Model Cache Invalidation**:
   - Dynamically baked disk models are stored in `GeckoLibCache.getBakedModels()`.
   - When Minecraft reloads resources (e.g. F3+T), `GeckoLibCache` is cleared by GeckoLib, purging disk-baked models.
   - Subsequent renders must re-read disk JSON files and re-bake models on the fly. `GeckoLibWereRenderer.bakeModelFromFile` handles re-baking, but lacks a dedicated file modification timestamp cache check.

---

## 4. Model Positioning, Scaling, Feet Alignment, and Yaw/Pitch Alignment

### 4.1 Matrix Stack & Entity Feet Alignment

In Minecraft entity rendering, `LivingEntityRenderer.render()` establishes the base matrix stack:
1. `poseStack.translate(x, y, z)` positions the render origin at the entity's feet (y = 0 at feet level).
2. `LivingEntityRenderer.setupRotations()` rotates `poseStack` by `180.0F - bodyYaw`.
3. `PlayerRaceLayer.render()` then receives `poseStack` aligned at `(0, 0, 0)` at feet level.

**Alignment Verification**:
- In `GeckoLibWereRenderer.renderBoneReflect()`, translation transforms are calculated as:
  `poseStack.translate((px + pivX) / 16.0f, (py + pivY) / 16.0f, (pz + pivZ) / 16.0f);`
- In Blockbench / GeckoLib `.geo.json` format, 1 unit = 1/16th of a block.
- For GeckoLib models designed with origin `(0, 0, 0)` at ground level, top-level bones render correctly aligned to player entity feet.
- **Caveat**: If a custom model's root bone pivot is placed at body center (e.g. y = 12 or y = 24), the model will render elevated above the player's feet unless an origin adjustment offset is applied.

### 4.2 Head Yaw & Pitch Alignment Deficiencies

**Current Flaw in `GeckoLibWereRenderer`**:
- `PlayerRaceLayer.render()` receives `netHeadYaw` (head yaw relative to body yaw) and `headPitch` (pitch angle looking up/down).
- However, `WereModelRenderer.renderWereForm` and `GeckoLibWereRenderer.renderGeckoModel` DO NOT accept or pass `netHeadYaw` and `headPitch`.
- As a result, when rendering GeckoLib bones in `renderBoneReflect()`, no rotational transformation is applied to head bones (`head`, `bipedHead`, `head_bone`).
- **Visual Impact**: Transformed player models remain completely rigid when looking around. The head does not track camera movement or pitch up/down.

### 4.3 Scale Integration & Pehkui Interaction

**Current Flaw**:
1. `PehkuiIntegration.applyRaceScales` applies `wereHeightScale` and `wereWidthScale` directly to Pehkui's entity scale attributes (`HEIGHT` and `WIDTH`) when transformed.
2. In `PlayerRaceLayer.render()`, the code ALSO executes:
   `poseStack.scale(wScale, hScale, wScale);`
3. When Pehkui is active, `LivingEntityRenderer` scales `poseStack` by Pehkui's entity scale *before* `PlayerRaceLayer.render()` is invoked. `PlayerRaceLayer` then scales `poseStack` a second time by `(wScale, hScale, wScale)`.
4. **Visual Impact**: Transformed players render at squared scale (`1.3 * 1.3 = 1.69x` height/width) when Pehkui is installed.

---

## 5. Missing Helper Classes, Resource Loaders, and Binding Bugs

### 5.1 Missing Components

1. **`GeckoAssetResolver.java`**:
   - A dedicated asset resolution helper is missing. It should encapsulate dual-path resolution for models, textures, and animations, managing path normalization, namespace defaulting, candidate location search (`geo/`, `models/were/`, `animations/`), and disk file existence validation.

2. **`GeckoAnimationController.java`**:
   - An animation state controller is missing. While `bakeAnimationsFromFile` parses animation keyframe JSONs, there is no keyframe tick evaluator to calculate time-varying bone rot/pos/scale transforms during player movement (idle, walk, attack, hurt).

### 5.2 Identified Binding & Rendering Bugs

1. **Player Invisibility Safety Flaw**:
   - `LivingEntityRendererMixin` suppresses base player cuboids before `PlayerRaceLayer.render()` runs.
   - If `GeckoLibWereRenderer.renderGeckoModel()` encounters a reflection error or empty bone tree during render time, `renderWereForm` returns `false`.
   - Because `LivingEntityRendererMixin` already suppressed the base player mesh during the HEAD phase, the base player model is not rendered, leaving the player entity **completely invisible**.

2. **Red Hurt Flash Overlay**:
   - In `GeckoLibWereRenderer.renderCubeReflect`:
     `int overlay = (player != null && player.hurtTime > 0) ? OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true)) : OverlayTexture.NO_OVERLAY;`
   - Overlay coordinates are passed into `vc.vertex(...)`. This correctly applies red hurt flash when using `RenderType.entityCutoutNoCull(textureLoc)`. However, validation tests are required to confirm color tinting across Fabric and Forge renderers.

---

## 6. Implementation Recommendations for M2 / R1

Based on this analysis, the following implementation recommendations are provided for Milestone 2 (M2):

1. **Implement `GeckoAssetResolver`**:
   - Unify resolution for models, textures, and animations.
   - Automatically normalize paths (e.g. `werewolf` -> `customraces:geo/werewolf.geo.json` or `customraces:models/were/werewolf.geo.json`).
   - Build a candidate location lookup sequence:
     1. Disk path: `config/custom_races/models/<path>` / `config/custom_races/models/were/<path>`
     2. Resource pack path: `assets/<namespace>/geo/<path>`
     3. Resource pack path: `assets/<namespace>/models/were/<path>`
     4. Default fallback: `customraces:models/were/default_werewolf.geo.json`

2. **Fix Head Rotation Yaw & Pitch Alignment**:
   - Update `GeckoLibWereRenderer.renderGeckoModel` to accept `netHeadYaw` and `headPitch`.
   - During bone hierarchy traversal, detect head bones (`head`, `bipedHead`, `head_bone`) and apply:
     `poseStack.mulPose(Axis.YP.rotationDegrees(netHeadYaw));`
     `poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));`

3. **Resolve Double Scaling with Pehkui**:
   - Modify `PlayerRaceLayer.render()` to check if Pehkui is active (`PehkuiIntegration.isPehkuiLoaded()`).
   - If Pehkui is active and scaling the entity, skip `poseStack.scale(wScale, hScale, wScale)` in `PlayerRaceLayer` to avoid double-scaling.

4. **Harden Suppression Guardrails against Player Invisibility**:
   - Update `LivingEntityRendererMixin` and `WereModelRenderer.isModelAvailable` to perform full model integrity verification (ensuring model is baked AND contains non-empty topLevelBones) before returning `true`.
   - If model integrity check fails, do NOT suppress base human player model mesh.

---
