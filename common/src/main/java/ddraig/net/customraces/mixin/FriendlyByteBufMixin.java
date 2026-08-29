package ddraig.net.customraces.mixin;

import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin into FriendlyByteBuf to prevent NullPointerExceptions when any mod or packet
 * serialization attempts to write a null UTF string.
 */
@Mixin(FriendlyByteBuf.class)
public class FriendlyByteBufMixin {

    @ModifyVariable(method = "writeUtf(Ljava/lang/String;I)Lnet/minecraft/network/FriendlyByteBuf;", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String customraces$sanitizeNullUtfString(String str) {
        return str == null ? "" : str;
    }
}
