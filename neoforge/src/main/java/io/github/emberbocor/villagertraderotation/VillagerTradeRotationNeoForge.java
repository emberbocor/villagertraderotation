package io.github.emberbocor.villagertraderotation;

import io.github.emberbocor.villagertraderotation.config.ServerConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@Mod(VillagerTradeRotation.MODID)
public class VillagerTradeRotationNeoForge {
    public VillagerTradeRotationNeoForge() {
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> ServerConfig.load());
    }
}
