package me.superneon4ik.selectiveglowing;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SelectiveGlowingCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("glow")
                .requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS)))
                .executes(context -> {
                    context.getSource().sendFeedback(() ->
                            Text.empty()
                                    .withColor(0xffadfa)
                                    .append(
                                            Text.literal("Selective Glowing ver. %s".formatted(SelectiveGlowing.VERSION))
                                                    .formatted(Formatting.BOLD)
                                    )
                                    .append(
                                            Text.literal("\nUsage:\n")
                                                    .formatted(Formatting.ITALIC)
                                    )
                                    .append(
                                            Text.literal(
                                                    "/glow <targets: entities> <displayplayers: players>\n" +
                                                    "/glow <targets: entities> *reset\n" +
                                                    "/glow *reset"
                                            )
                                    ),
                            false);

                    return Command.SINGLE_SUCCESS;
                })
                .then(argument("targets", EntityArgumentType.entities())
                        .then(argument("displayplayers", EntityArgumentType.players())
                                .executes(context -> {
                                    var targets = EntityArgumentType.getEntities(context, "targets");
                                    var displayPlayers = EntityArgumentType.getPlayers(context, "displayplayers");
                                    for (Entity target : targets) {
                                        SelectiveGlowing.setGlowing(target, displayPlayers);
                                    }
                                    context.getSource().sendFeedback(() -> Text.literal(String.format("%d entities are now glowing for %d player(s).",
                                            targets.size(), displayPlayers.size())), false);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(literal("*reset")
                                .executes(context -> {
                                    var targets = EntityArgumentType.getEntities(context, "targets");
                                    for (Entity target : targets) {
                                        SelectiveGlowing.resetGlowing(target);
                                    }
                                    context.getSource().sendFeedback(() -> Text.literal(String.format("Removed glowing overrides for %d entities.", targets.size())), false);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(literal("*reset")
                        .executes(context -> {
                            var targetIds = SelectiveGlowing.resetAllGlowing();
                            var minecraftServer = SelectiveGlowing.getMinecraftServer();
                            if (minecraftServer != null) {
                                for (ServerWorld world : minecraftServer.getWorlds()) {
                                    for (Entity entity : world.iterateEntities()) {
                                        if (!targetIds.contains(entity.getUuid())) continue;
                                        SelectiveGlowing.updateMetadata(entity);
                                    }
                                }
                            }
                            context.getSource().sendFeedback(() -> Text.literal(String.format("Removed glowing overrides for all %d entities.", targetIds.size())), false);
                            return Command.SINGLE_SUCCESS;
                        }))));
    }
}