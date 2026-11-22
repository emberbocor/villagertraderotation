package io.github.emberbocor.villagertraderotation.rotation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import io.github.emberbocor.villagertraderotation.VillagerTradeRotation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

/**
 * One explorer map per map trade kind of a villager, keyed by the structure destination and decoration of the
 * vanilla listing. While a rotation fills a villager's offers, map listings offer a copy of the cached map
 * instead of searching for a structure, and maps found by a search are added to the cache.
 */
public final class MapCache {
    private static final ThreadLocal<Active> ACTIVE = new ThreadLocal<>();
    private static final String DESTINATION_KEY = "destination";
    private static final String DECORATION_KEY = "decoration";
    private static final String MAP_KEY = "map";

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
                if (listing instanceof MapListing map) {
                    listings.add(map);
                }
            }
        });
        if (listings.isEmpty()) {
            return;
        }
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            if (!result.is(Items.FILLED_MAP) || MapItem.getMapId(result) == null) {
                continue;
            }
            Set<Key> kinds = new HashSet<>();
            for (MapListing listing : listings) {
                if (isMapOf(result, listing)) {
                    kinds.add(key(listing));
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
        ItemStack map = cache != null ? cache.maps.get(key(listing)) : null;
        if (map == null) {
            return null;
        }
        return new MerchantOffer(new ItemStack(Items.EMERALD, listing.villagertraderotation$getEmeraldCost()), new ItemStack(Items.COMPASS),
                map.copy(), listing.villagertraderotation$getMaxUses(), listing.villagertraderotation$getVillagerXp(), 0.2F);
    }

    public static void store(Entity trader, MapListing listing, @Nullable MerchantOffer offer) {
        MapCache cache = active(trader);
        if (cache != null && offer != null) {
            cache.maps.put(key(listing), offer.getResult().copy());
        }
    }

    ListTag save() {
        ListTag list = new ListTag();
        maps.forEach((key, map) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString(DESTINATION_KEY, key.destination().location().toString());
            entry.putString(DECORATION_KEY, decorationId(key.decoration()));
            entry.put(MAP_KEY, map.save(new CompoundTag()));
            list.add(entry);
        });
        return list;
    }

    void load(@Nullable Tag tag) {
        maps.clear();
        if (!(tag instanceof ListTag list)) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation destination = ResourceLocation.tryParse(entry.getString(DESTINATION_KEY));
            MapDecoration.Type decoration = decoration(entry.getString(DECORATION_KEY));
            ItemStack map = ItemStack.of(entry.getCompound(MAP_KEY));
            if (destination == null || decoration == null || map.isEmpty()) {
                VillagerTradeRotation.LOGGER.warn("Failed to load the map cache entry {}", entry);
                continue;
            }
            maps.put(new Key(TagKey.create(Registries.STRUCTURE, destination), decoration), map);
        }
    }

    @Nullable
    private static MapCache active(Entity trader) {
        Active active = ACTIVE.get();
        return active != null && active.villager() == trader ? active.cache() : null;
    }

    private static boolean isMapOf(ItemStack map, MapListing listing) {
        if (!map.hasCustomHoverName() || !(map.getHoverName().getContents() instanceof TranslatableContents translatable)
                || !translatable.getKey().equals(listing.villagertraderotation$getDisplayName())) {
            return false;
        }
        CompoundTag tag = map.getTag();
        ListTag decorations = tag != null ? tag.getList("Decorations", Tag.TAG_COMPOUND) : new ListTag();
        for (int i = 0; i < decorations.size(); i++) {
            if (decorations.getCompound(i).getByte("type") == listing.villagertraderotation$getDecoration().getIcon()) {
                return true;
            }
        }
        return false;
    }

    private static Key key(MapListing listing) {
        return new Key(listing.villagertraderotation$getDestination(), listing.villagertraderotation$getDecoration());
    }

    /**
     * The decoration is saved under the ID it has as a registry entry in later Minecraft versions, for example
     * "minecraft:monument".
     */
    private static String decorationId(MapDecoration.Type decoration) {
        return ResourceLocation.DEFAULT_NAMESPACE + ":" + decoration.name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static MapDecoration.Type decoration(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !location.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            return null;
        }
        for (MapDecoration.Type type : MapDecoration.Type.values()) {
            if (type.name().toLowerCase(Locale.ROOT).equals(location.getPath())) {
                return type;
            }
        }
        return null;
    }

    private record Key(TagKey<Structure> destination, MapDecoration.Type decoration) {
    }

    private record Active(Villager villager, MapCache cache) {
    }
}
