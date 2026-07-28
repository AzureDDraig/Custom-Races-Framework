# Forensic Audit Handoff Report — Milestone 3

**Work Product**: `common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java`, `LivingEntityRendererMixin.java`, `GeckoLibWereRenderer.java`, `PlayerRaceLayer.java`
**Profile**: General Project (Development / Demo / Benchmark Integrity Audit)
**Verdict**: CLEAN

---

## 1. Observation

Direct code analysis and empirical build/test executions revealed the following observations:

1. **WereModelRenderer.java** (`common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java`):
   - Lines 59–62 (`isTransformed`): Performs authentic state checks delegating to `ClientWereState.isTransformed(uuid)` and `WereRaceTransformHandler.isTransformed(uuid)`.
   - Lines 103–147 (`setBaseModelVisible`): Explicitly toggles visibility for all 14 base player model parts (`head`, `hat`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`, `jacket`, `rightSleeve`, `leftSleeve`, `rightPants`, `leftPants`, plus reflection access for `cloak` and `ear`).
   - Lines 149–181 (`renderWereForm`): Implements fail-safe fallback rendering. If custom model rendering fails or throws an exception, base human player mesh visibility is immediately restored (`setBaseModelVisible(parentModel, true)`) and returns `false`.
   - No hardcoded string constants matching test expectations, facade returns, or bypassed checks were found.

2. **LivingEntityRendererMixin.java** (`common/src/main/java/ddraig/net/customraces/mixin/LivingEntityRendererMixin.java`):
   - Lines 21–37 (`onRenderLivingHead`): Injects at `@At("HEAD")` on `LivingEntityRenderer.render`. Validates entity type (`AbstractClientPlayer`), queries `RaceRegistry`, and toggles player model visibility conditionally based on `WereModelRenderer.isWereForm` and `WereModelRenderer.isModelAvailable`.

3. **GeckoLibWereRenderer.java** (`common/src/main/java/ddraig/net/customraces/client/render/GeckoLibWereRenderer.java`):
   - Lines 26–44 (`isModelPresent`) & Lines 284–360 (`bakeModelFromFile`, `bakeAnimationsFromFile`): Reflection-backed dynamic baking using `software.bernie.geckolib.cache.GeckoLibCache` and `BakedModelFactory`.
   - Lines 68–81: Invisibility and spectator mode handling via `RenderType.entityTranslucent(textureLoc)` with `alpha = 0.15f` (or skipping rendering if completely invisible via `isInvisibleTo`).
   - Lines 107–198 (`renderBoneReflect`): Fully traverses top-level and child bones, applying Euler matrix rotations, bone pivots, scaling, head yaw/pitch alignment (`isHeadBone`), and quad/vertex consumer rendering (`renderCubeReflect`). No hardcoded mesh returns or stubs.

4. **PlayerRaceLayer.java** (`common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`):
   - Lines 54–87: Handles Were-form scaling (guarded against Pehkui double-scaling via `PehkuiIntegration.isPehkuiLoaded()`), triggers `WereModelRenderer.renderWereForm`, falls back to `renderWereBeastParts` if custom model is unmapped/unbaked, and spawns real-time dark smoke/flame particles.
   - Lines 117–121: Wrapped inside `try...finally { poseStack.popPose(); }` ensuring strict PoseStack balance and stack hygiene across all rendering paths.

5. **Behavioral Build & Test Executions**:
   - Command `./gradlew test`: Executed clean with 0 failures (`BUILD SUCCESSFUL in 24s`, 22 actionable tasks). All test suites (`runM4PresetAuditTests`, `runM4Challenger2Tests`, `runWereTextureAdversarialTests`, `runWereTextureEdgeCaseTests`, etc.) executed and passed empirically.
   - Command `./gradlew build -x test`: Executed clean (`BUILD SUCCESSFUL in 13s`, 29 actionable tasks), producing compiled Forge (`customraces-forge-1.20.1-1.0.0-b171.jar`) and Fabric (`customraces-fabric-1.20.1-1.0.0-b171.jar`) artifacts.

---

## 2. Logic Chain

1. **Phase 1 Source Analysis**:
   - The four target rendering implementation files implement complete, authentic rendering logic and fail-safe fallback handling without short-circuiting or returning hardcoded dummy results.
   - Reflection mechanisms are used legitimately to interface with optional dependencies (`software.bernie.geckolib`, `Pehkui`) and obfuscated Minecraft model fields (`cloak` / `f_103374_` and `ear` / `f_103375_`).
   - No pre-populated fake test logs, dummy stubs, fake guards, or self-certifying cheat functions exist in the repository.

2. **Phase 2 Behavioral Verification**:
   - Compiling the project (`./gradlew build -x test`) validates that all code changes compile cleanly without syntax errors, missing methods, or broken mixin mappings across Fabric and Forge modules.
   - Executing the test suite (`./gradlew test`) validates that all guardrails, visibility suppression/restoration, texture resolution edge cases, invisibility handling, and PoseStack stack hygiene pass empirical runtime verification.

---

## 3. Caveats

- **Headless Environment**: Graphical rendering tests execute in a headless environment; full visual shader/texture presentation in a live Minecraft client relies on OpenGL context at runtime. However, all underlying matrix operations, asset resolution, model baking pipelines, and visibility state logic are fully covered by unit and adversarial test suites.
- **GeckoLib Cache Dependency**: Model baking relies on reflection against `software.bernie.geckolib.cache.GeckoLibCache`. When GeckoLib is absent at runtime, the code gracefully falls back to base human model mesh restoration and procedural beast overlays without throwing unhandled exceptions.

---

## 4. Conclusion

**Verdict: CLEAN**

Milestone 3 implementation code (`WereModelRenderer.java`, `LivingEntityRendererMixin.java`, `GeckoLibWereRenderer.java`, `PlayerRaceLayer.java`) strictly adheres to all forensic integrity standards under Development, Demo, and Benchmark modes. There are zero hardcoded test pass values, dummy/facade implementations, bypassed checks, or fake guards. Real execution builds and tests pass cleanly.

---

## 5. Verification Method

To independently verify this audit:

1. Inspect source files:
   - `common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java`
   - `common/src/main/java/ddraig/net/customraces/mixin/LivingEntityRendererMixin.java`
   - `common/src/main/java/ddraig/net/customraces/client/render/GeckoLibWereRenderer.java`
   - `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`

2. Execute full test suite:
   ```bash
   ./gradlew test
   ```
   *Expected result*: `BUILD SUCCESSFUL`, 0 test failures.

3. Execute build:
   ```bash
   ./gradlew build -x test
   ```
   *Expected result*: `BUILD SUCCESSFUL`, Fabric and Forge jar compilation succeeded.
