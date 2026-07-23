package ddraig.net.customraces.integration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

public class IronSpellsHandlerTest {

    public enum MockCastSource {
        SPELLBOOK, INNATE, SCROLL
    }

    public enum MockUnrelatedEnum {
        FOO, BAR
    }

    public enum MockCustomCastSourceEnum {
        MODE_A, MODE_B
    }

    public static class MockMagicData {
        public int mana = 100;
    }

    public static class MockSpellOverloads {
        // Method A: 5 params onCast
        public void onCast(Level level, int spellLevel, LivingEntity entity, MockCastSource castSource, MockMagicData magicData) {}
        // Method B: 4 params onCast
        public void onCast(Level level, int spellLevel, LivingEntity entity, MockMagicData magicData) {}
        // Method C: 5 params castSpell
        public void castSpell(Level level, int spellLevel, LivingEntity entity, MockCastSource castSource, MockMagicData magicData) {}
        // Method D: 5 params onCastSpell
        public void onCastSpell(Level level, int spellLevel, LivingEntity entity, MockCastSource castSource, MockMagicData magicData) {}
        // Method E: 6 params onCast with Object (extra context)
        public void onCast(Level level, int spellLevel, LivingEntity entity, MockCastSource castSource, MockMagicData magicData, Object extraContext) {}
        // Method F: 6 params onCast with primitive boolean
        public void onCast(Level level, int spellLevel, LivingEntity entity, MockCastSource castSource, MockMagicData magicData, boolean flag) {}
        // Method G: 6 params onCast with primitive float
        public void onCast(Level level, int spellLevel, LivingEntity entity, MockCastSource castSource, MockMagicData magicData, float power) {}
    }

    public static class MockNullSupplier implements Supplier<Object> {
        @Override
        public Object get() {
            return null;
        }
    }

