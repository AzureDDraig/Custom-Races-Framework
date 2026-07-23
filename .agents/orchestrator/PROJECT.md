# Project: Comprehensive Iron's Spells Native Spell Casting Resolution

## Architecture
- Common module: core spell handling, capability/data attachments, reflection wrappers for optional Iron's Spells integration.
- Fabric module: Fabric-specific keybinds, network packets, event handlers.
- Forge module: Forge-specific keybinds, network packets, event handlers.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Exploration & Architecture Analysis | Search codebase for native spell implementation, reflection mechanisms, keybinds, Were-form slots | none | DONE |
| 2 | M2: Core Native Spell & Reflection Compatibility | Refactor reflection wrapper to handle Iron's Spells API variations in 1.20.1 and invoke `onCast`/`castSpell` cleanly | M1 | DONE |
| 3 | M3: Native Spell Input & Keybind Binding Integration | Connect active skills `native_spell_1`..`5` in base & Were forms, actionbar feedback | M2 | DONE |
| 4 | M4: Comprehensive Build & Multi-Platform Verification | Gradle build fabric & forge, verification, CHANGELOG update | M3 | IN_PROGRESS |

## Interface Contracts
### Keybind / Active Skill ↔ Spell Execution Engine
- Input: Active skill slot (`native_spell_1` to `native_spell_5`), Player entity, Form (human or Were-form)
- Action: Resolve target spell ID, check cooldowns and requirements, invoke spell casting method, deliver actionbar feedback.

## Code Layout
- `common/`: Shared logic, spell reflection bridge, data components, active skills
- `fabric/`: Fabric entrypoints, keybind registries, networking
- `forge/`: Forge entrypoints, keybind registries, networking
