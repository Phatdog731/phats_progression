package com.phatdog.phatsprogression;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Resolves the tier of items and blocks based on config assignments.
 * <p>
 * On 1.21.11+, vanilla removed the Tier/Tiers system in favor of data components.
 * As a result, this branch does NOT have the vanilla tool tier fallback that
 * the main (1.21.1) branch has. Tools without an explicit config entry will
 * return null and our mixin will defer to vanilla behavior.
 * <p>
 * Modpack developers should ensure their custom tools are listed in the
 * tool_tiers.json config or are tagged with c:tools/* and matched via
 * keyword+tag entries.
 */
public final class TierResolver {

    private TierResolver() {}

    @Nullable
    public static Integer tierOfTool(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        String itemIdStr = itemId.toString();
        String pathOnly  = itemId.getPath();

        Integer best = null;
        for (Map.Entry<Integer, List<TierEntry>> tierBucket : TierConfig.toolEntries().entrySet()) {
            int tier = tierBucket.getKey();
            if (best != null && tier <= best) continue;
            for (TierEntry entry : tierBucket.getValue()) {
                if (entryMatchesTool(entry, stack, itemIdStr, pathOnly)) {
                    best = tier;
                    break;
                }
            }
        }
        return best;
    }

    @Nullable
    public static Integer tierOfBlock(BlockState state) {
        Block block = state.getBlock();
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        String blockIdStr = blockId.toString();
        String pathOnly   = blockId.getPath();

        Integer best = null;
        for (Map.Entry<Integer, List<TierEntry>> tierBucket : TierConfig.blockEntries().entrySet()) {
            int tier = tierBucket.getKey();
            if (best != null && tier <= best) continue;
            for (TierEntry entry : tierBucket.getValue()) {
                if (entryMatchesBlock(entry, state, blockIdStr, pathOnly)) {
                    best = tier;
                    break;
                }
            }
        }
        return best;
    }

    // --- Matching helpers ---

    private static boolean entryMatchesTool(TierEntry entry, ItemStack stack,
                                            String itemIdStr, String pathOnly) {
        if (entry.idPattern() != null && !GlobMatcher.matches(entry.idPattern(), itemIdStr)) {
            return false;
        }
        if (entry.tagId() != null) {
            TagKey<Item> tagKey = safeItemTag(entry.tagId());
            if (tagKey == null || !stack.is(tagKey)) return false;
        }
        if (entry.keyword() != null && !pathOnly.contains(entry.keyword())) {
            return false;
        }
        return true;
    }

    private static boolean entryMatchesBlock(TierEntry entry, BlockState state,
                                             String blockIdStr, String pathOnly) {
        if (entry.idPattern() != null && !GlobMatcher.matches(entry.idPattern(), blockIdStr)) {
            return false;
        }
        if (entry.tagId() != null) {
            TagKey<Block> tagKey = safeBlockTag(entry.tagId());
            if (tagKey == null || !state.is(tagKey)) return false;
        }
        if (entry.keyword() != null && !pathOnly.contains(entry.keyword())) {
            return false;
        }
        return true;
    }

    @Nullable
    private static TagKey<Item> safeItemTag(String tagId) {
        try {
            return TagKey.create(BuiltInRegistries.ITEM.key(), Identifier.parse(tagId));
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static TagKey<Block> safeBlockTag(String tagId) {
        try {
            return TagKey.create(BuiltInRegistries.BLOCK.key(), Identifier.parse(tagId));
        } catch (Exception e) {
            return null;
        }
    }
}