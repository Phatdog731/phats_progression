package com.phatdog.phatsprogression;

/**
 * Mining tier constants.
 * <p>
 * Tiers are integers from {@link #MIN} to {@link #MAX}. They have no inherent
 * material meaning — assignments are entirely config-driven. The mod ships with
 * defaults that match vanilla progression (wood=1, stone=2, iron=3, diamond=4,
 * netherite=5) but modpacks can reassign freely, including inserting new tiers
 * between vanilla ones.
 */
public final class Tier {

    /** No tool required (dirt, wool, etc). */
    public static final int HAND = 0;

    /** Minimum valid tier. */
    public static final int MIN = 0;
    /** Maximum valid tier. */
    public static final int MAX = 10;

    private Tier() {}

    /** Returns true if the given int is a valid tier (0-10). */
    public static boolean isValid(int tier) {
        return tier >= MIN && tier <= MAX;
    }
}