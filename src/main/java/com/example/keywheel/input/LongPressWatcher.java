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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LongPressWatcher {
    public static final HeldKeyState STATE = new HeldKeyState();

    private static List<String> cachedIds = new ArrayList<>();
    private static long cacheStamp = 0L;
    private static boolean suppressUntilRelease = false;
    private static Screen previousScreen = null;
    private static InputConstants.Key skipReleaseUntil = null;

    public static void suppressUntilRelease() {
        suppressUntilRelease = true;
    }

    public static void invalidateMemberCache() {
        cachedIds = new ArrayList<>();
        cacheStamp = 0L;
    }

    public static void clearSkipReleaseUntil() {
        skipReleaseUntil = null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        WheelConflictIndex.flushDirty();
        ActionExecutor.flushSetDown();

        var mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        if (mc.player == null) {
            ActionExecutor.releaseHeld();
            STATE.reset();
            previousScreen = currentScreen;
            return;
        }

        int threshold = KeyWheelConfig.HELD_TICKS_THRESHOLD.get();
        Set<InputConstants.Key> wheelKeys = WheelConflictIndex.wheelKeys();

        if (currentScreen instanceof WheelScreen ws) {
            if (ws.tickSelectOnRelease()) {
                ws.onClose();
            }
            previousScreen = currentScreen;
            return;
        }

        if (currentScreen != null) {
            previousScreen = currentScreen;
            return;
        }

        if (wheelKeys.isEmpty()) {
            STATE.reset();
            previousScreen = currentScreen;
            return;
        }

        if (suppressUntilRelease) {
            if (pickFirstWheelKeyPressed(mc, wheelKeys) == null) {
                suppressUntilRelease = false;
            }
            STATE.reset();
            previousScreen = currentScreen;
            return;
        }

        InputConstants.Key pressedKey = pickFirstWheelKeyPressed(mc, wheelKeys);
        boolean justClosedScreen = previousScreen != null;

        if (pressedKey == null) {
            if (STATE.isActive()) {
                if (STATE.thresholdReached) {
                } else if (skipReleaseUntil != null
                        && STATE.physicalKey != null
                        && STATE.physicalKey.equals(skipReleaseUntil)) {
                } else if (!STATE.nonMemberTargets.isEmpty()) {
                    KeyMapping primary = consumePrimary(STATE.nonMemberTargets, STATE.physicalKey);
                    if (primary != null) {
                        ActionExecutor.run(primary);
                    }
                }
            }
            skipReleaseUntil = null;
            STATE.reset();
            previousScreen = currentScreen;
            return;
        }

        if (justClosedScreen) {
            InputConstants.Key realPressed = pickFirstWheelKeyPressed(mc, wheelKeys);
            if (realPressed != null) {
                skipReleaseUntil = realPressed;
            }
            STATE.reset();
            previousScreen = currentScreen;
            return;
        }

        if (skipReleaseUntil != null && pressedKey.equals(skipReleaseUntil)) {
            previousScreen = currentScreen;
            return;
        }
        if (skipReleaseUntil != null && !pressedKey.equals(skipReleaseUntil)) {
            skipReleaseUntil = null;
        }

        if (!pressedKey.equals(STATE.physicalKey)) {
            STATE.physicalKey = pressedKey;
            STATE.ticksHeld = 0;
            STATE.thresholdReached = false;
            categorizeMappings(pressedKey);
        }

        STATE.ticksHeld++;

        if (STATE.ticksHeld == threshold && !STATE.thresholdReached) {
            STATE.thresholdReached = true;
            if (!STATE.memberTargets.isEmpty()) {
                openWheelFor(mc, STATE.memberTargets);
            }
        }

        previousScreen = currentScreen;
    }

    private static KeyMapping consumePrimary(List<KeyMapping> nonMembers, InputConstants.Key physicalKey) {
        if (nonMembers == null || nonMembers.isEmpty()) return null;
        if (physicalKey == null) return nonMembers.get(0);
        String physicalId = physicalKey.getName();
        String configured = KeyWheelConfig.getSwapPrimary(physicalId);
        KeyMapping picked = null;
        if (configured != null) {
            for (KeyMapping km : nonMembers) {
                if (configured.equals(km.getName())) {
                    picked = km;
                    break;
                }
            }
            KeyWheelConfig.setSwapPrimary(physicalId, null);
        }
        if (picked == null) picked = nonMembers.get(0);
        return picked;
    }

    private static InputConstants.Key pickFirstWheelKeyPressed(
            Minecraft mc, Set<InputConstants.Key> wheelKeys) {
        if (wheelKeys.isEmpty()) return null;
        long window = mc.getWindow().getWindow();
        for (InputConstants.Key key : wheelKeys) {
            if (PhysicalKeyState.isPressed(window, key)) return key;
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
        if (now - cacheStamp < 1000L) {
            return cachedIds;
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
