# Challenge Report — M2 Method Scoring & Invocation Challenge

## Challenge Summary

**Overall risk assessment**: **HIGH**

Empirical stress testing of `IronSpellsHandler.java` revealed multiple failure modes, parameter false positives, overload scoring inversions, and primitive reflection invocation failures.

---

## Challenges

### [High] Challenge 1: `isCastSourceType` & `isMagicDataType` Assignability False Positives for Generic Types (`Object`, `Enum`, `Comparable`, `Serializable`)

- **Assumption challenged**: `p.isAssignableFrom(castSource.getClass())` safely checks whether parameter type `p` is a valid `CastSource` type.
- **Attack scenario**: In Java reflection, `p.isAssignableFrom(subclass)` checks if `p` is a superclass or interface of `subclass`. Since `castSource` is an enum (e.g. `CastSource.SPELLBOOK`), its class extends `java.lang.Enum`, implements `java.lang.Comparable` & `java.io.Serializable`, and inherits from `java.lang.Object`. Consequently:
  - `isCastSourceType(Object.class, castSource)` returns `true`.
  - `isCastSourceType(Enum.class, castSource)` returns `true`.
  - `isCastSourceType(Comparable.class, castSource)` returns `true`.
  - `isMagicDataType(Object.class, magicData)` returns `true`.
- **Blast radius**: Any spell overload accepting an `Object`, `Enum`, `Comparable`, or `Serializable` parameter is falsely flagged as a 100% strict parameter match.
- **Mitigation**: Filter out generic superclasses (`Object.class`, `Enum.class`, `Comparable.class`, `Serializable.class`) before running `p.isAssignableFrom(...)`. Ensure `p` is specific to the target class/package (e.g. `CastSource.class.isAssignableFrom(p)`).

---

### [High] Challenge 2: Candidate Method Scoring Inversion for Overloaded Methods with Extra Parameters

- **Assumption challenged**: Candidate method sorting ranks the standard 5-parameter `onCast(Level, int, LivingEntity, CastSource, MagicData)` or 4-parameter `onCast(Level, int, LivingEntity, MagicData)` above methods with unrecognized/extra parameters.
- **Attack scenario**: Due to Challenge 1, a method signature such as `onCast(Level, int, LivingEntity, CastSource, MagicData, Object context)` receives `strict = true` because `Object` matches `isCastSourceType`. Because candidate sorting sorts by `parameterCount` descending (`m2.getParameterCount() - m1.getParameterCount()`), the 6-parameter method ranks **Rank 1**, pushing the standard 5-parameter method down to **Rank 2**!
- **Blast radius**: If a spell class has an overloaded `onCast` method taking an `Object` context argument, `invokeSpellCast` invokes the wrong overload with `castSource` passed into `Object context`.
- **Mitigation**: Disallow generic supertypes (`Object`, `Enum`, etc.) from satisfying strict parameter matching. When parameter counts differ among strict matches, prioritize standard 5-param and 4-param signatures over unexpected extra parameters.

---

### [Medium] Challenge 3: Primitive Reflection Invocation Crash on Non-Integer Primitives (`boolean`, `float`, `double`, `long`, `short`, `byte`)

- **Assumption challenged**: Primitive type parameters are gracefully handled or ignored during reflection argument binding.
- **Attack scenario**: `isStrictParameterMatch` and argument resolution only recognize `int.class` and `Integer.class`. Any primitive parameter like `boolean` or `float` falls through to the `else { args[i] = null; }` branch. When `Method.invoke(spellObj, args)` is called on a method expecting `boolean` or `float`, Java Reflection attempts to unbox `null`, throwing `java.lang.IllegalArgumentException` / `java.lang.NullPointerException`.
- **Blast radius**: Spells requiring primitive flags or modifiers (e.g. `onCast(Level, int, LivingEntity, CastSource, MagicData, boolean isCritical)`) crash during invocation attempt, causing spell casting to fail.
- **Mitigation**: Provide default primitive fallback values (`false` for `boolean`, `0.0f` for `float`, `0.0` for `double`, `0L` for `long`) instead of `null` when binding primitive parameters.

---

### [Medium] Challenge 4: Unwrapped Spell Holder Fallthrough for Empty Wrappers in `unwrapSpellHolder`

- **Assumption challenged**: `unwrapSpellHolder` returns `null` whenever a wrapper object does not contain a valid spell.
- **Attack scenario**: When `unwrapSpellHolder` inspects a `Supplier`, `Holder`, or `RegistryObject` whose `.get()` or `.value()` returns `null`, the check `if (val != null && val != obj)` evaluates to `false`. The method does not detect that the wrapper returned `null` and falls through to `return obj;`, returning the empty wrapper instance itself!
- **Blast radius**: `resolveSpellObject` receives a non-null wrapper object (e.g., `MockNullSupplier`), leading `invokeSpellCast` to attempt method lookup on `Supplier.class`, resulting in erroneous log spam ("No onCast / castSpell / onCastSpell method found") instead of falling back to vanilla registry or field lookups.
- **Mitigation**: Explicitly check if unwrapped value `val == null` for wrapper types (`Supplier`, `Holder`, `RegistryObject`), and return `null` immediately.

---

## Stress Test Results

- **Scenario 1**: `isCastSourceType(Object.class, castSource)`
  - Expected: `false`
  - Actual: `true` [FAIL — Vulnerability confirmed]
- **Scenario 2**: Candidate sorting: `onCast(..., Object)` (6 params) vs `onCast(..., CastSource, MagicData)` (5 params)
  - Expected: 5-param standard method ranks Rank 1
  - Actual: 6-param `Object` method ranks Rank 1 [FAIL — Sorting Inversion confirmed]
- **Scenario 3**: Invocation of method with primitive `boolean` parameter
  - Expected: Graceful default binding or execution
  - Actual: `java.lang.IllegalArgumentException: NullPointerException` unboxing `null` [FAIL — Primitive unboxing crash confirmed]
- **Scenario 4**: `unwrapSpellHolder` on `Supplier` returning `null`
  - Expected: `null`
  - Actual: `MockNullSupplier` instance returned [FAIL — Empty wrapper fallthrough confirmed]
- **Scenario 5**: `resolveCastSourceForParam` on unrelated enum without `SPELLBOOK`/`INNATE`
  - Expected: Handled gracefully or fallback
  - Actual: Falls back to `constants[0]` (`FOO`) [PASS with Caveat — fallback succeeds]

---

## Unchallenged Areas

- Attribute modifier logic in `applyIronSpellsAttributes`: Not challenged as it relies on live Minecraft attribute registries and player instances.
- Wild Magic random selection logic: Standard list index selection, low risk.
