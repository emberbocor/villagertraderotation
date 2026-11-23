package io.github.emberbocor.villagertraderotation.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.emberbocor.villagertraderotation.rotation.MapCache;
import io.github.emberbocor.villagertraderotation.rotation.MapListing;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$TreasureMapForEmeralds")
public abstract class TreasureMapForEmeraldsMixin implements MapListing {
    @Shadow
    @Final
    private int emeraldCost;

    @Shadow
    @Final
    private TagKey<Structure> destination;

    @Shadow
    @Final
    private String displayName;

    @Shadow
    @Final
    private Holder<MapDecorationType> destinationType;

    @Shadow
    @Final
    private int maxUses;

    @Shadow
    @Final
    private int villagerXp;

    @Override
    public int villagertraderotation$getEmeraldCost() {
        return emeraldCost;
    }

    @Override
    public TagKey<Structure> villagertraderotation$getDestination() {
        return destination;
    }

    @Override
    public String villagertraderotation$getDisplayName() {
        return displayName;
    }

    @Override
    public Holder<MapDecorationType> villagertraderotation$getDecoration() {
        return destinationType;
    }

    @Override
    public int villagertraderotation$getMaxUses() {
        return maxUses;
    }

    @Override
    public int villagertraderotation$getVillagerXp() {
        return villagerXp;
    }

    @Inject(method = "getOffer", at = @At("HEAD"), cancellable = true)
    private void villagertraderotation$offerCachedMap(ServerLevel level, Entity trader, RandomSource random, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer offer = MapCache.cachedOffer(trader, this);
        if (offer != null) {
            cir.setReturnValue(offer);
        }
    }

    @Inject(method = "getOffer", at = @At("RETURN"))
    private void villagertraderotation$cacheFoundMap(ServerLevel level, Entity trader, RandomSource random, CallbackInfoReturnable<MerchantOffer> cir) {
        MapCache.store(trader, this, cir.getReturnValue());
    }
}
