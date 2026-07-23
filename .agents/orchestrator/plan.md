# Execution Plan — Comprehensive Iron's Spells Native Spell Casting Resolution

## Milestone Overview
- **M1: Exploration & Architecture Analysis**
  - Dispatch Explorer agents to investigate native spell casting across common, fabric, and forge modules.
  - Locate `native_spell_1` through `native_spell_5` keybind bindings, spell resolution logic, reflection wrappers for Iron's Spells 'n Spellbooks API variations in 1.20.1, and actionbar feedback handlers.
  - Analyze differences between base form and Were-form spell slots.
- **M2: Core Native Spell & Reflection Compatibility Implementation**
  - Refactor/Implement reflection wrapper for Iron's Spells 'n Spellbooks API variations across common, fabric, forge.
  - Ensure resolution of spell IDs for all 5 slots (base and Were-form).
  - Ensure `onCast`/`castSpell` invocation with valid parameter signatures (`Level`, `spellLevel`, `ServerPlayer`/`LivingEntity`, `CastSource`, `MagicData`).
- **M3: Native Spell Input & Keybind Binding Integration**
  - Connect keybound active skills (`native_spell_1`..`5`) to native spell resolution and casting.
  - Provide actionbar feedback for unassigned slots, cooldowns, or missing requirements.
- **M4: Comprehensive Build & Multi-Platform Verification**
  - Perform `./gradlew build -x test` on Fabric and Forge via Worker agents.
  - Perform code reviews, challenger stress testing, and forensic audit.
  - Update `CHANGELOG.md`.
