# Original User Request

## 2026-07-23T19:33:35Z

# Teamwork Project Prompt — Comprehensive Iron's Spells Native Spell Casting Resolution

Working directory: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework
Integrity mode: development

## Requirements

### R1. Iron's Spells Native Spell Execution & Reflection Compatibility
Ensure all 5 Native Spell slots (base form and Were-form) correctly resolve spell IDs from Iron's Spells 'n Spellbooks across all 1.20.1 API variations, instantiate/unwrap spell objects, and invoke casting methods (`onCast`, `castSpell`) with valid parameter signatures (`Level`, `spellLevel`, `ServerPlayer`/`LivingEntity`, `CastSource`, `MagicData`).

### R2. Native Spell Input & Keybind Binding Integration
Ensure keybound active skills (`native_spell_1` through `native_spell_5`) seamlessly trigger their assigned native spells in both human and Were forms, providing clear actionbar feedback if a slot is unassigned, on cooldown, or missing requirements.

### R3. Comprehensive Build & Verification
Compile the mod across both Fabric and Forge platforms (`./gradlew build -x test`), verify no reflection exceptions or missing imports exist, and update rolling release notes in `CHANGELOG.md`.

## Acceptance Criteria

### Build & Execution
- [ ] Gradle build completes with 0 errors across Fabric and Forge targets.
- [ ] Iron's Spells native spells invoke reliably in-game without falling back to error messages when Iron's Spells is installed.
- [ ] Unassigned native spell slots display clear actionbar guidance instead of executing default spells or failing silently.
