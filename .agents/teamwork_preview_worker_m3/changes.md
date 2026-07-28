# Milestone 3 Code Changes — Base Human Player Model Suppression Guardrails (R2)

## 1. `common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java`
- **Extended `setBaseModelVisible`**:
  - Added reflection-backed field suppression for `model.cloak` (Cape) and `model.ear` (Deadmau5 ears) alongside the standard 12 parts (`head`, `hat`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`, `jacket`, `rightSleeve`, `leftSleeve`, `rightPants`, `leftPants`). Included obfuscation mapping fallback (`f_103374_` and `f_103375_`).
- **Enhanced `isModelAvailable`**:
  - Delegated model presence checking to `GeckoLibWereRenderer.isModelPresent(loc, race.wereModelPath)` to verify model file existence, dynamic baking, and non-empty `topLevelBones`.
- **Fail-Safe Fallback in `renderWereForm`**:
  - Wrapped `renderGeckoLibWereModel` in try-catch.
  - If GeckoLib model loading, baking, or bone traversal fails or returns `false`, `renderWereForm` restores `setBaseModelVisible(parentModel, true)` and returns `false`, triggering procedural beast features (`renderWereBeastParts`) in `PlayerRaceLayer` so players are NEVER invisible.

## 2. `common/src/main/java/ddraig/net/customraces/client/render/GeckoLibWereRenderer.java`
- **Bone Structure Guardrail in `isModelPresent`**:
  - Updated `isModelPresent(modelLoc, rawPath)` to verify that baked models are non-null and `topLevelBones()` returns a non-null, non-empty list of top-level bones.
- **Invisibility Effect & Spectator Mode Handling**:
  - Checked `player.isInvisible()` and `player.isSpectator()`.
  - Checked `player.isInvisibleTo(clientPlayer)`:
    - If true (completely invisible to local viewing client), returns `true` without drawing any model geometry.
    - If false (visible as translucent to spectators/team members), uses `RenderType.entityTranslucent(textureLoc)` buffer rendering and sets vertex color alpha to `0.15f` (translucent).

## 3. `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`
- **Invisibility & Spectator Mode Handling**:
  - Checked `player.isInvisible()` and `player.isSpectator()`.
  - If `player.isInvisibleTo(clientPlayer)` is `true`, returns early from `render()` without rendering preset body parts or spawning smoke/aura particles.
  - When rendering procedural beast parts (`renderWereBeastParts`) or preset parts (`renderPresetParts`) while invisible/spectator (and visible to team/spectator), uses `RenderType.entityTranslucent(WHITE_TEXTURE)` and scales vertex alpha by `0.15f` (`baseAlpha`).

## 4. `common/src/test/java/ddraig/net/customraces/client/render/M3SuppressionAndFallbackVerificationTest.java`
- Created unit test suite verifying:
  - 14-part base model visibility toggling (`setBaseModelVisible`).
  - Model availability guardrails (`isModelAvailable`).
  - Fail-safe fallback visibility restoration (`renderWereForm`).
  - Model presence & top-level bone structure checks (`isModelPresent`).
  - Multi-threaded concurrent suppression thread safety.
- Registered test task `runM3SuppressionAndFallbackTests` in `common/build.gradle`.
