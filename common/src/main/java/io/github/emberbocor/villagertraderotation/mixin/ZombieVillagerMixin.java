package io.github.emberbocor.villagertraderotation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.emberbocor.villagertraderotation.rotation.RotationData;
import io.github.emberbocor.villagertraderotation.rotation.RotationDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.monster.ZombieVillager;

@Mixin(ZombieVillager.class)
public abstract class ZombieVillagerMixin implements RotationDataHolder {
    @Unique
    private RotationData villagertraderotation$rotationData = new RotationData();

    @Override
    public RotationData villagertraderotation$getRotationData() {
        return villagertraderotation$rotationData;
    }

    @Override
    public void villagertraderotation$setRotationData(RotationData data) {
        villagertraderotation$rotationData = data;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void villagertraderotation$saveRotationData(CompoundTag tag, CallbackInfo ci) {
        villagertraderotation$rotationData.save(tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void villagertraderotation$loadRotationData(CompoundTag tag, CallbackInfo ci) {
        villagertraderotation$rotationData.load(tag);
    }
}
