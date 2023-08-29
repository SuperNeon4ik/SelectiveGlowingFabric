package me.superneon4ik.selectiveglowing.mixin;

import me.superneon4ik.selectiveglowing.SelectiveGlowing;
import me.superneon4ik.selectiveglowing.enums.EntityData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.EntityTrackingListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements EntityTrackingListener, TickablePacketListener, ServerPlayPacketListener {
    @Shadow public ServerPlayerEntity player;

    @Shadow @Final private ClientConnection connection;

    @Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", cancellable = true)
    private void sendPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo callbackInfo) {
        int observerId = player.getId();
        if (packet instanceof EntityTrackerUpdateS2CPacket entityTrackerUpdatePacket) {
            packet = SelectiveGlowing.cloneAndOverridePacket(entityTrackerUpdatePacket, observerId);
        }
        else if (packet instanceof BundleS2CPacket bundlePacket) {
            var packets = bundlePacket.getPackets();
            var newPackets = new ArrayList<Packet<ClientPlayPacketListener>>();
            for (Packet<ClientPlayPacketListener> oldPacket : packets) {
                if (oldPacket instanceof EntityTrackerUpdateS2CPacket entityTrackerUpdatePacket) {
                    newPackets.add(SelectiveGlowing.cloneAndOverridePacket(entityTrackerUpdatePacket, observerId));
                    continue;
                }
                newPackets.add(oldPacket);
            }
            packet = new BundleS2CPacket(newPackets);
        }

        try {
            connection.send(packet, callbacks);
        } catch (Throwable var6) {
            CrashReport crashReport = CrashReport.create(var6, "Sending packet");
            CrashReportSection crashReportSection = crashReport.addElement("Packet being sent");
            Packet<?> finalPacket = packet;
            crashReportSection.add("Packet class", () -> finalPacket.getClass().getCanonicalName());
            throw new CrashException(crashReport);
        }

        callbackInfo.cancel();
    }
}
