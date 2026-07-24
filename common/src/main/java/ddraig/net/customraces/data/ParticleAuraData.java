package ddraig.net.customraces.data;

/**
 * Data structure holding particle aura layer configuration for a race.
 */
public class ParticleAuraData {
    public String particleType = "minecraft:flame"; // e.g. minecraft:flame, minecraft:portal, etc.
    public float count = 1.0f;
    public float speed = 0.05f;
    public float spread = 0.5f;

    public ParticleAuraData() {}

    public ParticleAuraData(String particleType, float count, float speed, float spread) {
        this.particleType = particleType;
        this.count = count;
        this.speed = speed;
        this.spread = spread;
    }

    public int getScaledParticleCount(int raceParticleCount) {
        int effectiveCount = raceParticleCount > 0 ? raceParticleCount : 5;
        return Math.max(1, Math.round(this.count * (effectiveCount / 5.0f)));
    }
}
