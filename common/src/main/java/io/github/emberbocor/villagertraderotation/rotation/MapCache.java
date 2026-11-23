package io.github.emberbocor.villagertraderotation.rotation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.emberbocor.villagertraderotation.VillagerTradeRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * One explorer map per map trade kind of a villager, keyed by the structure destination and decoration of the
 * trade's exploration_map function. While a rotation fills a villager's offers, exploration_map returns a
 * placeholder instead of searching; when the trade's offer is complete, the placeholder becomes a copy of the
 * cached map of its kind, or a map found by a search, which is added to the cache.
 */
public final class MapCache {
    private static final String MARKER_KEY = VillagerTradeRotation.MODID + ":deferred_map";
    private static final MapId PLACEHOLDER_MAP_ID = new MapId(-1);
    private static final ThreadLocal<Active> ACTIVE = new ThreadLocal<>();
    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.holderSet(Registries.STRUCTURE).fieldOf("destination").forGetter(entry -> entry.key().destination()),
            ResourceKey.codec(Registries.MAP_DECORATION_TYPE).fieldOf("decoration").forGetter(entry -> entry.key().decoration()),
            ItemStack.CODEC.fieldOf("map").forGetter(Entry::map)
    ).apply(instance, (destination, decoration, map) -> new Entry(new Key(destination, decoration), map)));

    private final Map<Key, ItemStack> maps = new LinkedHashMap<>();

    boolean isEmpty() {
        return maps.isEmpty();
    }

    /**
     * Puts the explorer maps in the villager's offers into the cache, as far as they can be matched to exactly one
     * map trade kind of the profession's trade sets.
     */
    void addMapsFrom(Villager villager, ServerLevel level, VillagerProfession profession) {
        List<ItemStack> offered = villager.getOffers().stream()
                .map(MerchantOffer::getResult)
                .filter(result -> result.getItem() instanceof MapItem && result.has(DataComponents.MAP_ID))
                .toList();
        if (offered.isEmpty()) {
            return;
        }
        List<ItemStack> placeholders = mapPlaceholders(villager, level, profession);
        for (ItemStack map : offered) {
            Set<Key> kinds = new HashSet<>();
            for (ItemStack placeholder : placeholders) {
                Search search = search(placeholder, level.registryAccess());
                Key key = search != null ? search.key() : null;
                if (key != null && isMapOf(map, placeholder, key)) {
                    kinds.add(key);
                }
            }
            if (kinds.size() == 1) {
                maps.put(kinds.iterator().next(), map.copy());
            }
        }
    }

    /**
     * Runs {@code filler} with this cache active for trades that {@code villager} generates.
     */
    void fill(Villager villager, Runnable filler) {
        withActive(new Active(villager, this), filler);
    }

    /**
     * The placeholder exploration_map returns for an active villager, or null to run the vanilla function.
     */
    @Nullable
    public static ItemStack placeholder(ItemStack stack, LootContext context, HolderSet<Structure> destination, Holder<MapDecorationType> decoration,
            byte zoom, int searchRadius, boolean skipKnownStructures) {
        if (active(context) == null || stack.isEmpty() || context.getOptional(LootContextParams.ORIGIN) == null) {
            return null;
        }
        Tag marker = Search.CODEC.encodeStart(context.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),
                new Search(destination, decoration, zoom, searchRadius, skipKnownStructures)).getOrThrow();
        stack.set(DataComponents.MAP_ID, PLACEHOLDER_MAP_ID);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(MARKER_KEY, marker));
        return stack;
    }

    /**
     * Replaces the placeholder map of a complete trade offer by the cached map of its kind or by a found map. A failed
     * search gives no offer, like vanilla's map trades, whose filtered function discards a map without a target.
     */
    @Nullable
    public static MerchantOffer resolve(LootContext context, @Nullable MerchantOffer offer) {
        Active active = active(context);
        MapCache cache = active != null ? active.cache() : null;
        ItemStack placeholder = offer != null ? offer.getResult() : ItemStack.EMPTY;
        Search search = cache != null ? search(placeholder, context.getLevel().registryAccess()) : null;
        if (search == null) {
            return offer;
        }
        Key key = search.key();
        ItemStack cached = key != null ? cache.maps.get(key) : null;
        ItemStack stack = withoutPlaceholder(placeholder);
        ItemStack map;
        if (cached != null) {
            map = cached.transmuteCopy(stack.getItem(), stack.getCount());
            map.applyComponents(stack.getComponentsPatch().forget(type -> type == DataComponents.MAP_ID));
        } else {
            ServerLevel level = context.getLevel();
            Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
            if (origin == null) {
                return null;
            }
            BlockPos originPos = BlockPos.containing(origin);
            if (search.skipKnownStructures()) {
                claimStructureAt(level, originPos, search.destination());
            }
            BlockPos target = level.findNearestMapStructure(search.destination(), originPos, search.searchRadius(), search.skipKnownStructures());
            if (target == null) {
                return null;
            }
            map = stack;
            MapItem.applyNewSavedData(level, map, target.getX(), target.getZ(), search.zoom(), true, true);
            MapItem.renderBiomePreviewMap(level, map);
            MapItemSavedData.addTargetDecoration(map, target, "+", search.decoration());
        }
        if (cached == null && key != null) {
            cache.maps.put(key, map.copy());
        }
        return new MerchantOffer(offer.getItemCostA(), offer.getItemCostB(), map, offer.getUses(), offer.getMaxUses(), offer.getXp(), offer.getPriceMultiplier());
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

    /**
     * The placeholders of every map trade of the profession's trade sets that the villager can offer, generated
     * without any structure search and with a separate random source, so no world or villager random is used.
     */
    private static List<ItemStack> mapPlaceholders(Villager villager, ServerLevel level, VillagerProfession profession) {
        LootContext context = new LootContext.Builder(new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, villager.position())
                .withParameter(LootContextParams.THIS_ENTITY, villager)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE))
                .withOptionalRandomSource(RandomSource.create())
                .create(Optional.empty());
        List<ItemStack> placeholders = new ArrayList<>();
        withActive(new Active(villager, null), () -> profession.tradeSetsByLevel().values().forEach(key ->
                level.registryAccess().lookupOrThrow(Registries.TRADE_SET).getOptional(key).map(TradeSet::trades).ifPresent(trades -> {
                    for (Holder<VillagerTrade> trade : trades) {
                        MerchantOffer offer = trade.value().getOffer(context);
                        if (offer != null && search(offer.getResult(), level.registryAccess()) != null) {
                            placeholders.add(offer.getResult());
                        }
                    }
                })));
        return placeholders;
    }

    private static void withActive(Active active, Runnable action) {
        Active previous = ACTIVE.get();
        ACTIVE.set(active);
        try {
            action.run();
        } finally {
            ACTIVE.set(previous);
        }
    }

    @Nullable
    private static Active active(LootContext context) {
        Active active = ACTIVE.get();
        return active != null && context.getOptional(LootContextParams.THIS_ENTITY) == active.villager() ? active : null;
    }

    @Nullable
    private static Search search(ItemStack stack, HolderLookup.Provider registries) {
        Tag marker = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().get(MARKER_KEY);
        return marker != null ? Search.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), marker).result().orElse(null) : null;
    }

    /**
     * The placeholder's item without the placeholder map ID and marker: the trade's map item as vanilla's
     * exploration_map receives it.
     */
    private static ItemStack withoutPlaceholder(ItemStack placeholder) {
        ItemStack stack = placeholder.copy();
        stack.remove(DataComponents.MAP_ID);
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).update(tag -> tag.remove(MARKER_KEY));
        if (customData.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, customData);
        }
        return stack;
    }

    /**
     * Marks the destination structure at the trader's position as referenced before a search that skips known
     * structures, as vanilla's exploration_map does.
     */
    private static void claimStructureAt(ServerLevel level, BlockPos pos, HolderSet<Structure> destination) {
        StructureManager structureManager = level.structureManager();
        StructureStart start = structureManager.getStructureAt(pos, destination);
        if (start.isValid() && start.canBeReferenced()) {
            structureManager.addReference(start);
        }
    }

    private static boolean isMapOf(ItemStack map, ItemStack placeholder, Key key) {
        Component name = placeholder.get(DataComponents.ITEM_NAME);
        return name != null && Objects.equals(map.get(DataComponents.ITEM_NAME), name)
                && map.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY).decorations().values().stream()
                        .anyMatch(entry -> entry.type().is(key.decoration()));
    }

    private record Search(HolderSet<Structure> destination, Holder<MapDecorationType> decoration, byte zoom, int searchRadius, boolean skipKnownStructures) {
        private static final Codec<Search> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryCodecs.holderSet(Registries.STRUCTURE).fieldOf("destination").forGetter(Search::destination),
                MapDecorationType.CODEC.fieldOf("decoration").forGetter(Search::decoration),
                Codec.BYTE.fieldOf("zoom").forGetter(Search::zoom),
                Codec.INT.fieldOf("search_radius").forGetter(Search::searchRadius),
                Codec.BOOL.fieldOf("skip_existing_chunks").forGetter(Search::skipKnownStructures)
        ).apply(instance, Search::new));

        @Nullable
        Key key() {
            return decoration.unwrapKey().map(decorationKey -> new Key(destination, decorationKey)).orElse(null);
        }
    }

    /**
     * A structure tag destination is compared by its tag key, a direct structure list by its entries.
     */
    private record Key(HolderSet<Structure> destination, ResourceKey<MapDecorationType> decoration) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && destination.unwrap().equals(key.destination.unwrap()) && decoration.equals(key.decoration);
        }

        @Override
        public int hashCode() {
            return Objects.hash(destination.unwrap(), decoration);
        }
    }

    private record Entry(Key key, ItemStack map) {
    }

    private record Active(Villager villager, @Nullable MapCache cache) {
    }
}
