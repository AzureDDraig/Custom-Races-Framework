# Task Brief — Worker 2 (M2 Remediation & Refinement)

## Objective
Apply critical fixes to `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java` based on Reviewer 2, Challenger 1, and Challenger 2 feedback.

## Specific Required Fixes
1. **Unbounded Recursion Guard in `unwrapSpellHolder`**:
   - Add recursion depth parameter `unwrapSpellHolder(Object obj, int depth)` with `depth > 10` returning `null`.
2. **Container Fall-Through Fix for `VoidSpell` / `null`**:
   - Ensure that when unwrapping an inner object (`Holder`, `Supplier`, `RegistryObject`, `Optional`) returns `null` or a void spell, `unwrapSpellHolder` propagates `null` upward instead of falling through to return the raw wrapper object (`return obj;`).
3. **Primitive Parameter Defaults in `invokeSpellCast`**:
   - Map primitive parameter types (`boolean.class`, `int.class`, `float.class`, `double.class`, `long.class`, etc.) to non-null defaults (`false`, `0`, `0.0f`, `0.0`, `0L`) when argument values are null.
4. **Strict Type Assignability Guards**:
   - Exclude root generic types (`Object.class`, `Enum.class`, `Comparable.class`, `Serializable.class`) in `isCastSourceType` and `isMagicDataType`.
5. **Candidate Method Scoring Optimization**:
   - Ensure target 5-parameter (`Level`, `int`, `LivingEntity/ServerPlayer`, `CastSource`, `MagicData`) and 4-parameter (`Level`, `int`, `LivingEntity/ServerPlayer`, `MagicData`) signatures score highest, penalizing unmapped generic parameter overloads.
6. **ResourceLocation Safety**:
   - Wrap `new ResourceLocation(spellId)` inside `resolveSpellObject` with a try-catch for `ResourceLocationException` / `IllegalArgumentException`, logging a warning and returning `null` if malformed.

## Build & Verify
- Execute `.\gradlew build -x test` from project root to ensure clean compilation across Common, Fabric, and Forge.

## Output
Write `changes.md` and `handoff.md` in `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m2_remediation`.
