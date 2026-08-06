package com.example.keywheel;

import com.example.keywheel.config.KeyWheelConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(KeyWheel.MODID)
@SuppressWarnings({"removal", "deprecation"})
public class KeyWheel {
    public static final String MODID = "keywheel";
    public static final Logger LOG = LogUtils.getLogger();

    public KeyWheel() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, KeyWheelConfig.SPEC);
        LOG.info("KeyWheel booted");
    }
}
