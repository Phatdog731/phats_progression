package com.phatdog.phatsprogression;

import org.jetbrains.annotations.Nullable;

/**
 * A single entry in a tier bucket. Can match by item/block ID (with optional
 * globs), by tag, by keyword, or any combination (logical AND).
 */
public record TierEntry(
        @Nullable String idPattern,   // null means "don't check ID"
        @Nullable String tagId,       // null means "don't check tag"
        @Nullable String keyword      // null means "don't check keyword"
) {
    /** True if at least one of the fields is non-null (entry has some matcher). */
    public boolean isValid() {
        return idPattern != null || tagId != null || keyword != null;
    }
}