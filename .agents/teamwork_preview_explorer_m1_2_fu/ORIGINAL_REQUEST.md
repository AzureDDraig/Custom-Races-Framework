## 2026-07-24T18:50:34Z
<USER_REQUEST>
You are Explorer 2 for Milestone 1 of the Custom Races Framework project.
Your working directory is: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_2_fu

Objective: Investigate Requirement R2 (VIP / Permission-Locked Races) and Requirement R3 (Configurable First-Join Selection GUI Toggle).
Tasks:
1. Read and analyze `RaceRegistry.java` and `RaceData.java` for adding `permissionLock` (String) field, NBT/JSON codec serialization, and checking if player possesses permission node before allowing race selection.
2. Read and analyze `RaceSelectionScreen.java` to determine how to render "🔒 VIP / LOCKED" badge with tooltip "§cRequires Permission: §e" + permissionLock and disable the select button for locked races.
3. Read and analyze `RaceRegistry.java`, config JSON handling, and `FirstJoinHandler.java` for implementing `autoOpenSelectionOnJoin` (boolean, default: true) and checking it on first join.
4. Write a comprehensive report (`analysis.md`) and state handoff (`handoff.md`) in your working directory.
5. Send a message to your parent with a summary of findings and the path to your handoff report.
</USER_REQUEST>
