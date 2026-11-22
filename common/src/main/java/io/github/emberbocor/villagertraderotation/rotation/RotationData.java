package io.github.emberbocor.villagertraderotation.rotation;

import org.jetbrains.annotations.Nullable;

import io.github.emberbocor.villagertraderotation.VillagerTradeRotation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

/**
 * The mod's data on a villager or zombie villager: the day of the last rotation and the map cache.
 */
public final class RotationData {
    private static final String NBT_KEY = VillagerTradeRotation.MODID + ":rotation";
    private static final String LAST_DAY_KEY = "last_day";
    private static final String MAPS_KEY = "maps";

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

    public void save(RegistryAccess registries, CompoundTag tag) {
        if (lastDay == null && maps.isEmpty()) {
            return;
        }
        CompoundTag data = new CompoundTag();
        if (lastDay != null) {
            data.putLong(LAST_DAY_KEY, lastDay);
        }
        if (!maps.isEmpty()) {
            maps.save(registries.createSerializationContext(NbtOps.INSTANCE)).ifPresent(saved -> data.put(MAPS_KEY, saved));
        }
        tag.put(NBT_KEY, data);
    }

    public void load(RegistryAccess registries, CompoundTag tag) {
        if (!tag.contains(NBT_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag data = tag.getCompound(NBT_KEY);
        lastDay = data.contains(LAST_DAY_KEY, Tag.TAG_LONG) ? data.getLong(LAST_DAY_KEY) : null;
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        maps.load(ops, data.get(MAPS_KEY));
    }
}
