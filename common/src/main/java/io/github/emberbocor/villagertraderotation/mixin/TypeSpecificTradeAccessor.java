package io.github.emberbocor.villagertraderotation.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.npc.villager.VillagerType;

@Mixin(targets = "net.minecraft.world.entity.npc.villager.VillagerTrades$TypeSpecificTrade")
public interface TypeSpecificTradeAccessor {
    @Accessor("trades")
    Map<ResourceKey<VillagerType>, VillagerTrades.ItemListing> villagertraderotation$getTrades();
}
