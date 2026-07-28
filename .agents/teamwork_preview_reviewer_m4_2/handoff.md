# Handoff Report — Reviewer 2: Milestone 4 Verification (R3)

**Verdict**: REQUEST_CHANGES

---

## 1. Observation

### Code Review Findings & Detailed Analysis

1. **Test Failure in `./gradlew test`**:
   - **Command executed**: `.\gradlew test`
   - **Result**: `BUILD FAILED`
   - **Failing Task**: `:common:runM4Challenger2ParticleAndSkinTests`
   - **Test Suite**: `M4Challenger2ParticleAndSkinTest` (6 PASSED, 2 FAILED out of 8 tests).
   - **Failure 1**:
     - **Location**: `M4Challenger2ParticleAndSkinTest.java:251` (Test 5: `ParticleAuraData Scaling & Boundary Sanitization`)
     - **Error**: `java.lang.AssertionError: Scaled count fallback for particleCount=0 Expected: [1], Actual: [3]`
     - **Cause**: In `ParticleAuraData.java` line 22–23:
       ```java
       public int getScaledParticleCount(int raceParticleCount) {
           int effectiveCount = raceParticleCount > 0 ? raceParticleCount : 5;
           return Math.max(1, Math.round(this.count * (effectiveCount / 5.0f)));
       }
       ```
       When `raceParticleCount = 0`, `effectiveCount` becomes `5`. For `aura.count = 2.5f`, `Math.round(2.5f * 1.0f) = 3`. The test expectation in `M4Challenger2ParticleAndSkinTest` was hardcoded to `1`, creating an assertion mismatch.
   - **Failure 2**:
     - **Location**: `M4Challenger2ParticleAndSkinTest.java:305` (Test 7: `Dynamic Skin Override: Fallback Resolution when Player/Skin is Null`)
     - **Error**: `java.lang.AssertionError: Missing texture file fallback to default Expected: [customraces:textures/were/default_werewolf.png], Actual: [customraces:textures/nonexistent_texture_file_12345.png]`
     - **Cause**: In `GeckoAssetResolver.java` line 211:
       ```java
       public static boolean isResourcePresentOnClient(ResourceLocation loc) {
           if (loc == null) return false;
           try {
               Minecraft mc = Minecraft.getInstance();
               if (mc != null && mc.getResourceManager() != null) {
                   return mc.getResourceManager().getResource(loc).isPresent();
               }
           } catch (Throwable ignored) {}
           return true;
       }
       ```
       In headless unit test environments where `Minecraft.getInstance()` is null or uninitialized, `isResourcePresentOnClient` returns `true` for all candidate paths. As a result, `resolveTextureLocation` returns `customraces:textures/nonexistent_texture_file_12345.png` instead of falling through to `getSafeDefaultTexture`.

2. **Dynamic Skin Texture Override Resolution in `GeckoAssetResolver.java`**:
   - **Location**: `common/src/main/java/ddraig/net/customraces/client/render/GeckoAssetResolver.java`, lines 73–105.
   - **Implementation**:
     ```java
     public static ResourceLocation resolveTextureLocation(AbstractClientPlayer player, String rawPath) {
         if (rawPath == null || rawPath.trim().isEmpty() || "none".equalsIgnoreCase(rawPath.trim())) {
             return getSafeDefaultTexture(player);
         }

         String path = rawPath.trim();
         String lowerPath = path.toLowerCase(java.util.Locale.ROOT);

         if ("skin".equals(lowerPath) || "player".equals(lowerPath) || "player_skin".equals(lowerPath) || "skin_texture".equals(lowerPath) || "dynamic_skin".equals(lowerPath) || "use_skin".equals(lowerPath) || "dynamic".equals(lowerPath) || "player_texture".equals(lowerPath) || "default_skin".equals(lowerPath)) {
             if (player != null) {
                 ResourceLocation skinLoc = player.getSkinTextureLocation();
                 if (skinLoc != null) {
                     return skinLoc;
                 }
             }
             return getSafeDefaultTexture(player);
         }
         ...
     ```
   - **Analysis**: Correctly intercepts 9 skin keywords (`"skin"`, `"player"`, `"player_skin"`, `"skin_texture"`, `"dynamic_skin"`, `"use_skin"`, `"dynamic"`, `"player_texture"`, `"default_skin"`) and resolves to `player.getSkinTextureLocation()` when `player` is non-null.

