package me.superneon4ik.selectiveglowing;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import me.superneon4ik.selectiveglowing.config.SelectiveGlowingConfig;
import me.superneon4ik.selectiveglowing.config.SelectiveGlowingState;
import me.superneon4ik.selectiveglowing.enums.EntityData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public final class SelectiveGlowing {
    private static final TrackedData<Byte> FLAGS = getByteTrackedData();
    private static final int ENTITY_STATE_INDEX = 0;
    private static MinecraftServer minecraftServer = null;

    public static final String MOD_ID = "selectiveglowing";
    public static String VERSION = "unknown";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Path configFile;
    private static SelectiveGlowingConfig config;
    private static Path stateFile;
    private static SelectiveGlowingState state;

    private final static Gson GSON = new Gson();


    private SelectiveGlowing() { }

    /**
     * Runs the mod logic initializer.
     */
    public static void init() {
        VERSION = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");

        var configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        configFile = configDir.resolve(MOD_ID + ".json");
        stateFile = configDir.resolve("state.json");

        LOGGER.info("Config path: {}", configDir);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            minecraftServer = server;

            loadConfig();
            loadState();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // we don't really need to save the config *right now* as you can't edit it yet
            saveState();
        });
    }

    private static void loadConfig() {
        try {
            if (!Files.exists(configFile)) {
                createDefaultConfig();
                return;
            }

            var json = GSON.fromJson(Files.readString(configFile), JsonElement.class);
            config = SelectiveGlowingConfig.CODEC.parse(JsonOps.INSTANCE, json)
                    .getOrThrow();
        } catch (Exception e) {
            LOGGER.error("Failed to load config (using defaults): {}", e.toString());
            createDefaultConfig();
        }
    }

    private static void createDefaultConfig() {
        config = new SelectiveGlowingConfig(true, true);
        LOGGER.info("Created new default config");
        saveConfig();
    }

    private static void saveConfig() {
        try {
            var json = SelectiveGlowingConfig.CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow();
            var parentDir = configFile.getParent();
            if (parentDir != null) Files.createDirectories(parentDir);
            Files.writeString(configFile, GSON.toJson(json),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("Failed to save config: {}", e.toString());
        }
    }

    private static void loadState() {
        if (!config.isLoadState()) return;

        try {
            if (!Files.exists(stateFile)) {
                createDefaultState();
                return;
            }

            var json = GSON.fromJson(Files.readString(stateFile), JsonElement.class);
            state = SelectiveGlowingState.CODEC.parse(JsonOps.INSTANCE, json)
                    .getOrThrow();

            LOGGER.info("State had {} entries:", state.getState().size());
            state.getState().forEach((uuid, uuids) -> {
                LOGGER.info("{} -> {}", uuid.toString(), String.join(", ", uuids.stream().map(UUID::toString).collect(Collectors.toSet())));
            });
        } catch (Exception e) {
            LOGGER.error("Failed to load state: {}", e.toString());
            createDefaultState();
        }
    }

    private static void createDefaultState() {
        state = new SelectiveGlowingState(new HashMap<>());
        LOGGER.info("Created new default state");
        saveState();
    }

    private static void saveState() {
        if (!config.isSaveState()) return;

        try {
            var json = SelectiveGlowingState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
            Files.writeString(stateFile, GSON.toJson(json),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("Failed to save state: {}", e.toString());
        }
    }

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public static TrackedData<Byte> getFlagsTrackedData() {
        return FLAGS;
    }

    public static void setGlowing(Entity target, Collection<ServerPlayerEntity> displayPlayers) {
        state.getState().put(target.getUuid(), displayPlayers.stream().map(Entity::getUuid).collect(Collectors.toSet()));
        updateMetadata(target);
    }

    public static void resetGlowing(Entity target) {
        state.getState().remove(target.getUuid());
        updateMetadata(target);
    }

    public static Set<UUID> resetAllGlowing() {
        var targetUuids = new HashSet<>(state.getState().keySet());
        state.getState().clear();
        return targetUuids;
    }

    @SuppressWarnings({"CallToPrintStackTrace"})
    public static void updateMetadata(Entity target) {
        try {
            if (FLAGS == null) return;
            byte bitmask = target.getDataTracker().get(FLAGS);

            for (PlayerEntity player : target.getWorld().getPlayers()) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    List<DataTracker.SerializedEntry<?>> list = new ArrayList<>();

                    if (isGlowing(target, serverPlayer)) bitmask = EntityData.GLOWING.setBit(bitmask);
                    else bitmask = EntityData.GLOWING.unsetBit(bitmask);

                    list.add(new DataTracker.SerializedEntry<>(0, FLAGS.dataType(), bitmask));
                    var packet = new EntityTrackerUpdateS2CPacket(target.getId(), list);
                    if (serverPlayer.distanceTo(target) <= 60) {
                        serverPlayer.networkHandler.sendPacket(packet);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unchecked", "JavaReflectionMemberAccess"})
    private static TrackedData<Byte> getByteTrackedData() {
        var entityClass = Entity.class;
        try {
            var field = entityClass.getDeclaredField("field_5990");
            field.setAccessible(true);
            return (TrackedData<Byte>) field.get(null);
        } catch (NoSuchFieldException e1) {
            try {
                var field = entityClass.getDeclaredField("FLAGS");
                field.setAccessible(true);
                return (TrackedData<Byte>) field.get(null);
            } catch (NoSuchFieldException | IllegalAccessException e2) {
                return null;
            }
        } catch (IllegalAccessException ignore) {
            return null;
        }
    }

    public static boolean isGlowing(int targetId, Entity observer) {
        var target = observer.getWorld().getEntityById(targetId);
        if (target == null) return false;

        return isGlowing(target, observer);
    }

    public static boolean isGlowing(Entity target, Entity observer) {
        if (!state.getState().containsKey(target.getUuid())) return false;
        return state.getState().get(target.getUuid()).contains(observer.getUuid());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static EntityTrackerUpdateS2CPacket cloneAndOverridePacket(EntityTrackerUpdateS2CPacket packet, ServerPlayerEntity observer) {
        int targetId = packet.id();
        var trackedValues = new ArrayList<DataTracker.SerializedEntry<?>>();
        for (var value : packet.trackedValues()) {
            if (value.id() == ENTITY_STATE_INDEX) {
                byte bitmask = (byte) value.value();
                if (SelectiveGlowing.isGlowing(targetId, observer)) {
                    bitmask = EntityData.GLOWING.setBit(bitmask);
                }
                var newEntry = new DataTracker.SerializedEntry(ENTITY_STATE_INDEX, value.handler(), bitmask);
                trackedValues.add(newEntry);
                continue;
            }
            trackedValues.add(value);
        }
        return new EntityTrackerUpdateS2CPacket(targetId, trackedValues);
    }
}