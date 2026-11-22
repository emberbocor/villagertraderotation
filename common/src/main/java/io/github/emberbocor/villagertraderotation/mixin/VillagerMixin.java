package io.github.emberbocor.villagertraderotation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.emberbocor.villagertraderotation.rotation.RotationData;
import io.github.emberbocor.villagertraderotation.rotation.RotationDataHolder;
import io.github.emberbocor.villagertraderotation.rotation.TradeRotation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;

@Mixin(Villager.class)
public abstract class VillagerMixin implements RotationDataHolder {
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
        villagertraderotation$rotationData.save(((Villager) (Object) this).registryAccess(), tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void villagertraderotation$loadRotationData(CompoundTag tag, CallbackInfo ci) {
        villagertraderotation$rotationData.load(((Villager) (Object) this).registryAccess(), tag);
    }

    @Inject(method = "shouldRestock", at = @At("HEAD"))
    private void villagertraderotation$rotateTrades(CallbackInfoReturnable<Boolean> cir) {
        TradeRotation.onRestockCheck((Villager) (Object) this);
    }
}
