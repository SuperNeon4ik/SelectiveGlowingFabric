package me.superneon4ik.selectiveglowing.mixin;

import io.netty.channel.ChannelFutureListener;
import me.superneon4ik.selectiveglowing.SelectiveGlowing;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class ServerCommonNetworkHandlerMixin {
    @Shadow @Final
    protected ClientConnection connection;
    
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"), 
            method = "send",
            cancellable = true)
    private void sendPacket(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        if (((ServerCommonNetworkHandler)(Object)this) instanceof ServerPlayNetworkHandler spnh) {
            int observerId = spnh.player.getId(); // i hope it doesn't break lmfao
            if (packet instanceof EntityTrackerUpdateS2CPacket entityTrackerUpdatePacket) {
                packet = SelectiveGlowing.cloneAndOverridePacket(entityTrackerUpdatePacket, observerId);
            } else if (packet instanceof BundleS2CPacket bundlePacket) {
                var packets = bundlePacket.getPackets();
                var newPackets = new ArrayList<Packet<? super ClientPlayPacketListener>>();
                for (Packet<? super ClientPlayPacketListener> oldPacket : packets) {
                    if (oldPacket instanceof EntityTrackerUpdateS2CPacket entityTrackerUpdatePacket) {
                        newPackets.add(SelectiveGlowing.cloneAndOverridePacket(entityTrackerUpdatePacket, observerId));
                        continue;
                    }
                    newPackets.add(oldPacket);
                }

                packet = new BundleS2CPacket(newPackets);
            }

            connection.send(packet, channelFutureListener);
            ci.cancel();
        }
    }
}
