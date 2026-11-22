package io.github.emberbocor.villagertraderotation;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(VillagerTradeRotation.MODID)
public class VillagerTradeRotation {
    public static final String MODID = "villagertraderotation";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VillagerTradeRotation(IEventBus modEventBus, ModContainer modContainer) {
    }
}
