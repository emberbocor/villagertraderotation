package io.github.emberbocor.villagertraderotation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.emberbocor.villagertraderotation.rotation.MapCache;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;

@Mixin(VillagerTrade.class)
public abstract class VillagerTradeMixin {
    @Inject(method = "getOffer", at = @At("RETURN"), cancellable = true)
    private void villagertraderotation$resolveMap(LootContext context, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer offer = cir.getReturnValue();
        MerchantOffer resolved = MapCache.resolve(context, offer);
        if (resolved != offer) {
            cir.setReturnValue(resolved);
        }
    }
}
