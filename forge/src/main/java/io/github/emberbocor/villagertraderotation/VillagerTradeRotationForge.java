package io.github.emberbocor.villagertraderotation;

import io.github.emberbocor.villagertraderotation.config.ServerConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(VillagerTradeRotation.MODID)
public class VillagerTradeRotationForge {
    public VillagerTradeRotationForge() {
        MinecraftForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> ServerConfig.load());
    }
}
