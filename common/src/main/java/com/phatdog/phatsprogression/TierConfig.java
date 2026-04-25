package com.phatdog.phatsprogression;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
 * Schema (both files):
 * <pre>
 * {
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
 */
public final class TierConfig {

    /** Tier -> entries (tool). */
    private static final Map<Integer, List<TierEntry>> TOOL_ENTRIES = new TreeMap<>();
    /** Tier -> entries (block). */
    private static final Map<Integer, List<TierEntry>> BLOCK_ENTRIES = new TreeMap<>();

    private TierConfig() {}

    public static void load() {
        TOOL_ENTRIES.clear();
        BLOCK_ENTRIES.clear();
        loadOrCreate(ConfigPaths.blockTiers(), BLOCK_ENTRIES, "block", DEFAULT_BLOCK_CONFIG);
        loadOrCreate(ConfigPaths.toolTiers(),  TOOL_ENTRIES,  "tool",  DEFAULT_TOOL_CONFIG);
    }

    private static void loadOrCreate(Path path,
                                     Map<Integer, List<TierEntry>> target,
                                     String label,
                                     String defaultContent) {
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
            parseConfig(root.getAsJsonObject(), target, label);
            PhatsProgression.LOGGER.info("Loaded {} tier assignments from {}", label, path);
        } catch (IOException e) {
            PhatsProgression.LOGGER.error("Failed to read {}_tiers.json: {}", label, e.getMessage());
        }
    }

    private static void parseConfig(JsonObject root,
                                    Map<Integer, List<TierEntry>> target,
                                    String label) {
        if (!root.has("tiers") || !root.get("tiers").isJsonObject()) return;
        JsonObject tiers = root.getAsJsonObject("tiers");

        for (Map.Entry<String, JsonElement> bucket : tiers.entrySet()) {
            String tierKey = bucket.getKey();
            if (tierKey.startsWith("_")) continue;

            int tier;
            try {
                tier = Integer.parseInt(tierKey);
            } catch (NumberFormatException e) {
                PhatsProgression.LOGGER.warn("{}_tiers.json: invalid tier key '{}', skipping",
                        label, tierKey);
                continue;
            }
            if (!Tier.isValid(tier)) {
                PhatsProgression.LOGGER.warn("{}_tiers.json: tier {} out of range 0-{}, skipping",
                        label, tier, Tier.MAX);
                continue;
            }

            if (!bucket.getValue().isJsonArray()) continue;
            List<TierEntry> entries = target.computeIfAbsent(tier, k -> new ArrayList<>());
            for (JsonElement el : bucket.getValue().getAsJsonArray()) {
                TierEntry entry = parseEntry(el, label);
                if (entry != null && entry.isValid()) entries.add(entry);
            }
        }
    }

    private static TierEntry parseEntry(JsonElement el, String label) {
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            String s = el.getAsString();
            if (s.startsWith("#")) {
                return new TierEntry(null, s.substring(1), null);
            }
            return new TierEntry(s, null, null);
        }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            String id = obj.has("id") && obj.get("id").isJsonPrimitive()
                    ? obj.get("id").getAsString() : null;
            String tag = obj.has("tag") && obj.get("tag").isJsonPrimitive()
                    ? obj.get("tag").getAsString() : null;
            String keyword = obj.has("keyword") && obj.get("keyword").isJsonPrimitive()
                    ? obj.get("keyword").getAsString() : null;
            if (tag != null && tag.startsWith("#")) tag = tag.substring(1);
            return new TierEntry(id, tag, keyword);
        }
        PhatsProgression.LOGGER.warn("{}_tiers.json: ignoring invalid entry: {}", label, el);
        return null;
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
              "_comment": "Tool tier assignments. Tools can mine blocks at or below their tier. Default progression: 1=wood, 2=stone, 3=copper, 4=iron/gold, 5=diamond, 6=netherite. Modpacks can extend up to tier 10.",
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
                  "*:copper_sword", "*:copper_pickaxe", "*:copper_axe", "*:copper_shovel", "*:copper_hoe",
                  {"keyword": "tin",    "tag": "c:tools/pickaxes"},
                  {"keyword": "bronze", "tag": "c:tools/pickaxes"},
                  {"keyword": "brass",  "tag": "c:tools/pickaxes"}
                ],
                "4": [
                  "minecraft:iron_sword", "minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:iron_shovel", "minecraft:iron_hoe",
                  "*:iron_sword", "*:iron_pickaxe", "*:iron_axe", "*:iron_shovel", "*:iron_hoe",
                  "minecraft:golden_sword", "minecraft:golden_pickaxe", "minecraft:golden_axe", "minecraft:golden_shovel", "minecraft:golden_hoe",
                  "*:golden_sword", "*:golden_pickaxe", "*:golden_axe", "*:golden_shovel", "*:golden_hoe",
                  "*:gold_sword",   "*:gold_pickaxe",   "*:gold_axe",   "*:gold_shovel",   "*:gold_hoe",
                  {"keyword": "steel",  "tag": "c:tools/pickaxes"},
                  {"keyword": "silver", "tag": "c:tools/pickaxes"}
                ],
                "5": [
                  "minecraft:diamond_sword", "minecraft:diamond_pickaxe", "minecraft:diamond_axe", "minecraft:diamond_shovel", "minecraft:diamond_hoe",
                  "*:diamond_sword", "*:diamond_pickaxe", "*:diamond_axe", "*:diamond_shovel", "*:diamond_hoe",
                  {"keyword": "mithril", "tag": "c:tools/pickaxes"}
                ],
                "6": [
                  "minecraft:netherite_sword", "minecraft:netherite_pickaxe", "minecraft:netherite_axe", "minecraft:netherite_shovel", "minecraft:netherite_hoe",
                  "*:netherite_sword", "*:netherite_pickaxe", "*:netherite_axe", "*:netherite_shovel", "*:netherite_hoe",
                  {"keyword": "adamantine", "tag": "c:tools/pickaxes"},
                  {"keyword": "black_steel","tag": "c:tools/pickaxes"}
                ]
              }
            }
            """;

    private static final String DEFAULT_BLOCK_CONFIG = """
            {
              "_comment": "Block tier assignments. A block requires a tool at or above this tier. Defaults gate iron ore behind copper tools, otherwise mirror vanilla progression bumped one tier higher.",
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