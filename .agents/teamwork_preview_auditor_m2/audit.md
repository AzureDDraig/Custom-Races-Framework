# Forensic Audit Report — M2 Integrity Audit

**Work Product**: `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java`  
**Profile**: General Project  
**Audit Date**: 2026-07-23  
**Verdict**: CLEAN  

---

## Executive Summary

A forensic integrity audit was conducted on `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java`. The objective was to verify that the implementation contains genuine reflection logic, proper spell object resolution, robust invocation mechanics, and dynamic attribute modifiers without any hardcoded test shortcuts, dummy returns, facade methods, or fake implementations.

---

## Forensic Check Results

| Check # | Check Name | Result | Observations & Details |
|---|---|---|---|
| 1 | Hardcoded Output Detection | **PASS** | No hardcoded test strings, fixed return values, or dummy spell objects were found. |
| 2 | Facade Detection | **PASS** | All methods contain complete, genuine logic for soft-reflection loading, spell unwrapping, parameter binding, and attribute modification. |
| 3 | Pre-populated Artifact Check | **PASS** | No pre-existing log files, test results, or verification output artifacts exist in the project workspace. |
| 4 | Self-Certifying Test Check | **PASS** | Code relies on standard Minecraft/Architectury APIs and dynamic Java reflection without self-certifying shortcuts. |
| 5 | Reflection & Unwrapping Integrity | **PASS** | Multi-tiered reflection lookup strategy with fallback registries, `Holder`/`Supplier`/`Optional` unwrapping, and `VoidSpell`/`NoneSpell` rejection. |
| 6 | Spell Invocation Integrity | **PASS** | Reflectively resolves `onCast`/`castSpell`/`onCastSpell`, ranks methods by parameter match score, maps parameters (`Level`, `Player`, `spellLevel`, `CastSource`, `MagicData`), and handles exceptions with stack traces. |
| 7 | Attribute Modification Integrity | **PASS** | Reflectively accesses `AttributeRegistry`, maps race passive keys to spell attributes (`MAX_MANA`, `MANA_REGEN`, etc.), and applies transient `AttributeModifier`s using deterministic UUIDs. |

---

## Detailed Code Analysis

### 1. Mod Presence Check (`isIronSpellsLoaded`)
- Uses `dev.architectury.platform.Platform.isModLoaded("irons_spellbooks")` to check for Iron's Spells 'n Spellbooks dynamically at runtime.

### 2. Native Spell Execution (`castNativeSpell`)
- Validates player and race inputs.
- Handles Wild Magic selection dynamically from `ALL_SPELLS` list.
- Resolves spell objects via `resolveSpellObject` and executes via `invokeSpellCast`.
- Provides visual feedback (client display message + particle effects on `ServerLevel`).
- Provides diagnostic messages if the mod is not loaded or the spell cannot be resolved.

### 3. Reflection Spell Resolution (`resolveSpellObject`)
Employs a 4-tier lookup strategy across target registry class names (`net.ironsspellbooks.api.registry.SpellRegistry`, `net.ironsspellbooks.spells.SpellRegistry`, etc.):
1. Direct static method invocation (`getSpell(ResourceLocation)`, `getSpell(String)`, `get(ResourceLocation)`, `get(String)`).
2. Field lookups (`REGISTRY`, `SPELL_REGISTRY`, `SPELLS`, `registry`, `spellRegistry`) and getter methods (`getRegistry`, `getSpellRegistry`, `getSpells`).
3. Static constant field matching on registry classes (e.g. `FIREBOLT_SPELL`, `FIREBOLT`).
4. Fallback lookup via Minecraft's `BuiltInRegistries.REGISTRY` under `irons_spellbooks:spells`.

### 4. Spell Holder Unwrapping (`unwrapSpellHolder`)
- Filters out dummy/null spells (`VoidSpell`, `NoneSpell`, or `"none"` identifier strings).
- Recursively unwraps `.value()` (MC 1.20 Holder), `.get()` (Supplier/RegistryObject/Optional), `.getSpell()`, and `.resolve()`.
- Validates `.isPresent()` and `.isEmpty()` checks.

### 5. Dynamic Spell Invocation (`invokeSpellCast`)
- Dynamically resolves `CastSource` enum (`SPELLBOOK` / `INNATE`).
- Obtains or instantiates `MagicData`.
- Collects and sorts `onCast`, `castSpell`, and `onCastSpell` candidate methods by strict parameter match score, method name priority, and parameter count.
- Binds arguments dynamically (`Level`, `spellLevel`, `Player`/`Entity`, `CastSource`, `MagicData`).
- Catches and logs `InvocationTargetException` / `IllegalAccessException` with detailed error traces.

### 6. Attribute Application (`applyIronSpellsAttributes`)
- Reflectively inspects `AttributeRegistry` for passive keys (`arcane_overflow`, `mana_fountain`, `fire_spell_mastery`, etc.).
- Derives deterministic `UUID` for each passive key using `UUID.nameUUIDFromBytes`.
- Instantiates and adds transient `AttributeModifier` to player attribute instances.

---

## Forensic Evidence Summary

- File inspected: `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java` (765 lines, 35,034 bytes).
- Prohibited patterns: **0 violations detected**.
- Implementation status: Genuine production code.
