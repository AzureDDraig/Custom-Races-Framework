# Analysis Report — Native Spell Slots (1-5) & Capabilities (Base Form vs Were-Form)

**Author:** Explorer 3 (Base Form vs Were-Form Spell Slots & Capabilities)  
**Date:** 2026-07-23  
**Project:** Custom Races Framework  

---

## Executive Summary
This report presents a complete investigation of how native spell slots (Slots 1–5), spell capabilities, form-switching overrides, and configuration handling are implemented and managed across base human form and Were-form in the Custom Races Framework codebase.

All active spell slot configurations are stored in `RaceData.java`, serialized via Gson in `RaceRegistry.java`, configured via `RaceCreatorScreen.java`, bound to client keybindings (`RaceKeybindings.java`), dispatched over C2S packets (`ModPackets.java`), evaluated server-side (`ActiveAbilityHandler.java` & `WereRaceTransformHandler.java`), and dynamically cast using soft-reflection integration with Iron's Spells 'n Spellbooks and T.O Tweaks (`IronSpellsHandler.java`).

---

## 1. Data Attachment & Capability Definitions

### 1.1 Central Data Structure (`RaceData.java`)
All race configuration attributes—including base statistics, body parts, active abilities, passive abilities, were-form parameters, and native spell configurations—are stored inside `ddraig.net.customraces.data.RaceData`.

`RaceData` maintains distinct, decoupled field sets for **Base Human Form** and **Were-Form**:

| Form | Global Toggle | Slot Fields (X = 1..5) | Shared Parameters |
|---|---|---|---|
| **Base Human Form** | `enableNativeSpells` (boolean, default: `true`) | `nativeSpellIdX` (String)<br>`wildMagicX` (boolean)<br>`nativeSpellLevelX` (int, default: 1) | `nativeSpellCooldown` (int, default: 100)<br>`nativeSpellManaCost` (int, default: 25) |
| **Were-Form** | `enableWereNativeSpells` (boolean, default: `true`) | `wereNativeSpellIdX` (String)<br>`wereWildMagicX` (boolean)<br>`wereNativeSpellLevelX` (int, default: 2) | `wereNativeSpellCooldown` (int, default: 60)<br>`wereNativeSpellManaCost` (int, default: 20) |

*Note on Slot 1 Legacy Compatibility:*  
`RaceData` maintains legacy fields for Slot 1 (`nativeSpellId`, `wildMagic`, `nativeSpellLevel` for base form; `wereNativeSpellId`, `wereWildMagic`, `wereNativeSpellLevel` for Were-form). The helper getter methods handle fallback smoothly.

### 1.2 Accessor / Resolution Logic (`RaceData.java`)
`RaceData` provides helper methods to query spell parameters by slot (1–5) and form state (`isWere`):

1. **`getNativeSpellId(int slot, boolean isWere)`**:
   - `isWere == true`: Returns `wereNativeSpellId2`..`wereNativeSpellId5` for slots 2–5. For slot 1/default, returns `wereNativeSpellId1` if non-empty, else falls back to `wereNativeSpellId`.
   - `isWere == false`: Returns `nativeSpellId2`..`nativeSpellId5` for slots 2–5. For slot 1/default, returns `nativeSpellId1` if non-empty, else falls back to `nativeSpellId`.

2. **`getWildMagic(int slot, boolean isWere)`**:
   - `isWere == true`: Returns `wereWildMagic2`..`wereWildMagic5` for slots 2–5, or `wereWildMagic1 || wereWildMagic` for slot 1.
   - `isWere == false`: Returns `wildMagic2`..`wildMagic5` for slots 2–5, or `wildMagic1 || wildMagic` for slot 1.

3. **`getNativeSpellLevel(int slot, boolean isWere)`**:
   - `isWere == true`: Returns `wereNativeSpellLevelX` if > 0 (fallback level 2).
   - `isWere == false`: Returns `nativeSpellLevelX` if > 0 (fallback level 1).

### 1.3 Active Skill Slot Maps
- `activeAbilities` (`Map<Integer, String>`): Maps keybind slots 1–5 to active ability IDs for base human form.
- `wereActiveAbilities` (`Map<Integer, String>`): Maps keybind slots 1–5 to active ability IDs for Were-form.

---

## 2. Form Switching & Override Pipeline

