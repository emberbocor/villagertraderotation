package io.github.emberbocor.villagertraderotation.rotation;

import org.jetbrains.annotations.Nullable;

import io.github.emberbocor.villagertraderotation.VillagerTradeRotation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * The mod's data on a villager or zombie villager: the day of the last rotation and the map cache.
 */
public final class RotationData {
    private static final String NBT_KEY = VillagerTradeRotation.MODID + ":rotation";
    private static final String LAST_DAY_KEY = "last_day";
    // Own key on 1.20.1: the map items use the old item NBT format, which later versions would read as blank maps.
    private static final String MAPS_KEY = "maps_1_20";

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

    public void save(CompoundTag tag) {
        if (lastDay == null && maps.isEmpty()) {
            return;
        }
        CompoundTag data = new CompoundTag();
        if (lastDay != null) {
            data.putLong(LAST_DAY_KEY, lastDay);
        }
        if (!maps.isEmpty()) {
            data.put(MAPS_KEY, maps.save());
        }
        tag.put(NBT_KEY, data);
    }

    public void load(CompoundTag tag) {
        if (!tag.contains(NBT_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag data = tag.getCompound(NBT_KEY);
        lastDay = data.contains(LAST_DAY_KEY, Tag.TAG_LONG) ? data.getLong(LAST_DAY_KEY) : null;
        maps.load(data.get(MAPS_KEY));
    }
}
