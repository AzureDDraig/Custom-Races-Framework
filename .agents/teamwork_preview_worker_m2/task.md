# Task Brief — Worker (M2: Core Native Spell & Reflection Compatibility)

## Objective
Refactor `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java` to resolve native spell IDs, unwrap spell objects, and invoke `onCast`/`castSpell` with strict parameter signatures across Iron's Spells 'n Spellbooks 1.20.1 API variations.

## Detailed Requirements
1. **Method Resolution**:
   - Instead of non-deterministic `getMethods()` matching, search specifically for methods named `onCast`, `castSpell`, or `onCastSpell`.
   - Match parameter types strictly: (`Level`, `int`/`Integer`, `LivingEntity`/`ServerPlayer`/`Player`, `CastSource` (or enum matching CastSource package/name), `MagicData`) or fallback variations without `CastSource`.
   - Ensure generic `p.isEnum()` does NOT assign arbitrary enums unless the class name or package matches `CastSource`.

2. **Spell Unwrapping & Registry Lookup**:
   - Ensure `unwrapSpellHolder` handles `Holder`, `RegistryObject`, `Supplier`, and `AbstractSpell` cleanly.
   - Ensure registry lookup resolves spell IDs across `net.ironsspellbooks.api.registry.SpellRegistry`, `net.ironsspellbooks.spells.SpellRegistry`, `io.github.elytra.irons_spellbooks.api.registry.SpellRegistry`, and `net.minecraft.core.Registry`.

3. **Exception Logging**:
   - Log explicit warning/error messages with stack traces on `InvocationTargetException` or `IllegalAccessException` so reflection issues can be debugged easily.

4. **Build & Verify**:
   - Run `./gradlew build -x test` on both Fabric and Forge targets to verify clean compilation.

## Output
Write `changes.md` and `handoff.md` in `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m2`.
