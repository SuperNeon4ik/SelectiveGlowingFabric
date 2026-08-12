package me.superneon4ik.selectiveglowing.extensions;

import net.minecraft.network.syncher.SynchedEntityData;

import java.util.List;

public interface SyncedEntityDataExtension {
    List<SynchedEntityData.DataValue<?>> selectiveglowing$packAll();
}