package com.phatdog.phatsprogression;

import dev.architectury.platform.Platform;

import java.nio.file.Path;

/** Central location for all config file paths. */
public final class ConfigPaths {

    private ConfigPaths() {}

    public static Path configRoot() {
        return Platform.getConfigFolder().resolve(PhatsProgression.MOD_ID);
    }

    public static Path blockTiers() {
        return configRoot().resolve("block_tiers.json");
    }

    public static Path toolTiers() {
        return configRoot().resolve("tool_tiers.json");
    }
}