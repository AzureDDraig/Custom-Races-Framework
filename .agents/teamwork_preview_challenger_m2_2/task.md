# Task Brief — Challenger 2 (M2 Method Scoring & Invocation Challenge)

## Objective
Adversarially evaluate the method scoring, unwrapping, and invocation logic in `IronSpellsHandler.java`. Stress-test candidate method sorting, parameter type coercion, and enum resolution.

## Actions
1. Inspect `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java`.
2. Evaluate method scoring algorithm: test whether overloaded methods (e.g. `onCast(Level, int, LivingEntity, CastSource, MagicData)` vs `onCast(Level, int, LivingEntity, MagicData)`) receive proper relative weights.
3. Test `isCastSourceType` check for false positives or false negatives.
4. Run `.\gradlew build -x test` to confirm build.
5. Write `challenge.md` and `handoff.md` in `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_challenger_m2_2`.
