package com.phatdog.phatsprogression.neoforge;

import net.neoforged.fml.common.Mod;

import com.phatdog.phatsprogression.PhatsProgression;

@Mod(PhatsProgression.MOD_ID)
public final class PhatsProgressionNeoForge {
    public PhatsProgressionNeoForge() {
        // Run our common setup.
        PhatsProgression.init();
    }
}
