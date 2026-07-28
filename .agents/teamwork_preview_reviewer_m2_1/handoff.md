# Review & Handoff Report — Reviewer M2 (GeckoLib Asset Resolution & Rendering R1)

**Agent:** Reviewer 1 (Milestone 2)  
**Working Directory:** `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_reviewer_m2_1`  
**Target Recipient:** Parent / Orchestrator (`8481d858-0416-4639-93eb-dca8a11c96f8`)  
**Milestone:** Milestone 2 (GeckoLib Asset Resolution & Rendering R1)  
**Verdict:** **FAIL / REQUEST_CHANGES**

---

## 1. Observation

Direct inspection of Worker M2's implementation files (`GeckoAssetResolver.java`, `WereModelRenderer.java`, `GeckoLibWereRenderer.java`, `PlayerRaceLayer.java`) and build system execution revealed the following direct findings:

1. **Gradle Build Verification (`./gradlew build -x test`)**:
   - Execution command: `./gradlew build -x test`
   - Output result:
     ```
     BUILD SUCCESSFUL in 22s
     29 actionable tasks: 21 executed, 8 up-to-date
     ```
   - `:common:build`, `:fabric:build`, and `:forge:build` compiled cleanly.

2. **Unit Test Execution (`./gradlew test`)**:
   - Execution command: `./gradlew test`
   - Task `:common:runGeckoAssetResolverTests` FAILED with exit code 1.
   - Verbatim console exception log output:
     ```
     --- Test 8: Malformed Path Inputs ---
     [FAIL] Test 8 (Malformed Paths): Unexpected exception thrown for malformed input 'invalid_namespace::path': Non [a-z0-9/._-] character in path of location: invalid_namespace:geo/:path.geo.json
     java.lang.AssertionError: Unexpected exception thrown for malformed input 'invalid_namespace::path': Non [a-z0-9/._-] character in path of location: invalid_namespace:geo/:path.geo.json
     	at ddraig.net.customraces.client.render.GeckoAssetResolverTest.testMalformedPathInputs(GeckoAssetResolverTest.java:326)
     	at ddraig.net.customraces.client.render.GeckoAssetResolverTest.main(GeckoAssetResolverTest.java:93)
     Caused by: net.minecraft.ResourceLocationException: Non [a-z0-9/._-] character in path of location: invalid_namespace:geo/:path.geo.json
     	at net.minecraft.resources.ResourceLocation.assertValidPath(ResourceLocation.java:252)
     	at net.minecraft.resources.ResourceLocation.<init>(ResourceLocation.java:47)
     	at ddraig.net.customraces.client.render.GeckoAssetResolver.parsePath(GeckoAssetResolver.java:321)
     	at ddraig.net.customraces.client.render.GeckoAssetResolver.resolveModelLocation(GeckoAssetResolver.java:50)
     ```

3. **Unhandled Exception in `GeckoAssetResolver.java:321`**:
   - `GeckoAssetResolver.java` lines 320-324:
     ```java
     ResourceLocation primaryLoc = !candidates.isEmpty() ? candidates.get(0) : new ResourceLocation(namespace, defaultSubfolderPrefix + filename);

     return new ParsedPath(namespace, normalizedRelPath, filename, primaryLoc, candidates);
     ```
   - When `rawPath` contains invalid characters (e.g. `invalid_namespace::path`, `:missing_namespace`, or spaces), `ResourceLocation.tryParse(...)` returns `null` for all candidate strings, leaving `candidates` empty.
   - Line 321 then calls `new ResourceLocation(namespace, defaultSubfolderPrefix + filename)`. The `ResourceLocation` constructor throws an unhandled `ResourceLocationException`. Because callers (`resolveModelLocation`, `resolveTextureLocation`, `resolveAnimationLocation`) do not catch this exception, the client crashes to desktop when reading malformed path configurations instead of falling back to default locations (`DEFAULT_MODEL_LOCATION`, `DEFAULT_TEXTURE_LOCATION`, `DEFAULT_ANIMATION_LOCATION`).

4. **Extension Normalization Flaw in `normalizedRelPath`**:
   - `GeckoAssetResolver.java` lines 282-294:
     ```java
     String filename = pathWithoutNamespace.replaceAll(".*/", "");
     if (!filename.toLowerCase().endsWith(defaultExtension)) {
         if (defaultExtension.equals(".geo.json") && filename.toLowerCase().endsWith(".json")) {
             filename = filename.substring(0, filename.length() - 5) + ".geo.json";
         } ...
     }

     String normalizedRelPath = pathWithoutNamespace;
     if (!normalizedRelPath.contains(".")) {
         normalizedRelPath = normalizedRelPath + defaultExtension;
     }
     ```
   - When `rawPath` is `"werewolf.json"`, `filename` is updated to `"werewolf.geo.json"`. However, `normalizedRelPath` remains `"werewolf.json"` because `"werewolf.json"` already contains a dot (`"."`).
   - Consequently, primary candidate `loc1` (`ResourceLocation.tryParse("customraces:werewolf.json")`) is created with `.json` instead of `.geo.json`.

