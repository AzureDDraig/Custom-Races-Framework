# Handoff Report: Were-Form Model & Texture Rendering Investigation (R1)

**Agent**: Explorer 1  
**Milestone**: Milestone 1 - R1 (Were-Form Model & Texture Rendering Fix)  
**Working Directory**: `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_1_fu`  
**Date**: 2026-07-24  

---

## 1. Observation

1. **Asset File Check (`default_werewolf.png`)**:
   - `find_by_name` result: `common/src/main/resources/assets/customraces/textures/were/default_werewolf.png` exists.
   - Built asset: `common/build/resources/main/assets/customraces/textures/were/default_werewolf.png` exists.
   - In `WereModelRenderer.java:25`:
     ```java
     public static final ResourceLocation DEFAULT_WERE_TEXTURE = new ResourceLocation("customraces", "textures/were/default_werewolf.png");
     ```
   - Physical texture file location matches `DEFAULT_WERE_TEXTURE` constant exactly.

2. **`wereTexturePath` Resolution (`WereModelRenderer.java`)**:
   - In `WereModelRenderer.java` lines 61-74:
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
   - In `WereModelRenderer.java` line 117:
     ```java
     ResourceLocation textureLoc = getValidWereTextureLocation(race);
     ```
   - Observation: `getValidWereTextureLocation` accepts only `RaceData race`, omitting `AbstractClientPlayer player`. It cannot access `player.getSkinTextureLocation()`.

3. **Keyword & Relative Path String Behavior**:
   - When `wereTexturePath` = `"skin"` or `"player"`, `ResourceLocation.tryParse("skin")` constructs `minecraft:skin`. Minecraft cannot locate `assets/minecraft/textures/skin.png` and renders the missing texture sprite `missingno` (purple/black checkerboard).
   - When `wereTexturePath` = `"textures/were/dark.png"` or `"dark_werewolf.png"`, `ResourceLocation.tryParse` defaults to `minecraft:` namespace (`minecraft:textures/were/dark.png`).
   - `ResourceLocation.tryParse()` only checks character syntax regex (`[a-z0-9_.-]+:[a-z0-9_./-]+`). It does NOT check asset existence in `Minecraft.getInstance().getResourceManager()`. Thus, non-existent paths pass syntax checks and return valid `ResourceLocation` instances, causing OpenGL missing texture rendering.

4. **Integration in `PlayerRaceLayer.java`**:
   - Lines 41 & 51: `PlayerRaceLayer` checks `WereModelRenderer.isWereForm(player, race)` and delegates rendering to `WereModelRenderer.renderWereForm(poseStack, buffer, packedLight, player, this.getParentModel(), race, netHeadYaw, headPitch)`.

---

## 2. Logic Chain

1. **Step 1 (Asset Verification)**: Observation 1 confirms `common/src/main/resources/assets/customraces/textures/were/default_werewolf.png` exists on disk and matches `DEFAULT_WERE_TEXTURE`. Therefore, fallback to `DEFAULT_WERE_TEXTURE` will successfully resolve to a valid dark fur texture asset.
2. **Step 2 (Keyword Support)**: Observation 2 shows `getValidWereTextureLocation(RaceData race)` lacks access to `AbstractClientPlayer player`. To support `"skin"` and `"player"` keywords, `getValidWereTextureLocation` must take `AbstractClientPlayer player` and check if `race.wereTexturePath` matches `"skin"` or `"player"`. If matched, it must return `player.getSkinTextureLocation()`.
3. **Step 3 (Missing Texture Diagnosis)**: Observation 3 shows that strings without namespaces default to `minecraft:`, strings missing `textures/` or `.png` resolve to invalid resource paths, and `ResourceLocation.tryParse` returns non-null for missing files. This causes Minecraft's `TextureManager` to fail texture loading and render `missingno` (purple/black checkerboard).
4. **Step 4 (Path Normalization & Fallback)**: To prevent `missingno` rendering, `getValidWereTextureLocation` must:
   - Normalize paths (default namespace to `"customraces"`, auto-add `"textures/"` prefix and `".png"` extension).
   - Validate asset existence on client via `Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()`.
   - Fall back to `DEFAULT_WERE_TEXTURE`, and if `DEFAULT_WERE_TEXTURE` is unresolvable, fall back to `player.getSkinTextureLocation()`.

---

## 3. Caveats

- **Client vs Server Environment**: `Minecraft.getInstance().getResourceManager()` is client-only code. Asset existence checks must be guarded by client-side checks (`player.level().isClientSide` or `FMLEnvironment` / Fabric client entrypoint) or contained within client renderer classes (`WereModelRenderer`).
- **Offline / Modded Skins**: When `player.getSkinTextureLocation()` is returned for `"skin"` / `"player"` keywords, skin resolution depends on Minecraft's skin loading thread. If the player is offline or skin download fails, Minecraft defaults `getSkinTextureLocation()` to `DefaultPlayerSkin.getDefaultTexture(player.getUUID())`.

---

## 4. Conclusion

1. `default_werewolf.png` asset file exists at `common/src/main/resources/assets/customraces/textures/were/default_werewolf.png` and matches `DEFAULT_WERE_TEXTURE`.
2. `"skin"` and `"player"` keyword binding requires overloading `getValidWereTextureLocation(AbstractClientPlayer player, RaceData race)` and returning `player.getSkinTextureLocation()`.
3. Purple/black missingno textures stem from namespace defaulting to `minecraft:`, missing path normalization, and `ResourceLocation.tryParse()` not verifying asset existence.
4. A 5-tier fallback hierarchy (Keywords -> Null/None Check -> Path Normalization -> Client ResourceManager Existence Check -> Default Were/Player Skin Fallback) guarantees clean texture rendering without `missingno` artifacts.

---

## 5. Verification Method

1. **File Inspection**:
   - Inspect `WereModelRenderer.java` (lines 61-74 and 117).
   - Inspect `analysis.md` report in working directory `.agents/teamwork_preview_explorer_m1_1_fu/analysis.md`.

2. **Automated Unit Test Command**:
   - Run: `./gradlew test --tests ddraig.net.customraces.event.WereTransformEdgeCaseTest` (or `./gradlew build -x test`).

3. **Invalidation Conditions**:
   - If `default_werewolf.png` is removed from `common/src/main/resources/assets/customraces/textures/were/`.
   - If `ResourceLocation.tryParse("skin")` is called without keyword intercept, resulting in `minecraft:skin`.

