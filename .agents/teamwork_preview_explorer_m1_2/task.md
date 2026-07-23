# Task Brief — Explorer 2 (Iron's Spells Reflection & Casting Bridge)

## Objective
Investigate how Iron's Spells 'n Spellbooks integration and reflection wrappers are currently implemented across common, fabric, and forge modules.

## Scope
- Locate reflection bridges/wrappers for Iron's Spells 'n Spellbooks.
- Analyze how spell IDs are resolved from registry/config.
- Trace how spell objects are instantiated or unwrapped.
- Analyze `onCast` and `castSpell` invocation signatures (`Level`, `spellLevel`, `ServerPlayer`/`LivingEntity`, `CastSource`, `MagicData`) and 1.20.1 API variations.
- Identify current error handling, fallbacks, or reflection failure modes.

## Output
Write report to `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_2\analysis.md` and deliver `handoff.md`.
