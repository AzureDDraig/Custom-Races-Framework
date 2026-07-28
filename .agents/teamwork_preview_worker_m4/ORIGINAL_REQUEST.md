## 2026-07-28T16:33:40Z
You are Worker M4 for Milestone 4 (Dynamic Animations, Combat Effects & Multi-Platform Build Verification - R3).
Your working directory is: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m4.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your objective:
1. Implement the GeckoLib keyframe animation controller mapping player state variables:
   - `wereIdleAnim`: standing still (`speed < 0.01f`)
   - `wereWalkAnim`: moving (`speed >= 0.01f`)
   - `wereAttackAnim`: swinging attack (`player.swingTime > 0` or swinging)
   - `wereHurtAnim`: taking damage (`player.hurtTime > 0`)
   - `wereFlyAnim`: flying (`player.getAbilities().flying`)
   - `wereSwimAnim`: swimming (`player.isVisuallySwimming()`)
2. Implement Red Hurt Flash Overlay during damage ticks (`player.hurtTime > 0`) in `GeckoLibWereRenderer`.
3. Support dynamic skin texture binding when configured in `RaceData`.
4. Implement particle aura emission in `PlayerRaceLayer.java` scaled with player scale during transformed state, guarded with 20 Hz tick checks.
5. Run automated unit tests (`./gradlew test`) and multi-platform build (`./gradlew build -x test`) to verify clean compilation across Common, Fabric, and Forge subprojects.
6. Write a detailed handoff report to `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m4\handoff.md`.
