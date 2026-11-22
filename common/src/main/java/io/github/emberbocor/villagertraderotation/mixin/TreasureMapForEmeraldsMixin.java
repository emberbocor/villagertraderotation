package io.github.emberbocor.villagertraderotation.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.emberbocor.villagertraderotation.rotation.MapCache;
import io.github.emberbocor.villagertraderotation.rotation.MapListing;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

// Package-private in vanilla; Fabric API widens it to public, which the Fabric Mixin processor reports.
@SuppressWarnings("public-target")
@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$TreasureMapForEmeralds")
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
    private MapDecoration.Type destinationType;

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
    public MapDecoration.Type villagertraderotation$getDecoration() {
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
    private void villagertraderotation$offerCachedMap(Entity trader, RandomSource random, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer offer = MapCache.cachedOffer(trader, this);
        if (offer != null) {
            cir.setReturnValue(offer);
        }
    }

    @Inject(method = "getOffer", at = @At("RETURN"))
    private void villagertraderotation$cacheFoundMap(Entity trader, RandomSource random, CallbackInfoReturnable<MerchantOffer> cir) {
        MapCache.store(trader, this, cir.getReturnValue());
    }
}
