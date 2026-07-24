# Comprehensive Analysis Report: Were-Form Model & Texture Rendering Fixes (R1)

**Working Directory**: `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_1_fu`  
**Milestone**: Milestone 1 - Requirement R1  
**Author**: Explorer 1  
**Date**: 2026-07-24  

---

## Executive Summary

This investigation analyzes Requirement R1 of the Custom Races Framework: the Were-Form model and texture rendering pipeline. It focuses on asset existence verification, code tracing of texture resolution in `WereModelRenderer.java` and `PlayerRaceLayer.java`, supporting `"skin"` and `"player"` keywords to directly bind player skin textures (`player.getSkinTextureLocation()`), diagnosing relative path parsing failures leading to missing purple/black checkerboard textures, and designing a robust 5-tier fallback mechanism.

---

## Section 1: Texture Asset Verification

### 1.1 Findings
- **Target Asset**: `default_werewolf.png`
- **Expected Location**: `assets/customraces/textures/were/default_werewolf.png`
- **Actual File Path**: `common/src/main/resources/assets/customraces/textures/were/default_werewolf.png` (Built asset: `common/build/resources/main/assets/customraces/textures/were/default_werewolf.png`).
- **Constant in Code**: `WereModelRenderer.DEFAULT_WERE_TEXTURE = new ResourceLocation("customraces", "textures/were/default_werewolf.png")`.

### 1.2 Asset Directory Audit
Directory inspection of `common/src/main/resources/assets/customraces/textures/`:
```
textures/
├── item/
│   └── orb_of_rebirth.png
└── were/
    └── default_werewolf.png
```
**Conclusion**: `default_werewolf.png` exists in the `common` module resources directory. The constant `DEFAULT_WERE_TEXTURE` in `WereModelRenderer.java` matches the physical path.

---

## Section 2: Code Tracing of `wereTexturePath` Resolution

### 2.1 Rendering Flow
1. **`PlayerRaceLayer.render(...)`** (`common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java:29-82`):
   - Queries `RaceData race = RaceRegistry.getPlayerRace(player.getUUID())`.
   - Checks `boolean isWereTransformed = WereModelRenderer.isWereForm(player, race)` (line 41).
   - If transformed, scales poseStack by `wereHeightScale` / `wereWidthScale` (lines 46-48).
   - Delegates rendering to `WereModelRenderer.renderWereForm(...)` (line 51).

2. **`WereModelRenderer.renderWereForm(...)`** (`common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java:107-125`):
   - Validates `isWereForm(player, race)`.
   - Checks `hasCustomModel(race)` (lines 113-119):
     - If true: hides base player mesh (`setBaseModelVisible(parentModel, false)`), resolves texture via `getValidWereTextureLocation(race)`, and calls `renderCustomWereMesh(...)`.
     - If false: keeps player model visible (`setBaseModelVisible(parentModel, true)`) for procedural overlay rendering.

3. **`WereModelRenderer.getValidWereTextureLocation(RaceData race)`** (`common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java:61-74`):
   ```java
   public static ResourceLocation getValidWereTextureLocation(RaceData race) {
       if (race == null || race.wereTexturePath == null || race.wereTexturePath.trim().isEmpty() || "none".equalsIgnoreCase(race.wereTexturePath.trim())) {
           return DEFAULT_WERE_TEXTURE;
       }
       String path = race.wereTexturePath.trim();
       ResourceLocation loc = ResourceLocation.tryParse(path);
       if (loc == null) {
           if (LOGGED_WARNINGS.add("texture:" + path)) {
               System.err.println("[CustomRaces] Invalid Were texture path '" + path + "', falling back to default: " + DEFAULT_WERE_TEXTURE);
           }
           return DEFAULT_WERE_TEXTURE;
       }
       return loc;
   }
   ```

---

## Section 3: Keyword Support for `"skin"` and `"player"`

### 3.1 Problem Definition
Currently, `getValidWereTextureLocation` accepts only `RaceData race` as a parameter and calls `ResourceLocation.tryParse(race.wereTexturePath.trim())`.
- When `race.wereTexturePath` is set to `"skin"` or `"player"`, `ResourceLocation.tryParse("skin")` returns `new ResourceLocation("minecraft", "skin")`.
- Minecraft's `TextureManager` attempts to load asset `assets/minecraft/skin` (or `assets/minecraft/textures/skin.png`), which does not exist.
- As a result, the texture manager binds `missingno` (purple/black checkerboard).

### 3.2 Required Signature Update & Logic
Update `getValidWereTextureLocation` to take `AbstractClientPlayer player`:

```java
public static ResourceLocation getValidWereTextureLocation(AbstractClientPlayer player, RaceData race) {
    if (race == null || race.wereTexturePath == null || race.wereTexturePath.trim().isEmpty() || "none".equalsIgnoreCase(race.wereTexturePath.trim())) {
        return DEFAULT_WERE_TEXTURE;
    }
    String path = race.wereTexturePath.trim().toLowerCase(java.util.Locale.ROOT);
    
    // Support skin/player keywords to bind player skin directly
    if ("skin".equals(path) || "player".equals(path) || "player_skin".equals(path) || "skin_texture".equals(path)) {
        if (player != null) {
            return player.getSkinTextureLocation();
        } else {
            return DEFAULT_WERE_TEXTURE;
        }
    }
    
    // Continue with normalized path parsing and resource resolution
    ...
}
```

