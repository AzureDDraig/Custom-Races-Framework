package ddraig.net.customraces.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Network packet sent from server to client containing metadata for the dynamically
 * generated custom races resource pack.
 */
public class ServerPackInfoPacket {

    public static final ResourceLocation ID = new ResourceLocation("customraces", "server_pack_info");

    private final String packUrl;
    private final String sha1Hash;
    private final long sizeBytes;
    private final boolean required;

    public ServerPackInfoPacket(String packUrl, String sha1Hash, long sizeBytes, boolean required) {
        this.packUrl = packUrl != null ? packUrl : "";
        this.sha1Hash = sha1Hash != null ? sha1Hash.toLowerCase() : "";
        this.sizeBytes = sizeBytes;
        this.required = required;
    }

    /**
     * Serializes this packet into the network buffer.
     *
     * @param buf the buffer to write to
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.packUrl, 32767);
        buf.writeUtf(this.sha1Hash, 64);
        buf.writeLong(this.sizeBytes);
        buf.writeBoolean(this.required);
    }

    /**
     * Deserializes a packet instance from the network buffer.
     *
     * @param buf the buffer to read from
     * @return decoded ServerPackInfoPacket
     */
    public static ServerPackInfoPacket decode(FriendlyByteBuf buf) {
        String packUrl = buf.readUtf(32767);
        String sha1Hash = buf.readUtf(64);
        long sizeBytes = buf.readLong();
        boolean required = buf.readBoolean();
        return new ServerPackInfoPacket(packUrl, sha1Hash, sizeBytes, required);
    }

    public String getPackUrl() {
        return packUrl;
    }

    public String getSha1Hash() {
        return sha1Hash;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        return "ServerPackInfoPacket{" +
                "packUrl='" + packUrl + '\'' +
                ", sha1Hash='" + sha1Hash + '\'' +
                ", sizeBytes=" + sizeBytes +
                ", required=" + required +
                '}';
    }
}
