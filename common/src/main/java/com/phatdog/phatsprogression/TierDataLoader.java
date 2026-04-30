package com.phatdog.phatsprogression;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Datapack reload listener that loads tier definitions from
 * {@code data/<namespace>/phats_progression/block_tiers/*.json} and
 * {@code data/<namespace>/phats_progression/tool_tiers/*.json}.
 * <p>
 * On every datapack reload (game start, /reload), this listener:
 * 1. Re-reads all matching JSON files across all loaded datapacks
 * 2. Re-reads the user config files
 * 3. Pushes the merged datapack data to {@link TierConfig}
 * <p>
 * Implementation note: 1.21.11 changed {@code SimpleJsonResourceReloadListener}
 * to require a Codec and a typed schema. To preserve our hybrid string-or-object
 * schema, we extend {@link SimplePreparableReloadListener} directly and parse
 * JsonElements ourselves via {@link ConfigParser}.
 */
public final class TierDataLoader extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {

    private static final String BLOCK_DIRECTORY = "phats_progression/block_tiers";
    private static final String TOOL_DIRECTORY  = "phats_progression/tool_tiers";

    private final String directory;
    private final boolean isBlockListener;

    private TierDataLoader(String directory, boolean isBlockListener) {
        this.directory = directory;
        this.isBlockListener = isBlockListener;
    }

    public static void registerAll() {
        dev.architectury.registry.ReloadListenerRegistry.register(
                net.minecraft.server.packs.PackType.SERVER_DATA,
                new TierDataLoader(BLOCK_DIRECTORY, true),
                Identifier.fromNamespaceAndPath(PhatsProgression.MOD_ID, "block_tiers_loader"));
        dev.architectury.registry.ReloadListenerRegistry.register(
                net.minecraft.server.packs.PackType.SERVER_DATA,
                new TierDataLoader(TOOL_DIRECTORY, false),
                Identifier.fromNamespaceAndPath(PhatsProgression.MOD_ID, "tool_tiers_loader"));
    }

    private static Map<Integer, List<TierEntry>> pendingBlocks = new TreeMap<>();
    private static Map<Integer, List<TierEntry>> pendingTools  = new TreeMap<>();

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager,
                                                   ProfilerFiller profiler) {
        Map<Identifier, JsonElement> out = new HashMap<>();
        // listResources returns files under the directory whose path ends with .json
        // The Identifier path returned has the leading "<directory>/" included.
        manager.listResources(directory, id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                        JsonElement el = JsonParser.parseReader(reader);
                        out.put(id, el);
                    } catch (IOException | RuntimeException e) {
                        PhatsProgression.LOGGER.warn("Failed to read tier file {}: {}",
                                id, e.getMessage());
                    }
                });
        return out;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> resources,
                         ResourceManager manager,
                         ProfilerFiller profiler) {
        Map<Integer, List<TierEntry>> merged = new TreeMap<>();
        String label = isBlockListener ? "datapack-block" : "datapack-tool";

        for (Map.Entry<Identifier, JsonElement> entry : resources.entrySet()) {
            JsonElement el = entry.getValue();
            if (!el.isJsonObject()) {
                PhatsProgression.LOGGER.warn("{}: file {} is not a JSON object",
                        label, entry.getKey());
                continue;
            }
            ConfigParser.parseInto(el.getAsJsonObject(), merged, label + ":" + entry.getKey());
        }

        if (isBlockListener) {
            pendingBlocks = merged;
            PhatsProgression.LOGGER.info("Loaded {} block tier files from datapacks",
                    resources.size());
        } else {
            pendingTools = merged;
            PhatsProgression.LOGGER.info("Loaded {} tool tier files from datapacks",
                    resources.size());
        }

        TierConfig.load();
        TierConfig.setDatapackEntries(pendingBlocks, pendingTools);
    }
}