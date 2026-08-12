package me.superneon4ik.selectiveglowing.mixin;

import io.netty.channel.ChannelFutureListener;
import me.superneon4ik.selectiveglowing.SelectiveGlowing;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
    @Shadow
    @Final
    protected Connection connection;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"),
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            cancellable = true)
    private void send(Packet<?> packet, @Nullable ChannelFutureListener listener, CallbackInfo ci) {
        if (!(((ServerCommonPacketListenerImpl) (Object) this) instanceof ServerGamePacketListenerImpl networkHandler))
            return;

        var observer = networkHandler.player;

        if (packet instanceof ClientboundSetEntityDataPacket entityTrackerUpdatePacket) {
            packet = SelectiveGlowing.cloneAndOverridePacket(entityTrackerUpdatePacket, observer);
        }
        else if (packet instanceof ClientboundBundlePacket bundlePacket) {
            var packets = bundlePacket.subPackets();
            var newPackets = new ArrayList<Packet<? super ClientGamePacketListener>>();
            for (Packet<? super ClientGamePacketListener> oldPacket : packets) {
                if (oldPacket instanceof ClientboundSetEntityDataPacket entityTrackerUpdatePacket) {
                    newPackets.add(SelectiveGlowing.cloneAndOverridePacket(entityTrackerUpdatePacket, observer));
                    continue;
                }
                newPackets.add(oldPacket);
            }

            packet = new ClientboundBundlePacket(newPackets);
        }

        connection.send(packet, listener);
        ci.cancel();
    }
}
