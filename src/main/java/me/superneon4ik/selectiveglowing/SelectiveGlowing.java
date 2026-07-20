package me.superneon4ik.selectiveglowing;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import me.superneon4ik.selectiveglowing.config.SelectiveGlowingConfig;
import me.superneon4ik.selectiveglowing.config.SelectiveGlowingState;
import me.superneon4ik.selectiveglowing.enums.EntityData;
import me.superneon4ik.selectiveglowing.mixin.EntityAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public final class SelectiveGlowing {
    private static final EntityDataAccessor<Byte> FLAGS = EntityAccessor.getDATA_SHARED_FLAGS_ID();
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

    public static EntityDataAccessor<Byte> getFlagsTrackedData() {
        return FLAGS;
    }

    public static void setGlowing(Entity target, Collection<ServerPlayer> displayPlayers) {
        state.getState().put(target.getUUID(), displayPlayers.stream().map(Entity::getUUID).collect(Collectors.toSet()));
        updateMetadata(target);
    }

    public static void resetGlowing(Entity target) {
        state.getState().remove(target.getUUID());
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
            byte bitmask = target.getEntityData().get(FLAGS);

            try (var level = target.level()) {
                for (Player player : level.players()) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        List<SynchedEntityData.DataValue<?>> list = new ArrayList<>();

                        if (isGlowing(target, serverPlayer)) bitmask = EntityData.GLOWING.setBit(bitmask);
                        else bitmask = EntityData.GLOWING.unsetBit(bitmask);

                        list.add(new SynchedEntityData.DataValue<>(0, FLAGS.serializer(), bitmask));
                        var packet = new ClientboundSetEntityDataPacket(target.getId(), list);
                        if (serverPlayer.distanceTo(target) <= 60) {
                            serverPlayer.connection.send(packet);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unchecked"})
    private static EntityDataAccessor<Byte> getByteTrackedData() {
        var entityClass = Entity.class;
        try {
            var field = entityClass.getDeclaredField("DATA_SHARED_FLAGS_ID");
            field.setAccessible(true);
            return (EntityDataAccessor<Byte>) field.get(null);
        } catch (IllegalAccessException | NoSuchFieldException ignore) {
            return null;
        }
    }

    public static boolean isGlowing(int targetId, Entity observer) {
        try (var level = observer.level()) {
            var target = level.getEntity(targetId);
            if (target == null) return false;

            return isGlowing(target, observer);
        } catch (Exception e) {
            LOGGER.error("Failed to check for glowing: {}", e.toString());
            return false;
        }
    }

    public static boolean isGlowing(Entity target, Entity observer) {
        if (!state.getState().containsKey(target.getUUID())) return false;
        return state.getState().get(target.getUUID()).contains(observer.getUUID());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ClientboundSetEntityDataPacket cloneAndOverridePacket(ClientboundSetEntityDataPacket packet, ServerPlayer observer) {
        int targetId = packet.id();
        var trackedValues = new ArrayList<SynchedEntityData.DataValue<?>>();
        for (var value : packet.packedItems()) {
            if (value.id() == ENTITY_STATE_INDEX) {
                byte bitmask = (byte) value.value();
                if (SelectiveGlowing.isGlowing(targetId, observer)) {
                    bitmask = EntityData.GLOWING.setBit(bitmask);
                }
                var newEntry = new SynchedEntityData.DataValue(ENTITY_STATE_INDEX, value.serializer(), bitmask);
                trackedValues.add(newEntry);
                continue;
            }
            trackedValues.add(value);
        }
        return new ClientboundSetEntityDataPacket(targetId, trackedValues);
    }
}