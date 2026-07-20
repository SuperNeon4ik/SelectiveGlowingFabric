package me.superneon4ik.selectiveglowing;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SelectiveGlowingCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("glow")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .executes(context -> {
                    context.getSource().sendSuccess(() ->
                            Component.empty()
                                    .withColor(0xffadfa)
                                    .append(
                                            Component.literal("Selective Glowing ver. %s".formatted(SelectiveGlowing.VERSION))
                                                    .withStyle(ChatFormatting.BOLD)
                                    )
                                    .append(
                                            Component.literal("\nUsage:\n")
                                                    .withStyle(ChatFormatting.ITALIC)
                                    )
                                    .append(
                                            Component.literal(
                                                    "/glow <targets: entities> <displayplayers: players>\n" +
                                                    "/glow <targets: entities> *reset\n" +
                                                    "/glow *reset"
                                            )
                                    ),
                            false);

                    return Command.SINGLE_SUCCESS;
                })
                .then(argument("targets", EntityArgument.entities())
                        .then(argument("displayplayers", EntityArgument.players())
                                .executes(context -> {
                                    var targets = EntityArgument.getEntities(context, "targets");
                                    var displayPlayers = EntityArgument.getPlayers(context, "displayplayers");
                                    for (Entity target : targets) {
                                        SelectiveGlowing.setGlowing(target, displayPlayers);
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal(String.format("%d entities are now glowing for %d player(s).",
                                            targets.size(), displayPlayers.size())), false);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(literal("*reset")
                                .executes(context -> {
                                    var targets = EntityArgument.getEntities(context, "targets");
                                    for (Entity target : targets) {
                                        SelectiveGlowing.resetGlowing(target);
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal(String.format("Removed glowing overrides for %d entities.", targets.size())), false);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(literal("*reset")
                        .executes(context -> {
                            var targetIds = SelectiveGlowing.resetAllGlowing();
                            var minecraftServer = SelectiveGlowing.getMinecraftServer();
                            if (minecraftServer != null) {
                                for (ServerLevel world : minecraftServer.getAllLevels()) {
                                    for (Entity entity : world.getAllEntities()) {
                                        if (!targetIds.contains(entity.getUUID())) continue;
                                        SelectiveGlowing.updateMetadata(entity);
                                    }
                                }
                            }
                            context.getSource().sendSuccess(() -> Component.literal(String.format("Removed glowing overrides for all %d entities.", targetIds.size())), false);
                            return Command.SINGLE_SUCCESS;
                        }))));
    }
}