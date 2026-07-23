# Handoff Report — Explorer 1 (Keybinds & Input Handling)

## 1. Observation

### 1.1 Keybinding & Client Tick Handling
- **File**: `common/src/main/java/ddraig/net/customraces/client/RaceKeybindings.java`
  - Lines 16-20: 5 `KeyMapping` objects declared for `key.customraces.ability_1` through `ability_5` with default keys G, V, B, N, M in category `key.categories.customraces`.
  - Lines 23-27: `KeyMappingRegistry.register(ABILITY_X)` registers all 5 keymappings.
  - Lines 29-37: `ClientTickEvent.CLIENT_POST.register(...)` checks `client.player == null || client.screen != null` and invokes `ModPackets.sendTriggerAbility(slot)` on click consumption (`consumeClick()`).
- **File**: `common/src/main/java/ddraig/net/customraces/client/CustomRacesClient.java`
  - Line 8: `RaceKeybindings.init()` called from client entrypoint.
- **File**: `fabric/src/main/java/ddraig/net/customraces/fabric/CustomRacesFabric.java`
  - Line 19: `CustomRacesClient.init()` called inside `onInitializeClient()`.
- **File**: `forge/src/main/java/ddraig/net/customraces/forge/CustomRacesForge.java`
  - Line 23: `CustomRacesClient.init()` called inside `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)`.

### 1.2 Networking & Packet Dispatch
- **File**: `common/src/main/java/ddraig/net/customraces/network/ModPackets.java`
  - Line 33: `TRIGGER_ABILITY_ID = new ResourceLocation("customraces", "trigger_ability")`.
  - Lines 138-144: Server C2S receiver registration:
    ```java
    NetworkManager.registerReceiver(NetworkManager.Side.C2S, TRIGGER_ABILITY_ID, (buf, context) -> {
        int slot = buf.readInt();
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        context.queue(() -> {
            ActiveAbilityHandler.triggerAbility(player, slot);
        });
    });
    ```
  - Lines 194-198: Client packet dispatch helper: `sendTriggerAbility(int slot)` writes `slot` int and calls `NetworkManager.sendToServer(...)`.

### 1.3 Active Skill Execution & Actionbar Messaging
- **File**: `common/src/main/java/ddraig/net/customraces/ability/ActiveAbilityHandler.java`
  - Lines 39-45: Resolves active ability string from `race.activeAbilities` or `race.wereActiveAbilities`.
  - Line 46: `if (abilityId == null || abilityId.isEmpty() || abilityId.equals("none")) return;` (Silent return for unassigned slots).
  - Lines 53-57: Cooldown check using fixed 10,000ms: `player.sendSystemMessage(Component.literal("§cAbility " + slot + " on cooldown! (" + remainingSec + "s)"), true)` (Sends to actionbar).
  - Line 60: `pMap.put(slot, now);` (Commits cooldown timestamp before spell validation).
  - Line 67: `player.sendSystemMessage(Component.literal("§aUsed Ability: §e" + abilityId.replace("_", " ")));` (Sends to Chat instead of Actionbar).
  - Lines 70-90: Case routing for `native_spell`, `native_spell_1`..`5`, `native_spell1`..`5` $\rightarrow$ calls `IronSpellsHandler.castNativeSpell(player, race, isWere, slot)`.

### 1.4 Native Spell Integration Handler
- **File**: `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java`
  - Lines 77-83: Extracts spell parameters via `race.getNativeSpellId(...)`, `race.getWildMagic(...)`, `race.getNativeSpellLevel(...)`.
  - Lines 104, 118, 120: Actionbar overlay messages (`displayClientMessage(Component, true)`) for spell cast success, missing mod, and invocation failure.
  - Absence of `enableNativeSpells` check: `castNativeSpell` never evaluates `race.enableNativeSpells` or `race.enableWereNativeSpells`.

---

## 2. Logic Chain

