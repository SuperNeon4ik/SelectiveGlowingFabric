package me.superneon4ik.selectiveglowing;

import static net.minecraft.server.command.CommandManager.*;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.text.Text;

public class SelectiveGlowing implements ModInitializer {
    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("glow")
                .requires(source -> source.hasPermissionLevel(4))
                .then(argument("target", EntityArgumentType.player())
                        .executes(context -> {
                            var target = EntityArgumentType.getPlayer(context, "target");
                            context.getSource().sendFeedback(Text.literal("Meow meow " + target.getName()), false);
                            return Command.SINGLE_SUCCESS;
                        }))));
    }
}
