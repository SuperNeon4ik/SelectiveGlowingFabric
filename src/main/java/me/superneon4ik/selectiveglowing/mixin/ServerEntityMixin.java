package me.superneon4ik.selectiveglowing.mixin;

import me.superneon4ik.selectiveglowing.SelectiveGlowing;
import me.superneon4ik.selectiveglowing.extensions.ServerEntityExtension;
import me.superneon4ik.selectiveglowing.extensions.SyncedEntityDataExtension;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin implements ServerEntityExtension {
    @Shadow @Final
    private Entity entity;

    @Shadow
    private @Nullable List<SynchedEntityData.DataValue<?>> trackedDataValues;

    @Shadow @Final
    private ServerEntity.Synchronizer synchronizer;

    @Inject(method = "sendPairingData", at = @At("TAIL"))
    private void selectiveglowing$sendFlags(ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> broadcast, CallbackInfo ci) {
        var flags = SelectiveGlowing.getFlagsTrackedData();
        byte bitmask = entity.getEntityData().get(flags);
        List<SynchedEntityData.DataValue<?>> list = List.of(new SynchedEntityData.DataValue<>(flags.id(), flags.serializer(), bitmask));
        broadcast.accept(new ClientboundSetEntityDataPacket(entity.getId(), list));
    }

    @Override
    public void selectiveglowing$sendAllEntityData() {
        var entityData = entity.getEntityData();
        var packedValues = ((SyncedEntityDataExtension) entityData).selectiveglowing$packAll();

        trackedDataValues = entityData.getNonDefaultValues();
        synchronizer.sendToTrackingPlayersAndSelf(new ClientboundSetEntityDataPacket(entity.getId(), packedValues));
    }
}
