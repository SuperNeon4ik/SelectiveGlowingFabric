package me.superneon4ik.selectiveglowing.mixin;

import me.superneon4ik.selectiveglowing.extensions.SyncedEntityDataExtension;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataMixin implements SyncedEntityDataExtension {
    @Shadow
    @Final
    private SynchedEntityData.DataItem<?>[] itemsById;

    @Override
    public List<SynchedEntityData.DataValue<?>> selectiveglowing$packAll() {
        var values = new ArrayList<SynchedEntityData.DataValue<?>>(itemsById.length);

        for (var item : itemsById) {
            values.add(item.value());
        }

        return values;
    }
}