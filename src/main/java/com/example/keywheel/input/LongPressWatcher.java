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
    private static Screen previousScreen = null;
    private static InputConstants.Key skipReleaseUntil = null;
    private static final WheelInputTracker INPUTS = new WheelInputTracker();
    private static final Set<InputConstants.Key> ignoredUntilRelease = new HashSet<>();

    public static void suppressUntilRelease() {
        suppressUntilRelease = true;
        cancelInputs();
    }

    public static void invalidateMemberCache() {
        cachedIds = new ArrayList<>();
        cacheStamp = 0L;
    }

    public static void clearSkipReleaseUntil() {
        skipReleaseUntil = null;
    }

    public static boolean recordPhysicalInput(InputConstants.Key key, int action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            cancelInputs();
            previousScreen = mc.screen;
            if (action == GLFW.GLFW_PRESS && WheelConflictIndex.wheelKeys().contains(key)) {
                ignoredUntilRelease.add(key);
                skipReleaseUntil = key;
            }
        }
        if (action == GLFW.GLFW_RELEASE) {
            INPUTS.release(key);
            ignoredUntilRelease.remove(key);
            if (key.equals(skipReleaseUntil)) skipReleaseUntil = null;
            if (suppressUntilRelease
                    && pickFirstWheelKeyPressed(mc, WheelConflictIndex.wheelKeys()) == null) {
                suppressUntilRelease = false;
            }
            return false;
        }
        if (mc.screen != null || mc.player == null || !PhysicalKeyState.isSupported(key)
                || !WheelConflictIndex.wheelKeys().contains(key)) return false;
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) return false;
        if (suppressUntilRelease || ignoredUntilRelease.contains(key)
                || key.equals(skipReleaseUntil)) return true;
        if (action == GLFW.GLFW_PRESS) {
            HeldKeyState state = INPUTS.press(key);
            if (state != null) categorizeMappings(state, key);
        }
        return true;
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
            cancelInputs();
            ignoredUntilRelease.clear();
            skipReleaseUntil = null;
            suppressUntilRelease = false;
            previousScreen = currentScreen;
            return;
        }

        int threshold = KeyWheelConfig.HELD_TICKS_THRESHOLD.get();
        Set<InputConstants.Key> wheelKeys = WheelConflictIndex.wheelKeys();

        if (currentScreen instanceof WheelScreen ws) {
            cancelInputs();
            if (ws.tickSelectOnRelease()) {
                ws.onClose();
            }
            previousScreen = currentScreen;
            return;
        }

        if (currentScreen != null) {
            cancelInputs();
            previousScreen = currentScreen;
            return;
        }

        if (wheelKeys.isEmpty()) {
            cancelInputs();
            previousScreen = currentScreen;
            return;
        }

        if (suppressUntilRelease) {
            if (pickFirstWheelKeyPressed(mc, wheelKeys) == null) {
                suppressUntilRelease = false;
            }
            cancelInputs();
            previousScreen = currentScreen;
            return;
        }

        long window = mc.getWindow().getWindow();
        ignoredUntilRelease.removeIf(key -> !PhysicalKeyState.isPressed(window, key));
        if (skipReleaseUntil != null && !PhysicalKeyState.isPressed(window, skipReleaseUntil)) {
            skipReleaseUntil = null;
        }
        if (previousScreen != null) {
            Set<InputConstants.Key> recorded = INPUTS.pressedKeys();
            for (InputConstants.Key key : wheelKeys) {
                if (!recorded.contains(key) && PhysicalKeyState.isPressed(window, key)) {
                    ignoredUntilRelease.add(key);
                }
            }
        }
        previousScreen = null;
        for (HeldKeyState state : INPUTS.heldStates()) {
            if (!wheelKeys.contains(state.physicalKey)) INPUTS.discard(state.physicalKey);
            else if (!PhysicalKeyState.isPressed(window, state.physicalKey)) INPUTS.release(state.physicalKey);
        }
        for (HeldKeyState state : INPUTS.drainReleased()) {
            if (state.thresholdReached || !wheelKeys.contains(state.physicalKey)
                    || ignoredUntilRelease.contains(state.physicalKey)) continue;
            KeyMapping primary = consumePrimary(state.nonMemberTargets, state.physicalKey);
            if (primary != null && primary.getKey().equals(state.physicalKey)) ActionExecutor.run(primary);
            if (mc.screen != null) {
                cancelInputs();
                previousScreen = mc.screen;
                return;
            }
        }
        List<HeldKeyState> heldStates = INPUTS.heldStates();
        for (HeldKeyState state : heldStates) {
            state.ticksHeld++;
            if (state.ticksHeld >= threshold && !state.thresholdReached) {
                state.thresholdReached = true;
                if (!state.memberTargets.isEmpty()) {
                    STATE.copyFrom(state);
                    openWheelFor(mc, state.memberTargets);
                    ignoredUntilRelease.addAll(INPUTS.pressedKeys());
                    INPUTS.cancel();
                    previousScreen = mc.screen;
                    return;
                }
            }
        }
        STATE.copyFrom(heldStates.isEmpty() ? null : heldStates.get(0));
    }

    private static void cancelInputs() {
        ignoredUntilRelease.addAll(INPUTS.pressedKeys());
        INPUTS.cancel();
        STATE.reset();
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

    private static void categorizeMappings(HeldKeyState state, InputConstants.Key key) {
        state.memberTargets.clear();
        state.nonMemberTargets.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;

        List<String> enabledIds = currentEnabledIds();
        Set<String> enabledSet = enabledIds.isEmpty() ? Set.of() : new HashSet<>(enabledIds);

        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getKey().equals(key)) {
                if (enabledSet.contains(km.getName())) {
                    state.memberTargets.add(km);
                } else {
                    state.nonMemberTargets.add(km);
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
        List<String> stored = KeyWheelConfig.currentMembers();
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
