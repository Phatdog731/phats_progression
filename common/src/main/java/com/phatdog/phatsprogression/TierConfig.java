package com.phatdog.phatsprogression;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads block_tiers.json and tool_tiers.json from the config folder.
 * <p>
 * Schema:
 * <pre>
 * {
 *   "ignore_datapacks": false,
 *   "tiers": {
 *     "6": [
 *       "exactmod:item_id",
 *       "*:copper_pickaxe",
 *       "#c:tools/pickaxes",
 *       {"keyword": "copper", "tag": "c:tools/pickaxes"}
 *     ]
 *   }
 * }
 * </pre>
 * <p>
 * The {@code ignore_datapacks} field, when true, makes that config file's
 * loaded entries replace any datapack-contributed entries (instead of merging).
 * Defaults to false (merge mode).
 */
public final class TierConfig {

    /** Tier -> entries (tool), merged from config files + datapacks. */
    private static final Map<Integer, List<TierEntry>> TOOL_ENTRIES = new TreeMap<>();
    /** Tier -> entries (block), merged from config files + datapacks. */
    private static final Map<Integer, List<TierEntry>> BLOCK_ENTRIES = new TreeMap<>();

    /** Tier -> entries (tool), from datapack only. Set by TierDataLoader. */
    private static final Map<Integer, List<TierEntry>> DATAPACK_TOOL_ENTRIES = new TreeMap<>();
    /** Tier -> entries (block), from datapack only. Set by TierDataLoader. */
    private static final Map<Integer, List<TierEntry>> DATAPACK_BLOCK_ENTRIES = new TreeMap<>();

    /** Tier -> entries (tool), from config file only. */
    private static final Map<Integer, List<TierEntry>> CONFIG_TOOL_ENTRIES = new TreeMap<>();
    /** Tier -> entries (block), from config file only. */
    private static final Map<Integer, List<TierEntry>> CONFIG_BLOCK_ENTRIES = new TreeMap<>();

    private static boolean toolConfigIgnoresDatapacks = false;
    private static boolean blockConfigIgnoresDatapacks = false;

    private TierConfig() {}

    /** Load configs from disk. Called on mod init and on /reload. */
    public static void load() {
        loadConfigFile(ConfigPaths.blockTiers(), CONFIG_BLOCK_ENTRIES, "block", DEFAULT_BLOCK_CONFIG, true);
        loadConfigFile(ConfigPaths.toolTiers(),  CONFIG_TOOL_ENTRIES,  "tool",  DEFAULT_TOOL_CONFIG,  false);
        rebuildMerged();
    }

    /**
     * Set the datapack-loaded entries. Called by TierDataLoader after datapacks load.
     */
    public static void setDatapackEntries(Map<Integer, List<TierEntry>> blocks,
                                          Map<Integer, List<TierEntry>> tools) {
        DATAPACK_BLOCK_ENTRIES.clear();
        DATAPACK_BLOCK_ENTRIES.putAll(blocks);
        DATAPACK_TOOL_ENTRIES.clear();
        DATAPACK_TOOL_ENTRIES.putAll(tools);
        rebuildMerged();
    }

    /** Combine datapack and config sources into the merged maps the rest of the mod uses. */
    private static void rebuildMerged() {
        BLOCK_ENTRIES.clear();
        if (!blockConfigIgnoresDatapacks) {
            mergeIn(BLOCK_ENTRIES, DATAPACK_BLOCK_ENTRIES);
        }
        mergeIn(BLOCK_ENTRIES, CONFIG_BLOCK_ENTRIES);

        TOOL_ENTRIES.clear();
        if (!toolConfigIgnoresDatapacks) {
            mergeIn(TOOL_ENTRIES, DATAPACK_TOOL_ENTRIES);
        }
        mergeIn(TOOL_ENTRIES, CONFIG_TOOL_ENTRIES);
    }