### 2.1 Transformation Tracking (`WereRaceTransformHandler.java`)
Transformation checks run on server tick (`TickEvent.PLAYER_POST`, every 40 ticks / 2 seconds).

- **Transformation Triggers**: Evaluated against `race.wereTriggerCondition`:
  - `FULL_MOON` (night & moon phase 0)
  - `NEW_MOON` (night & moon phase 4)
  - `NIGHT` (isNight)
  - `DAY` (!isNight)
  - `WATER` / `SUBMERGED` (in water)
  - `RAGE` / `LOW_HEALTH` (health <= 30%)
  - `KEY` / `MANUAL` (toggled via active ability `"transform_were"` / `"were_transform"` or packet `TOGGLE_WERE_FORM_ID`).
- **State Storage & Sync**:
  - Server Map: `WereRaceTransformHandler.TRANSFORMED_PLAYERS` (`Map<UUID, Boolean>`).
  - Client Map: `ClientWereState.TRANSFORMED_PLAYERS` (`Map<UUID, Boolean>`), synchronized via S2C packet `SYNC_WERE_STATE_ID`.
  - Global check: `WereRaceTransformHandler.isTransformed(UUID uuid)` checks server map first, falling back to `ClientWereState.isTransformed(uuid)`.

### 2.2 Active Ability Override Mechanism
When a player triggers keybind slot $S$ (1–5):
```java
String abilityId = race.activeAbilities != null ? race.activeAbilities.get(slot) : null;
if (WereRaceTransformHandler.isTransformed(player.getUUID()) && race.wereActiveAbilities != null) {
    String wAbility = race.wereActiveAbilities.get(slot);
    if (wAbility != null && !wAbility.isEmpty() && !"none".equalsIgnoreCase(wAbility)) {
        abilityId = wAbility;
    }
}
```
If the player is in Were-form and `wereActiveAbilities` defines a valid ability for slot $S$, it **overrides** `activeAbilities.get(S)`.

---

## 3. Active Skill & Keybinding Architecture

```
[ Client Key Press (G, V, B, N, M) ]
             │
             ▼
   RaceKeybindings.java  ──(ABILITY_1..5.consumeClick())──► ModPackets.sendTriggerAbility(slot)
                                                                     │
                                                               C2S Packet (TRIGGER_ABILITY_ID)
                                                                     │
                                                                     ▼
                                                         ActiveAbilityHandler.triggerAbility(player, slot)
                                                                     │
                                                     Is player in Were-form?
                                                     ├── YES ──► Check race.wereActiveAbilities.get(slot)
                                                     └── NO  ──► Check race.activeAbilities.get(slot)
                                                                     │
                                                                     ▼
                                                         Switch (abilityId)
                                                         ├── "native_spell_1" ──► IronSpellsHandler.castNativeSpell(player, race, isWere, 1)
                                                         ├── "native_spell_2" ──► IronSpellsHandler.castNativeSpell(player, race, isWere, 2)
                                                         ├── "native_spell_3" ──► IronSpellsHandler.castNativeSpell(player, race, isWere, 3)
                                                         ├── "native_spell_4" ──► IronSpellsHandler.castNativeSpell(player, race, isWere, 4)
                                                         └── "native_spell_5" ──► IronSpellsHandler.castNativeSpell(player, race, isWere, 5)
```

---

## 4. Runtime Casting & Iron's Spells Integration (`IronSpellsHandler.java`)

When `IronSpellsHandler.castNativeSpell(Player player, RaceData race, boolean isWereForm, int slot)` is invoked:

1. **Parameter Resolution**:
   - `isWildMagic` = `race.getWildMagic(slot, isWereForm)`
   - `spellId` = `race.getNativeSpellId(slot, isWereForm)`
   - `spellLevel` = `race.getNativeSpellLevel(slot, isWereForm)` (clamped between 1 and 10).

2. **Wild Magic Trigger**:
   - If `isWildMagic` is `true`, `spellId` is randomly selected from `ALL_SPELLS` (catalogue of 64 spells spanning Fire, Ice, Lightning, Holy, Ender, Blood, Evocation, Eldritch/Celestial, and T.O Tweaks).
   - System message sent: `"✨ [Wild Magic] Casting random spell: <spellId>"`.

