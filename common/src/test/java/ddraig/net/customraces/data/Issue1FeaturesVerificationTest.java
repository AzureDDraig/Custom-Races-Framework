package ddraig.net.customraces.data;

import ddraig.net.customraces.integration.IronSpellsHandler;

public class Issue1FeaturesVerificationTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  GITHUB ISSUE #1: FEATURES & FIXES VERIFICATION TEST SUITE       ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: ParticleAuraData placement and form condition
        try {
            System.out.println("\n--- Running Test 1: ParticleAuraData Placement & Conditions ---");
            ParticleAuraData aura = new ParticleAuraData("minecraft:flame", 2.0f, 0.1f, 0.4f, "head", "were_only");
            assertEquals("head", aura.getValidPlacement());
            assertEquals("were_only", aura.getValidFormCondition());
            assertTrue(!aura.matchesForm(false), "Were only should not match base form");
            assertTrue(aura.matchesForm(true), "Were only should match were form");

            ParticleAuraData normalOnlyAura = new ParticleAuraData("irons_spellbooks:fire_spark", 1.0f, 0.05f, 0.5f, "feet", "normal_only");
            assertEquals("feet", normalOnlyAura.getValidPlacement());
            assertTrue(normalOnlyAura.matchesForm(false), "Normal only should match base form");
            assertTrue(!normalOnlyAura.matchesForm(true), "Normal only should not match were form");

            ParticleAuraData fallbackAura = new ParticleAuraData(null, -1.0f, -0.05f, -0.2f, "invalid_pos", "invalid_cond");
            assertEquals("body", fallbackAura.getValidPlacement());
            assertEquals("always", fallbackAura.getValidFormCondition());
            assertEquals("minecraft:flame", fallbackAura.getValidParticleType());
            assertTrue(fallbackAura.getSafeSpread() >= 0.1f, "Safe spread must be >= 0.1f");
            assertTrue(fallbackAura.getSafeSpeed() >= 0.0f, "Safe speed must be >= 0.0f");

            System.out.println("  [PASS] ParticleAuraData placement, form condition, and sanitization verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: PassiveAbilityDescriptions Dictionary
        try {
            System.out.println("\n--- Running Test 2: PassiveAbilityDescriptions Dictionary ---");
            PassiveAbilityDescriptions.AbilityInfo nv = PassiveAbilityDescriptions.get("night_vision");
            assertTrue(nv != null, "night_vision info must not be null");
            assertEquals("Night Vision", nv.displayName());
            assertEquals("Elemental", nv.category());
            assertTrue(!nv.description().isEmpty(), "Description must not be empty");
            assertTrue(!nv.stats().isEmpty(), "Stats must not be empty");

            PassiveAbilityDescriptions.AbilityInfo custom = PassiveAbilityDescriptions.get("custom_ability_unknown");
            assertTrue(custom != null, "Fallback info must not be null");
            assertEquals("Custom Ability Unknown", custom.displayName());
            assertEquals("General", custom.category());

            System.out.println("  [PASS] PassiveAbilityDescriptions dictionary and fallbacks verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: RaceData Cooldowns & Restricted Items
        try {
            System.out.println("\n--- Running Test 3: RaceData Cooldowns & Restricted Items ---");
            RaceData race = new RaceData();
            race.abilityCooldowns.put(1, 5);  // Slot 1: 5s
            race.abilityCooldowns.put(2, 20); // Slot 2: 20s
            race.wereAbilityCooldowns.put(1, 2); // Were Slot 1: 2s

            assertEquals(5, race.getAbilityCooldown(1, false));
            assertEquals(20, race.getAbilityCooldown(2, false));
            assertEquals(10, race.getAbilityCooldown(3, false)); // Default 10s

            assertEquals(2, race.getAbilityCooldown(1, true));
            assertEquals(20, race.getAbilityCooldown(2, true)); // Fallback to base

            race.restrictedItems.add("minecraft:shield");
            race.restrictedItems.add("epicfight:greatsword");
            race.restrictedFoods.add("minecraft:cooked_beef");

            assertTrue(race.isItemRestricted("minecraft:shield"), "Shield must be restricted");
            assertTrue(race.isItemRestricted("  MINECRAFT:SHIELD  "), "Whitespace/case insensitive");
            assertTrue(race.isItemRestricted("epicfight:greatsword"), "Epic fight sword restricted");
            assertTrue(!race.isItemRestricted("minecraft:iron_sword"), "Iron sword not restricted");

            assertTrue(race.isFoodRestricted("minecraft:cooked_beef"), "Cooked beef restricted");
            assertTrue(!race.isFoodRestricted("minecraft:apple"), "Apple not restricted");

            System.out.println("  [PASS] Custom ability cooldowns and item/food restrictions verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 4: Iron's Spells Catalogue Expansion
        try {
            System.out.println("\n--- Running Test 4: Iron's Spells Catalogue Expansion ---");
            assertTrue(!IronSpellsHandler.ALL_SPELLS.isEmpty(), "Spell list must not be empty");
            assertTrue(IronSpellsHandler.ALL_SPELLS.contains("irons_spellbooks:firebolt"), "Contains firebolt");
            assertTrue(IronSpellsHandler.ALL_SPELLS.contains("irons_spellbooks:ray_of_frost"), "Contains ray of frost");
            assertTrue(IronSpellsHandler.ALL_SPELLS.contains("irons_spellbooks:poison_breath"), "Contains poison breath");
            assertTrue(IronSpellsHandler.ALL_SPELLS.contains("irons_spellbooks:divine_smite"), "Contains divine smite");
            assertTrue(IronSpellsHandler.ALL_SPELLS.contains("irons_spellbooks:sculk_tentacles"), "Contains sculk tentacles");

            System.out.println("  [PASS] Iron's Spells comprehensive catalogue expansion verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================================");
        System.out.println("  SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================================");

        if (failed > 0) {
            throw new RuntimeException("Issue1FeaturesVerificationTest failed with " + failed + " errors");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
        }
    }
}
