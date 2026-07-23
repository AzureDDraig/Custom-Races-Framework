# Detailed Analysis Report: Native Spell Keybinds & Input Handling

**Module Scope**: `common`, `fabric`, `forge`  
**Target Capabilities**: `native_spell_1` through `native_spell_5` keybinds, network packets, active skill triggers, dynamic reflection engine, and actionbar feedback.

---

## 1. Executive Summary

The Custom Races Framework provides active skill input routing across Fabric and Forge using Architectury's abstraction layer. Active skills 1 through 5 are bound to client keymappings (default keys `G`, `V`, `B`, `N`, `M`), which transmit C2S packet payloads via `customraces:trigger_ability`. Upon receipt, the server resolves active skill mappings for either the human or Were-form, checks cooldown timers, and routes `native_spell_1`..`5` identifiers to `IronSpellsHandler.castNativeSpell(...)`.

While the pipeline is end-to-end functional for standard spell casting, several input handling and actionbar feedback gaps exist that degrade player feedback and configuration precision.

---

## 2. Client Keybinding Registration & Input Handling

### 2.1 Keymapping Declaration (`RaceKeybindings.java`)
Keymappings are defined in `common/src/main/java/ddraig/net/customraces/client/RaceKeybindings.java`:
- Category: `"key.categories.customraces"`
- Slot 1 (`ABILITY_1`): Key `G` (`GLFW_KEY_G`), ID `"key.customraces.ability_1"`
- Slot 2 (`ABILITY_2`): Key `V` (`GLFW_KEY_V`), ID `"key.customraces.ability_2"`
- Slot 3 (`ABILITY_3`): Key `B` (`GLFW_KEY_B`), ID `"key.customraces.ability_3"`
- Slot 4 (`ABILITY_4`): Key `N` (`GLFW_KEY_N`), ID `"key.customraces.ability_4"`
- Slot 5 (`ABILITY_5`): Key `M` (`GLFW_KEY_M`), ID `"key.customraces.ability_5"`

### 2.2 Cross-Platform Initialization
- **Registration**: Calls `dev.architectury.registry.client.keymappings.KeyMappingRegistry.register(ABILITY_X)` for each key during `RaceKeybindings.init()`.
- **Client Entrypoint**: Called inside `CustomRacesClient.init()`.
  - **Fabric**: Triggered in `CustomRacesFabric.onInitializeClient()`.
  - **Forge**: Triggered via `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> CustomRacesClient::init)` in `CustomRacesForge` constructor.
- **Client Tick Listener**: Uses `ClientTickEvent.CLIENT_POST.register(...)`:
  ```java
  ClientTickEvent.CLIENT_POST.register(client -> {
      if (client.player == null || client.screen != null) return;
      while (ABILITY_1.consumeClick()) ModPackets.sendTriggerAbility(1);
      while (ABILITY_2.consumeClick()) ModPackets.sendTriggerAbility(2);
      while (ABILITY_3.consumeClick()) ModPackets.sendTriggerAbility(3);
      while (ABILITY_4.consumeClick()) ModPackets.sendTriggerAbility(4);
      while (ABILITY_5.consumeClick()) ModPackets.sendTriggerAbility(5);
  });
  ```

---

## 3. Network Packet Pipeline (`ModPackets.java`)

### 3.1 Network Architecture
Packet handling is built on `dev.architectury.networking.NetworkManager`.

### 3.2 Packet Specifications
- **Resource Location**: `customraces:trigger_ability` (`TRIGGER_ABILITY_ID`)
- **Direction**: Client to Server (C2S)
- **Payload**: Integer `slot` (values `1` to `5`)

### 3.3 Execution Flow
1. **Client Trigger**: `ModPackets.sendTriggerAbility(int slot)` allocates a `FriendlyByteBuf`, writes the integer `slot`, and dispatches via `NetworkManager.sendToServer(TRIGGER_ABILITY_ID, buf)`.
2. **Server Receiver Registration**: Registered in `ModPackets.register()`:
   ```java
   NetworkManager.registerReceiver(NetworkManager.Side.C2S, TRIGGER_ABILITY_ID, (buf, context) -> {
       int slot = buf.readInt();
       ServerPlayer player = (ServerPlayer) context.getPlayer();
       context.queue(() -> {
           ActiveAbilityHandler.triggerAbility(player, slot);
       });
   });
   ```

---

## 4. Active Skill Trigger Engine (`ActiveAbilityHandler.java`)

### 4.1 Form Mappings & Ability Resolution
When `ActiveAbilityHandler.triggerAbility(player, slot)` executes:
1. Validates input (`player != null`, `1 <= slot <= 5`).
2. Obtains player race via `RaceRegistry.getPlayerRace(player.getUUID())`.
3. Determines state `boolean isWere = WereRaceTransformHandler.isTransformed(player.getUUID())`.
4. Resolves `abilityId`:
   - Checks `race.activeAbilities.get(slot)` for human form.
   - If `isWere` is `true` and `race.wereActiveAbilities.get(slot)` is non-null, non-empty, and not `"none"`, overrides with Were-form active skill.

