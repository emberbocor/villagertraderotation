package io.github.emberbocor.villagertraderotation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.emberbocor.villagertraderotation.rotation.RotationDataHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "convertTo", at = @At("RETURN"))
    private void villagertraderotation$copyRotationData(EntityType<?> entityType, boolean transferInventory, CallbackInfoReturnable<Mob> cir) {
        if ((Object) this instanceof RotationDataHolder from && cir.getReturnValue() instanceof RotationDataHolder to) {
            to.villagertraderotation$setRotationData(from.villagertraderotation$getRotationData());
        }
    }
}