    private static void mergeIn(Map<Integer, List<TierEntry>> target,
                                Map<Integer, List<TierEntry>> source) {
        for (Map.Entry<Integer, List<TierEntry>> e : source.entrySet()) {
            target.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).addAll(e.getValue());
        }
    }

    private static void loadConfigFile(Path path,
                                       Map<Integer, List<TierEntry>> target,
                                       String label,
                                       String defaultContent,
                                       boolean isBlockConfig) {
        target.clear();

        if (!Files.exists(path)) {
            PhatsProgression.LOGGER.info("No {}_tiers.json found, generating defaults at {}",
                    label, path);
            writeDefault(path, defaultContent);
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                PhatsProgression.LOGGER.error("{}_tiers.json is not a JSON object", label);
                return;
            }
            JsonObject obj = root.getAsJsonObject();
            boolean ignoreDatapacks = obj.has("ignore_datapacks")
                    && obj.get("ignore_datapacks").isJsonPrimitive()
                    && obj.get("ignore_datapacks").getAsBoolean();
            if (isBlockConfig) {
                blockConfigIgnoresDatapacks = ignoreDatapacks;
            } else {
                toolConfigIgnoresDatapacks = ignoreDatapacks;
            }
            ConfigParser.parseInto(obj, target, label);
            PhatsProgression.LOGGER.info("Loaded {} tier assignments from {} (ignore_datapacks={})",
                    label, path, ignoreDatapacks);
        } catch (IOException e) {
            PhatsProgression.LOGGER.error("Failed to read {}_tiers.json: {}", label, e.getMessage());
        }
    }

    private static void writeDefault(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                w.write(content);
            }
        } catch (IOException e) {
            PhatsProgression.LOGGER.error("Failed to write default config: {}", e.getMessage());
        }
    }

    // --- Accessors ---

    public static Map<Integer, List<TierEntry>> toolEntries()  { return TOOL_ENTRIES;  }
    public static Map<Integer, List<TierEntry>> blockEntries() { return BLOCK_ENTRIES; }

    // --- Default file contents ---

    private static final String DEFAULT_TOOL_CONFIG = """
            {
              "_comment": "Tool tier assignments. Tools can mine blocks at or below their tier. ignore_datapacks=true makes this config skip merging with datapack-shipped tier definitions.",
              "ignore_datapacks": false,
              "tiers": {
                "1": [
                  "minecraft:wooden_sword", "minecraft:wooden_pickaxe", "minecraft:wooden_axe", "minecraft:wooden_shovel", "minecraft:wooden_hoe",
                  "*:wooden_sword", "*:wooden_pickaxe", "*:wooden_axe", "*:wooden_shovel", "*:wooden_hoe",
                  "*:wood_sword",   "*:wood_pickaxe",   "*:wood_axe",   "*:wood_shovel",   "*:wood_hoe"
                ],
                "2": [
                  "minecraft:stone_sword", "minecraft:stone_pickaxe", "minecraft:stone_axe", "minecraft:stone_shovel", "minecraft:stone_hoe",
                  "*:stone_sword", "*:stone_pickaxe", "*:stone_axe", "*:stone_shovel", "*:stone_hoe"
                ],
                "3": [
                  "minecraft:copper_sword", "minecraft:copper_pickaxe", "minecraft:copper_axe", "minecraft:copper_shovel", "minecraft:copper_hoe",
                  "*:copper_sword", "*:copper_pickaxe", "*:copper_axe", "*:copper_shovel", "*:copper_hoe"
                ],
                "4": [
                  "minecraft:iron_sword", "minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:iron_shovel", "minecraft:iron_hoe",
                  "*:iron_sword", "*:iron_pickaxe", "*:iron_axe", "*:iron_shovel", "*:iron_hoe",
                  "minecraft:golden_sword", "minecraft:golden_pickaxe", "minecraft:golden_axe", "minecraft:golden_shovel", "minecraft:golden_hoe",
                  "*:golden_sword", "*:golden_pickaxe", "*:golden_axe", "*:golden_shovel", "*:golden_hoe",
                  "*:gold_sword",   "*:gold_pickaxe",   "*:gold_axe",   "*:gold_shovel",   "*:gold_hoe"
                ],
                "5": [
                  "minecraft:diamond_sword", "minecraft:diamond_pickaxe", "minecraft:diamond_axe", "minecraft:diamond_shovel", "minecraft:diamond_hoe",
                  "*:diamond_sword", "*:diamond_pickaxe", "*:diamond_axe", "*:diamond_shovel", "*:diamond_hoe"
                ],
                "6": [
                  "minecraft:netherite_sword", "minecraft:netherite_pickaxe", "minecraft:netherite_axe", "minecraft:netherite_shovel", "minecraft:netherite_hoe",
                  "*:netherite_sword", "*:netherite_pickaxe", "*:netherite_axe", "*:netherite_shovel", "*:netherite_hoe"
                ]
              }
            }
            """;

    private static final String DEFAULT_BLOCK_CONFIG = """
            {
              "_comment": "Block tier assignments. A block requires a tool at or above this tier. ignore_datapacks=true makes this config skip merging with datapack-shipped tier definitions.",
              "ignore_datapacks": false,
              "tiers": {
                "2": [
                  "#minecraft:needs_stone_tool"
                ],
                "3": [
                  "minecraft:iron_ore",
                  "minecraft:deepslate_iron_ore"
                ],
                "4": [
                  "#minecraft:needs_iron_tool",
                  "minecraft:nether_gold_ore"
                ],
                "5": [
                  "#minecraft:needs_diamond_tool"
                ]
              }
            }
            """;
}