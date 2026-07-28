package com.example.keywheel.input;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.screen.WheelConflictIndex;
import com.example.keywheel.screen.WheelScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LongPressWatcher {
    public static final HeldKeyState STATE = new HeldKeyState();

    private static List<String> cachedIds = new ArrayList<>();
    private static long cacheStamp = 0L;
    private static boolean suppressUntilRelease = false;

    public static void suppressUntilRelease() {
        suppressUntilRelease = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        ActionExecutor.flushSetDown();

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int threshold = KeyWheelConfig.HELD_TICKS_THRESHOLD.get();
        Set<InputConstants.Key> wheelKeys = WheelConflictIndex.wheelKeys();

        if (mc.screen instanceof WheelScreen ws) {
            if (ws.tickSelectOnRelease()) {
                ws.onClose();
            }
            return;
        }

        if (wheelKeys.isEmpty()) {
            STATE.reset();
            return;
        }

        if (suppressUntilRelease) {
            if (pickFirstWheelKeyPressed(mc, wheelKeys) == null) {
                suppressUntilRelease = false;
            }
            STATE.reset();
            return;
        }

        InputConstants.Key pressedKey = pickFirstWheelKeyPressed(mc, wheelKeys);

        if (pressedKey == null) {
            if (STATE.isActive() && !STATE.thresholdReached) {
                if (!STATE.nonMemberTargets.isEmpty()) {
                    ActionExecutor.runBatch(STATE.nonMemberTargets);
                }
            }
            STATE.reset();
            return;
        }

        if (!pressedKey.equals(STATE.physicalKey)) {
            STATE.physicalKey = pressedKey;
            STATE.ticksHeld = 0;
            STATE.thresholdReached = false;
            categorizeMappings(pressedKey);
            // wheel key detected
        }

        STATE.ticksHeld++;

        if (STATE.ticksHeld == threshold && !STATE.thresholdReached) {
            STATE.thresholdReached = true;
            if (!STATE.memberTargets.isEmpty()) {
                openWheelFor(mc, STATE.memberTargets);
            }
        }
    }

    private static InputConstants.Key pickFirstWheelKeyPressed(
            Minecraft mc, Set<InputConstants.Key> wheelKeys) {
        if (wheelKeys.isEmpty()) return null;
        long window = mc.getWindow().getWindow();
        for (InputConstants.Key key : wheelKeys) {
            if (key.getType() == InputConstants.Type.KEYSYM) {
                if (GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS) {
                    return key;
                }
            }
        }
        return null;
    }

    private static void categorizeMappings(InputConstants.Key key) {
        STATE.memberTargets.clear();
        STATE.nonMemberTargets.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;

        List<String> enabledIds = currentEnabledIds();
        Set<String> enabledSet = enabledIds.isEmpty() ? Set.of() : new HashSet<>(enabledIds);

        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getKey().equals(key)) {
                if (enabledSet.contains(km.getName())) {
                    STATE.memberTargets.add(km);
                } else {
                    STATE.nonMemberTargets.add(km);
                }
            }
        }
    }

    private static void openWheelFor(Minecraft mc, List<KeyMapping> members) {
        Screen prev = mc.screen;
        mc.setScreen(new WheelScreen(new ArrayList<>(members), prev));
    }

    private static List<String> currentEnabledIds() {
        long now = System.currentTimeMillis();
        if (now - cacheStamp < 1000L && !cachedIds.isEmpty() || (now - cacheStamp < 250L)) {
            if (now - cacheStamp < 500L) return cachedIds;
        }
        List<String> out = new ArrayList<>();
        List<String> stored = KeyWheelConfig.MEMBERS.get();
        if (stored != null) {
            for (String id : stored) {
                if (id != null) out.add(id);
            }
        }
        cachedIds = out;
        cacheStamp = now;
        return out;
    }
}
