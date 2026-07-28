package com.example.keywheel.registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class KeyWheelKeyMappings {
    private KeyWheelKeyMappings() {}

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.keywheel.open_settings",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.keywheel"
    );

    public static final KeyMapping DEMO_WHEEL = new KeyMapping(
            "key.keywheel.demo_wheel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.keywheel"
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
        event.register(DEMO_WHEEL);
    }
}
