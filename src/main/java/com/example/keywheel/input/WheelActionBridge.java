package com.example.keywheel.input;

import net.minecraft.client.KeyMapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class WheelActionBridge {
    private WheelActionBridge() {}

    private static final Set<KeyMapping> FORCE_ALLOW =
            Collections.synchronizedSet(new HashSet<>());

    public static void addForceAllow(KeyMapping km) {
        if (km != null) FORCE_ALLOW.add(km);
    }

    public static boolean isForceAllowed(KeyMapping km) {
        return km != null && FORCE_ALLOW.contains(km);
    }

    public static void clearForceAllow() {
        FORCE_ALLOW.clear();
    }
}
