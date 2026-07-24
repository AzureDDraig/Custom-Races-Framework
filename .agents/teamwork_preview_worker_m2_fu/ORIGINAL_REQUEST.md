## 2026-07-24T18:51:33Z
<USER_REQUEST>
You are Worker M2 for Milestone 2 of the Custom Races Framework project.
Your working directory is: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m2_fu

Objective: Implement Requirement R1 (Were-Form Model & Texture Rendering Fix).
Detailed Task Instructions:
1. Refer to the M1 Explorer analysis report at `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_1_fu\handoff.md` and `analysis.md`.
2. Verify asset existence of `default_werewolf.png` at `common/src/main/resources/assets/customraces/textures/were/default_werewolf.png`.
3. In `WereModelRenderer.java`:
   - Overload/update `getValidWereTextureLocation` to `public static ResourceLocation getValidWereTextureLocation(AbstractClientPlayer player, RaceData race)`.
   - Intercept `"skin"` and `"player"` keywords (case-insensitive, trimmed) and return `player.getSkinTextureLocation()`.
   - Implement null/empty/"none" checks returning `DEFAULT_WERE_TEXTURE`.
   - Implement path & extension normalization (default namespace `customraces`, prefix `textures/`, suffix `.png` if missing).
   - Implement client-side `ResourceManager` existence validation (`Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()`) to verify physical asset presence.
   - Implement a safe fallback: if custom asset does not exist on disk, fall back to `DEFAULT_WERE_TEXTURE`; if `DEFAULT_WERE_TEXTURE` cannot be loaded, fall back to `player.getSkinTextureLocation()`.
4. Update all call sites in `WereModelRenderer.java` and `PlayerRaceLayer.java` to invoke the updated method passing `player`.
5. Run build verification: `./gradlew build -x test` and document exact command and results.
6. Write a full handoff report (`handoff.md`) and summary of changes (`changes.md`) in your working directory.
7. Send a message to your parent with build results and path to your handoff report.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
</USER_REQUEST>