    private static Object invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = IronSpellsHandler.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    public static void main(String[] args) {
        try {
            System.out.println("=== RUNNING IRON SPELLS HANDLER EMPIRICAL TESTS ===");
            testIsCastSourceTypeVulnerabilities();
            testIsMagicDataTypeVulnerabilities();
            testCandidateMethodSorting();
            testPrimitiveArgumentInvocationFailure();
            testUnwrapSpellHolderEdgeCases();
            testResolveCastSourceForParamEnumFallback();
            System.out.println("=== EMPIRICAL TESTS COMPLETE ===");
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static void testIsCastSourceTypeVulnerabilities() throws Exception {
        Object castSource = MockCastSource.SPELLBOOK;

        boolean isCastSource = (boolean) invokePrivateStatic("isCastSourceType", new Class<?>[]{Class.class, Object.class}, MockCastSource.class, castSource);
        System.out.println("isCastSourceType(MockCastSource.class, castSource) = " + isCastSource);

        boolean isObjectCastSource = (boolean) invokePrivateStatic("isCastSourceType", new Class<?>[]{Class.class, Object.class}, Object.class, castSource);
        System.out.println("isCastSourceType(Object.class, castSource) = " + isObjectCastSource + " [VULNERABILITY: Object matches as CastSource]");

        boolean isEnumCastSource = (boolean) invokePrivateStatic("isCastSourceType", new Class<?>[]{Class.class, Object.class}, Enum.class, castSource);
        System.out.println("isCastSourceType(Enum.class, castSource) = " + isEnumCastSource + " [VULNERABILITY: Enum matches as CastSource]");

        boolean isComparableCastSource = (boolean) invokePrivateStatic("isCastSourceType", new Class<?>[]{Class.class, Object.class}, Comparable.class, castSource);
        System.out.println("isCastSourceType(Comparable.class, castSource) = " + isComparableCastSource + " [VULNERABILITY: Comparable matches as CastSource]");
    }

    public static void testIsMagicDataTypeVulnerabilities() throws Exception {
        Object magicData = new MockMagicData();

        boolean isObjectMagicData = (boolean) invokePrivateStatic("isMagicDataType", new Class<?>[]{Class.class, Object.class}, Object.class, magicData);
        System.out.println("isMagicDataType(Object.class, magicData) = " + isObjectMagicData + " [VULNERABILITY: Object matches as MagicData]");
    }

    public static void testCandidateMethodSorting() throws Exception {
        Object castSource = MockCastSource.SPELLBOOK;
        Object magicData = new MockMagicData();

        Method mA = MockSpellOverloads.class.getMethod("onCast", Level.class, int.class, LivingEntity.class, MockCastSource.class, MockMagicData.class);
        Method mB = MockSpellOverloads.class.getMethod("onCast", Level.class, int.class, LivingEntity.class, MockMagicData.class);
        Method mC = MockSpellOverloads.class.getMethod("castSpell", Level.class, int.class, LivingEntity.class, MockCastSource.class, MockMagicData.class);
        Method mD = MockSpellOverloads.class.getMethod("onCastSpell", Level.class, int.class, LivingEntity.class, MockCastSource.class, MockMagicData.class);
        Method mE = MockSpellOverloads.class.getMethod("onCast", Level.class, int.class, LivingEntity.class, MockCastSource.class, MockMagicData.class, Object.class);
        Method mF = MockSpellOverloads.class.getMethod("onCast", Level.class, int.class, LivingEntity.class, MockCastSource.class, MockMagicData.class, boolean.class);

        List<Method> candidates = new ArrayList<>(Arrays.asList(mA, mB, mC, mD, mE, mF));

        candidates.sort((m1, m2) -> {
            try {
                boolean strict1 = (boolean) invokePrivateStatic("isStrictParameterMatch", new Class<?>[]{Method.class, Object.class, Object.class}, m1, castSource, magicData);
                boolean strict2 = (boolean) invokePrivateStatic("isStrictParameterMatch", new Class<?>[]{Method.class, Object.class, Object.class}, m2, castSource, magicData);
                if (strict1 != strict2) return strict1 ? -1 : 1;

                int nameScore1 = (int) invokePrivateStatic("getNameScore", new Class<?>[]{String.class}, m1.getName());
                int nameScore2 = (int) invokePrivateStatic("getNameScore", new Class<?>[]{String.class}, m2.getName());
                if (nameScore1 != nameScore2) return Integer.compare(nameScore1, nameScore2);

                return Integer.compare(m2.getParameterCount(), m1.getParameterCount());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Sorted candidates order:");
        for (int i = 0; i < candidates.size(); i++) {
            System.out.println("  Rank " + (i + 1) + ": " + candidates.get(i).getName() + " with " + candidates.get(i).getParameterCount() + " params");
        }
    }

    public static void testPrimitiveArgumentInvocationFailure() throws Exception {
        Object castSource = MockCastSource.SPELLBOOK;
        Object magicData = new MockMagicData();

        Method mF = MockSpellOverloads.class.getMethod("onCast", Level.class, int.class, LivingEntity.class, MockCastSource.class, MockMagicData.class, boolean.class);
        Method mG = MockSpellOverloads.class.getMethod("onCast", Level.class, int.class, LivingEntity.class, MockCastSource.class, MockMagicData.class, float.class);

        // Test argument construction for mF (boolean param)
        Class<?>[] pTypesF = mF.getParameterTypes();
        Object[] argsF = new Object[pTypesF.length];
        for (int i = 0; i < pTypesF.length; i++) {
            Class<?> p = pTypesF[i];
            if (Level.class.isAssignableFrom(p)) argsF[i] = null;
            else if (p == int.class || p == Integer.class) argsF[i] = 1;
            else if (LivingEntity.class.isAssignableFrom(p)) argsF[i] = null;
            else if ((boolean) invokePrivateStatic("isCastSourceType", new Class<?>[]{Class.class, Object.class}, p, castSource)) argsF[i] = castSource;
            else if ((boolean) invokePrivateStatic("isMagicDataType", new Class<?>[]{Class.class, Object.class}, p, magicData)) argsF[i] = magicData;
            else argsF[i] = null; // boolean parameter gets set to null!
        }

        System.out.println("Arg constructed for primitive boolean param: argsF[5] = " + argsF[5]);
        try {
            mF.invoke(new MockSpellOverloads(), argsF);
            System.out.println("mF.invoke succeeded unexpectedly");
        } catch (IllegalArgumentException e) {
            System.out.println("mF.invoke failed as expected with IllegalArgumentException: " + e.getMessage() + " [LIMITATION: Unhandled primitive parameters cause Reflection failure]");
        } catch (Exception e) {
            System.out.println("mF.invoke failed with: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static void testUnwrapSpellHolderEdgeCases() throws Exception {
        // Null object
        Object res1 = invokePrivateStatic("unwrapSpellHolder", new Class<?>[]{Object.class}, (Object) null);
        System.out.println("unwrapSpellHolder(null) = " + res1);

        // String "none"
        Object res2 = invokePrivateStatic("unwrapSpellHolder", new Class<?>[]{Object.class}, "none");
        System.out.println("unwrapSpellHolder(\"none\") = " + res2);

        // Optional.empty()
        Object res3 = invokePrivateStatic("unwrapSpellHolder", new Class<?>[]{Object.class}, Optional.empty());
        System.out.println("unwrapSpellHolder(Optional.empty()) = " + res3);

        // Supplier returning null
        MockNullSupplier nullSupplier = new MockNullSupplier();
        Object res4 = invokePrivateStatic("unwrapSpellHolder", new Class<?>[]{Object.class}, nullSupplier);
        System.out.println("unwrapSpellHolder(Supplier returning null) = " + res4 + " [EDGE CASE: Returns empty Supplier object instead of null]");
    }

    public static void testResolveCastSourceForParamEnumFallback() throws Exception {
        Object castSource = MockCastSource.SPELLBOOK;

        // Resolve for MockUnrelatedEnum (no SPELLBOOK or INNATE constant)
        Object res = invokePrivateStatic("resolveCastSourceForParam", new Class<?>[]{Class.class, Object.class}, MockUnrelatedEnum.class, castSource);
        System.out.println("resolveCastSourceForParam(MockUnrelatedEnum.class, castSource) = " + res + " (type: " + (res != null ? res.getClass().getName() : "null") + ")");
    }
}
