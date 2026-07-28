# Changes Summary — Milestone 2: GeckoLib Model Override & Dual Asset Resolution (R1)

## Files Created / Modified

### 1. `common/src/main/java/ddraig/net/customraces/client/render/GeckoAssetResolver.java` (Created)
- Implemented dedicated asset resolution helper class in package `ddraig.net.customraces.client.render`.
- Resolves models (`.geo.json`), textures (`.png`), and animation files (`.animation.json`) across disk config paths (`config/custom_races/models/`, `textures/`, `animations/`) and mod resource pack paths (`assets/customraces/geo/`, `textures/`, `animations/`).
- Normalizes path strings: defaults namespace to `"customraces"`, handles missing file extensions, and checks candidate paths with and without subfolder prefixes (`geo/`, `models/were/`, `animations/`, `textures/`).
- Intercepts skin keywords (`"skin"`, `"player"`, `"player_skin"`, `"skin_texture"`) to bind player skin textures directly.
- Provides `getModelContent()` and `getAnimationContent()` for reading JSON definitions from disk or resource packs.

### 2. `common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java` (Modified)
- Integrated `GeckoAssetResolver` into `getValidWereModelLocation()`, `getValidWereTextureLocation()`, `getValidWereAnimationLocation()`, `isResourcePresentOnClient()`, and `clearCaches()`.
- Extended `setBaseModelVisible()` to toggle `model.cloak` and `model.ear` visibility along with base player cuboids.
- Updated `renderWereForm()` and `renderGeckoLibWereModel()` to pass `netHeadYaw` and `headPitch` parameters to `GeckoLibWereRenderer.renderGeckoModel()`.

### 3. `common/src/main/java/ddraig/net/customraces/client/render/GeckoLibWereRenderer.java` (Modified)
- Updated `renderGeckoModel()` and `renderBoneReflect()` to accept `netHeadYaw` and `headPitch`.
- Implemented `isHeadBone(boneName)` check for bone names matching `"head"`, `"bipedHead"`, `"head_bone"`, or `"headbone"`.
- Applied rotational matrix transforms (`netHeadYaw` around Y-axis, `headPitch` around X-axis) when traversing head bones.
- Updated `bakeModelFromFile()` and `bakeAnimationsFromFile()` to use `GeckoAssetResolver` content resolution.

### 4. `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java` (Modified)
- Guarded `poseStack.scale(wScale, hScale, wScale)` with `if (!PehkuiIntegration.isPehkuiLoaded())` during transformed Were-form rendering to coordinate scaling with Pehkui and eliminate quadratic double-scaling.
