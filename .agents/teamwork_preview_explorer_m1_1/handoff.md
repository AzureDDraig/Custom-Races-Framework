# Handoff Report — Explorer 1 (M1) Focus Area R1

**Agent:** Explorer 1 (M1)  
**Working Directory:** `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_1`  
**Target Recipient:** Orchestrator / Parent Agent  
**Milestone:** M1 (Exploration & Architecture Analysis) -> M2 (GeckoLib Model Override & Asset Resolution)  

---

## 1. Observation

Direct code inspections of the render pipeline and GeckoLib integration revealed the following exact lines and behaviors:

1. **Path Normalization Asymmetry**:
   - `WereModelRenderer.java:95-156`: `getValidWereTextureLocation()` performs full path normalization (defaulting namespace to `"customraces"`, adding `"textures/"` prefix, adding `".png"` suffix, and checking `isResourcePresentOnClient()` & `loadDiskTextureDynamic()`).
   - `WereModelRenderer.java:80-93`: `getValidWereModelLocation()` only executes `ResourceLocation.tryParse(path)`. If `path` is `"werewolf.geo.json"`, `tryParse` produces `ResourceLocation("minecraft", "werewolf.geo.json")`.
   - `WereModelRenderer.java:219-232`: `getValidWereAnimationLocation()` also executes `ResourceLocation.tryParse(path)` without prefixing `"animations/"` or adding `".animation.json"`.

2. **Asset Resolution Search Paths**:
   - `GeckoLibWereRenderer.java:240-264`: `bakeModelFromFile()` checks disk paths `config/custom_races/models/<cleanPath>` and `config/custom_races/models/were/<cleanPath>`, then falls back to `mc.getResourceManager().getResource(modelLoc)`.
   - Mod resource pack files at standard GeckoLib location `assets/customraces/geo/<name>.geo.json` are NOT found if `modelLoc` is `customraces:models/were/default_werewolf.geo.json` or `customraces:werewolf.geo.json` without searching candidate resource subfolders (`geo/` vs `models/were/`).

3. **Head Yaw & Pitch Alignment**:
   - `PlayerRaceLayer.java:30, 51`: `render()` receives `netHeadYaw` and `headPitch` and passes them to `WereModelRenderer.renderWereForm()`.
   - `WereModelRenderer.java:250, 263`: `renderWereForm()` receives `netHeadYaw` and `headPitch`, but DOES NOT pass them to `GeckoLibWereRenderer.renderGeckoModel()`.
   - `GeckoLibWereRenderer.java:41, 75`: `renderGeckoModel()` and `renderBoneReflect()` do NOT apply head rotations (`netHeadYaw` or `headPitch`) to head bones (`head`, `bipedHead`, `head_bone`).

4. **Double Scaling with Pehkui Integration**:
   - `PehkuiIntegration.java:59-108`: `applyRaceScales()` applies `wereHeightScale` and `wereWidthScale` to Pehkui's entity scale attributes (`HEIGHT` and `WIDTH`) when transformed.
   - `PlayerRaceLayer.java:48`: `render()` executes `poseStack.scale(wScale, hScale, wScale)` whenever `isWereTransformed` is `true`.
   - When Pehkui is installed, `LivingEntityRenderer` scales `poseStack` by Pehkui entity scale prior to `PlayerRaceLayer.render()`, resulting in double scaling (`hScale^2`, `wScale^2`).

5. **Invisibility Safety Guardrail Gap**:
   - `LivingEntityRendererMixin.java:29-34`: Injects at `@At("HEAD")` of `LivingEntityRenderer.render()` and calls `setBaseModelVisible(playerModel, false)` if `isModelAvailable(race)` returns `true`.
   - `WereModelRenderer.java:74-78`: `isModelAvailable()` calls `GeckoLibWereRenderer.isModelPresent(loc)`, which checks if `bakedModels` map contains `loc` or if `bakeModelFromFile(loc) != null`.
   - If `bakeModelFromFile` succeeds in parsing JSON structure, but `renderGeckoModel()` fails during bone hierarchy traversal (e.g. empty `topLevelBones` or vertex buffer error), `renderWereForm()` returns `false`. Because base model parts were suppressed at `@At("HEAD")`, the base player mesh is not drawn, making the player **completely invisible**.

