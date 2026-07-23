package ddraig.net.customraces.item;

import ddraig.net.customraces.network.ModPackets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * "Orb of Rebirth" item allowing players to reset their race and trigger the selection screen.
 */
public class RaceOrbItem extends Item {

    public RaceOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.serverLevel() != null) {
                serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);
                serverPlayer.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
                serverPlayer.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 15, 0.4, 0.6, 0.4, 0.05);
            }
            ModPackets.openRaceSelection(serverPlayer);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
