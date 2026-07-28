## 2026-07-28T11:25:41Z
You are Worker M3 for Custom Race GeckoLib Player Model Overhaul.

Your working directory is: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m3
Project scope document: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\orchestrator\PROJECT.md
Original user request: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\orchestrator\ORIGINAL_REQUEST.md
Explorer 2 Report: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_2\handoff.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

REQUIREMENT SCOPE — Milestone 3: Base Human Player Model Suppression Guardrails (R2)
1. **Extend Base Model Suppression in `WereModelRenderer.java`**:
   - In `setBaseModelVisible(PlayerModel<?> model, boolean visible)`, add suppression for `model.cloak` (Cape) and `model.ear` (Deadmau5 ears) alongside head, hat, body, arms, legs, and clothing overlays (`jacket`, `rightSleeve`, `leftSleeve`, `rightPants`, `leftPants`).

2. **Fail-Safe Fallback Guardrails ("Never Invisible")**:
   - Verify `LivingEntityRendererMixin.java` and `WereModelRenderer.isModelAvailable()` condition base model suppression strictly on valid GeckoLib model baking and bone structure integrity.
   - If a custom GeckoLib model fails to load, falls back, is unassigned, has empty top-level bones, or encounters a rendering error, `WereModelRenderer.renderWereForm()` MUST return `false`, restore base model visibility (`setBaseModelVisible(true)`), and fall back to `renderWereBeastParts()` (procedural ears/tail/snout), guaranteeing players are NEVER invisible under any circumstance.

3. **Invisibility Effect & Spectator Handling**:
   - In `GeckoLibWereRenderer.java` and `PlayerRaceLayer.java`, handle `player.isInvisible()` and `player.isSpectator()`.
   - Ensure transformed players with the Invisibility status effect or in Spectator mode use translucent buffer rendering (`RenderType.entityTranslucent()`) or properly respect entity invisibility instead of rendering fully opaque.

4. **Multi-Platform Build & Test Verification**:
   - Execute `./gradlew build -x test` and `./gradlew test` to verify multi-platform compilation across Fabric and Forge.
   - Document all file modifications and test results in `handoff.md`.

Create your working directory `.agents/teamwork_preview_worker_m3`, write `progress.md`, `changes.md`, and `handoff.md`, then send a completion message to parent when finished.