---

## 2. Logic Chain

1. **Path Resolution Deficiencies -> Asset Load Failures**:
   - Observation 1 demonstrates that model and animation paths are not normalized like texture paths.
   - If a user config specifies `werewolf.geo.json`, `tryParse` returns `minecraft:werewolf.geo.json`. `ResourceManager` attempts to load `assets/minecraft/werewolf.geo.json`, which fails because the asset is in `assets/customraces/geo/werewolf.geo.json`.
   - Therefore, dual-path resolution between `config/custom_races/models/` and `assets/customraces/geo/` fails unless candidate path prefixes (`geo/`, `models/were/`) and namespaces (`customraces`) are systematically attempted by a helper class.

2. **Omitted Head Rotation Parameters -> Rigid Head Rendering**:
   - Observation 3 shows `netHeadYaw` and `headPitch` are dropped between `WereModelRenderer.renderWereForm` and `GeckoLibWereRenderer.renderGeckoModel`.
   - `renderBoneReflect` applies bone animation angles but never applies camera pitch/head yaw to the head bone.
   - Therefore, transformed player models cannot look up/down or turn their heads independently of body rotation.

3. **Uncoordinated Scaling -> Quadratic Pehkui Scaling**:
   - Observation 4 shows Pehkui entity scale and `PlayerRaceLayer` PoseStack scale are both active simultaneously.
   - Therefore, when Pehkui is active, transformed models render at `scale^2` size.

4. **Premature Base Mesh Suppression -> Invisibility Risk**:
   - Observation 5 shows `LivingEntityRendererMixin` suppresses base player cuboids at `@At("HEAD")` based solely on model presence, before render-time bone verification occurs.
   - If bone rendering fails at render time, base model parts remain hidden.
   - Therefore, a stricter model validity check (verifying non-empty topLevelBones and valid vertex structure) is required before suppressing base player cuboids.

---

## 3. Caveats

- **No Code Changes Introduced**: This investigation was strictly read-only per M1 guidelines. No files under `src/` were modified.
- **GeckoLib Version Assumptions**: Code inspection verified reflection calls target GeckoLib 4.x API (`software.bernie.geckolib.*`). Keyframe animation playback logic (R3 requirement) was analyzed for M2/M3 alignment, but full keyframe interpolation engine design belongs to R3/M4.
- **Environment Execution**: Direct GPU rendering could not be visually observed in head-less CLI mode, but matrix stack flow and method calls were verified by tracing call chains and line-by-line inspection.

---

## 4. Conclusion

The current GeckoLib player model override system provides a solid foundation via reflection-based bone rendering, but requires four critical architectural improvements for M2 implementation:
1. Create `GeckoAssetResolver` to handle path normalization and candidate search for disk config (`config/custom_races/models/`, `textures/`, `animations/`) and mod resource pack paths (`assets/customraces/geo/`, `textures/`, `animations/`).
2. Pass `netHeadYaw` and `headPitch` to `GeckoLibWereRenderer` and apply rotational transformations to designated head bones.
3. Coordinate scale logic between `PehkuiIntegration` and `PlayerRaceLayer` to prevent double-scaling.
4. Strengthen `isModelAvailable()` and `LivingEntityRendererMixin` guardrails so base human player model mesh is suppressed ONLY when GeckoLib model integrity is fully verified.

---

## 5. Verification Method

To independently verify these findings:

1. **Inspect Path Normalization**:
   - Open `common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java` and compare `getValidWereTextureLocation` (lines 95-156) with `getValidWereModelLocation` (lines 80-93).
2. **Inspect Head Yaw/Pitch Flow**:
   - Open `common/src/main/java/ddraig/net/customraces/client/render/GeckoLibWereRenderer.java` line 41 (`renderGeckoModel`) and confirm `netHeadYaw` and `headPitch` are missing from parameter list.
3. **Inspect Pehkui Interaction**:
   - Check `PehkuiIntegration.java` lines 59-108 vs `PlayerRaceLayer.java` line 48 to confirm double scaling.
4. **Compile Test Suite**:
   - Run `./gradlew build -x test` to verify current project compilation integrity.

---
