# Handoff Report — M2 Method Scoring & Invocation Challenge

## 1. Observation

### Build Commands & Execution Results
- **Command**: `.\gradlew build -x test`
  - **Result**: `BUILD SUCCESSFUL in 13s` (29 actionable tasks executed/up-to-date across `:common`, `:fabric`, `:forge`).
- **Empirical Test Command**: `.\gradlew :common:runEmpiricalTests` (via temporary test runner running `IronSpellsHandlerTest.java` in `common/src/test/java/ddraig/net/customraces/integration/IronSpellsHandlerTest.java`)
  - **Output**:
    ```
    === RUNNING IRON SPELLS HANDLER EMPIRICAL TESTS ===
    isCastSourceType(MockCastSource.class, castSource) = true
    isCastSourceType(Object.class, castSource) = true [VULNERABILITY: Object matches as CastSource]
    isCastSourceType(Enum.class, castSource) = true [VULNERABILITY: Enum matches as CastSource]
    isCastSourceType(Comparable.class, castSource) = true [VULNERABILITY: Comparable matches as CastSource]
    isMagicDataType(Object.class, magicData) = true [VULNERABILITY: Object matches as MagicData]
    Sorted candidates order:
      Rank 1: onCast with 6 params
      Rank 2: onCast with 5 params
      Rank 3: onCast with 4 params
      Rank 4: castSpell with 5 params
      Rank 5: onCastSpell with 5 params
      Rank 6: onCast with 6 params
    Arg constructed for primitive boolean param: argsF[5] = null
    mF.invoke failed as expected with IllegalArgumentException: java.lang.NullPointerException: Cannot invoke "java.lang.Number.intValue()" because the return value of "sun.invoke.util.ValueConversions.primitiveConversion(sun.invoke.util.Wrapper, Object, boolean)" is null [LIMITATION: Unhandled primitive parameters cause Reflection failure]
    unwrapSpellHolder(null) = null
    unwrapSpellHolder("none") = null
    unwrapSpellHolder(Optional.empty()) = null
    unwrapSpellHolder(Supplier returning null) = ddraig.net.customraces.integration.IronSpellsHandlerTest$MockNullSupplier@6f2b958e [EDGE CASE: Returns empty Supplier object instead of null]
    resolveCastSourceForParam(MockUnrelatedEnum.class, castSource) = FOO (type: ddraig.net.customraces.integration.IronSpellsHandlerTest$MockUnrelatedEnum)
    === EMPIRICAL TESTS COMPLETE ===
    ```

### Code Line Observations in `IronSpellsHandler.java`
- **Line 585**: `if (castSource != null && p.isAssignableFrom(castSource.getClass()))` -> Returns `true` for `Object.class`, `Enum.class`, `Comparable.class`, `Serializable.class`.
- **Line 625**: `if (magicData != null && p.isAssignableFrom(magicData.getClass()))` -> Returns `true` for `Object.class`.
- **Lines 506–516**: Candidate sorting comparator prioritizes `isStrictParameterMatch` (which returns `true` for `Object`), then sorts by `parameterCount` descending (`Integer.compare(m2.getParameterCount(), m1.getParameterCount())`).
- **Lines 522–540**: Argument resolution loop sets unhandled primitive parameters (e.g. `boolean`, `float`) to `null`, causing `Method.invoke()` to throw `IllegalArgumentException` / `NullPointerException` during unboxing.
- **Lines 366–395**: `unwrapSpellHolder` getter checks (`value()`, `get()`) do not check if returned value `val == null`, falling through to `return obj;` (returning empty wrapper object).

---

## 2. Logic Chain

1. **Observation**: `isCastSourceType` checks `p.isAssignableFrom(castSource.getClass())`. Since `castSource` is an Enum, `castSource.getClass()` inherits from `Object` and implements `Comparable` and `Serializable`.
2. **Step**: `Object.class.isAssignableFrom(castSource.getClass())` evaluates to `true` in Java reflection.
3. **Step**: `isStrictParameterMatch` loops through all parameter types of candidate methods. When it checks a method like `onCast(Level, int, LivingEntity, CastSource, MagicData, Object extra)`, every parameter—including `Object extra`—returns `true`.
4. **Step**: `isStrictParameterMatch` returns `true` for the 6-parameter overload.
5. **Step**: Candidate sorting compares candidate methods: both the 6-param overload and the standard 5-param overload have `strict = true` and `nameScore = 1`. Parameter count comparison (`m2.getParameterCount() - m1.getParameterCount()`) ranks the 6-param overload ABOVE the standard 5-param overload (Rank 1 vs Rank 2).
6. **Step**: When `invokeSpellCast` executes Rank 1, it passes `castSource` as the 6th argument (`Object extra`). If the spell expects a specific context object, invocation fails or behaves unpredictably.
7. **Step**: Primitive parameters (`boolean`, `float`) are assigned `null` in argument resolution, causing Reflection unboxing crashes (`IllegalArgumentException: NullPointerException`).
8. **Conclusion**: `IronSpellsHandler.java` exhibits candidate method scoring inversions, false positive parameter matching for `Object`, reflection unboxing crashes on primitive types, and empty wrapper fallthrough in `unwrapSpellHolder`.

---

## 3. Caveats

- **Mod Environment**: Iron's Spells mod classes were mocked in `IronSpellsHandlerTest.java` since Iron's Spells is an optional soft dependency and not bundled at compile-time.
- **Minecraft Player Instance**: Player and Level parameters were passed as null or mock values during reflection tests; full game loop behavior depends on runtime mod loading.
- **No Implementation Code Modified**: As required by challenger guidelines, no changes were made to `IronSpellsHandler.java`.

---

## 4. Conclusion

Empirical evaluation of `IronSpellsHandler.java` proves that:
1. `isCastSourceType` and `isMagicDataType` suffer from false positive type assignability when parameter types are generic superclasses (`Object`, `Enum`, `Comparable`, `Serializable`).
2. Candidate method scoring can invert, placing non-standard overloads with extra `Object` parameters ahead of standard 5-param or 4-param `onCast` methods.
3. Non-integer primitive parameters (`boolean`, `float`, etc.) cause reflection unboxing crashes because they receive `null` arguments.
4. `unwrapSpellHolder` falls through to return empty wrapper objects when `.get()` or `.value()` returns `null`.

The Gradle build succeeds cleanly across all modules (`.\gradlew build -x test`).

---

## 5. Verification Method

To independently verify these findings:
1. Inspect test suite file: `common/src/test/java/ddraig/net/customraces/integration/IronSpellsHandlerTest.java`.
2. Inspect challenge report: `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_challenger_m2_2\challenge.md`.
3. Run Gradle build: `.\gradlew build -x test`.
4. Invalidation condition: If `isCastSourceType(Object.class, castSource)` is changed to return `false`, or candidate sorting prioritizes exact parameter types over raw parameter count, the reported vulnerability will be invalidated.
