package io.github.emberbocor.villagertraderotation.rotation;

import org.jetbrains.annotations.Nullable;

import io.github.emberbocor.villagertraderotation.config.ServerConfig;
import io.github.emberbocor.villagertraderotation.mixin.AbstractVillagerInvoker;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.trading.MerchantOffers;

public final class TradeRotation {
    private static final int TRADES_PER_LEVEL = 2;

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
        long today = level.getDayCount();
        Long lastDay = data.lastDay();
        if (lastDay == null || today < lastDay) {
            data.setLastDay(today);
            return;
        }
        if (today - lastDay < settings.rerollIntervalDays() || villager.isTrading()) {
            return;
        }
        Int2ObjectMap<VillagerTrades.ItemListing[]> pool = tradePool(level, profession.unwrapKey().orElse(null));
        if (pool == null || pool.isEmpty()) {
            return;
        }
        rotate(villager, level, data.maps(), pool);
        data.setLastDay(today);
        TradePeekCompat.discardFutureTrades(villager);
    }

    private static void rotate(Villager villager, ServerLevel level, MapCache maps, Int2ObjectMap<VillagerTrades.ItemListing[]> pool) {
        maps.addMapsFrom(villager.getOffers(), pool);
        MerchantOffers offers = new MerchantOffers();
        int currentLevel = villager.getVillagerData().level();
        maps.fill(villager, () -> {
            for (int villagerLevel = VillagerData.MIN_VILLAGER_LEVEL; villagerLevel <= currentLevel; villagerLevel++) {
                VillagerTrades.ItemListing[] listings = pool.get(villagerLevel);
                if (listings != null) {
                    ((AbstractVillagerInvoker) villager).villagertraderotation$addOffersFromItemListings(level, offers, listings, TRADES_PER_LEVEL);
                }
            }
        });
        villager.setOffers(offers);
    }

    /**
     * The trade pool vanilla's Villager.updateTrades uses for the profession.
     */
    @Nullable
    private static Int2ObjectMap<VillagerTrades.ItemListing[]> tradePool(ServerLevel level, @Nullable ResourceKey<VillagerProfession> profession) {
        if (profession == null) {
            return null;
        }
        if (level.enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
            Int2ObjectMap<VillagerTrades.ItemListing[]> experimental = VillagerTrades.EXPERIMENTAL_TRADES.get(profession);
            if (experimental != null) {
                return experimental;
            }
        }
        return VillagerTrades.TRADES.get(profession);
    }
}
