# Handoff Report — Explorer 2 (Iron's Spells Reflection & Casting Bridge)

**Working Directory**: `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_2`  
**Target Analysis File**: `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_2\analysis.md`

---

## 1. Observation

1. **Reflection Integration Entry Point**:
   - `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java` (lines 1–379).
   - Contains soft-reflection spell casting (`castNativeSpell`, `resolveSpellObject`, `unwrapSpellHolder`, `invokeSpellCast`), Wild Magic catalogue (`ALL_SPELLS`), and racial attribute registration (`applyIronSpellsAttributes`).

2. **Spell Data & Resolution**:
   - `common/src/main/java/ddraig/net/customraces/data/RaceData.java` (lines 184–242):
     - `getNativeSpellId(int slot, boolean isWere)`
     - `getWildMagic(int slot, boolean isWere)`
     - `getNativeSpellLevel(int slot, boolean isWere)`
   - Storage fields in `RaceData.java`: `nativeSpellId1`..`5`, `wereNativeSpellId1`..`5`, `wildMagic1`..`5`, `wereWildMagic1`..`5`, `nativeSpellLevel1`..`5`, `wereNativeSpellLevel1`..`5`.

3. **Registry Resolution & Unwrapping**:
   - `IronSpellsHandler.java` (lines 129–166): Class target search includes `net.ironsspellbooks.api.registry.SpellRegistry`, `io.github.elytra.irons_spellbooks.api.registry.SpellRegistry`, `net.ironsspellbooks.spells.SpellRegistry`, and `net.ironsspellbooks.api.spells.AbstractSpell`.
   - `IronSpellsHandler.java` (lines 168–188): `unwrapSpellHolder(Object obj)` handles `Holder`, `RegistryObject`, and `Supplier` wrappers by recursively calling `.value()` and `.get()`.

4. **Cast Invocation Signature & Parameter Matching**:
   - `IronSpellsHandler.java` (lines 190–248): `invokeSpellCast(Player player, Object spellObj, int spellLevel)` searches `getMethods()` for names containing `"oncast"`, `"cast"`, or `"initiate"`.
   - Resolves `CastSource` (`SPELLBOOK` / `INNATE`) via `getCastSourceEnum()` (lines 264–290) and `MagicData` via `getPlayerMagicData(player)` (lines 292–313).
   - Heuristically maps parameters: `Level`, `int`/`Integer`, `Player`/`LivingEntity`/`ServerPlayer`, `CastSource`, `MagicData`, and generic enum (`p.isEnum() -> castSource`).

5. **Cross-Platform Delivery**:
   - Client keybind inputs (`RaceKeybindings.java`, lines 32–36) send `TRIGGER_ABILITY_ID` network packets (`ModPackets.java`, lines 138–144).
   - `ActiveAbilityHandler.java` (lines 70–90) handles packet execution on `ServerPlayer` and delegates `native_spell_1`..`5` to `IronSpellsHandler.castNativeSpell(...)`.
   - `fabric` and `forge` modules delegate initialization entirely to `common` via `CustomRaces.init()`.

---

## 2. Logic Chain

1. **Observation 1 & 5** demonstrate that Iron's Spells integration is implemented centrally in the `common` module (`IronSpellsHandler.java`) using Architectury's multi-platform packet framework. Both `fabric` and `forge` modules invoke shared server-side spell execution routines without platform-specific reflection code.
2. **Observation 2** shows that native spell configuration is fully supported for both human and Were forms across 5 slots in `RaceData`, supporting spell ID lookup, level scaling, and Wild Magic randomness.
3. **Observation 3 & 4** show that spell object resolution and unwrapping correctly account for `Holder` and `RegistryObject` wrappers in 1.20.1 API structures. However, invocation heuristics in `invokeSpellCast` suffer from critical reflection vulnerabilities:
   - `getMethods()` order is non-deterministic, risking invocation of non-casting methods (e.g. `canCast`).
   - Generic `p.isEnum()` mapping causes `IllegalArgumentException` on non-`CastSource` enums.
   - Exceptions inside `m.invoke` are silently caught, hiding underlying NPEs or parameter mismatch errors.

---

## 3. Caveats

- Runtime testing with actual Iron's Spells 'n Spellbooks JAR files was not performed in this exploration phase, as the environment is read-only code analysis.
- Third-party add-on spells (e.g. `totweaks:*`) rely on `IronSpellsHandler.ALL_SPELLS` hardcoded list for Wild Magic, but registry lookup relies on Iron's Spells' `SpellRegistry`. If add-ons register spells in separate registries, resolution relies on standard `ResourceLocation` lookup within Iron's Spells registry.

---

## 4. Conclusion

The Iron's Spells reflection bridge in `IronSpellsHandler.java` provides a solid, cross-platform foundation for spell resolution and execution. To prepare for **Milestone 2 (Core Native Spell & Reflection Compatibility)**, the casting engine should be refactored to:
1. Target explicit `onCast(Level, int, LivingEntity, CastSource, MagicData)` method signatures rather than unconstrained `getMethods()` iteration.
2. Provide strict parameter type matching without blind enum fallbacks.
3. Properly log reflection errors and handle `MagicData` mana/cooldown integration if desired.

---

## 5. Verification Method

To independently verify these findings:
1. View `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java` and inspect lines 129–248.
2. View `common/src/main/java/ddraig/net/customraces/data/RaceData.java` and inspect lines 184–242.
3. View `common/src/main/java/ddraig/net/customraces/ability/ActiveAbilityHandler.java` and inspect lines 70–90.
4. Verify project compilation by inspecting `analysis.md` in the working directory.
