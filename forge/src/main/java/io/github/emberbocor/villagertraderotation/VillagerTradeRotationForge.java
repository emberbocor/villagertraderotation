package io.github.emberbocor.villagertraderotation;

import io.github.emberbocor.villagertraderotation.config.ServerConfig;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(VillagerTradeRotation.MODID)
public class VillagerTradeRotationForge {
    public VillagerTradeRotationForge() {
        ServerAboutToStartEvent.BUS.addListener(event -> ServerConfig.load());
    }
}
