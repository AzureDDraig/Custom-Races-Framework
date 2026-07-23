# Task Brief — Reviewer 1 (M2 Remediation Review)

## Objective
Verify the 6 remediation fixes in `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java`.

## Scope
1. Verify recursion depth limit (`depth > 10`) in `unwrapSpellHolder`.
2. Verify container null propagation for void/none/null inner spell objects.
3. Verify primitive type default mapping in `invokeSpellCast`.
4. Verify root generic type exclusions (`Object`, `Enum`, `Comparable`, `Serializable`) in `isCastSourceType` and `isMagicDataType`.
5. Verify tiered candidate method scoring (`getTier`).
6. Verify `ResourceLocationException` try-catch in `resolveSpellObject`.
7. Execute `.\gradlew build -x test` to verify multi-platform build.

## Output
Write `review.md` and `handoff.md` in `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_reviewer_m2_remediation_1`.
