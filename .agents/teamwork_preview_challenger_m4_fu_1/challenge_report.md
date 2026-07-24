# Milestone 4 Adversarial Challenge Report: Requirement R4 & Transform / NBT Verification

**Challenger**: Challenger 1 (Milestone 4 — Body Part Transforms, Presets & NBT Serialization)  
**Date**: 2026-07-24  
**Working Directory**: `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_challenger_m4_fu_1`  
**Target Files**: `PartTransformData.java`, `RaceData.java`, `PlayerRaceLayer.java`

---

## Challenge Summary

**Overall risk assessment**: MEDIUM  
**Verdict**: CONCERNS IDENTIFIED (1 Security/Robustness Flaw in Scale Clamping; NBT Serialization & Preset Logic Passed 100%)

---

## Challenges & Empirical Findings

### [Medium/High] Challenge 1: `Float.NaN` Escapes `PartTransformData` Safe Scale Clamping

- **Assumption challenged**: `PartTransformData.getSafeScaleX()`, `getSafeScaleY()`, and `getSafeScaleZ()` sanitize any float input into the safe scale range `[0.01f, 5.0f]`.
- **Attack scenario**:
  An invalid or corrupted config/NBT entry containing `NaN` (or a mathematical operation resulting in `Float.NaN`) is set in `PartTransformData.scaleX` (or `scaleY`, `scaleZ`).
- **Empirical Execution & Trace**:
  In `PartTransformData.java` (lines 31–41):
  ```java
  public float getSafeScaleX() {
      return Math.max(0.01f, Math.min(5.0f, scaleX <= 0 ? 1.0f : scaleX));
  }
  ```
  1. In Java IEEE 754 float comparison semantics, `Float.NaN <= 0` evaluates to `false`.
  2. The ternary expression `scaleX <= 0 ? 1.0f : scaleX` evaluates to `Float.NaN`.
  3. `Math.min(5.0f, Float.NaN)` returns `Float.NaN` according to standard Java `Math.min(float, float)` specification.
  4. `Math.max(0.01f, Float.NaN)` returns `Float.NaN`.
  5. `getSafeScaleX()` returns `Float.NaN`!
- **Blast Radius**:
  When `PlayerRaceLayer` passes `Float.NaN` to `poseStack.scale(pt.getSafeScaleX(), pt.getSafeScaleY(), pt.getSafeScaleZ())`, all matrix transformation entries in `PoseStack` become `NaN`. This corrupts rendering of player models, causing invisible entities, black boxes, or rendering pipeline failure.
- **Empirical Proof**:
  Verified in `M4Challenger1AdversarialTest.java` (Test 4):
  `pt.scaleX = Float.NaN; Float.isNaN(pt.getSafeScaleX())` returned `true`!
- **Mitigation**:
  In `PartTransformData.java`, explicitly check `Float.isNaN(...)`:
  ```java
  public float getSafeScaleX() {
      if (Float.isNaN(scaleX) || scaleX <= 0.0f) return 1.0f;
      return Math.max(0.01f, Math.min(5.0f, scaleX));
  }
  public float getSafeScaleY() {
      if (Float.isNaN(scaleY) || scaleY <= 0.0f) return 1.0f;
      return Math.max(0.01f, Math.min(5.0f, scaleY));
  }
  public float getSafeScaleZ() {
      if (Float.isNaN(scaleZ) || scaleZ <= 0.0f) return 1.0f;
      return Math.max(0.01f, Math.min(5.0f, scaleZ));
  }
  ```

---

### [Low/Informational] Challenge 2: Rotation Fields Lack Range Clamping

- **Assumption challenged**: Rotation angles (`rotPitch`, `rotYaw`, `rotRoll`) are clamped or sanitized against NaN.
- **Attack scenario**:
  `rotPitch = Float.NaN` or `rotPitch = 1e9f` set in NBT or GUI.
- **Blast Radius**:
  `Math.toRadians(Float.NaN)` yields `NaN`, which when passed to `Axis.XP.rotation(...)` produces a matrix with NaN components.
- **Mitigation**:
  Add rotation safety helpers or sanitize NaN in `applyPartTransforms`.

---

## Stress Test Results

| Scenario | Input | Expected | Actual Result | Status |
|---|---|---|---|---|
| Zero Scale Clamping | `scaleX = 0.0f` | `1.0f` | `1.0f` | **PASS** |
| Negative Scale Clamping | `scaleX = -5.0f` | `1.0f` | `1.0f` | **PASS** |
| Sub-minimum Scale | `scaleX = 0.0001f` | `0.01f` | `0.01f` | **PASS** |
| Super-maximum Scale | `scaleX = 100.0f` | `5.0f` | `5.0f` | **PASS** |
| Positive Infinity Scale | `scaleX = Float.POSITIVE_INFINITY` | `5.0f` | `5.0f` | **PASS** |
| Negative Infinity Scale | `scaleX = Float.NEGATIVE_INFINITY` | `1.0f` | `1.0f` | **PASS** |
| **NaN Scale Clamping** | `scaleX = Float.NaN` | `1.0f` / `0.01f` | `Float.NaN` | **FAIL (BUG CONFIRMED)** |
| 6 Body Part Presets NBT Roundtrip | All preset strings (`dog`, `cat`, `dragon`, `bunny`, `feathered`, `demon`, `ram`, `unicorn`, `angel`, `flower`) | 100% restored | 100% restored | **PASS** |
| Leg Types & Counts NBT Roundtrip | `spider`, `centaur`, `legCount` 0..8, -2, 100 | 100% restored | 100% restored | **PASS** |
| Custom Part ID & Colors NBT Roundtrip | `customPartId`, `#RRGGBB` hex map | 100% restored | 100% restored | **PASS** |
| 9-DOF Part Transforms Map NBT Roundtrip | Position, Rotation, Scale per part key | 100% restored (<0.001 delta) | 100% restored (<0.001 delta) | **PASS** |
| Null & Empty Tag NBT Robustness | `fromNBT(null)`, `fromNBT(emptyTag)`, `toNBT(null)` | No NPE, safe fallbacks | Safe fallbacks, no NPE | **PASS** |
| Null Field Fallbacks during Serialization | `earType=null`, `legType=null`, `customPartId=null` | Default fallbacks written to NBT | Default fallbacks written | **PASS** |

---

## Unchallenged Areas

- **Client GUI Text Input Validation**: `RaceCreatorScreen` text field inputs were not tested for live keyboard entry sanitization (handled in separate GUI test suite).
