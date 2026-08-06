package com.example.keywheel.input;

import com.example.keywheel.mixin.KeyMappingClickCountAccessor;
import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.List;

public final class ActionExecutor {
    private ActionExecutor() {}

    private static final List<KeyMapping> pendingSetDownFalse = new ArrayList<>();

    public static void run(KeyMapping target) {
        if (target == null) return;
        execute(List.of(target));
    }

    public static void runBatch(List<KeyMapping> targets) {
        if (targets == null || targets.isEmpty()) return;
        execute(targets);
    }

    private static void execute(List<KeyMapping> targets) {
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
                pendingSetDownFalse.addAll(activated);
            }
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
