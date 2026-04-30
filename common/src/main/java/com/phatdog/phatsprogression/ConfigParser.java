package com.phatdog.phatsprogression;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses JSON tier-bucket structures into {@link TierEntry} maps. Used by both
 * config file loading and datapack loading.
 */
public final class ConfigParser {

    private ConfigParser() {}

    /**
     * Parse a JSON object with a {@code tiers} field into the target map.
     * Skips invalid tier keys and entries with warnings logged.
     */
    public static void parseInto(JsonObject root,
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
                PhatsProgression.LOGGER.warn("{}: invalid tier key '{}', skipping",
                        label, tierKey);
                continue;
            }
            if (!Tier.isValid(tier)) {
                PhatsProgression.LOGGER.warn("{}: tier {} out of range 0-{}, skipping",
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
        PhatsProgression.LOGGER.warn("{}: ignoring invalid entry: {}", label, el);
        return null;
    }
}