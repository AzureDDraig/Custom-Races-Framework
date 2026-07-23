package ddraig.net.customraces.event;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Handles custom in-game sound FX playback for Hurt, Death, and Ambient sounds per race and Were-form.
 */
public class RaceSoundHandler {

    public static void init() {
        // 1. Custom Damage/Hurt Sounds
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player && amount > 0) {
                RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
                if (race != null) {
                    boolean isWere = WereRaceTransformHandler.isTransformed(player.getUUID());
                    String soundId = isWere && race.enableWereRace ? race.wereHurtSound : race.hurtSound;
                    playSound(player, soundId, 1.0f, 1.0f);
                }
            }
            return EventResult.pass();
        });

        // 2. Custom Death Sounds
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
                if (race != null) {
                    boolean isWere = WereRaceTransformHandler.isTransformed(player.getUUID());
                    String soundId = isWere && race.enableWereRace ? race.wereDeathSound : race.deathSound;
                    playSound(player, soundId, 1.2f, 0.9f);
                }
            }
            return EventResult.pass();
        });

        // 3. Custom Ambient Sounds (Every ~20s)
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % 400 == 0) {
                RaceData race = RaceRegistry.getPlayerRace(serverPlayer.getUUID());
                if (race != null) {
                    boolean isWere = WereRaceTransformHandler.isTransformed(serverPlayer.getUUID());
                    String soundId = isWere && race.enableWereRace ? race.wereAmbientSound : race.ambientSound;
                    playSound(serverPlayer, soundId, 0.6f, 1.0f);
                }
            }
        });
    }

    private static void playSound(ServerPlayer player, String soundId, float volume, float pitch) {
        if (soundId == null || soundId.trim().isEmpty() || !(player.level() instanceof ServerLevel level)) return;
        try {
            ResourceLocation loc = new ResourceLocation(soundId.trim());
            SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(loc);
            if (sound != null) {
                level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
            }
        } catch (Exception ignored) {}
    }
}
