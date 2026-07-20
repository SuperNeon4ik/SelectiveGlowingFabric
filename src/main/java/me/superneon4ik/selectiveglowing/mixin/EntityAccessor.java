package me.superneon4ik.selectiveglowing.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.Entity.class)
public interface EntityAccessor {
    @Accessor
    static EntityDataAccessor<Byte> getDATA_SHARED_FLAGS_ID() {
        throw new UnsupportedOperationException();
    }
}