### 4.2 Native Spell Alias Routing
The handler normalizes `abilityId` and matches the following cases:
- `"native_spell"`, `"native_spell_1"`, `"native_spell1"` $\rightarrow$ `IronSpellsHandler.castNativeSpell(player, race, isWere, 1)`
- `"native_spell_2"`, `"native_spell2"` $\rightarrow$ `IronSpellsHandler.castNativeSpell(player, race, isWere, 2)`
- `"native_spell_3"`, `"native_spell3"` $\rightarrow$ `IronSpellsHandler.castNativeSpell(player, race, isWere, 3)`
- `"native_spell_4"`, `"native_spell4"` $\rightarrow$ `IronSpellsHandler.castNativeSpell(player, race, isWere, 4)`
- `"native_spell_5"`, `"native_spell5"` $\rightarrow$ `IronSpellsHandler.castNativeSpell(player, race, isWere, 5)`

---

## 5. IronSpellsHandler Integration & Reflection Engine

### 5.1 Spell Data Access (`RaceData.java`)
- `race.getNativeSpellId(slot, isWere)`: Resolves `nativeSpellId1`..`5` or `wereNativeSpellId1`..`5`.
- `race.getWildMagic(slot, isWere)`: Resolves `wildMagic1`..`5` or `wereWildMagic1`..`5`.
- `race.getNativeSpellLevel(slot, isWere)`: Resolves `nativeSpellLevel1`..`5` or `wereNativeSpellLevel1`..`5` (clamped between 1 and 10).

### 5.2 Dynamic Reflection Mechanism
`IronSpellsHandler.castNativeSpell(player, race, isWereForm, slot)`:
1. **Wild Magic Processing**: If `isWildMagic` is `true`, selects a random spell ID from `ALL_SPELLS` catalogue (66 entries) and posts a chat message to the player.
2. **Spell Resolution (`resolveSpellObject`)**: Reflectively queries `SpellRegistry` classes (`net.ironsspellbooks.api.registry.SpellRegistry`, `io.github.elytra.irons_spellbooks.api.registry.SpellRegistry`, etc.) and unwraps `Holder` or `AbstractSpell` instances.
3. **Spell Invocation (`invokeSpellCast`)**: Searches target spell class for `onCast`, `cast`, or `initiate` methods and reflects method arguments (`Level`, `spellLevel`, `Player`, `CastSource`, `MagicData`).

---

## 6. Actionbar Feedback Handlers & Key Gaps

### 6.1 Current Actionbar Message Deliveries
Messages sent with overlay flag `true` (`displayClientMessage(..., true)` or `sendSystemMessage(..., true)`):
- **Cooldown Warning**: `§cAbility <slot> on cooldown! (<remainingSec>s)` (`ActiveAbilityHandler.java:55`)
- **Spell Cast Success**: `§d✨ [Native Spell <slot>] §fCast <spell_name> (Lvl <level>)` (`IronSpellsHandler.java:104`)
- **Missing Mod Diagnostic**: `§c[Native Spell <slot>] §f<spell_id> §7(Requires Iron's Spells mod)` (`IronSpellsHandler.java:118`)
- **Invocation Failure**: `§c[Native Spell <slot>] §fCould not invoke spell: §e<spell_id> §7(Verify spell ID format)` (`IronSpellsHandler.java:120`)

### 6.2 Identified Deficiencies & Gaps

| # | Gap / Defect | Location | Description & Impact |
|---|--------------|----------|----------------------|
| 1 | **Unassigned Slot Silent Return** | `ActiveAbilityHandler.java:46` | If an active slot has no bound skill (`null`, `""`, or `"none"`), the server returns silently. Players receive no actionbar message indicating the slot is empty. |
| 2 | **Ignored `enableNativeSpells` Toggle** | `IronSpellsHandler.java:77-90` | `RaceData.enableNativeSpells` and `enableWereNativeSpells` flags are defined in `RaceData.java` but never checked in `castNativeSpell`. Spells cast even if native spells are toggled off in the race creator GUI. |
| 3 | **Hardcoded Global Cooldown** | `ActiveAbilityHandler.java:31,53` | `ActiveAbilityHandler` enforces a static 10-second cooldown (`DEFAULT_COOLDOWN_MS = 10000`) for all skills, ignoring `race.nativeSpellCooldown` and `race.wereNativeSpellCooldown`. |
| 4 | **Chat vs Actionbar Inconsistency** | `ActiveAbilityHandler.java:67` | `player.sendSystemMessage(...)` is called without the boolean `true` parameter, sending non-spell ability activation messages to the player's Chat box instead of Actionbar. |
| 5 | **Premature Cooldown Timestamp** | `ActiveAbilityHandler.java:60` | Cooldown timestamp is committed to `COOLDOWNS` map before `castNativeSpell` is invoked or validated. If spell resolution fails or is invalid, the player is still penalized with a 10s cooldown. |
