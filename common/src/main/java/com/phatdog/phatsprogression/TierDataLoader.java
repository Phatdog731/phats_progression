package com.phatdog.phatsprogression;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
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
 * 2. Re-reads the user config files (so /reload also picks up config edits)
 * 3. Pushes the merged datapack data to {@link TierConfig}
 */
public final class TierDataLoader extends SimpleJsonResourceReloadListener {

    /** Subdirectory under each datapack namespace for our data. */
    private static final String BLOCK_DIRECTORY = "phats_progression/block_tiers";
    private static final String TOOL_DIRECTORY  = "phats_progression/tool_tiers";

    private final boolean isBlockListener;

    private TierDataLoader(String directory, boolean isBlockListener) {
        super(new Gson(), directory);
        this.isBlockListener = isBlockListener;
    }

    /** Register both block and tool data loaders with Architectury. */
    public static void registerAll() {
        // Architectury's reload listener registry is the cross-loader API.
        // We use it because it works on both Fabric and NeoForge identically.
        dev.architectury.registry.ReloadListenerRegistry.register(
                net.minecraft.server.packs.PackType.SERVER_DATA,
                new TierDataLoader(BLOCK_DIRECTORY, true));
        dev.architectury.registry.ReloadListenerRegistry.register(
                net.minecraft.server.packs.PackType.SERVER_DATA,
                new TierDataLoader(TOOL_DIRECTORY, false));
    }

    /**
     * State holder so block and tool data both contribute before we push to TierConfig.
     */
    private static Map<Integer, List<TierEntry>> pendingBlocks = new TreeMap<>();
    private static Map<Integer, List<TierEntry>> pendingTools  = new TreeMap<>();

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager manager,
                         ProfilerFiller profiler) {
        Map<Integer, List<TierEntry>> merged = new TreeMap<>();
        String label = isBlockListener ? "datapack-block" : "datapack-tool";

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
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

        // Reload user config files too (handles /reload picking up config edits)
        // and push the merged result.
        TierConfig.load();
        TierConfig.setDatapackEntries(pendingBlocks, pendingTools);
    }
}