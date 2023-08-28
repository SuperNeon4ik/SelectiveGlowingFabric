package me.superneon4ik.selectiveglowing;

import static net.minecraft.server.command.CommandManager.*;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectiveGlowing implements ModInitializer {
    private static final Map<Integer, List<Integer>> GLOWING_MAP = new HashMap<>();

    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("glow")
                .requires(source -> source.hasPermissionLevel(4))
                .then(argument("target", EntityArgumentType.player())
                        .then(argument("displayplayers", EntityArgumentType.players())
                                .executes(context -> {
                                    var target = EntityArgumentType.getPlayer(context, "target");
                                    var displayPlayers = EntityArgumentType.getPlayers(context, "displayplayers");
                                    GLOWING_MAP.put(target.getId(), displayPlayers.stream().map(Entity::getId).toList());
                                    context.getSource().sendFeedback(Text.literal(String.format("Player %s is now glowing for %d players.",
                                            target.getEntityName(), displayPlayers.size())), false);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(literal("*reset")
                                .executes(context -> {
                                    var target = EntityArgumentType.getPlayer(context, "target");
                                    GLOWING_MAP.remove(target.getId());
                                    context.getSource().sendFeedback(Text.literal("Removed glowing overrides for ")
                                            .append(target.getName()), false);
                                    return Command.SINGLE_SUCCESS;
                                })))));
    }

    public static boolean isGlowing(int targetId, int observerId) {
        if (!GLOWING_MAP.containsKey(targetId)) return false;
        return GLOWING_MAP.get(targetId).contains(observerId);
    }
}
