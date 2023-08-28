package me.superneon4ik.selectiveglowing.mixin;

import com.mojang.logging.LogUtils;
import me.superneon4ik.selectiveglowing.enums.EntityData;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityTrackerUpdateS2CPacket.class)
public abstract class EntityTrackerUpdateS2CPacketMixin implements Packet<ClientPlayPacketListener> {
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(at = @At("HEAD"), method = "write(Lnet/minecraft/network/PacketByteBuf;)V")
    private void asd(PacketByteBuf buf, CallbackInfo info) {
        var t = ((EntityTrackerUpdateS2CPacket)(Object) this);
        if (t.trackedValues().stream().noneMatch(v -> v.id() == 0)) return;
        LOGGER.info(String.format("id: %d", t.id()));
        for (var value : t.trackedValues()) {
            if (value.id() == 0) {
                byte bitmask = (byte) value.value();
                bitmask = EntityData.GLOWING.setBit(bitmask);
                var newEntry = new DataTracker.SerializedEntry(0, value.handler(), bitmask);
                t.trackedValues().remove(value);
                t.trackedValues().add(newEntry);
                LOGGER.info(String.format("-> id: %d, old: %s, new: %s", value.id(), value.value().toString(), newEntry.value().toString()));
                break;
            }
        }
    }
}
