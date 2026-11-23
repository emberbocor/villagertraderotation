package io.github.emberbocor.villagertraderotation.rotation;

import org.jetbrains.annotations.Nullable;

import io.github.emberbocor.villagertraderotation.VillagerTradeRotation;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The mod's data on a villager or zombie villager: the day of the last rotation and the map cache.
 */
public final class RotationData {
    private static final String NBT_KEY = VillagerTradeRotation.MODID + ":rotation";
    private static final String LAST_DAY_KEY = "last_day";
    private static final String MAPS_KEY = "maps";
    // The cached map items have no vanilla data fixer, so a cache saved by another game version is not loaded.
    // Caches saved by Minecraft 26.2 (data version 4903) up to the running version are accepted.
    private static final String DATA_VERSION_KEY = "data_version";
    private static final int FIRST_DATA_VERSION = 4903;

    @Nullable
    private Long lastDay;
    private final MapCache maps = new MapCache();

    @Nullable
    Long lastDay() {
        return lastDay;
    }

    void setLastDay(long day) {
        lastDay = day;
    }

    MapCache maps() {
        return maps;
    }

    public void save(HolderLookup.Provider registries, ValueOutput output) {
        if (lastDay == null && maps.isEmpty()) {
            return;
        }
        CompoundTag data = new CompoundTag();
        if (lastDay != null) {
            data.putLong(LAST_DAY_KEY, lastDay);
        }
        if (!maps.isEmpty()) {
            maps.save(registries.createSerializationContext(NbtOps.INSTANCE)).ifPresent(saved -> {
                data.putInt(DATA_VERSION_KEY, currentDataVersion());
                data.put(MAPS_KEY, saved);
            });
        }
        output.store(NBT_KEY, CompoundTag.CODEC, data);
    }

    public void load(HolderLookup.Provider registries, ValueInput input) {
        input.read(NBT_KEY, CompoundTag.CODEC).ifPresent(data -> {
            lastDay = data.getLong(LAST_DAY_KEY).orElse(null);
            Tag saved = data.get(MAPS_KEY);
            int dataVersion = data.getIntOr(DATA_VERSION_KEY, 0);
            if (saved != null && (dataVersion < FIRST_DATA_VERSION || dataVersion > currentDataVersion())) {
                VillagerTradeRotation.LOGGER.debug("Discarding a map cache saved by another game version");
                saved = null;
            }
            maps.load(registries.createSerializationContext(NbtOps.INSTANCE), saved);
        });
    }

    private static int currentDataVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }
}
