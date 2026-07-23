# Task Brief — Worker 3 (M3 Native Spell Input & Keybind Binding Integration)

## Objective
Enhance `ActiveAbilityHandler.java` and `IronSpellsHandler.java` to fulfill requirement R2 for native spell input, keybinding integration, and actionbar feedback.

## Detailed Requirements
1. **Unassigned Slot Actionbar Feedback**:
   - In `ActiveAbilityHandler.java`, if an active skill slot (1-5) key is pressed and `abilityId` is null, empty, or `"none"`, send actionbar overlay message: `player.displayClientMessage(Component.literal("§cActive Skill Slot " + slot + " is unassigned!"), true)`.

2. **Form Toggle Enforcement**:
   - In `IronSpellsHandler.castNativeSpell`, check `race.enableNativeSpells` (if human form) or `race.enableWereNativeSpells` (if Were-form).
   - If disabled, send actionbar overlay message: `player.displayClientMessage(Component.literal("§cNative Spells are disabled for this race form!"), true)` and return `false`.

3. **Form-Specific Cooldowns & Deferred Cooldown Commitment**:
   - In `ActiveAbilityHandler.java` or `IronSpellsHandler.java`, query form-specific cooldowns (`race.nativeSpellCooldown` vs `race.wereNativeSpellCooldown`).
   - Move cooldown timestamp commitment (`pMap.put(slot, now)`) so it is only committed when spell execution succeeds or is validated, not before.
   - Standardize all cooldown warning messages to Actionbar (`player.displayClientMessage(..., true)`).

4. **Actionbar Overlay Delivery Normalization**:
   - Ensure all ability activation notifications use `player.displayClientMessage(..., true)` (actionbar overlay) rather than posting to Chat.

5. **Build & Verification**:
   - Execute `.\gradlew build -x test` from project root to confirm clean compilation across Common, Fabric, and Forge.

## Output
Write `changes.md` and `handoff.md` in `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m3`.
