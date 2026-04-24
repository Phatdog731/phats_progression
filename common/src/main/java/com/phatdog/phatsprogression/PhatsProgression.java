package com.phatdog.phatsprogression;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class PhatsProgression {

    public static final String MOD_ID = "phats_progression";
    public static final Logger LOGGER = LogUtils.getLogger();

    private PhatsProgression() {}

    public static void init() {
        LOGGER.info("Initializing Phat's Progression Framework");
        TierConfig.load();
    }
}