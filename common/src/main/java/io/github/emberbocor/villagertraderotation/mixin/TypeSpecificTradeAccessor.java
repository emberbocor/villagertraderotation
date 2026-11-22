package io.github.emberbocor.villagertraderotation.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;

@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$TypeSpecificTrade")
public interface TypeSpecificTradeAccessor {
    @Accessor("trades")
    Map<VillagerType, VillagerTrades.ItemListing> villagertraderotation$getTrades();
}
