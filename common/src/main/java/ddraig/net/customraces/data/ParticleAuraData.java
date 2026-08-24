package ddraig.net.customraces.data;

/**
 * Data structure holding particle aura layer configuration for a race.
 * Supports placement offsets (body, head, feet, hands, eyes, ambient),
 * form conditions (always, normal_only, were_only), and modded particle IDs.
 */
public class ParticleAuraData {
    public String particleType = "minecraft:flame"; // e.g. minecraft:flame, irons_spellbooks:fire_spark, etc.
    public float count = 1.0f;
    public float speed = 0.05f;
    public float spread = 0.5f;

    // Advanced Placement & Form Conditions
    public String placement = "body"; // "body", "head", "feet", "hands", "eyes", "ambient"
    public String formCondition = "always"; // "always", "normal_only", "were_only"
    public float offsetX = 0.0f;
    public float offsetY = 0.0f;
    public float offsetZ = 0.0f;

    public ParticleAuraData() {}

    public ParticleAuraData(String particleType, float count, float speed, float spread) {
        this.particleType = particleType;
        this.count = count;
        this.speed = speed;
        this.spread = spread;
    }

    public ParticleAuraData(String particleType, float count, float speed, float spread, String placement, String formCondition) {
        this(particleType, count, speed, spread);
        this.placement = placement;
        this.formCondition = formCondition;
    }

    public int getScaledParticleCount(int raceParticleCount) {
        int effectiveCount = raceParticleCount > 0 ? raceParticleCount : 5;
        return Math.max(1, Math.round(this.count * (effectiveCount / 5.0f)));
    }

    public float getSafeSpread() {
        return Math.max(0.1f, Math.abs(this.spread));
    }

    public float getSafeSpeed() {
        return Math.abs(this.speed);
    }

    public String getValidParticleType() {
        return (this.particleType != null && !this.particleType.trim().isEmpty()) ? this.particleType.trim() : "minecraft:flame";
    }

    public String getValidPlacement() {
        if (this.placement == null) return "body";
        String p = this.placement.trim().toLowerCase();
        if (p.equals("head") || p.equals("feet") || p.equals("hands") || p.equals("eyes") || p.equals("ambient")) {
            return p;
        }
        return "body";
    }

    public String getValidFormCondition() {
        if (this.formCondition == null) return "always";
        String f = this.formCondition.trim().toLowerCase();
        if (f.equals("normal_only") || f.equals("were_only")) {
            return f;
        }
        return "always";
    }

    public boolean matchesForm(boolean isWereForm) {
        String cond = getValidFormCondition();
        if ("normal_only".equals(cond)) return !isWereForm;
        if ("were_only".equals(cond)) return isWereForm;
        return true;
    }
}