1. **Input Pipeline Trace**:
   - Pressing keys G, V, B, N, or M in-game consumes clicks in `RaceKeybindings.java` and triggers `ModPackets.sendTriggerAbility(1..5)`.
   - `sendTriggerAbility` sends `customraces:trigger_ability` C2S payload via Architectury `NetworkManager`.
   - `ModPackets` server receiver enqueues `ActiveAbilityHandler.triggerAbility(player, slot)` on the server thread.
   - *Logic Conclusion*: The keybind-to-packet pipeline is fully functional and symmetric across Fabric and Forge.

2. **Active Skill & Native Spell Mapping**:
   - `ActiveAbilityHandler` reads `race.activeAbilities.get(slot)` or `race.wereActiveAbilities.get(slot)` when transformed.
   - Aliases `native_spell_1` through `native_spell_5` cleanly map to `IronSpellsHandler.castNativeSpell(player, race, isWere, 1..5)`.
   - *Logic Conclusion*: Skill routing to native spells is correctly wired, but fails to check if native spells are enabled or configured.

3. **Actionbar Feedback & User Experience Gaps**:
   - When an unassigned slot is pressed, `ActiveAbilityHandler.java:46` returns immediately without giving the player feedback.
   - When a native spell is pressed, feedback is delivered to the Actionbar, but standard active skills post to Chat because `sendSystemMessage` is missing the `true` overlay parameter.
   - Cooldown is set prematurely before `IronSpellsHandler` confirms spell validity.
   - *Logic Conclusion*: Actionbar feedback requires normalization to handle empty slots, disable toggles, custom cooldown durations, and consistent overlay delivery.

---

## 3. Caveats

- **Runtime Mod Integration Testing**: Did not execute active game client with Iron's Spells 'n Spellbooks JAR loaded; analysis is based on static reflection code inspection of `IronSpellsHandler.java`.
- **Architectury Event Differences**: Architectury handles cross-platform keybinding registration uniformly via `KeyMappingRegistry`, but minor version differences in keybinding consumption across Minecraft 1.20.1 versions were not tested dynamically.

---

## 4. Conclusion

The native spell keybind and input handling system (`native_spell_1` through `native_spell_5`) has a complete architectural foundation across Common, Fabric, and Forge. However, to fulfill the Milestone 3 requirements, the following actionable improvements should be implemented in M3:

1. **Actionbar Feedback for Unassigned Slots**: Add explicit actionbar feedback (`player.displayClientMessage(Component.literal("§cActive Skill Slot " + slot + " is unassigned!"), true)`) when an unbound slot key is pressed.
2. **Honor `enableNativeSpells` Toggles**: Enforce `race.enableNativeSpells` (human form) and `race.enableWereNativeSpells` (Were-form) in `IronSpellsHandler.castNativeSpell`.
3. **Dynamic Cooldown Integration**: Replace the static 10s cooldown in `ActiveAbilityHandler` with `race.nativeSpellCooldown` / `race.wereNativeSpellCooldown` for native spells.
4. **Actionbar Message Normalization**: Standardize all ability feedback messages to use actionbar overlay (`displayClientMessage(..., true)`).
5. **Defer Cooldown Commitment**: Move `pMap.put(slot, now)` after successful spell validation to prevent penalizing players on failed casts.

---

## 5. Verification Method

### 5.1 Codebase Inspection Verification
- Inspect `common/src/main/java/ddraig/net/customraces/client/RaceKeybindings.java` lines 16-37 to verify keymappings and client tick loop.
- Inspect `common/src/main/java/ddraig/net/customraces/network/ModPackets.java` lines 138-144 and 194-198 to verify packet ID `customraces:trigger_ability`.
- Inspect `common/src/main/java/ddraig/net/customraces/ability/ActiveAbilityHandler.java` lines 33-90 to verify slot resolution and `native_spell_1`..`5` routing.
- Inspect `common/src/main/java/ddraig/net/customraces/integration/IronSpellsHandler.java` lines 77-127 to verify spell execution and actionbar display calls.

### 5.2 Build Command Verification
Run Gradle build to ensure no compilation errors:
```bash
./gradlew build
```
In invalidation conditions: build failures or class not found errors in `RaceKeybindings` or `ModPackets`.
