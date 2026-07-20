package me.superneon4ik.selectiveglowing.mixin;

import me.superneon4ik.selectiveglowing.SelectiveGlowing;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {
    @Shadow @Final
    private Entity entity;

    @Inject(method = "sendPairingData", at = @At("TAIL"))
    private void selectiveglowing$sendFlags(ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> sender, CallbackInfo ci) {
        var flags = SelectiveGlowing.getFlagsTrackedData();
        if (flags == null) return;

        byte bitmask = entity.getEntityData().get(flags);
        List<SynchedEntityData.DataValue<?>> list = List.of(new SynchedEntityData.DataValue<>(0, flags.serializer(), bitmask));
        sender.accept(new ClientboundSetEntityDataPacket(entity.getId(), list));
    }
}