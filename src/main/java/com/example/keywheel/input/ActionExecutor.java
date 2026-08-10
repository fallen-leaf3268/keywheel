package com.example.keywheel.input;

import com.example.keywheel.mixin.KeyMappingClickCountAccessor;
import com.example.keywheel.config.KeyWheelConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ActionExecutor {
    private ActionExecutor() {}

    private static final List<KeyMapping> pendingSetDownFalse = new ArrayList<>();
    private static final Set<KeyMapping> heldMappings = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<KeyMapping, InputConstants.Key> pendingReplayReleases = new IdentityHashMap<>();
    private static final Map<KeyMapping, InputConstants.Key> heldReplayKeys = new IdentityHashMap<>();

    public static void run(KeyMapping target) {
        if (target == null) return;
        executeOneShot(List.of(target));
    }

    public static void runWheelAction(KeyMapping target) {
        if (target == null) return;
        InputConstants.Key key = target.getKey();
        boolean hold = KeyWheelConfig.isHoldEnabled(target.getName());
        Minecraft mc = Minecraft.getInstance();
        if (!shouldQueueReplay(SyntheticInputReplayer.supports(key), mc != null)) {
            execute(List.of(target), hold);
            return;
        }
        mc.tell(() -> replayWheelAction(mc, target, key, hold));
    }

    static boolean shouldQueueReplay(boolean supported, boolean clientReady) {
        return supported && clientReady;
    }

    private static void replayWheelAction(
            Minecraft mc, KeyMapping target, InputConstants.Key key, boolean hold) {
        if (!isCurrentMapping(mc, target, key)) return;
        if (!SyntheticInputReplayer.replay(target, key, GLFW.GLFW_PRESS)) {
            execute(List.of(target), hold);
            return;
        }
        synchronized (pendingSetDownFalse) {
            if (hold) {
                pendingReplayReleases.remove(target);
                heldReplayKeys.put(target, key);
            } else {
                heldReplayKeys.remove(target);
                pendingReplayReleases.put(target, key);
            }
        }
    }

    private static boolean isCurrentMapping(
            Minecraft mc, KeyMapping target, InputConstants.Key capturedKey) {
        if (mc.options == null || !target.getKey().equals(capturedKey)) return false;
        for (KeyMapping mapping : mc.options.keyMappings) {
            if (mapping == target) return true;
        }
        return false;
    }

    public static void runBatch(List<KeyMapping> targets) {
        if (targets == null || targets.isEmpty()) return;
        executeOneShot(targets);
    }

    static boolean shouldReplayShortPress(boolean supported, boolean clientReady) {
        return supported && clientReady;
    }

    private static void executeOneShot(List<KeyMapping> targets) {
        Minecraft mc = Minecraft.getInstance();
        for (KeyMapping target : targets) {
            if (target == null) continue;
            InputConstants.Key key = target.getKey();
            if (!shouldReplayShortPress(SyntheticInputReplayer.supports(key), mc != null)) {
                execute(List.of(target), false);
                continue;
            }
            if (!isCurrentMapping(mc, target, key)) continue;
            if (!SyntheticInputReplayer.replay(target, key, GLFW.GLFW_PRESS)) {
                execute(List.of(target), false);
                continue;
            }
            synchronized (pendingSetDownFalse) {
                pendingSetDownFalse.removeIf(mapping -> mapping == target);
                heldMappings.remove(target);
                heldReplayKeys.remove(target);
                pendingReplayReleases.put(target, key);
            }
        }
    }

    private static void execute(List<KeyMapping> targets, boolean hold) {
        List<KeyMapping> activated = new ArrayList<>(targets.size());
        try {
            for (KeyMapping target : targets) {
                WheelActionBridge.addForceAllow(target);
            }
            for (KeyMapping target : targets) {
                ((KeyMappingClickCountAccessor)(Object) target).keywheel$setClickCount(1);
                target.setDown(true);
                activated.add(target);
            }
        } finally {
            WheelActionBridge.clearForceAllow();
            synchronized (pendingSetDownFalse) {
                for (KeyMapping target : activated) {
                    if (hold) {
                        pendingSetDownFalse.removeIf(mapping -> mapping == target);
                        heldMappings.add(target);
                    } else {
                        heldMappings.remove(target);
                        pendingSetDownFalse.add(target);
                    }
                }
            }
        }
    }

    public static void clear(KeyMapping target) {
        if (target == null) return;
        ((KeyMappingClickCountAccessor) (Object) target).keywheel$setClickCount(0);
        InputConstants.Key replayKey;
        synchronized (pendingSetDownFalse) {
            pendingSetDownFalse.removeIf(mapping -> mapping == target);
            heldMappings.remove(target);
            InputConstants.Key pendingKey = pendingReplayReleases.remove(target);
            InputConstants.Key heldKey = heldReplayKeys.remove(target);
            replayKey = heldKey != null ? heldKey : pendingKey;
        }
        if (replayKey != null) releaseReplay(target, replayKey);
        else target.setDown(false);
    }

    public static void clearHeld(KeyMapping target) {
        if (target == null) return;
        boolean removed;
        InputConstants.Key replayKey;
        synchronized (pendingSetDownFalse) {
            removed = heldMappings.remove(target);
            replayKey = heldReplayKeys.remove(target);
        }
        if (replayKey != null) releaseReplay(target, replayKey);
        else if (removed) target.setDown(false);
    }

    public static void releaseHeldOnInput(int action) {
        if (shouldReleaseHeldInput(action)) releaseHeld();
    }

    static boolean shouldReleaseHeldInput(int action) {
        return action == GLFW.GLFW_PRESS;
    }

    public static void releaseHeld() {
        List<KeyMapping> releasing;
        Map<KeyMapping, InputConstants.Key> replaying;
        synchronized (pendingSetDownFalse) {
            if (heldMappings.isEmpty() && heldReplayKeys.isEmpty()) return;
            releasing = new ArrayList<>(heldMappings);
            replaying = new IdentityHashMap<>(heldReplayKeys);
            heldMappings.clear();
            heldReplayKeys.clear();
        }
        for (KeyMapping mapping : releasing) {
            mapping.setDown(false);
        }
        replaying.forEach(ActionExecutor::releaseReplay);
    }

    public static boolean isHolding(KeyMapping mapping) {
        synchronized (pendingSetDownFalse) {
            return mapping != null
                    && (heldMappings.contains(mapping) || heldReplayKeys.containsKey(mapping));
        }
    }

    public static void flushSetDown() {
        Map<KeyMapping, InputConstants.Key> replaying;
        synchronized (pendingSetDownFalse) {
            for (KeyMapping km : pendingSetDownFalse) {
                km.setDown(false);
            }
            pendingSetDownFalse.clear();
            replaying = new IdentityHashMap<>(pendingReplayReleases);
            pendingReplayReleases.clear();
        }
        replaying.forEach(ActionExecutor::releaseReplay);
    }

    private static void releaseReplay(KeyMapping mapping, InputConstants.Key key) {
        SyntheticInputReplayer.replay(mapping, key, GLFW.GLFW_RELEASE);
        mapping.setDown(false);
    }
}
