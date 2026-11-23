package io.github.emberbocor.villagertraderotation.rotation;

import io.github.emberbocor.villagertraderotation.config.ServerConfig;
import io.github.emberbocor.villagertraderotation.mixin.AbstractVillagerInvoker;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.timeline.Timelines;

public final class TradeRotation {
    private TradeRotation() {
    }

    /**
     * Called at the start of the vanilla restock check, which a villager runs each time it works at its job site.
     */
    public static void onRestockCheck(Villager villager, ServerLevel level) {
        ServerConfig.Settings settings = ServerConfig.settings();
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        if (!settings.enabled() || profession.is(VillagerProfession.NONE) || profession.is(VillagerProfession.NITWIT)
                || settings.excludedProfessions().contains(BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession.value()))) {
            return;
        }
        RotationData data = ((RotationDataHolder) villager).villagertraderotation$getRotationData();
        long today = day(level);
        Long lastDay = data.lastDay();
        if (lastDay == null || today < lastDay) {
            data.setLastDay(today);
            return;
        }
        if (today - lastDay < settings.rerollIntervalDays() || villager.isTrading()) {
            return;
        }
        if (profession.value().tradeSetsByLevel().isEmpty()) {
            return;
        }
        rotate(villager, level, data.maps(), profession.value());
        data.setLastDay(today);
        TradePeekCompat.discardFutureTrades(villager);
    }

    private static void rotate(Villager villager, ServerLevel level, MapCache maps, VillagerProfession profession) {
        maps.addMapsFrom(villager, level, profession);
        MerchantOffers offers = new MerchantOffers();
        int currentLevel = villager.getVillagerData().level();
        maps.fill(villager, () -> {
            for (int villagerLevel = VillagerData.MIN_VILLAGER_LEVEL; villagerLevel <= currentLevel; villagerLevel++) {
                ResourceKey<TradeSet> tradeSet = profession.getTrades(villagerLevel);
                if (tradeSet != null) {
                    ((AbstractVillagerInvoker) villager).villagertraderotation$addOffersFromTradeSet(level, offers, tradeSet);
                }
            }
        });
        villager.setOffers(offers);
    }

    /**
     * The day index vanilla's Villager.shouldRestock uses: the period count of the overworld day timeline.
     */
    private static long day(ServerLevel level) {
        return level.registryAccess().get(Timelines.OVERWORLD_DAY)
                .map(timeline -> timeline.value().getPeriodCount(level.clockManager()))
                .orElse(0);
    }
}
