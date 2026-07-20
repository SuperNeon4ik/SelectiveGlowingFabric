package me.superneon4ik.selectiveglowing.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SelectiveGlowingConfig {
    public static final Codec<SelectiveGlowingConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("saveState").forGetter(SelectiveGlowingConfig::isSaveState),
            Codec.BOOL.fieldOf("loadState").forGetter(SelectiveGlowingConfig::isLoadState)
    ).apply(instance, SelectiveGlowingConfig::new));

    private boolean saveState = true;
    private boolean loadState = true;

    public SelectiveGlowingConfig(boolean saveState, boolean loadState) {
        this.saveState = saveState;
        this.loadState = loadState;
    }

    public boolean isSaveState() {
        return saveState;
    }

    public boolean isLoadState() {
        return loadState;
    }
}
