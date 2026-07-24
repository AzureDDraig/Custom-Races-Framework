package ddraig.net.customraces.data;

/**
 * Position, rotation, and scale transform settings for custom or attached body parts.
 */
public class PartTransformData {
    public float posX = 0.0f;
    public float posY = 0.0f;
    public float posZ = 0.0f;
    public float rotPitch = 0.0f;
    public float rotYaw = 0.0f;
    public float rotRoll = 0.0f;
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public float scaleZ = 1.0f;

    public PartTransformData() {}

    public PartTransformData(float posX, float posY, float posZ, float rotPitch, float rotYaw, float rotRoll, float scaleX, float scaleY, float scaleZ) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.rotPitch = rotPitch;
        this.rotYaw = rotYaw;
        this.rotRoll = rotRoll;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }

    public float getSafeScaleX() {
        if (Float.isNaN(scaleX) || scaleX <= 0.0f) return 1.0f;
        return Math.max(0.01f, Math.min(5.0f, scaleX));
    }

    public float getSafeScaleY() {
        if (Float.isNaN(scaleY) || scaleY <= 0.0f) return 1.0f;
        return Math.max(0.01f, Math.min(5.0f, scaleY));
    }

    public float getSafeScaleZ() {
        if (Float.isNaN(scaleZ) || scaleZ <= 0.0f) return 1.0f;
        return Math.max(0.01f, Math.min(5.0f, scaleZ));
    }
}