3. **20 Hz Tick-Guarded Particle Aura & Transformed Emission in `PlayerRaceLayer.java`**:
   - **Location**: `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`, lines 24, 53–62, 64–101, 110–131.
   - **Tick Guard**: Uses `ConcurrentHashMap<UUID, Integer> LAST_PARTICLE_TICKS` to ensure particles emit at most once per 20 Hz tick per player, preventing frame-rate dependent particle flooding.
   - **Scale Factors**: Uses `wScale`, `hScale`, and `scaleFactor = Math.max(wScale, hScale)` to scale particle random offset spread, Y-offsets, and velocities during transformed state.

4. **Multi-Platform Build Result**:
   - **Command executed**: `.\gradlew clean build -x test`
   - **Result**: `BUILD SUCCESSFUL in 1m 39s`. Artifacts compiled cleanly for Common, Fabric, and Forge.

---

## 2. Logic Chain

1. **Build vs. Test Execution**:
   - Observation: `.\gradlew clean build -x test` succeeds without errors across Common, Fabric, and Forge.
   - Observation: `.\gradlew test` fails in `:common:runM4Challenger2ParticleAndSkinTests`.
   - Logic: A project milestone cannot be approved (PASS) if the unit test suite fails during automated testing (`./gradlew test`).

2. **Test Failure 1 Analysis**:
   - Observation: `ParticleAuraData.getScaledParticleCount(0)` returned `3`, whereas `M4Challenger2ParticleAndSkinTest` expected `1`.
   - Logic: `ParticleAuraData` defaults `effectiveCount` to `5` when `raceParticleCount <= 0`. `2.5 * (5/5) = 2.5`, which rounds to `3`. Either `ParticleAuraData` should treat `raceParticleCount = 0` as 0/1 or the test assertion in `M4Challenger2ParticleAndSkinTest` must align with the intended domain logic.

3. **Test Failure 2 Analysis**:
   - Observation: Headless test `testSkinFallbackResolutionNullPlayer` expects `DEFAULT_TEXTURE_LOCATION` for missing texture files.
   - Logic: `GeckoAssetResolver.isResourcePresentOnClient` returns `true` when `Minecraft.getInstance()` is null. This tricks asset resolution into thinking non-existent resource locations exist during headless unit tests, causing test failure and improper fallback behavior off-client.

---

## 3. Findings & Required Fixes

### Finding 1 [Major - Test Failure]: Mismatch in `ParticleAuraData` Scaled Particle Count Fallback
- **Where**: `common/src/main/java/ddraig/net/customraces/data/ParticleAuraData.java:22` and `M4Challenger2ParticleAndSkinTest.java:251`
- **Why**: Test 5 in `M4Challenger2ParticleAndSkinTest` fails with `Expected: [1], Actual: [3]`.
- **Suggestion**: Reconcile `getScaledParticleCount` behavior for `0` particle count between `ParticleAuraData` and `M4Challenger2ParticleAndSkinTest`.

### Finding 2 [Major - Headless Fallback Bug]: `isResourcePresentOnClient` returns `true` when Minecraft Client is null
- **Where**: `common/src/main/java/ddraig/net/customraces/client/render/GeckoAssetResolver.java:211`
- **Why**: When running in headless test environments (or off-client thread), returning `true` causes candidate check to return fake locations (`customraces:textures/nonexistent...`) instead of checking disk files or falling back to default textures.
- **Suggestion**: Modify `isResourcePresentOnClient` to check if `mc == null` or `mc.getResourceManager() == null`, and in headless mode return `false` (or check file system) so fallback logic works correctly.

---

## 4. Caveats

- The implementation logic for 20 Hz tick guarding, scale-aware particle offsets, skin keyword interception, and matrix hygiene in `PlayerRaceLayer.java` and `GeckoAssetResolver.java` is well-constructed. However, test failures block a PASS verdict.

---

## 5. Conclusion

- **Verdict**: **REQUEST_CHANGES**
- Work cannot be approved until `.\gradlew test` passes 100% cleanly without test failures.

---

## 6. Verification Method

To re-verify after fixes are applied:

1. **Run Unit Tests**:
   ```powershell
   .\gradlew test
   ```
   *Expected result*: `BUILD SUCCESSFUL` with all tasks passing, including `:common:runM4Challenger2ParticleAndSkinTests`.

2. **Run Build Command**:
   ```powershell
   .\gradlew build -x test
   ```
   *Expected result*: `BUILD SUCCESSFUL`.
