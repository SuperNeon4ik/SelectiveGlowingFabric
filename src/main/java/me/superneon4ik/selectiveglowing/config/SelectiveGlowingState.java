package me.superneon4ik.selectiveglowing.config;

import com.mojang.serialization.Codec;
import net.minecraft.util.Uuids;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SelectiveGlowingState {
    public static final Codec<SelectiveGlowingState> CODEC = Codec.unboundedMap(Uuids.CODEC, Uuids.SET_CODEC).xmap(
            SelectiveGlowingState::new,
            SelectiveGlowingState::getState
    );

    private final Map<UUID, Set<UUID>> state;

    public SelectiveGlowingState(Map<UUID, Set<UUID>> state) {
        this.state = new HashMap<>(state);
    }

    public Map<UUID, Set<UUID>> getState() {
        return state;
    }
}
