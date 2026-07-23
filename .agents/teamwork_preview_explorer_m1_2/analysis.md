# Analysis Report: Iron's Spells 'n Spellbooks Reflection & Casting Bridge

**Author**: Explorer 2 (Iron's Spells Reflection & Casting Bridge)  
**Date**: 2026-07-23  
**Target Module**: `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java`  
**Related Components**: `RaceData.java`, `ActiveAbilityHandler.java`, `PassiveAbilityHandler.java`, `ModPackets.java`, `RaceKeybindings.java`

---

## 1. Executive Summary

The **Custom Races Framework** implements optional, soft-reflection integration with **Iron's Spells 'n Spellbooks** (and optional add-ons like *T.O Tweaks*) inside `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java`. 

The integration allows players of custom races (in human or Were-form) to bind native spells to Active Ability slots 1–5 (`native_spell_1` through `native_spell_5`) or trigger **Wild Magic** (random spell invocation from a catalogue of 66 spells). 

Because the mod supports both Fabric and Forge without hard compile-time dependencies on Iron's Spells 'n Spellbooks, all calls to Iron's Spells classes, registries, spell holders, and cast methods are performed via Java Reflection.

---

## 2. Cross-Platform Module Architecture

```
[Client Keybind (G/V/B/N/M)] 
       │
       ▼ (RaceKeybindings.java - Architectury ClientTickEvent)
[ModPackets.sendTriggerAbility(slot)] (C2S Packet)
       │
       ▼ (ModPackets.java - ServerReceiver C2S)
[ActiveAbilityHandler.triggerAbility(player, slot)]
       │
       ▼ (Checks RaceData & Were-Transform State)
[IronSpellsHandler.castNativeSpell(player, race, isWere, slot)] (common)
```

- **`common` module**: Contains all integration logic (`IronSpellsHandler.java`), data models (`RaceData.java`), packet handlers (`ModPackets.java`), and ability listeners (`ActiveAbilityHandler.java`, `PassiveAbilityHandler.java`). Uses `dev.architectury.platform.Platform.isModLoaded("irons_spellbooks")` for dynamic mod detection.
- **`fabric` module**: Delegates initialization to `CustomRaces.init()`. No Fabric-specific reflection or spell handling code required.
- **`forge` module**: Delegates initialization to `CustomRaces.init()`. No Forge-specific reflection or spell handling code required.

---

## 3. Spell ID Storage & Resolution Mechanics

### 3.1 Data Model (`RaceData.java`)
In `RaceData.java`, native spell configurations are stored separately for human and Were forms:

- **Human Form**:
  - Slots 1-5: `nativeSpellId1` .. `nativeSpellId5` (String)
  - Slot 1 Legacy Fallback: `nativeSpellId`
  - Wild Magic Flags: `wildMagic1` .. `wildMagic5` (boolean, fallback `wildMagic`)
  - Spell Levels: `nativeSpellLevel1` .. `nativeSpellLevel5` (int, fallback `nativeSpellLevel`, default 1)
- **Were Form**:
  - Slots 1-5: `wereNativeSpellId1` .. `wereNativeSpellId5` (String)
  - Slot 1 Legacy Fallback: `wereNativeSpellId`
  - Wild Magic Flags: `wereWildMagic1` .. `wereWildMagic5` (boolean, fallback `wereWildMagic`)
  - Spell Levels: `wereNativeSpellLevel1` .. `wereNativeSpellLevel5` (int, fallback `wereNativeSpellLevel`, default 2)

Resolution methods in `RaceData.java` (lines 184–242):
- `getNativeSpellId(int slot, boolean isWere)`
- `getWildMagic(int slot, boolean isWere)`
- `getNativeSpellLevel(int slot, boolean isWere)`

### 3.2 Pre-processing (`IronSpellsHandler.java: castNativeSpell`)
1. **Wild Magic Override**: If `isWildMagic` is `true`, `spellId` is randomly selected from `IronSpellsHandler.ALL_SPELLS` (66 spell entries). System message sent: `✨ [Wild Magic] Casting random spell: <id>`.
2. **Sanitization & Namespace Formatting**:
   - `spellId` is trimmed.
   - If `spellId` contains no `:` namespace separator, `irons_spellbooks:` is automatically prepended (e.g. `"firebolt"` -> `"irons_spellbooks:firebolt"`).
   - Value `"none"` or blank strings immediately return without casting.

---

## 4. Registry Lookup & Spell Object Unwrapping

### 4.1 Registry Lookup (`resolveSpellObject`)
Located at `IronSpellsHandler.java:129-166`:

```java
String[] registryClasses = {
    "net.ironsspellbooks.api.registry.SpellRegistry",           // 1.20.1+ Official API
    "io.github.elytra.irons_spellbooks.api.registry.SpellRegistry", // Legacy / Alt API
    "net.ironsspellbooks.spells.SpellRegistry",                // Legacy internal
    "net.ironsspellbooks.api.spells.AbstractSpell"             // Class fallback
};
```

For each class, the resolver attempts 3 getter method signatures:
1. `getSpell(ResourceLocation)` with `new ResourceLocation(spellId)`
2. `getSpell(String)` with full `spellId` (e.g., `"irons_spellbooks:firebolt"`)
3. `getSpell(String)` with path only (e.g., `"firebolt"`)

### 4.2 Holder & Object Unwrapping (`unwrapSpellHolder`)
Located at `IronSpellsHandler.java:168-188`:
When `SpellRegistry.getSpell(...)` is invoked in Minecraft 1.20.1, it often returns a `Holder<AbstractSpell>` or `RegistryObject<AbstractSpell>` rather than the direct `AbstractSpell` instance.

Unwrapping logic:
1. Checks for null or dummy void/none spell classes (`VoidSpell`, `NoneSpell`, or `.toString()` containing `"none"`). Returns `null`.
2. Checks if the object class or superclass name contains `"AbstractSpell"`. If true, returns `obj`.
3. Checks for `.value()` method (e.g., `Holder.value()` or `RegistryObject.value()`). Recursively invokes `unwrapSpellHolder(val)`.
4. Checks for `.get()` method (e.g., `Supplier.get()` or `Holder.get()`). Recursively invokes `unwrapSpellHolder(val)`.
5. Returns `obj` as fallback.

---

## 5. Reflection Casting Engine (`invokeSpellCast`)

Located at `IronSpellsHandler.java:190-248`.

### 5.1 Dynamic Parameter Resolution
Before scanning methods on the unwrapped spell object, the engine attempts to resolve necessary context instances:

1. **`CastSource` Enum**:
   - Class paths checked: `net.ironsspellbooks.api.spells.CastSource`, `io.github.elytra.irons_spellbooks.api.spells.CastSource`, `com.io.github.elytra.irons_spellbooks.api.spells.CastSource`.
   - Resolves `Enum.valueOf(clazz, "SPELLBOOK")`, falling back to `Enum.valueOf(clazz, "INNATE")`.
2. **`MagicData` Instance**:
   - Class paths checked: `net.ironsspellbooks.api.magic.MagicData`, `io.github.elytra.irons_spellbooks.api.magic.MagicData`.
   - Resolves via static call `MagicData.getPlayerMagicData(player)` or `MagicData.get(player)`.
   - Fallback: Instantiates `new MagicData()` via default constructor if static getter returns null.

### 5.2 Method Signature Matching & Parameter Heuristics
The engine iterates over `spellObj.getClass().getMethods()` and looks for any method whose name (lowercase) contains `"oncast"`, `"cast"`, or `"initiate"`.

For each matching method, parameters are populated using the following heuristics:
- `net.minecraft.world.level.Level` -> `player.level()`
- `int` / `Integer` -> `spellLevel`
- `Player` / `LivingEntity` / `ServerPlayer` -> `player`
- `CastSource` (assignable from resolved `castSource`) -> `castSource`
- `MagicData` (assignable from resolved `magicData`) -> `magicData`
- `p.isEnum()` -> `castSource` (Generic Enum Fallback)
- Any unmatched parameter -> `null`

Method invocation sequence:
```java
m.setAccessible(true);
m.invoke(spellObj, args);
return true;
```
If `m.invoke` executes without throwing an exception, casting is marked successful.

---

## 6. Racial Attribute Integration (`applyIronSpellsAttributes`)

Located at `IronSpellsHandler.java:316-377`:
Applies attribute modifiers to players when specific passive abilities are enabled on their race (e.g. `arcane_overflow`, `mana_fountain`, `arcane_amplification`, `spell_ward`, `fire_spell_mastery`, etc.).

- Targets `net.ironsspellbooks.api.registry.AttributeRegistry` or `io.github.elytra...`.
- Resolves attributes from static fields (e.g. `MAX_MANA`, `MANA_REGEN`, `SPELL_POWER`, `SPELL_RESIST`, school powers).
- Adds transient `AttributeModifier` with UUID generated deterministically from passive key.

---

## 7. Error Handling, Fallbacks & Actionbar Feedback

1. **Mod Absent**:
   - Actionbar overlay: `§c[Native Spell X] irons_spellbooks:<spell> (Requires Iron's Spells mod)`
   - Visual FX: `DRAGON_BREATH` and `WITCH` particles spawned around player.
2. **Mod Present, Spell Resolution or Casting Failed**:
   - Console log: `[CustomRaces] Failed to cast Iron's Spell: <spellId> - <error>`
   - Actionbar overlay: `§c[Native Spell X] Could not invoke spell: <spellId> (Verify spell ID format)`
   - Visual FX: `DRAGON_BREATH` and `WITCH` particles spawned around player.
3. **Successful Cast**:
   - Actionbar overlay: `§d✨ [Native Spell X] Cast <spell> (Lvl <level>)`
   - Visual FX: `ENCHANT` particle cloud (25 particles) around player.

---

## 8. Critical Findings, Reflection Flaws & Vulnerabilities for M2

During deep inspection of `IronSpellsHandler.java`, several architectural bugs and failure modes were identified that must be addressed in **Milestone 2**:

1. **Undefined Method Iteration Order (`getMethods()`)**:
   - `Class.getMethods()` order is non-deterministic in Java.
   - If a spell class defines utility methods containing `"cast"` (e.g., `canCast(Level, int, LivingEntity, MagicData)` or `checkCastRequirements(...)`), reflection may match and invoke `canCast` before `onCast`. Since `canCast` returns a `boolean` without throwing, `invokeSpellCast` returns `true` prematurely without actually casting the spell!
2. **Flawed `p.isEnum()` Fallback**:
   - In `invokeSpellCast`, any parameter of type `enum` is blindly assigned `castSource`. If a spell method signature takes a different enum (such as `SpellSchool`, `TargetType`, or `AnimationType`), `m.invoke` throws an `IllegalArgumentException`.
3. **Swallowed Invocation Exceptions**:
   - The method invocation loop wraps `m.invoke` in a silent `catch (Exception ignored) {}`. If parameter construction produces a `null` value for a required parameter, `m.invoke` throws a `NullPointerException` inside Iron's Spells code, which is silently swallowed, causing the engine to fail casting or jump to an incorrect overload.
4. **Bypassed Mana & Internal Cooldown Systems**:
   - `IronSpellsHandler` does not check or consume player mana (`MagicData.getMana()`), nor does it update Iron's Spells' internal player cooldown tracker (`MagicData.getPlayerCooldowns()`). Cooldown is currently handled strictly by `ActiveAbilityHandler`'s fixed 10-second `DEFAULT_COOLDOWN_MS`.
5. **Registry Class Pathfinder Mismatch**:
   - `getSpellRegistryClass()` includes path `"com.io.github.elytra.irons_spellbooks.api.registry.SpellRegistry"`, but `resolveSpellObject()` omits `"com.io.github.elytra..."` from its local array.

---

## 9. Verification & Conclusion

- **Verification Command**: Clean build across all modules using `./gradlew build` or `./gradlew check`.
- **Status**: Exploration complete. All findings documented for M2 implementation.
