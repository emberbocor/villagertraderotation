package io.github.emberbocor.villagertraderotation.rotation;

import org.jetbrains.annotations.Nullable;

import io.github.emberbocor.villagertraderotation.config.ServerConfig;
import io.github.emberbocor.villagertraderotation.mixin.AbstractVillagerInvoker;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

public final class TradeRotation {
    private static final int TRADES_PER_LEVEL = 2;

    private TradeRotation() {
    }

    /**
     * Called at the start of the vanilla restock check, which a villager runs each time it works at its job site.
     */
    public static void onRestockCheck(Villager villager) {
        ServerConfig.Settings settings = ServerConfig.settings();
        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (!settings.enabled() || profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT
                || settings.excludedProfessions().contains(BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession))) {
            return;
        }
        RotationData data = ((RotationDataHolder) villager).villagertraderotation$getRotationData();
        long today = villager.level().getDayTime() / Level.TICKS_PER_DAY;
        Long lastDay = data.lastDay();
        if (lastDay == null || today < lastDay) {
            data.setLastDay(today);
            return;
        }
        if (today - lastDay < settings.rerollIntervalDays() || villager.isTrading()) {
            return;
        }
        Int2ObjectMap<VillagerTrades.ItemListing[]> pool = tradePool(villager, profession);
        if (pool == null || pool.isEmpty()) {
            return;
        }
        rotate(villager, data.maps(), pool);
        data.setLastDay(today);
        TradePeekCompat.discardFutureTrades(villager);
    }

    private static void rotate(Villager villager, MapCache maps, Int2ObjectMap<VillagerTrades.ItemListing[]> pool) {
        maps.addMapsFrom(villager.getOffers(), pool);
        MerchantOffers offers = new MerchantOffers();
        int currentLevel = villager.getVillagerData().getLevel();
        maps.fill(villager, () -> {
            for (int level = VillagerData.MIN_VILLAGER_LEVEL; level <= currentLevel; level++) {
                VillagerTrades.ItemListing[] listings = pool.get(level);
                if (listings != null) {
                    ((AbstractVillagerInvoker) villager).villagertraderotation$addOffersFromItemListings(offers, listings, TRADES_PER_LEVEL);
                }
            }
        });
        villager.setOffers(offers);
    }

    /**
     * The trade pool vanilla's Villager.updateTrades uses for the profession.
     */
    @Nullable
    private static Int2ObjectMap<VillagerTrades.ItemListing[]> tradePool(Villager villager, VillagerProfession profession) {
        if (villager.level().enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
            Int2ObjectMap<VillagerTrades.ItemListing[]> experimental = VillagerTrades.EXPERIMENTAL_TRADES.get(profession);
            if (experimental != null) {
                return experimental;
            }
        }
        return VillagerTrades.TRADES.get(profession);
    }
}