5. **Head Rotation & Pehkui Integration Verification**:
   - `GeckoLibWereRenderer.java:82, 140-147`: Bone traversal checks `isHeadBone(boneName)` (`"head"`, `"bipedhead"`, `"head_bone"`, `"headbone"`) and applies `poseStack.mulPose(Axis.YP.rotationDegrees(netHeadYaw))` and `poseStack.mulPose(Axis.XP.rotationDegrees(headPitch))`.
   - `PlayerRaceLayer.java:48-50`: Visual layer scaling is guarded with `if (!PehkuiIntegration.isPehkuiLoaded()) poseStack.scale(...)`, successfully preventing double-scaling when Pehkui is active.

---

## 2. Logic Chain

1. **Gradle Build Verification**:
   - Running `./gradlew build -x test` builds `:common`, `:fabric`, and `:forge` without compilation errors.

2. **Test Suite Failure Analysis**:
   - Worker M2 omitted running `./gradlew test` (or missed checking test execution results).
   - Execution of `./gradlew test` triggers `:common:runGeckoAssetResolverTests`.
   - Test 8 (`testMalformedPathInputs`) passes malformed string input `'invalid_namespace::path'` to `resolveModelLocation()`.
   - `parsePath()` extracts `namespace = "invalid_namespace"` and `pathWithoutNamespace = ":path"`.
   - `filename` becomes `":path.geo.json"`.
   - `ResourceLocation.tryParse(...)` returns `null` for invalid characters (colons in filename/path), so `candidates` list remains empty.
   - Line 321 invokes `new ResourceLocation("invalid_namespace", "geo/:path.geo.json")`.
   - The `ResourceLocation` constructor throws `ResourceLocationException: Non [a-z0-9/._-] character in path of location: invalid_namespace:geo/:path.geo.json`.
   - Because `resolveModelLocation()` does not catch `ResourceLocationException`, the exception propagates out and fails the test suite. In game, this uncaught exception crashes the client to desktop when a player race has a malformed config path.

3. **Extension Normalization Analysis**:
   - If input path is `"werewolf.json"`, `filename` correctly becomes `"werewolf.geo.json"`.
   - However, `normalizedRelPath` check `!normalizedRelPath.contains(".")` fails because `"werewolf.json"` contains a `.`.
   - Therefore `normalizedRelPath` remains `"werewolf.json"`, causing candidate location #1 to be `customraces:werewolf.json` (invalid extension for GeckoLib geometry).

4. **Conclusion Rationale**:
   - A core requirement of Milestone 2 is robust fallback hierarchy and path normalization that prevents rendering crashes under unexpected path strings.
   - Because malformed path inputs cause an uncaught `ResourceLocationException` that crashes the application/test suite, the verdict must be **FAIL / REQUEST_CHANGES**.

---

## 3. Caveats

- **No Caveats**: The test suite failure and root cause in `GeckoAssetResolver.java` line 321 are fully reproduced and documented with exact line numbers and verbatim stack traces. Head rotation matrix transforms and Pehkui scale guards were independently verified as correct.

---

## 4. Conclusion

**Verdict: FAIL / REQUEST_CHANGES**

### Summary of Findings:
- **Finding 1 [CRITICAL - Crash Vulnerability]**: `GeckoAssetResolver.java:321` throws an unhandled `ResourceLocationException` when processing malformed path strings (`invalid_namespace::path`, leading colons, spaces, uppercase), crashing the client/test suite instead of returning safe fallback locations (`DEFAULT_MODEL_LOCATION`, `DEFAULT_TEXTURE_LOCATION`, `DEFAULT_ANIMATION_LOCATION`).
- **Finding 2 [MAJOR - Normalization Bug]**: `GeckoAssetResolver.java:292` fails to update `.json` to `.geo.json` / `.animation.json` for `normalizedRelPath` when input has a `.json` extension.
- **Finding 3 [MINOR - Dead Code]**: `WereModelRenderer.java:164-211` contains unused private method `loadAndBakeGeckoModel`.

### Pass / Approved Components:
- **Build Compilation**: `./gradlew build -x test` builds `:common`, `:fabric`, `:forge` cleanly.
- **Head Rotational Transforms**: Head pitch and yaw rotation matrices are correctly applied around joint pivots in `GeckoLibWereRenderer.java`.
- **Pehkui Scaling Guard**: `PlayerRaceLayer.java` correctly guards against double-scaling when Pehkui is active.

---

## 5. Verification Method

To independently reproduce and verify this finding:

1. **Execute Unit Test Command**:
   - Command: `./gradlew test`
   - Observe task `:common:runGeckoAssetResolverTests` failure on Test 8 (`testMalformedPathInputs`).

2. **Inspect Source Code**:
   - File: `common/src/main/java/ddraig/net/customraces/client/render/GeckoAssetResolver.java`
   - Line 321: `ResourceLocation primaryLoc = !candidates.isEmpty() ? candidates.get(0) : new ResourceLocation(namespace, defaultSubfolderPrefix + filename);`
   - Observe missing `try-catch` / `ResourceLocation.tryParse` guard when constructing `primaryLoc` or returning fallback.
