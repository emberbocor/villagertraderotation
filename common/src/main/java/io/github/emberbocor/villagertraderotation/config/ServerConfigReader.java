package io.github.emberbocor.villagertraderotation.config;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import io.github.emberbocor.villagertraderotation.VillagerTradeRotation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

final class ServerConfigReader {
    private static final Set<String> KEYS = Set.of("enabled", "rerollIntervalDays", "excludedProfessions");

    private final Path file;

    ServerConfigReader(Path file) {
        this.file = file;
    }

    ServerConfig.Settings read(UnmodifiableConfig config) {
        ServerConfig.Settings defaults = ServerConfig.DEFAULTS;
        if (config == null) {
            return defaults;
        }
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
            if (!KEYS.contains(entry.getKey())) {
                VillagerTradeRotation.LOGGER.warn("{}: unknown option '{}', ignored", file, entry.getKey());
            }
        }
        return new ServerConfig.Settings(enabled(config, defaults.enabled()),
                rerollIntervalDays(config, defaults.rerollIntervalDays()),
                excludedProfessions(config, defaults.excludedProfessions()));
    }

    private boolean enabled(UnmodifiableConfig config, boolean fallback) {
        Object value = config.get("enabled");
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        VillagerTradeRotation.LOGGER.warn("{}: enabled must be true or false, got '{}'; using {}", file, value, fallback);
        return fallback;
    }

    private int rerollIntervalDays(UnmodifiableConfig config, int fallback) {
        Object value = config.get("rerollIntervalDays");
        if (value == null) {
            return fallback;
        }
        if ((value instanceof Integer || value instanceof Long) && ((Number) value).longValue() >= ServerConfig.MIN_INTERVAL_DAYS
                && ((Number) value).longValue() <= ServerConfig.MAX_INTERVAL_DAYS) {
            return ((Number) value).intValue();
        }
        VillagerTradeRotation.LOGGER.warn("{}: rerollIntervalDays must be a whole number from {} to {}, got '{}'; using {}",
                file, ServerConfig.MIN_INTERVAL_DAYS, ServerConfig.MAX_INTERVAL_DAYS, value, fallback);
        return fallback;
    }

    // Forge deprecates the vanilla profession registry in favour of ForgeRegistries, which common code cannot use.
    @SuppressWarnings("deprecation")
    private Set<ResourceLocation> excludedProfessions(UnmodifiableConfig config, Set<ResourceLocation> fallback) {
        Object value = config.get("excludedProfessions");
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof List<?> list)) {
            VillagerTradeRotation.LOGGER.warn("{}: excludedProfessions must be a list of profession IDs such as [\"minecraft:librarian\"], got '{}'; using []",
                    file, value);
            return fallback;
        }
        Set<ResourceLocation> professions = new HashSet<>();
        for (Object element : list) {
            ResourceLocation id = element instanceof String string ? ResourceLocation.tryParse(string) : null;
            if (id == null) {
                VillagerTradeRotation.LOGGER.warn("{}: excludedProfessions must only hold profession IDs such as \"minecraft:librarian\", got '{}'; using []",
                        file, element);
                return fallback;
            }
            professions.add(id);
        }
        for (ResourceLocation id : professions) {
            if (!BuiltInRegistries.VILLAGER_PROFESSION.containsKey(id)) {
                VillagerTradeRotation.LOGGER.warn("{}: excludedProfessions contains '{}', which is not a known profession", file, id);
            }
        }
        return professions;
    }
}
