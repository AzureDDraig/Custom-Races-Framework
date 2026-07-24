# Summary of Changes — M4 Remediation

## 1. PoseStack Hygiene Fixes
- **`common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`**:
  - In `renderWereBeastParts(...)`: Wrapped `poseStack.pushPose()` and `poseStack.popPose()` in a `try { ... } finally { poseStack.popPose(); }` block.
  - Guarantees matrix stack restoration even if rendering or vertex builder calls throw an exception.
- **`common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java`**:
  - In `renderCustomWereMesh(...)`: Wrapped outer `pushPose()` and head/limb overlay `pushPose()` blocks in `try { ... } finally { poseStack.popPose(); }`.
  - Ensures outer and inner pose pushes are safely popped on any exception during box/vertex buffer rendering.

## 2. Float.NaN Scale Clamping
- **`common/src/main/java/ddraig/net/customraces/data/PartTransformData.java`**:
  - Updated `getSafeScaleX()`, `getSafeScaleY()`, `getSafeScaleZ()`.
  - Added explicit NaN checks: `if (Float.isNaN(scaleX) || scaleX <= 0.0f) return 1.0f;` (and corresponding checks for `scaleY` and `scaleZ`).
  - Prevents `Float.NaN` values from evaluating `scale <= 0` as `false` and escaping `Math.min`/`Math.max` clamping.

## 3. Test Suite Compilation & Verification Fix
- **`common/src/test/java/ddraig/net/customraces/client/render/M4PoseStackHygieneTest.java`**:
  - Updated simulated test routines `testExceptionInWereBeastParts()` and `testExceptionInCustomWereMesh()` to incorporate the remediated `try-finally` blocks.
  - Confirmed test 3 and test 4 now verify zero matrix depth leaks under simulated rendering exceptions.
