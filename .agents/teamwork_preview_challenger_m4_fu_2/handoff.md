# Handoff Report — Milestone 4 Challenger 2 (PoseStack Hygiene & Multi-Platform Build Verification)

## 1. Observation

- **PoseStack Matrix Leak Observations**:
  - `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`, lines 114–128:
    ```java
    poseStack.pushPose();
    this.getParentModel().getHead().translateAndRotate(poseStack);
    renderColoredBox(...);
    ...
    poseStack.popPose();
    ```
    Line 114 executes `poseStack.pushPose()` without a `try-finally` block wrapping line 128 `poseStack.popPose()`.
  - `common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java`, lines 201–240:
    ```java
    poseStack.pushPose(); // Outer mesh push
    poseStack.pushPose(); // Head overlay push
    ...
    poseStack.popPose();
    ...
    poseStack.popPose();
    ```
    Lines 201 and 204 execute `poseStack.pushPose()` without `try-finally` blocks.
  - `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`, lines 153–196 (Preset parts comparison):
    ```java
    poseStack.pushPose();
    try {
        this.getParentModel().getHead().translateAndRotate(poseStack);
        ...
    } finally {
        poseStack.popPose();
    }
    ```
    Preset body parts (ears, horns, halo, wings, tail, legs) use `try-finally` blocks around all `pushPose()` / `popPose()` calls.

- **Empirical Test Suite Execution**:
  - Command: `./gradlew runM4Challenger2Tests`
  - Output excerpt:
    ```text
    --- Test 3: Simulated Exception in Were Beast Parts ---
      Initial depth: 1, Final depth after exception: 2
      [VULNERABILITY CONFIRMED] PoseStack depth leaked! Initial: 1, Final: 2 (Delta: +1)
    [FAIL] Test 3 (Exception in Were Beast Parts): PoseStack hygiene violation in renderWereBeastParts: Stack depth leaked +1 matrix push(es) after rendering exception!

    --- Test 4: Simulated Exception in Custom Were Mesh ---
      Initial depth: 1, Final depth after exception: 3
      [VULNERABILITY CONFIRMED] Custom Were Mesh PoseStack depth leaked! Initial: 1, Final: 3 (Delta: +2)
    [FAIL] Test 4 (Exception in Custom Were Mesh): PoseStack hygiene violation in WereModelRenderer.renderCustomWereMesh: Stack depth leaked +2 matrix push(es)!

    --- Test 5: Exception Resilience in Preset Body Parts ---
      Initial depth: 1, Final depth after protected exception: 1
    [PASS] Preset parts try-finally protection verified balanced stack restoration.
    ```

- **Multi-Platform Build Verification**:
  - Command: `./gradlew build -x test`
  - Result: `BUILD SUCCESSFUL in 20s` across Fabric and Forge submodules with 0 build errors.

---

## 2. Logic Chain

1. Observation 1 shows that `PlayerRaceLayer.renderWereBeastParts` and `WereModelRenderer.renderCustomWereMesh` execute `poseStack.pushPose()` before rendering geometry, but lack `try-finally` blocks to guarantee `poseStack.popPose()` execution if a rendering exception occurs.
2. Observation 1 also shows that `PlayerRaceLayer.renderPresetParts` correctly wraps `pushPose()` in `try-finally` blocks.
3. When an exception occurs during `renderWereBeastParts` or `renderCustomWereMesh`, control jumps directly to `PlayerRaceLayer.render()`'s `catch (Exception ignored)` block, skipping the inner `poseStack.popPose()` calls.
4. `PlayerRaceLayer.render()`'s `finally` block executes `poseStack.popPose()` exactly once for its own outer `pushPose()`.
5. As empirically verified in Observation 2, `renderWereBeastParts` leaks +1 un-popped matrix level, and `renderCustomWereMesh` leaks +2 un-popped matrix levels onto Minecraft's `PoseStack`.
6. Therefore, while normal rendering passes maintain matrix stack balance, PoseStack hygiene fails under rendering exceptions in procedural and custom Werebeast rendering routines.

---

## 3. Caveats

- **Normal Rendering Passes**: Under non-exceptional conditions, matrix push/pop calls are 1:1 balanced across all renderers.
- **Scope**: Audit focused on `PlayerRaceLayer.java` and `WereModelRenderer.java`. GUI screens (`RaceCreatorScreen.java`, `RaceSelectionScreen.java`, `AdminDashboardScreen.java`) render using `GuiGraphics` which manage their own matrix stacks.

---

## 4. Conclusion

- **PoseStack Hygiene**: **FAIL**. `renderWereBeastParts()` and `renderCustomWereMesh()` leak +1 and +2 matrix poses respectively under rendering exceptions. Mitigation requires adding `try-finally` blocks around `pushPose()` / `popPose()` calls in those two methods.
- **Multi-Platform Compilation**: **PASS**. `./gradlew build -x test` completes with 0 build errors across Fabric and Forge modules.

---

## 5. Verification Method

- Run the empirical test suite:
  ```bash
  ./gradlew runM4Challenger2Tests
  ```
- Run full multi-platform build:
  ```bash
  ./gradlew build -x test
  ```
- Inspect test source file:
  `common/src/test/java/ddraig/net/customraces/client/render/M4PoseStackHygieneTest.java`
