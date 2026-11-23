package io.github.emberbocor.villagertraderotation.rotation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.emberbocor.villagertraderotation.VillagerTradeRotation;
import io.github.emberbocor.villagertraderotation.mixin.TypeSpecificTradeAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

/**
 * One explorer map per map trade kind of a villager, keyed by the structure destination and decoration of the
 * vanilla listing. While a rotation fills a villager's offers, map listings offer a copy of the cached map
 * instead of searching for a structure, and maps found by a search are added to the cache.
 */
public final class MapCache {
    private static final ThreadLocal<Active> ACTIVE = new ThreadLocal<>();
    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(Registries.STRUCTURE).fieldOf("destination").forGetter(entry -> entry.key().destination()),
            ResourceKey.codec(Registries.MAP_DECORATION_TYPE).fieldOf("decoration").forGetter(entry -> entry.key().decoration()),
            ItemStack.CODEC.fieldOf("map").forGetter(Entry::map)
    ).apply(instance, (destination, decoration, map) -> new Entry(new Key(destination, decoration), map)));

    private final Map<Key, ItemStack> maps = new LinkedHashMap<>();

    boolean isEmpty() {
        return maps.isEmpty();
    }

    /**
     * Puts the explorer maps in the given offers into the cache, as far as they can be matched to exactly one map
     * trade kind of the pool.
     */
    void addMapsFrom(List<MerchantOffer> offers, Int2ObjectMap<VillagerTrades.ItemListing[]> pool) {
        List<MapListing> listings = new ArrayList<>();
        pool.values().forEach(level -> {
            for (VillagerTrades.ItemListing listing : level) {
                collectMapListings(listing, listings);
            }
        });
        if (listings.isEmpty()) {
            return;
        }
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            if (!result.is(Items.FILLED_MAP) || !result.has(DataComponents.MAP_ID)) {
                continue;
            }
            Set<Key> kinds = new HashSet<>();
            for (MapListing listing : listings) {
                Key key = key(listing);
                if (key != null && isMapOf(result, listing)) {
                    kinds.add(key);
                }
            }
            if (kinds.size() == 1) {
                maps.put(kinds.iterator().next(), result.copy());
            }
        }
    }

    /**
     * Runs {@code filler} with this cache active for map listings asked for an offer by {@code villager}.
     */
    void fill(Villager villager, Runnable filler) {
        Active previous = ACTIVE.get();
        ACTIVE.set(new Active(villager, this));
        try {
            filler.run();
        } finally {
            ACTIVE.set(previous);
        }
    }

    @Nullable
    public static MerchantOffer cachedOffer(Entity trader, MapListing listing) {
        MapCache cache = active(trader);
        Key key = cache != null ? key(listing) : null;
        ItemStack map = key != null ? cache.maps.get(key) : null;
        if (map == null) {
            return null;
        }
        return new MerchantOffer(new ItemCost(Items.EMERALD, listing.villagertraderotation$getEmeraldCost()), Optional.of(new ItemCost(Items.COMPASS)),
                map.copy(), listing.villagertraderotation$getMaxUses(), listing.villagertraderotation$getVillagerXp(), 0.2F);
    }

    public static void store(Entity trader, MapListing listing, @Nullable MerchantOffer offer) {
        MapCache cache = active(trader);
        Key key = cache != null && offer != null ? key(listing) : null;
        if (key != null) {
            cache.maps.put(key, offer.getResult().copy());
        }
    }

    Optional<Tag> save(RegistryOps<Tag> ops) {
        List<Entry> entries = maps.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).toList();
        return ENTRY_CODEC.listOf().encodeStart(ops, entries)
                .resultOrPartial(Util.prefix("Failed to save the map cache: ", VillagerTradeRotation.LOGGER::error));
    }

    void load(RegistryOps<Tag> ops, @Nullable Tag tag) {
        maps.clear();
        if (tag == null) {
            return;
        }
        ENTRY_CODEC.listOf().parse(ops, tag)
                .resultOrPartial(Util.prefix("Failed to load the map cache: ", VillagerTradeRotation.LOGGER::warn))
                .ifPresent(entries -> entries.forEach(entry -> maps.put(entry.key(), entry.map())));
    }

    @Nullable
    private static MapCache active(Entity trader) {
        Active active = ACTIVE.get();
        return active != null && active.villager() == trader ? active.cache() : null;
    }

    private static void collectMapListings(VillagerTrades.ItemListing listing, List<MapListing> listings) {
        if (listing instanceof MapListing map) {
            listings.add(map);
        } else if (listing instanceof TypeSpecificTradeAccessor typeSpecific) {
            typeSpecific.villagertraderotation$getTrades().values().forEach(inner -> collectMapListings(inner, listings));
        }
    }

    private static boolean isMapOf(ItemStack map, MapListing listing) {
        Component name = map.get(DataComponents.ITEM_NAME);
        if (name == null || !(name.getContents() instanceof TranslatableContents translatable)
                || !translatable.getKey().equals(listing.villagertraderotation$getDisplayName())) {
            return false;
        }
        ResourceKey<MapDecorationType> decoration = listing.villagertraderotation$getDecoration().unwrapKey().orElse(null);
        return decoration != null && map.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY).decorations().values().stream()
                .anyMatch(entry -> entry.type().is(decoration));
    }

    @Nullable
    private static Key key(MapListing listing) {
        return listing.villagertraderotation$getDecoration().unwrapKey()
                .map(decoration -> new Key(listing.villagertraderotation$getDestination(), decoration))
                .orElse(null);
    }

    private record Key(TagKey<Structure> destination, ResourceKey<MapDecorationType> decoration) {
    }

    private record Entry(Key key, ItemStack map) {
    }

    private record Active(Villager villager, MapCache cache) {
    }
}
