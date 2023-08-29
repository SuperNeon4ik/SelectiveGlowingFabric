package me.superneon4ik.selectiveglowing;

import com.mojang.brigadier.Command;
import com.mojang.logging.LogUtils;
import me.superneon4ik.selectiveglowing.enums.EntityData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SelectiveGlowing implements ModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();
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
                                    updateMetadata(target);
                                    context.getSource().sendFeedback(Text.literal(String.format("Player %s is now glowing for %d players.",
                                            target.getEntityName(), displayPlayers.size())), false);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(literal("*reset")
                                .executes(context -> {
                                    var target = EntityArgumentType.getPlayer(context, "target");
                                    GLOWING_MAP.remove(target.getId());
                                    updateMetadata(target);
                                    context.getSource().sendFeedback(Text.literal("Removed glowing overrides for ")
                                            .append(target.getName()), false);
                                    return Command.SINGLE_SUCCESS;
                                })))));
    }

    @SuppressWarnings({"unchecked", "CallToPrintStackTrace"})
    private static void updateMetadata(ServerPlayerEntity target) {
        try {
            var entityClass = Entity.class;
            var field = entityClass.getDeclaredField("FLAGS");
            field.setAccessible(true);
            TrackedData<Byte> flags = (TrackedData<Byte>) field.get(null);
            byte bitmask = target.getDataTracker().get(flags);

            for (ServerPlayerEntity player : target.getWorld().getPlayers()) {
                List<DataTracker.SerializedEntry<?>> list = new ArrayList<>();

                if (isGlowing(target.getId(), player)) bitmask = EntityData.GLOWING.setBit(bitmask);
                else bitmask = EntityData.GLOWING.unsetBit(bitmask);

                list.add(new DataTracker.SerializedEntry<>(0, flags.getType(), bitmask));
                var packet = new EntityTrackerUpdateS2CPacket(target.getId(), list);
                if (player.distanceTo(target) <= 60) {
                    player.networkHandler.sendPacket(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isGlowing(int targetId, ServerPlayerEntity observer) {
        if (isGlowing(targetId, observer.getId())) return true;
        var target = getPlayerById(observer.getWorld(), targetId);
        if (target == null) {
            LOGGER.info("No target.");
            return false;
        }
        return target.isGlowing();
    }

    public static boolean isGlowing(int targetId, int observerId) {
        if (!GLOWING_MAP.containsKey(targetId)) return false;
        return GLOWING_MAP.get(targetId).contains(observerId);
    }

    public static ServerPlayerEntity getPlayerById(ServerWorld world, int id) {
        return world.getPlayers().stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static EntityTrackerUpdateS2CPacket cloneAndOverridePacket(EntityTrackerUpdateS2CPacket packet, int observerId) {
        int targetId = packet.id();
        var trackedValues = new ArrayList<DataTracker.SerializedEntry<?>>();
        for (var value : packet.trackedValues()) {
            if (value.id() == 0) {
                byte bitmask = (byte) value.value();
                if (SelectiveGlowing.isGlowing(targetId, observerId)) bitmask = EntityData.GLOWING.setBit(bitmask);
                var newEntry = new DataTracker.SerializedEntry(0, value.handler(), bitmask);
                trackedValues.add(newEntry);
                continue;
            }
            trackedValues.add(value);
        }
        return new EntityTrackerUpdateS2CPacket(targetId, trackedValues);
    }
}
