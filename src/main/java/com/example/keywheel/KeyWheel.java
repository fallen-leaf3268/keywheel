package com.example.keywheel;

import com.example.keywheel.config.KeyWheelConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(KeyWheel.MODID)
public class KeyWheel {
    public static final String MODID = "keywheel";
    public static final Logger LOG = LogUtils.getLogger();

    public KeyWheel() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.register(this);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, KeyWheelConfig.SPEC);

        LOG.info("KeyWheel booted");
    }
}
