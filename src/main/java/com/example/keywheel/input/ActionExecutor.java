package com.example.keywheel.input;

import com.example.keywheel.mixin.KeyMappingClickCountAccessor;
import com.example.keywheel.config.KeyWheelConfig;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class ActionExecutor {
    private ActionExecutor() {}

    private static final List<KeyMapping> pendingSetDownFalse = new ArrayList<>();
    private static final Set<KeyMapping> heldMappings = Collections.newSetFromMap(new IdentityHashMap<>());

    public static void run(KeyMapping target) {
        if (target == null) return;
        execute(List.of(target), false);
    }

    public static void runWheelAction(KeyMapping target) {
        if (target == null) return;
        execute(List.of(target), KeyWheelConfig.isHoldEnabled(target.getName()));
    }

    public static void runBatch(List<KeyMapping> targets) {
        if (targets == null || targets.isEmpty()) return;
        execute(targets, false);
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
        target.setDown(false);
        synchronized (pendingSetDownFalse) {
            pendingSetDownFalse.removeIf(mapping -> mapping == target);
            heldMappings.remove(target);
        }
    }

    public static void clearHeld(KeyMapping target) {
        if (target == null) return;
        boolean removed;
        synchronized (pendingSetDownFalse) {
            removed = heldMappings.remove(target);
        }
        if (removed) {
            target.setDown(false);
        }
    }

    public static void releaseHeldOnInput(int action) {
        if (shouldReleaseHeldInput(action)) releaseHeld();
    }

    static boolean shouldReleaseHeldInput(int action) {
        return action == GLFW.GLFW_PRESS;
    }

    public static void releaseHeld() {
        List<KeyMapping> releasing;
        synchronized (pendingSetDownFalse) {
            if (heldMappings.isEmpty()) return;
            releasing = new ArrayList<>(heldMappings);
            heldMappings.clear();
        }
        for (KeyMapping mapping : releasing) {
            mapping.setDown(false);
        }
    }

    public static boolean isHolding(KeyMapping mapping) {
        synchronized (pendingSetDownFalse) {
            return mapping != null && heldMappings.contains(mapping);
        }
    }

    public static void flushSetDown() {
        synchronized (pendingSetDownFalse) {
            for (KeyMapping km : pendingSetDownFalse) {
                km.setDown(false);
            }
            pendingSetDownFalse.clear();
        }
    }
}
