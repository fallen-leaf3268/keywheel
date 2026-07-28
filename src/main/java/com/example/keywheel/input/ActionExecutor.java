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
        WheelActionBridge.addForceAllow(target);
        target.setDown(true);
        WheelActionBridge.clearForceAllow();
        ((KeyMappingClickCountAccessor)(Object) target).keywheel$setClickCount(1);
        synchronized (pendingSetDownFalse) {
            pendingSetDownFalse.add(target);
        }
    }

    public static void runBatch(List<KeyMapping> targets) {
        if (targets == null || targets.isEmpty()) return;
        for (KeyMapping t : targets) {
            WheelActionBridge.addForceAllow(t);
            ((KeyMappingClickCountAccessor)(Object) t).keywheel$setClickCount(1);
            t.setDown(true);
        }
        WheelActionBridge.clearForceAllow();
        synchronized (pendingSetDownFalse) {
            pendingSetDownFalse.addAll(targets);
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
