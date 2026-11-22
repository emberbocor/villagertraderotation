package io.github.emberbocor.villagertraderotation;

import io.github.emberbocor.villagertraderotation.config.ServerConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class VillagerTradeRotationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerConfig.load());
    }
}
