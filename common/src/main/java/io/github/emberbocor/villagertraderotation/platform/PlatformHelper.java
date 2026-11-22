package io.github.emberbocor.villagertraderotation.platform;

import java.nio.file.Path;

public interface PlatformHelper {
    Path getConfigDirectory();

    boolean isModLoaded(String modId);
}
