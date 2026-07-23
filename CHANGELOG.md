# 📜 Custom Races Framework - Rolling Changelog

All notable changes, features, bug fixes, and build deployments for **Custom Races Framework** are documented here.

---

## [1.0.0-b044a] - 2026-07-22

### 🔍 Real-Time Deep Search Across Passives, Actives, and Drawbacks
- **GUI Search Boxes (`RaceCreatorScreen.java`)**:
  - Added dedicated search input bars at `contentTop` for **Tab 3 (Passives)**, **Tab 4 (Actives)**, and **Tab 10 (Drawbacks)**.
  - Shifted grid and checkbox layouts cleanly to `contentTop + 24`, guaranteeing zero overlap over headers or search inputs.
- **Deep Tooltip & Description Searching**:
  - Search queries match **BOTH** item/drawback names **AND** their full description/tooltip text (e.g. searching *"fire"*, *"armor"*, *"speed"*, *"iron"*, *"damage"*, *"slowness"* instantly isolates all matching traits and drawbacks).

---

## [1.0.0-b043a] - 2026-07-22

### 🔍 Native Spells & Iron's Spells System Audit
- **Full System Audit for Native Spells (`IronSpellsHandler.java`, `ActiveAbilityHandler.java`, `RaceCreatorScreen.java`)**:
  - Conducted line-by-line verification of Native Spells integration across data persistence, dynamic UI tab positioning, soft reflection execution engine, active skill keybinds, and 10 language localizations.
