package ddraig.net.customraces.data;

/**
 * Data structure holding mob alliance/neutrality configuration for a race.
 */
public class MobAllianceData {
    public String mobId = "";
    public String stance = "neutral"; // "neutral" or "friendly"

    public MobAllianceData() {}

    public MobAllianceData(String mobId, String stance) {
        this.mobId = mobId;
        this.stance = stance;
    }

    public String getValidMobId() {
        return mobId != null ? mobId.trim() : "";
    }

    public String getValidStance() {
        return stance != null ? stance.trim().toLowerCase() : "neutral";
    }
}