Maintain backwards-compatible overload:
```java
public static ResourceLocation getValidWereTextureLocation(RaceData race) {
    return getValidWereTextureLocation(null, race);
}
```

Update caller in `WereModelRenderer.renderWereForm`:
```java
ResourceLocation textureLoc = getValidWereTextureLocation(player, race);
```

---

## Section 4: Relative Texture Path String Parsing & Missing Texture Root Cause

### 4.1 Root Causes of Purple/Black Checkerboard (`missingno`) Textures

| Cause | Example Input | Parsed `ResourceLocation` | Resulting Missing Asset Path | Cause Analysis |
|---|---|---|---|---|
| **Keyword Unhandled** | `"skin"` / `"player"` | `minecraft:skin` | `assets/minecraft/textures/skin.png` | Parsed as literal string instead of retrieving `player.getSkinTextureLocation()`. |
| **Namespace Defaulting** | `"textures/were/dark.png"` | `minecraft:textures/were/dark.png` | `assets/minecraft/textures/were/dark.png` | String without colon defaults to `minecraft:` namespace instead of `customraces:`. |
| **Shorthand Path Omission** | `"dark_werewolf.png"` or `"were/dark.png"` | `minecraft:dark_werewolf.png` | `assets/minecraft/dark_werewolf.png` | Missing `textures/` folder prefix or `customraces:` namespace prefix. |
| **Missing Asset File** | `"customraces:textures/were/missing.png"` | `customraces:textures/were/missing.png` | File not found on disk | `ResourceLocation.tryParse()` only validates string regex syntax, NOT asset existence. Non-existent assets pass syntax checks and return valid `ResourceLocation` instances, causing OpenGL/Blaze3D to bind `missingno`. |

### 4.2 Path Normalization Algorithm
When a custom path string is supplied (e.g. `"were/dark"` or `"textures/were/dark.png"` or `"customraces:textures/were/dark.png"`):
1. Trim leading/trailing whitespace.
2. Separate namespace and relative path:
   - If `:` is present, split into `[namespace, path]`.
   - If `:` is NOT present, set `namespace = "customraces"`, `path = input`.
3. Prepend `"textures/"` to `path` if it does not start with `"textures/"`.
4. Append `".png"` to `path` if it does not end with `".png"`.
5. Construct `ResourceLocation loc = ResourceLocation.tryParse(namespace + ":" + path)`.

---

## Section 5: Multi-Tier Fallback Mechanism Design

To eliminate missing texture (`missingno`) rendering, `WereModelRenderer.getValidWereTextureLocation` should execute a 5-tier fallback hierarchy:

```
[Tier 1: Keyword Check]
  path in ("skin", "player", "player_skin", "skin_texture")?
  ├── YES ──> Return player != null ? player.getSkinTextureLocation() : DEFAULT_WERE_TEXTURE
  └── NO  ──> Proceed to Tier 2

[Tier 2: Null / Empty / "none" Check]
  path is null, empty, or "none"?
  ├── YES ──> Return DEFAULT_WERE_TEXTURE
  └── NO  ──> Proceed to Tier 3

[Tier 3: Path Normalization]
  Normalize string (default namespace "customraces:", prefix "textures/", extension ".png").
  ResourceLocation.tryParse(...) -> loc
  loc == null?
  ├── YES ──> Log warning, proceed to Tier 5
  └── NO  ──> Proceed to Tier 4

[Tier 4: Client-Side Resource Existence Check]
  Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()?
  ├── YES ──> Return loc
  └── NO  ──> Log warning ("Custom Were texture asset not found: " + loc), proceed to Tier 5

[Tier 5: Safe Default & Player Skin Fallback]
  Check DEFAULT_WERE_TEXTURE via getResourceManager().getResource(...)
  ├── YES ──> Return DEFAULT_WERE_TEXTURE
  └── NO  ──> Return player != null ? player.getSkinTextureLocation() : DefaultPlayerSkin.getDefaultTexture()
```

---

## Summary Table of Proposed Implementation Changes

| Component | Target Location | Description |
|---|---|---|
| **Method Signature Update** | `WereModelRenderer.java:61` | Change signature to `getValidWereTextureLocation(AbstractClientPlayer player, RaceData race)` + overloaded `getValidWereTextureLocation(RaceData race)`. |
| **Keyword Parsing** | `WereModelRenderer.java:64-70` | Check `"skin"`, `"player"`, `"player_skin"`, `"skin_texture"` and return `player.getSkinTextureLocation()`. |
| **Path Normalization** | `WereModelRenderer.java:71-85` | Normalize relative texture paths (default namespace `"customraces"`, auto-add `textures/` prefix and `.png` extension). |
| **Resource Existence Check** | `WereModelRenderer.java:86-95` | Use `Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()` to verify asset existence prior to binding. |
| **Render Call Update** | `WereModelRenderer.java:117` | Pass `player` to `getValidWereTextureLocation(player, race)` in `renderWereForm()`. |

