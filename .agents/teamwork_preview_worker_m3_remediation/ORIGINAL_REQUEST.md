## 2026-07-23T19:13:40Z

You are Worker M3 Remediation (Configurable Ambient Particle Count Settings Remediation Worker).
Working directory: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m3_remediation

PROJECT SCOPE:
c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\orchestrator\PROJECT.md
ORIGINAL REQUEST:
c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\orchestrator\ORIGINAL_REQUEST.md

FORENSIC AUDITOR M3 AUDIT EVIDENCE REPORT (MUST ADDRESS ALL VIOLATIONS):
c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_auditor_m3\handoff.md
REVIEWER M3 REPORT:
c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_reviewer_m3\handoff.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

YOUR REMEDIATION TASK (MILESTONE 3 REMEDIATION):
Implement the missing Configurable Ambient Particle Count Settings:
1. **Data Model (`RaceData.java`)**:
   - Add `particleCount` (int, default: 5) and `wereParticleCount` (int, default: 10) fields.
   - Add getters/setters (`getParticleCount()`, `setParticleCount(int)`, `getWereParticleCount()`, `setWereParticleCount(int)`).
   - Update NBT serialization/deserialization (`toNBT` / `fromNBT`), Codec mappings, network serialization/deserialization (`ModPackets`), and constructor defaults.
2. **GUI Controls (`RaceCreatorScreen.java`)**:
   - Add GUI EditBox / Slider controls for `particleCount` (range 0–100, default 5) and `wereParticleCount` (range 0–100, default 10).
   - Ensure labels and controls are properly positioned and rendered in `RaceCreatorScreen`.
   - Wire input changes to update `RaceData` when saving/applying race configuration.
3. **Particle Spawning Logic (`PlayerRaceLayer.java` / `ParticleAuraData.java`)**:
   - Update particle spawning logic in `PlayerRaceLayer.java` (and `ParticleAuraData.java`) so ambient particle emission rates scale dynamically based on `race.getParticleCount()` (in human form) or `race.getWereParticleCount()` (when in Were-form).
4. **Build Verification**:
   - Run `./gradlew build -x test` to verify build succeeds with 0 errors across Fabric and Forge targets.

Write your remediation report and handoff to `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m3_remediation\handoff.md` and report via send_message to parent.
