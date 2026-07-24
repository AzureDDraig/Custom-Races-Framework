# Adversarial Challenge Report — Milestone 2 (R1 Texture Resolution & Client Resource Manager)

## Challenge Summary

**Overall risk assessment**: MEDIUM

While the baseline texture resolution ladder and keyword interception logic in `WereModelRenderer.java` perform well under typical inputs, empirical stress testing revealed two notable failure modes and edge-case risks in resource lookup and namespace parsing.

---

## Challenges

### [Medium] Challenge 1: Exception Suppressing in `isResourcePresentOnClient` Returns `true` on Resource Lookup Errors

- **Assumption challenged**: `isResourcePresentOnClient` assumes that catching `Throwable` means the resource exists, returning `true` by default when `getResource(loc)` throws an exception.
- **Attack scenario**: If a resource pack or custom texture location triggers an exception inside `mc.getResourceManager().getResource(loc)` (e.g., due to corrupt resource pack zip stream, forbidden character in asset lookup, or unhandled runtime error in resource pack wrapper), the `try-catch` block catches `Throwable` and falls through to `return true;`.
- **Blast radius**: `WereModelRenderer` is falsely informed that the missing or corrupted texture IS present on client. It bypasses the safe fallback ladder (`getSafeDefaultTexture`), passing the invalid `ResourceLocation` to `RenderType.entityCutoutNoCull(textureLoc)`. This causes rendering failure (purple/black missing texture grid or client render exception).
- **Mitigation**: Update `isResourcePresentOnClient` so that catching `Throwable` or failing to retrieve resource returns `false` when `mc != null && mc.getResourceManager() != null`.

```java
public static boolean isResourcePresentOnClient(ResourceLocation loc) {
    if (loc == null) return false;
    try {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.getResourceManager() != null) {
            return mc.getResourceManager().getResource(loc).isPresent();
        }
    } catch (Throwable ignored) {
        return false; // Return false on resource lookup failure when client exists
    }
    return true;
}
```

### [Low] Challenge 2: Leading Colon Namespace Parse Defaults to `minecraft` Namespace

- **Assumption challenged**: Input path starting with a colon (e.g. `":no_namespace"`) is assumed to be an invalid path syntax that falls back to default.
- **Attack scenario**: If a user inputs `:werewolf` or `:textures/were/werewolf.png`, `colonIndex` evaluates to `0`. `path.substring(0, 0)` sets `namespace = ""`. Line 99 constructs `ResourceLocation.tryParse(":textures/werewolf.png")`. Minecraft's `ResourceLocation` parser defaults an empty namespace to `"minecraft"`, resolving to `minecraft:textures/werewolf.png` instead of `customraces:textures/werewolf.png` or triggering fallback.
- **Blast radius**: Unexpected cross-namespace leak where empty namespace inputs quietly attempt to load from vanilla `minecraft:` domain instead of mod domain `customraces:`.
- **Mitigation**: Check if `namespace.trim().isEmpty()` when `colonIndex >= 0`; if empty, treat as invalid syntax or set `namespace = "customraces"`.

---

## Stress Test Results

- **Keyword Interception ("skin", "player", "player_skin", "skin_texture") with active skin** → Expected: `player.getSkinTextureLocation()` → Actual: `minecraft:textures/entity/player/slim/alex.png` → **PASS**
- **Keyword Interception with null player / offline skin** → Expected: `DEFAULT_WERE_TEXTURE` → Actual: `customraces:textures/were/default_werewolf.png` → **PASS**
- **Path Normalization Shorthand ("werewolf", "were/wolf")** → Expected: `customraces:textures/...png` → Actual: `customraces:textures/werewolf.png`, `customraces:textures/were/wolf.png` → **PASS**
- **Custom Namespace ("mymod:werewolf")** → Expected: `mymod:textures/werewolf.png` → Actual: `mymod:textures/werewolf.png` → **PASS**
- **Invalid Syntax (Uppercase "UpperMod:wolf", spaces "mod:a b", special chars "mod:a#b")** → Expected: `DEFAULT_WERE_TEXTURE` → Actual: `customraces:textures/were/default_werewolf.png` → **PASS**
- **Overloaded Method Delegation `getValidWereTextureLocation(race)`** → Expected: delegates to `getValidWereTextureLocation(null, race)` → Actual: identical output → **PASS**
- **Deduplication Stress (1,000 consecutive invalid path resolutions)** → Expected: 1 warning logged, zero memory leak → Actual: Set deduplication worked cleanly → **PASS**
- **Client ResourceManager missing asset lookup exception (`getResource` throwing)** → Expected: `isResourcePresentOnClient` returns `false` → Actual: returns `true` on exception → **FAIL (CHALLENGE 1)**
- **Leading Colon Path Input (`":no_namespace"`)** → Expected: `customraces:textures/...` or fallback → Actual: resolves to `minecraft:textures/no_namespace.png` → **FAIL (CHALLENGE 2)**

---

## Unchallenged Areas

- **GPU Buffer & Shader Rendering (`RenderType.entityCutoutNoCull`)**: Out of scope for headless unit test runner; requires full rendering pipeline.
- **Pehkui Render Layer Hooks**: Covered in separate integration suite.
