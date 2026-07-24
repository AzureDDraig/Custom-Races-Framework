# Summary of Changes — Milestone 4 (Requirement R4)

## Overview
Worker M4 implemented Requirement R4 (Dynamic Body Part Model Preset Audit & Verification), completing full 9-DOF transform application (position, 3D rotation in radians, 3D safe scale), preset sub-type geometry branching, Preset #6 (Extra Legs), NBT compound tag serialization/deserialization for body part presets, color maps, and part transform maps, as well as providing interactive UI controls in `RaceCreatorScreen.java` Tab 2.

---

## 1. `PlayerRaceLayer.java`
- **9-DOF Transforms (`applyPartTransforms`)**:
  - Implemented `applyPartTransforms(PoseStack poseStack, PartTransformData pt)` to apply translation (`posX`, `posY`, `posZ`), 3D rotation (`rotPitch`, `rotYaw`, `rotRoll` converted to radians via `Math.toRadians()` and `Axis.XP/YP/ZP`), and 3D scaling using `getSafeScaleX()`, `getSafeScaleY()`, `getSafeScaleZ()`.
- **Sub-type Geometry Branching**:
  - Implemented distinct cuboid transforms and geometries for all preset sub-types:
    - **Ears (`earType`)**: `dog` (floppy), `cat` (pointed with inner ear accent), `dragon` (webbed fins), `bunny` (upright rabbit ears), and default/fallback.
    - **Horns (`hornType`)**: `demon` (steep curved backward), `ram` (wide side curling), `dragon` (swept-back spiky), `unicorn` (center forehead horn), and default/fallback.
    - **Halo (`haloType`)**: `angel` (golden floating ring), `flower` (ring with 4 petal accents), `demon` (dark spiky crown), and default/fallback.
    - **Wings (`wingType`)**: `feathered` (multi-layered feather panels), `dragon` (bat/dragon membrane with struts), and default/fallback (with animated flapping during flight).
    - **Tail (`tailType`)**: `dog` (bushy upward), `cat` (slender long), `camel` (hump/tuft accent), `fish` (tail body with caudal fin span), `dragon` (thick spiky), and default/fallback.
- **Preset #6: Extra Legs & Custom Part**:
  - Implemented rendering for Preset #6 (`legType` and `legCount`): `spider` (procedural extra leg pairs extending outward along torso sides), `centaur` (quadruped rear body torso extension and rear leg pair), and generic extra leg pairs.
  - Implemented rendering for `customPartId` ("custom" part transform key).
- **MatrixStack / PoseStack Exception Hygiene**:
  - Encapsulated all nested `pushPose()` operations within `try { ... } finally { poseStack.popPose(); }` blocks across head and body attachments, preventing matrix stack corruption or underflow/overflow leaks during rendering exceptions.

---

## 2. `RaceData.java`
- **NBT Serialization (`toNBT` & `fromNBT`)**:
  - Added full compound tag saving and reading for preset fields: `earType`, `wingType`, `tailType`, `hornType`, `haloType`, `legType`, `legCount`, and `customPartId`.
  - Added sub-tag serialization for `bodyPartColors` (RGB hex map).
  - Added sub-tag serialization for `partTransforms` map (writing and reading `posX`, `posY`, `posZ`, `rotPitch`, `rotYaw`, `rotRoll`, `scaleX`, `scaleY`, `scaleZ` per part key).

---

## 3. `RaceCreatorScreen.java`
- **Tab 2 (Part Transforms) Controls**:
  - Implemented part selection button bar across the top of Tab 2 for all 7 part keys: `Ears`, `Wings`, `Tail`, `Horns`, `Halo`, `Legs`, `Custom`.
  - Provided interactive `EditBox` inputs for the selected part:
    - **Position Offset**: `posX`, `posY`, `posZ`
    - **Rotation Degrees**: `rotPitch`, `rotYaw`, `rotRoll`
    - **Scale Multipliers**: `scaleX`, `scaleY`, `scaleZ`
  - Updated string labels in `render()` to accurately display current selected part and header labels.

---

## 4. Test Suite & Build Verification
- Created `M4PresetAuditVerificationTest.java` to test NBT roundtrips of presets, colors, and 9-DOF transform maps, as well as safe scale boundary clamping `[0.01f, 5.0f]`.
- Registered `runM4PresetAuditTests` in `common/build.gradle` and linked it to `test.dependsOn`.
- Verified Multi-Platform Build (`./gradlew build`): 0 compilation errors across `common`, `fabric`, and `forge`.
- Verified Unit Tests (`./gradlew test`): 8/8 test suites passed with 0 failures.
