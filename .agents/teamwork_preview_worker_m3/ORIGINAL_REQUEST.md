## 2026-07-23T19:12:43Z
You are Worker M3 (Configurable Ambient Particle Count Settings Worker).
Working directory: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m3

PROJECT SCOPE:
c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\orchestrator\PROJECT.md
ORIGINAL REQUEST:
c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\orchestrator\ORIGINAL_REQUEST.md

EXPLORER 3 ANALYSIS REPORT TO READ AND IMPLEMENT:
- c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_3\analysis.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

YOUR TASK (MILESTONE 3):
Implement Configurable Ambient Particle Count Settings:
1. **Data Model (`RaceData.java`)**:
   - Add `particleCount` (int, default: 5) and `wereParticleCount` (int, default: 10) fields to `RaceData.java`.
   - Update NBT serialization (`toNBT` / `fromNBT`), Codec definitions, network packet serialization/deserialization, and getters/setters.
2. **GUI Controls (`RaceCreatorScreen.java`)**:
   - Add input/slider controls to `RaceCreatorScreen.java` allowing users to configure `particleCount` and `wereParticleCount`.
   - Connect the GUI components to read from and write to `RaceData` during race creation/editing.
3. **Particle Emission Rate Scaling (`PlayerRaceLayer.java` / `ParticleAuraData.java`)**:
   - Connect ambient particle spawning logic in `PlayerRaceLayer.java` (and/or `ParticleAuraData.java`) so particle density/emission rates dynamically scale based on `particleCount` (in human form) or `wereParticleCount` (in Were form).
4. **Build Verification**:
   - Run `./gradlew build -x test` to ensure 0 errors across Fabric and Forge targets.

Write your handoff report to `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m3\handoff.md` and report via send_message to parent.
