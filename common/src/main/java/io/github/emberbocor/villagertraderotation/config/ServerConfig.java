package io.github.emberbocor.villagertraderotation.config;

import java.nio.file.Path;
import java.util.Set;

import io.github.emberbocor.villagertraderotation.platform.Services;
import net.minecraft.resources.Identifier;

public final class ServerConfig {
    static final int MIN_INTERVAL_DAYS = 1;
    static final int MAX_INTERVAL_DAYS = 100;
    static final Settings DEFAULTS = new Settings(true, 1, Set.of());

    private static volatile Settings settings = DEFAULTS;

    private ServerConfig() {
    }

    public static void load() {
        settings = DEFAULTS;
        Path file = Services.PLATFORM.getConfigDirectory().resolve(ServerConfigFile.FILE_NAME);
        settings = new ServerConfigReader(file).read(ServerConfigFile.load(file).orElse(null));
    }

    public static Settings settings() {
        return settings;
    }

    public record Settings(boolean enabled, int rerollIntervalDays, Set<Identifier> excludedProfessions) {
        public Settings {
            excludedProfessions = Set.copyOf(excludedProfessions);
        }
    }
}
