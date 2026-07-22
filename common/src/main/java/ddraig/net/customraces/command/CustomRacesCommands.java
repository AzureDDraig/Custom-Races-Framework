package ddraig.net.customraces.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.integration.PehkuiIntegration;
import ddraig.net.customraces.network.ModPackets;
import ddraig.net.customraces.pack.PackManager;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Command dispatcher for /custom_races administration and player utilities.
 */
public class CustomRacesCommands {

    private static final SuggestionProvider<CommandSourceStack> RACE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(RaceRegistry.loadedRaces.keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> PASSIVE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of(
                "night_vision", "water_breathing", "fire_resistance", "flight", "slow_falling",
                "regeneration", "wither_immunity", "fall_damage_immunity", "lava_swimming", "climbing"
            ), builder);

    private static final SuggestionProvider<CommandSourceStack> ACTIVE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(List.of(
                "flame_breath", "teleport_dash", "lightning_strike", "frost_nova", "healing_touch",
                "super_launch", "sonic_dash", "dragon_roar", "thunder_stomp", "shield_wall",
                "fireball_volley", "web_trap_throw", "transform_were", "were_howl", "summon_minions"
            ), builder);

    private static final SuggestionProvider<CommandSourceStack> SOUND_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(RaceRegistry.CACHED_SOUNDS, builder);

    private static final SuggestionProvider<CommandSourceStack> PROJECTILE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(RaceRegistry.CACHED_PROJECTILES, builder);

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, selection) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("custom_races")
                // Player command: /custom_races select
                .then(Commands.literal("select")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ModPackets.openRaceSelection(player);
                            return 1;
                        })
                )
                // Player command: /custom_races codex
                .then(Commands.literal("codex")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            // Client packet or GUI trigger for Codex
                            context.getSource().sendSuccess(() -> Component.literal("Opening Race Codex..."), false);
                            return 1;
                        })
                )
                // Query player race: /custom_races get [player]
                .then(Commands.literal("get")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
                            String raceName = race != null ? race.name : "None (Default)";
                            context.getSource().sendSuccess(() -> Component.literal("Your current race is: " + raceName), false);
                            return 1;
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    RaceData race = RaceRegistry.getPlayerRace(target.getUUID());
                                    String raceName = race != null ? race.name : "None (Default)";
                                    context.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + "'s current race is: " + raceName), false);
                                    return 1;
                                })
                        )
                )
                // Admin Commands: /custom_races admin ...
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))

                        // /custom_races admin editor / gui
                        .then(Commands.literal("editor")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ModPackets.openCreatorGui(player);
                                    context.getSource().sendSuccess(() -> Component.literal("Opening Race Creator Admin GUI..."), false);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("gui")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ModPackets.openCreatorGui(player);
                                    context.getSource().sendSuccess(() -> Component.literal("Opening Race Creator Admin GUI..."), false);
                                    return 1;
                                })
                        )

                        // /custom_races admin race-select <player> [race]
                        .then(Commands.literal("race-select")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            RaceRegistry.setPlayerRace(target.getUUID(), null);
                                            PehkuiIntegration.resetPlayerScales(target);
                                            ModPackets.syncRacesToAll(context.getSource().getServer());
                                            ModPackets.openRaceSelection(target);
                                            context.getSource().sendSuccess(() -> Component.literal("Reset race for " + target.getScoreboardName() + " and opened Selection Screen."), true);
                                            return 1;
                                        })
                                        .then(Commands.argument("race", StringArgumentType.string())
                                                .suggests(RACE_SUGGESTIONS)
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    String raceId = StringArgumentType.getString(context, "race");
                                                    if (!RaceRegistry.loadedRaces.containsKey(raceId)) {
                                                        context.getSource().sendFailure(Component.literal("Unknown race: " + raceId));
                                                        return 0;
                                                    }
                                                    RaceRegistry.setPlayerRace(target.getUUID(), raceId);
                                                    RaceData race = RaceRegistry.getRace(raceId);
                                                    PehkuiIntegration.applyRaceScales(target, race);
                                                    ModPackets.syncRacesToAll(context.getSource().getServer());
                                                    context.getSource().sendSuccess(() -> Component.literal("Set " + target.getScoreboardName() + "'s race to: " + race.name), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // /custom_races admin status
                        .then(Commands.literal("status")
                                .executes(context -> {
                                    int count = RaceRegistry.loadedRaces.size();
                                    int assigned = RaceRegistry.playerRaces.size();
                                    context.getSource().sendSuccess(() -> Component.literal("Custom Races Status: " + count + " races loaded, " + assigned + " players assigned."), false);
                                    return 1;
                                })
                        )

                        // /custom_races admin reload
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    RaceRegistry.loadRaces();
                                    RaceRegistry.loadPlayerRaces();
                                    ModPackets.syncRacesToAll(context.getSource().getServer());
                                    context.getSource().sendSuccess(() -> Component.literal("Reloaded all race configurations!"), true);
                                    return 1;
                                })
                        )
                )
                // Pack Export and Import Commands
                .then(Commands.literal("pack")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("export")
                                .then(Commands.argument("pack_name", StringArgumentType.string())
                                        .executes(context -> {
                                            String packName = StringArgumentType.getString(context, "pack_name");
                                            boolean success = PackManager.exportPack(packName);
                                            if (success) {
                                                context.getSource().sendSuccess(() -> Component.literal("Successfully exported race pack to config/custom_races/exports/" + packName + ".zip"), true);
                                                return 1;
                                            } else {
                                                context.getSource().sendFailure(Component.literal("Failed to export race pack: " + packName));
                                                return 0;
                                            }
                                        })
                                )
                        )
                        .then(Commands.literal("import")
                                .then(Commands.argument("file_name", StringArgumentType.string())
                                        .executes(context -> {
                                            String fileName = StringArgumentType.getString(context, "file_name");
                                            boolean success = PackManager.importPack(fileName);
                                            if (success) {
                                                RaceRegistry.loadRaces();
                                                ModPackets.syncRacesToAll(context.getSource().getServer());
                                                context.getSource().sendSuccess(() -> Component.literal("Successfully imported race pack: " + fileName), true);
                                                return 1;
                                            } else {
                                                context.getSource().sendFailure(Component.literal("Failed to import race pack: " + fileName));
                                                return 0;
                                            }
                                        })
                                )
                        )
                )
        );
    }
}
