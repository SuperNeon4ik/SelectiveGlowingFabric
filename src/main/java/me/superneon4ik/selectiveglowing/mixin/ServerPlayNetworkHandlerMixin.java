package me.superneon4ik.selectiveglowing.mixin;

import me.superneon4ik.selectiveglowing.SelectiveGlowing;
import me.superneon4ik.selectiveglowing.enums.EntityData;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V")
    private void sendPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo callbackInfo) {
        if (packet instanceof EntityTrackerUpdateS2CPacket entityTrackerUpdatePacket) {
            int observerId = player.getId();
            int targetId = entityTrackerUpdatePacket.id();
            if (!SelectiveGlowing.isGlowing(targetId, observerId)) return;
            for (var value : entityTrackerUpdatePacket.trackedValues()) {
                if (value.id() == 0) {
                    byte bitmask = (byte) value.value();
                    bitmask = EntityData.GLOWING.setBit(bitmask);
                    var newEntry = new DataTracker.SerializedEntry(0, value.handler(), bitmask);
                    entityTrackerUpdatePacket.trackedValues().remove(value);
                    entityTrackerUpdatePacket.trackedValues().add(newEntry);
                    break;
                }
            }
        }
    }
}
