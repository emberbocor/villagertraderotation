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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    private void villagertraderotation$saveRotationData(ValueOutput output, CallbackInfo ci) {
        villagertraderotation$rotationData.save(((Villager) (Object) this).registryAccess(), output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void villagertraderotation$loadRotationData(ValueInput input, CallbackInfo ci) {
        villagertraderotation$rotationData.load(((Villager) (Object) this).registryAccess(), input);
    }

    @Inject(method = "shouldRestock", at = @At("HEAD"))
    private void villagertraderotation$rotateTrades(ServerLevel level, CallbackInfoReturnable<Boolean> cir) {
        TradeRotation.onRestockCheck((Villager) (Object) this, level);
    }
}
