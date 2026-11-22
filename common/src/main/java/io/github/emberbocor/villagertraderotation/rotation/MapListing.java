package io.github.emberbocor.villagertraderotation.rotation;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

/**
 * The fields of a vanilla explorer map listing (VillagerTrades.TreasureMapForEmeralds), added by a mixin.
 */
public interface MapListing {
    int villagertraderotation$getEmeraldCost();

    TagKey<Structure> villagertraderotation$getDestination();

    String villagertraderotation$getDisplayName();

    Holder<MapDecorationType> villagertraderotation$getDecoration();

    int villagertraderotation$getMaxUses();

    int villagertraderotation$getVillagerXp();
}
