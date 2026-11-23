package io.github.emberbocor.villagertraderotation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;

@Mixin(AbstractVillager.class)
public interface AbstractVillagerInvoker {
    @Invoker("addOffersFromTradeSet")
    void villagertraderotation$addOffersFromTradeSet(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> tradeSet);
}
