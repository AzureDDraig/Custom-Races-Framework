# Milestone 4 Adversarial Audit & Build Verification Report
**Target**: Requirement R4 (PoseStack Hygiene & Matrix Balance) and Multi-Platform Compilation Verification  
**Auditor**: Empirical Challenger 2  
**Date**: 2026-07-24  

---

## Executive Summary

- **PoseStack Hygiene Assessment**: **FAIL (HIGH / CRITICAL RISK)** under simulated rendering exceptions.
- **Multi-Platform Compilation Assessment**: **PASS (0 Build Errors)** across Fabric and Forge modules.
- **Empirical Test Suite**: `M4PoseStackHygieneTest.java` written and executed via Gradle (`./gradlew runM4Challenger2Tests`).

While normal rendering passes restore matrix stack balance, `PlayerRaceLayer.java` (`renderWereBeastParts`) and `WereModelRenderer.java` (`renderCustomWereMesh`) lack `try-finally` protection around intermediate matrix stack pushes (`poseStack.pushPose()`). When rendering exceptions occur (e.g. invalid texture, buffer stream error, NPE in JOML matrix operations), inner `popPose()` calls are bypassed. The outer catch block in `PlayerRaceLayer.render()` only pops 1 outer pose, leaking **+1 to +2 un-popped matrix levels** onto Minecraft's `PoseStack`.

---

## Challenge Summary

**Overall Risk Assessment**: **HIGH** (PoseStack pollution under rendering exceptions causing matrix stack overflow / client render pipeline corruption).

---

## Detailed Challenges

### [HIGH] Challenge 1: Unbalanced Matrix Push Leak in `PlayerRaceLayer.renderWereBeastParts`

- **Assumption Challenged**: Assumed that catching exceptions at the top level of `PlayerRaceLayer.render()` safely cleans up all matrix pushes made during beast part rendering.
- **Attack Scenario**:
  1. Player transforms into procedural Werebeast form (`isWereTransformed = true`, `hasCustomModel = false`).
  2. `PlayerRaceLayer.render()` pushes Pose 1 (outer render pose, line 39).
  3. `renderWereBeastParts()` is called and pushes Pose 2 (line 114).
  4. An exception is thrown during line 118 (`renderColoredBox` -> `buffer.getBuffer` or matrix operations).
  5. Line 128 `poseStack.popPose()` is skipped due to the exception.
  6. Exception is caught by `catch (Exception ignored)` at line 105 in `PlayerRaceLayer.render()`.
  7. Outer `finally` block executes `poseStack.popPose()` (line 107) once.
- **Blast Radius**: Pose 2 remains un-popped on `PoseStack` (`Delta: +1`). Over subsequent rendering frames or entity layers, the matrix stack leaks un-popped matrix states, causing visual matrix corruption, screen skewing, offset entity rendering, or `IllegalStateException: Stack overflow` in `PoseStack`.
- **Empirical Proof**: `M4PoseStackHygieneTest.testExceptionInWereBeastParts` failed with `AssertionError: Stack depth leaked +1 matrix push(es)`.
- **Mitigation**: Wrap `renderWereBeastParts` push/pop sequence in a `try-finally` block:
  ```java
  poseStack.pushPose();
  try {
      this.getParentModel().getHead().translateAndRotate(poseStack);
      renderColoredBox(...);
  } finally {
      poseStack.popPose();
  }
  ```

---

### [HIGH] Challenge 2: Double Matrix Push Leak in `WereModelRenderer.renderCustomWereMesh`

- **Assumption Challenged**: Assumed that custom Werebeast mesh rendering in `WereModelRenderer.java` restores matrix balance if an exception occurs during mesh overlay rendering.
- **Attack Scenario**:
  1. Player transforms into custom Werebeast form (`isWereTransformed = true`, `hasCustomModel = true`).
  2. `PlayerRaceLayer.render()` pushes Pose 1 (outer render pose, line 39).
  3. `WereModelRenderer.renderWereForm()` -> `renderCustomWereMesh()` is called.
  4. `renderCustomWereMesh()` pushes Pose 2 (outer mesh, line 201) and Pose 3 (head overlay, line 204).
  5. An exception occurs during `renderBox()` for the head overlay (line 206).
  6. Lines 212 and 240 `poseStack.popPose()` are skipped.
  7. Outer `PlayerRaceLayer.render()` finally block executes `poseStack.popPose()` ONCE.
- **Blast Radius**: Poses 2 and 3 remain un-popped on `PoseStack` (`Delta: +2`). Matrix stack accumulates +2 unbalanced matrix pushes per rendering exception, accelerating matrix stack corruption.
- **Empirical Proof**: `M4PoseStackHygieneTest.testExceptionInCustomWereMesh` failed with `AssertionError: Stack depth leaked +2 matrix push(es)`.
- **Mitigation**: Refactor `renderCustomWereMesh` to wrap each mesh section (`head`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`) in defensive `try-finally` blocks around `pushPose()` and `popPose()`.

---

## Stress Test Results

| Test Scenario | Location | Expected Behavior | Actual Behavior | Result |
|---|---|---|---|---|
| Test 1: Normal Human PoseStack Balance | `PlayerRaceLayer.java` | Depth returns to initial state (1) | Initial: 1, Final: 1 | **PASS** |
| Test 2: Normal Were Procedural Balance | `PlayerRaceLayer.java` | Depth returns to initial state (1) | Initial: 1, Final: 1 | **PASS** |
| Test 3: Exception in Were Beast Parts | `PlayerRaceLayer.java:114-128` | Depth returns to initial state (1) | Initial: 1, Final: 2 (`+1` leak) | **FAIL** |
| Test 4: Exception in Custom Were Mesh | `WereModelRenderer.java:201-240` | Depth returns to initial state (1) | Initial: 1, Final: 3 (`+2` leak) | **FAIL** |
| Test 5: Exception in Preset Body Parts | `PlayerRaceLayer.java:153-279` | Depth returns to initial state (1) | Initial: 1, Final: 1 | **PASS** |

---

## Multi-Platform Build Verification

- **Command**: `./gradlew build -x test`
- **Result**: **BUILD SUCCESSFUL** in 20s
- **Actionable Tasks**: 30 actionable tasks executed cleanly across Common, Fabric, and Forge submodules.
- **Build Error Count**: 0 build errors.

---

## Unchallenged Areas

- **Particle Spawning Performance**: Verified that particle spawning logic (`addParticle`) does not alter `PoseStack` state.
- **Pehkui Scaling Application**: Scaling operations (`poseStack.scale`) mutate the current matrix pose without pushing/popping, so scale math itself does not leak matrix depth.
