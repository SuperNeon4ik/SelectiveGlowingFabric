package me.superneon4ik.selectiveglowing.mixin;

import me.superneon4ik.selectiveglowing.SelectiveGlowing;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {
    @Shadow @Final
    private Entity entity;

    @Inject(method = "sendPackets", at = @At("TAIL"))
    private void selectiveglowing$sendFlags(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> sender, CallbackInfo ci) {
        var flags = SelectiveGlowing.getFlagsTrackedData();
        if (flags == null) return;

        byte bitmask = entity.getDataTracker().get(flags);
        List<DataTracker.SerializedEntry<?>> list = List.of(new DataTracker.SerializedEntry<>(0, flags.dataType(), bitmask));
        sender.accept(new EntityTrackerUpdateS2CPacket(entity.getId(), list));
    }
}