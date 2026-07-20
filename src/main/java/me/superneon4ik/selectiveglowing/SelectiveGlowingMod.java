package me.superneon4ik.selectiveglowing;

import net.fabricmc.api.ModInitializer;

public class SelectiveGlowingMod implements ModInitializer {

    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        SelectiveGlowing.init();
        SelectiveGlowingCommands.register();
    }
}