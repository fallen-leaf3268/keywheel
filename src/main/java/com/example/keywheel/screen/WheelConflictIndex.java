package com.example.keywheel.screen;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.input.PhysicalKeyState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WheelConflictIndex {
    private static final Set<InputConstants.Key> CONFLICT_KEYS = new HashSet<>();
    private static boolean initialized = false;

    private WheelConflictIndex() {}

    public static boolean contains(InputConstants.Key k) {
        if (k == null || !PhysicalKeyState.isSupported(k.getType())) return false;
        ensure();
        return CONFLICT_KEYS.contains(k);
    }

    public static int size() {
        ensure();
        return CONFLICT_KEYS.size();
    }

    public static void reset() {
        CONFLICT_KEYS.clear();
        initialized = false;
        wheelKeysCache = Set.of();
        wheelKeysStamp = 0L;
    }

    private static Set<InputConstants.Key> wheelKeysCache = Set.of();
    private static long wheelKeysStamp = 0L;

    public static Set<InputConstants.Key> wheelKeys() {
        long now = System.currentTimeMillis();
        if (now - wheelKeysStamp < 2000L) {
            return wheelKeysCache;
        }
        Set<InputConstants.Key> keys = new HashSet<>();
        try {
            List<String> members = KeyWheelConfig.MEMBERS.get();
            if (members != null && !members.isEmpty()) {
                var mc = Minecraft.getInstance();
                if (mc != null && mc.options != null && mc.options.keyMappings != null) {
                    Set<String> memberSet = new HashSet<>(members);
                    for (KeyMapping km : mc.options.keyMappings) {
                        boolean member = memberSet.contains(km.getName());
                        if (!member) continue;
                        InputConstants.Key key = km.getKey();
                        boolean supported = PhysicalKeyState.isSupported(key.getType());
                        if (supported && shouldIncludeWheelKey(member, contains(key))) keys.add(key);
                    }
                }
            }
        } catch (Throwable ignored) {}
        wheelKeysCache = Set.copyOf(keys);
        wheelKeysStamp = now;
        return wheelKeysCache;
    }

    static boolean shouldIncludeWheelKey(boolean member, boolean currentConflict) {
        return member && currentConflict;
    }

    private static void ensure() {
        if (initialized) return;
        initialized = true;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.options == null || mc.options.keyMappings == null) return;
        Map<InputConstants.Key, Integer> cnt = new HashMap<>();
        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getCategory().equals("key.categories.keywheel")) continue;
            if (!PhysicalKeyState.isSupported(km.getKey().getType())) continue;
            cnt.merge(km.getKey(), 1, Integer::sum);
        }
        for (var e : cnt.entrySet()) {
            if (e.getValue() >= 2) CONFLICT_KEYS.add(e.getKey());
        }
    }
}
