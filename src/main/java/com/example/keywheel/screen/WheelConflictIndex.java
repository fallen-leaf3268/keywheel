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
    private static boolean dirty = false;

    private WheelConflictIndex() {}

    public static boolean contains(InputConstants.Key k) {
        if (!PhysicalKeyState.isSupported(k)) return false;
        if (dirty) return CONFLICT_KEYS.contains(k);
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
        dirty = false;
        wheelKeysCache = Set.of();
        wheelKeysStamp = 0L;
    }

    public static void markDirty() {
        dirty = true;
    }

    public static boolean flushDirty() {
        if (!dirty) return false;
        reset();
        return true;
    }

    private static Set<InputConstants.Key> wheelKeysCache = Set.of();
    private static long wheelKeysStamp = 0L;

    public static Set<InputConstants.Key> wheelKeys() {
        if (dirty) {
            reset();
        }
        long now = System.currentTimeMillis();
        if (now - wheelKeysStamp < 2000L) {
            return wheelKeysCache;
        }
        Set<InputConstants.Key> keys = new HashSet<>();
        var mc = Minecraft.getInstance();
        if (mc != null && mc.options != null && mc.options.keyMappings != null && KeyWheelConfig.SPEC.isLoaded()) {
            List<String> members = KeyWheelConfig.currentMembers();
            if (!members.isEmpty()) {
                Set<String> memberSet = new HashSet<>(members);
                rebuildConflicts(mc.options.keyMappings);
                for (KeyMapping km : mc.options.keyMappings) {
                    InputConstants.Key key = km.getKey();
                    if (PhysicalKeyState.isSupported(key)
                            && shouldIncludeWheelKey(memberSet.contains(km.getName()), CONFLICT_KEYS.contains(key))) {
                        keys.add(key);
                    }
                }
            }
        }
        wheelKeysCache = Set.copyOf(keys);
        wheelKeysStamp = now;
        return wheelKeysCache;
    }

    static boolean shouldIncludeWheelKey(boolean member, boolean currentConflict) {
        return member && currentConflict;
    }

    private static void ensure() {
        if (initialized) return;
        var mc = Minecraft.getInstance();
        if (mc == null || mc.options == null || mc.options.keyMappings == null) return;
        rebuildConflicts(mc.options.keyMappings);
    }

    private static void rebuildConflicts(KeyMapping[] mappings) {
        Map<InputConstants.Key, Integer> cnt = new HashMap<>();
        for (KeyMapping km : mappings) {
            if (km.getCategory().equals("key.categories.keywheel")) continue;
            if (!PhysicalKeyState.isSupported(km.getKey())) continue;
            cnt.merge(km.getKey(), 1, Integer::sum);
        }
        CONFLICT_KEYS.clear();
        for (var e : cnt.entrySet()) {
            if (e.getValue() >= 2) CONFLICT_KEYS.add(e.getKey());
        }
        initialized = true;
    }
}
