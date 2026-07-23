# Adversarial Challenge Report — IronSpellsHandler Reflection Implementation

**Target File**: `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java`  
**Reviewer**: Challenger 1 (M2 Adversarial Verification)  
**Overall Risk Assessment**: MEDIUM  

## Executive Summary
An adversarial evaluation of `IronSpellsHandler.java` was conducted to test soft-reflection safety, missing mod scenarios, malformed spell IDs, parameter scoring logic, type coercion, and thread safety.

Empirical test harnesses and static analysis confirmed that basic soft-reflection degrades gracefully without throwing unhandled `ClassNotFoundException`s when Iron's Spells is missing. However, **5 concrete bugs and failure modes** were discovered in parameter reflection, mod detection scope, class loading edge cases, thread safety, and attribute cleanup.

---

## Challenges & Failure Modes

### 1. [HIGH] Argument Type Mismatch on Primitive Parameter Types (Non-Integer Primitives)
- **Assumption Challenged**: `invokeSpellCast` assumes all parameter types of spell methods fall into known Minecraft/IronSpells object classes or primitive `int`.
- **Attack Scenario**: If a spell's `onCast` or `castSpell` method signature includes non-integer primitive parameter types (e.g. `float`, `double`, `boolean`, `long`, `short`, `byte`, or `char`, such as `onCast(Level level, int spellLevel, LivingEntity entity, float damageMultiplier)`):
  - In `invokeSpellCast` (line 538), unassigned parameters default to `args[i] = null`.
  - Passing `null` to a primitive parameter during `m.invoke(spellObj, args)` throws `java.lang.IllegalArgumentException: argument type mismatch`.
- **Blast Radius**: Any spell in Iron's Spells or third-party add-on mods using primitive parameters other than `int` fails to cast with an uncaught `IllegalArgumentException` during reflection invocation.
- **Mitigation**: Handle primitive default values in parameter matching (e.g. `0.0f` for float, `false` for boolean, `0.0` for double) or filter out methods requiring unsupported primitive arguments.

### 2. [MEDIUM] Mod Availability Check Omits T.O. Tweaks Mod Scope
- **Assumption Challenged**: `isIronSpellsLoaded()` checks `Platform.isModLoaded("irons_spellbooks")` and is used as the sole gatekeeper for spell casting.
- **Attack Scenario**: `ALL_SPELLS` contains spells from T.O. Tweaks (`totweaks:time_stop`, `totweaks:spatial_rend`, etc.).
  - If `totweaks` is installed without `irons_spellbooks`, `isIronSpellsLoaded()` returns `false`, preventing `castNativeSpell` from attempting resolution and displaying `(Requires Iron's Spells mod)` even though T.O. Tweaks is present.
  - Furthermore, `resolveSpellObject` only inspects Iron's Spells registry classes (`net.ironsspellbooks.api.registry.SpellRegistry`, etc.) and does not inspect T.O. Tweaks registry classes (`totweaks.*`).
- **Blast Radius**: T.O. Tweaks spells cannot be resolved unless T.O. Tweaks registers them into Iron's Spells' `SpellRegistry`.
- **Mitigation**: Update `isIronSpellsLoaded()` or introduce `isSpellModLoaded(String namespace)` and include T.O. Tweaks registry class lookups in `resolveSpellObject`.

### 3. [MEDIUM] Unsynchronized `MODIFIER_UUIDS` Map Subject to Thread Corruption
- **Assumption Challenged**: `applyIronSpellsAttributes` runs strictly on single-threaded synchronous ticks.
- **Attack Scenario**: `MODIFIER_UUIDS` is initialized as a plain `java.util.HashMap`:
  `private static final Map<String, UUID> MODIFIER_UUIDS = new HashMap<>();`
  `applyIronSpellsAttributes` calls `MODIFIER_UUIDS.computeIfAbsent(...)`. If passive updates occur across threads (e.g., async network packets, multi-threaded player sync), concurrent modification of `HashMap` can cause data races or infinite loop rehash bugs in Java's `HashMap`.
- **Blast Radius**: Server thread lock or JVM crash under concurrent player passive updates.
- **Mitigation**: Replace `HashMap` with `ConcurrentHashMap`.

### 4. [LOW] Malformed ResourceLocation Uncaught Exception in `resolveSpellObject`
- **Assumption Challenged**: `resolveSpellObject` handles all string inputs safely without throwing exceptions.
- **Attack Scenario**: `new net.minecraft.resources.ResourceLocation(spellId)` is called at line 140 before any try-catch block inside `resolveSpellObject`. If `spellId` contains uppercase letters, spaces, or invalid characters (e.g. `"irons_spellbooks:FireBall"`), `new ResourceLocation` throws `net.minecraft.ResourceLocationException`. While `castNativeSpell` wraps `resolveSpellObject` in a try-catch block, calling `resolveSpellObject` directly from external callers causes an uncaught `ResourceLocationException`.
- **Blast Radius**: Uncaught runtime exception if `resolveSpellObject` is invoked directly with raw user input.
- **Mitigation**: Wrap `new ResourceLocation(...)` or validate/lowercase `spellId` with `spellId.toLowerCase(Locale.ROOT)` before instantiating `ResourceLocation`.

### 5. [LOW] Passive Attribute Modifier Leak on Race Switch
- **Assumption Challenged**: `applyIronSpellsAttributes` handles both addition and removal of attribute modifiers.
- **Attack Scenario**: `applyIronSpellsAttributes` only adds attribute modifiers for active passives (`inst.addTransientModifier(...)`). If a player switches races or loses a passive, previous passive attribute modifiers are never removed.
- **Blast Radius**: Attribute modifiers persist indefinitely on the player entity until relog/respawn.
- **Mitigation**: Implement cleanup logic to remove stale Custom Races attribute modifiers when passives change.

---

## Stress Test Results

| Test Scenario | Expected Behavior | Actual Behavior | Pass/Fail |
|---|---|---|---|
| Mod Missing Soft-Reflection | Safe degradation, no `ClassNotFoundException` | Safely degrades, prints requirement message | PASS |
| Null / Invalid Spell ID | Safe return or error message | Safely handled in `castNativeSpell` | PASS |
| Non-Integer Primitive Method Params | Fallback or default handling | Throws `IllegalArgumentException` on `m.invoke` | **FAIL** |
| Malformed ResourceLocation String | Handled inside `resolveSpellObject` | Throws uncaught `ResourceLocationException` in `resolveSpellObject` | **FAIL** |
| Unsynchronized HashMap Access | Thread-safe concurrent access | Non-thread-safe `HashMap` | **FAIL** |

---

## Unchallenged Areas
- Core Minecraft 1.20.1 particle rendering fallback (verified standard `ServerLevel` particle calls).
