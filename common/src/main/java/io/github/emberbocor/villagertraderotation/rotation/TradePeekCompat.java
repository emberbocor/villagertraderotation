package io.github.emberbocor.villagertraderotation.rotation;

import java.lang.reflect.Method;

import org.jetbrains.annotations.Nullable;

import io.github.emberbocor.villagertraderotation.VillagerTradeRotation;
import io.github.emberbocor.villagertraderotation.platform.Services;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Optional Villager Trade Peek support without a dependency. After a rotation the stored future trades are removed,
 * so Villager Trade Peek previews new ones from the villager's new trades the next time the trading screen opens.
 */
final class TradePeekCompat {
    private static final String MODID = "villagertradepeek";

    @Nullable
    private static final Method REMOVE = Services.PLATFORM.isModLoaded(MODID) ? find() : null;

    private TradePeekCompat() {
    }

    static void discardFutureTrades(Villager villager) {
        if (REMOVE == null) {
            return;
        }
        try {
            REMOVE.invoke(null, villager);
        } catch (ReflectiveOperationException | RuntimeException e) {
            VillagerTradeRotation.LOGGER.warn("Failed to reset the Villager Trade Peek future trades of {}", villager, e);
        }
    }

    @Nullable
    private static Method find() {
        try {
            return Class.forName("io.github.emberbocor.villagertradepeek.trade.FutureTradeStorage").getMethod("remove", Villager.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            VillagerTradeRotation.LOGGER.warn("Villager Trade Peek is installed but its future trades could not be accessed; the preview is not reset on rotation", e);
            return null;
        }
    }
}
