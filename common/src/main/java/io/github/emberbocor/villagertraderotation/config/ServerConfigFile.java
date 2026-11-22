package io.github.emberbocor.villagertraderotation.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlFormat;

import io.github.emberbocor.villagertraderotation.VillagerTradeRotation;

final class ServerConfigFile {
    static final String FILE_NAME = VillagerTradeRotation.MODID + "-server.toml";

    private static final String DEFAULT_TEXT = """
            # Villager Trade Rotation server config.
            # Created on the first server or world start.
            # Restart the server or reopen the singleplayer world to apply changes.

            # Rotate villager trades. False stops all rotation; the data saved on villagers is kept.
            enabled = true

            # Days between two rotations of the same villager, a whole number from 1 to 100.
            # A new day starts at sunrise; sleeping through the night also starts a new day.
            # 1 means today's trades, new ones tomorrow.
            rerollIntervalDays = 1

            # Profession IDs whose villagers never rotate, for example
            # ["minecraft:librarian", "minecraft:cartographer"]. Professions from other mods work too.
            excludedProfessions = []
            """;

    private ServerConfigFile() {
    }

    static Optional<UnmodifiableConfig> load(Path file) {
        if (!Files.exists(file)) {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT_TEXT, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                VillagerTradeRotation.LOGGER.info("Created {}", file);
            } catch (IOException e) {
                VillagerTradeRotation.LOGGER.warn("Could not create {}, using the default settings: {}", file, e.getMessage());
            }
            return parse(file, DEFAULT_TEXT);
        }

        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return parse(file, text.startsWith("﻿") ? text.substring(1) : text);
        } catch (IOException e) {
            VillagerTradeRotation.LOGGER.warn("Could not read {}, using the default settings; file left unchanged: {}", file, e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<UnmodifiableConfig> parse(Path file, String text) {
        try {
            return Optional.of(TomlFormat.instance().createParser().parse(text));
        } catch (RuntimeException e) {
            VillagerTradeRotation.LOGGER.warn("Could not parse {}, using the default settings; file left unchanged: {}", file, e.getMessage());
            return Optional.empty();
        }
    }
}
