package com.example.keywheel.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public final class KeyWheelConfig {
    private KeyWheelConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue HELD_TICKS_THRESHOLD;
    public static final ForgeConfigSpec.IntValue DEAD_ZONE_PX;
    public static final ForgeConfigSpec.IntValue WHEEL_RADIUS_PX;
    public static final ForgeConfigSpec.BooleanValue SWAP_MODE;
    public static final ForgeConfigSpec.ConfigValue<List<String>> MEMBERS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        HELD_TICKS_THRESHOLD = b.defineInRange("held_ticks_threshold", 5, 1, 60);
        DEAD_ZONE_PX = b.defineInRange("dead_zone_radius_px", 24, 0, 256);
        WHEEL_RADIUS_PX = b.defineInRange("wheel_radius_px", 96, 32, 512);
        SWAP_MODE = b.define("swap_mode", false);
        b.pop();

        b.push("members");
        MEMBERS = (ForgeConfigSpec.ConfigValue<List<String>>)
                (ForgeConfigSpec.ConfigValue<?>) b.defineListAllowEmpty(
                        "members",
                        () -> {
                            List<String> empty = new ArrayList<>();
                            return empty;
                        },
                        s -> s instanceof String
                );
        b.pop();

        SPEC = b.build();
    }
}