3. **Spell Object Resolution (`resolveSpellObject`)**:
   - Formats `spellId` (prepends `"irons_spellbooks:"` if non-namespaced).
   - Dynamically inspects known Iron's Spells registry classes (`net.ironsspellbooks.api.registry.SpellRegistry`, `io.github.elytra.irons_spellbooks.api.registry.SpellRegistry`, etc.) via Java Reflection.
   - Invokes `getSpell(ResourceLocation)` / `getSpell(String)` and unwraps holder objects (`unwrapSpellHolder`).

4. **Spell Casting Invocation (`invokeSpellCast`)**:
   - Obtains `CastSource` enum (`SPELLBOOK` or `INNATE`).
   - Obtains or instantiates `MagicData` for player.
   - Iterates through spell methods searching for `onCast`, `cast`, or `initiate`.
   - Dynamically constructs parameters (`Level`, `spellLevel`, `Player`/`LivingEntity`, `CastSource`, `MagicData`).
   - Invokes method reflectively. On success, renders enchant particles and actionbar text (`"✨ [Native Spell X] Cast <spell> (Lvl Y)"`).

5. **Fallback Handling**:
   - If Iron's Spells is not loaded or spell invocation fails, actionbar feedback is displayed:  
     `"§c[Native Spell X] <spellId> (Requires Iron's Spells mod)"` or `"§c[Native Spell X] Could not invoke spell: <spellId>"` along with dragon breath/witch particles.

6. **Iron's Spells Attribute Modifiers (`applyIronSpellsAttributes`)**:
   - During passive ability processing (`PassiveAbilityHandler.java`), passives like `arcane_overflow`, `mana_fountain`, `arcane_amplification`, `spell_ward`, and school masteries (`fire_spell_mastery`, etc.) dynamically apply transient `AttributeModifier`s from Iron's Spells `AttributeRegistry` (e.g., `MAX_MANA`, `MANA_REGEN`, `SPELL_POWER`, etc.).

---

## 5. Configuration & UI Integration (`RaceCreatorScreen.java`)

### 5.1 JSON Storage (`races.json`)
All race definitions are stored in `config/custom_races/races.json` and saved atomically via `RaceRegistry.saveRaces()`.

### 5.2 Race Creator GUI (Tab 11 - "Native Spells (Slots 1 to 5)")
- **Form Context**: Controlled by `editingWereForm` toggle.
- **Global Toggle**: Checkbox for `Enable Native Spells` (`enableNativeSpells` / `enableWereNativeSpells`).
- **Slot Selection**: 5 sub-buttons (`Slot 1` through `Slot 5`) updating `selectedNativeSpellSlot`.
- **Slot Controls**:
  - `✨ Wild Magic` checkbox.
  - `Spell ID` EditBox (with `▶ Cycle Spell` button cycling through all 64 registered spells).
  - `Spell Level` EditBox (1–10).
  - `⚡ Auto-Assign to Active Slot X` button: Puts `"native_spell_X"` directly into `activeAbilities.put(X, "native_spell_X")` (or `wereActiveAbilities` if editing Were-form).

---

## 6. Key Findings & Architectural Insights

1. **Complete Form Decoupling**:
   Base human form and Were-form spell configurations are 100% independent in `RaceData`. A character can have an Ender/Teleport spell in base form, and automatically switch to a Fire/Flame Breath spell in Were-form.

2. **Active Ability Routing Precision**:
   `ActiveAbilityHandler.triggerAbility` uses the mapped `abilityId` string to determine which slot index (1–5) to pass to `IronSpellsHandler.castNativeSpell`.
   - `native_spell` / `native_spell_1` / `native_spell1` -> slot 1
   - `native_spell_2` / `native_spell2` -> slot 2
   - `native_spell_3` / `native_spell3` -> slot 3
   - `native_spell_4` / `native_spell4` -> slot 4
   - `native_spell_5` / `native_spell5` -> slot 5

3. **Reflection-Based Integration**:
   `IronSpellsHandler` uses reflection against multiple potential package paths for `SpellRegistry`, `CastSource`, `MagicData`, and `AttributeRegistry`. This provides robust compatibility across different versions of Iron's Spells 'n Spellbooks in 1.20.1.

4. **Actionbar Feedback & Diagnostics**:
   Casting native spells delivers clear actionbar messages for success, Wild Magic triggers, missing mod warnings, and invalid spell IDs.
