package com.example.keywheel.config;

import com.example.keywheel.input.LongPressWatcher;
import com.example.keywheel.screen.WheelConflictIndex;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@SuppressWarnings("unchecked")
public final class KeyWheelConfig {
    private static final char SWAP_PRIMARY_SEPARATOR = '|';

    private KeyWheelConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue HELD_TICKS_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<List<String>> SWAP_MODE_KEYS;
    public static final ForgeConfigSpec.ConfigValue<List<String>> SWAP_PRIMARY_TARGETS;
    public static final ForgeConfigSpec.ConfigValue<List<String>> MEMBERS;
    public static final ForgeConfigSpec.ConfigValue<List<String>> ICONS;
    public static final ForgeConfigSpec.ConfigValue<List<String>> BANNED;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        HELD_TICKS_THRESHOLD = b.defineInRange("held_ticks_threshold", 5, 1, 60);
        SWAP_MODE_KEYS = (ForgeConfigSpec.ConfigValue<List<String>>)
                (ForgeConfigSpec.ConfigValue<?>) b.defineListAllowEmpty("swap_mode_keys", ArrayList::new,
                        s -> s instanceof String);
        SWAP_PRIMARY_TARGETS = (ForgeConfigSpec.ConfigValue<List<String>>)
                (ForgeConfigSpec.ConfigValue<?>) b.defineListAllowEmpty("swap_primary_targets", ArrayList::new,
                        s -> s instanceof String);
        b.pop();

        b.push("members");
        MEMBERS = (ForgeConfigSpec.ConfigValue<List<String>>)
                (ForgeConfigSpec.ConfigValue<?>) b.defineListAllowEmpty("members", ArrayList::new, s -> s instanceof String);
        ICONS = (ForgeConfigSpec.ConfigValue<List<String>>)
                (ForgeConfigSpec.ConfigValue<?>) b.defineListAllowEmpty("icons", ArrayList::new, s -> s instanceof String);
        BANNED = (ForgeConfigSpec.ConfigValue<List<String>>)
                (ForgeConfigSpec.ConfigValue<?>) b.defineListAllowEmpty("banned", ArrayList::new, s -> s instanceof String);
        b.pop();

        SPEC = b.build();
    }

    public static String getIcon(String kmName) {
        for (String entry : ICONS.get()) {
            if (entry != null && entry.startsWith(kmName + ":")) {
                return entry.substring(kmName.length() + 1);
            }
        }
        return null;
    }

    public static void setIcon(String kmName, String itemId) {
        List<String> list = new ArrayList<>();
        for (String entry : ICONS.get()) {
            if (entry != null && !entry.startsWith(kmName + ":")) list.add(entry);
        }
        if (itemId != null && !itemId.isEmpty()) list.add(kmName + ":" + itemId);
        ICONS.set(list);
        ICONS.save();
    }

    public static boolean isBanned(String kmName) {
        return BANNED.get().contains(kmName);
    }

    public static boolean isMember(String kmName) {
        return kmName != null && MEMBERS.get().contains(kmName);
    }

    public static boolean isSwapMode(String physicalKeyId) {
        return physicalKeyId != null && SWAP_MODE_KEYS.get().contains(physicalKeyId);
    }

    public static void setSwapMode(String physicalKeyId, boolean enabled) {
        if (physicalKeyId == null) return;
        SWAP_MODE_KEYS.set(updatedSwapModeKeys(SWAP_MODE_KEYS.get(), physicalKeyId, enabled));
        SWAP_MODE_KEYS.save();
    }

    static List<String> updatedSwapModeKeys(List<String> keys, String physicalKeyId, boolean enabled) {
        return updatedMembership(keys, physicalKeyId, enabled);
    }

    public static String getSwapPrimary(String physicalKeyId) {
        return findSwapPrimary(SWAP_PRIMARY_TARGETS.get(), physicalKeyId);
    }

    public static void setSwapPrimary(String physicalKeyId, String mappingId) {
        if (physicalKeyId == null) return;
        SWAP_PRIMARY_TARGETS.set(updatedSwapPrimaryEntries(
                SWAP_PRIMARY_TARGETS.get(), physicalKeyId, mappingId));
        SWAP_PRIMARY_TARGETS.save();
    }

    static List<String> updatedSwapPrimaryEntries(
            List<String> entries, String physicalKeyId, String mappingId) {
        List<String> out = new ArrayList<>();
        if (entries != null) {
            for (String entry : entries) {
                int separator = entry == null ? -1 : entry.indexOf(SWAP_PRIMARY_SEPARATOR);
                if (separator <= 0 || separator == entry.length() - 1) continue;
                if (!entry.substring(0, separator).equals(physicalKeyId)) out.add(entry);
            }
        }
        if (physicalKeyId != null && mappingId != null && !mappingId.isEmpty()) {
            out.add(physicalKeyId + SWAP_PRIMARY_SEPARATOR + mappingId);
        }
        return out;
    }

    static String findSwapPrimary(List<String> entries, String physicalKeyId) {
        if (entries == null || physicalKeyId == null) return null;
        String prefix = physicalKeyId + SWAP_PRIMARY_SEPARATOR;
        for (String entry : entries) {
            if (entry != null && entry.startsWith(prefix) && entry.length() > prefix.length()) {
                return entry.substring(prefix.length());
            }
        }
        return null;
    }

    public static void removeMismatchedSwapPrimary(String mappingId, String currentPhysicalKeyId) {
        List<String> current = SWAP_PRIMARY_TARGETS.get();
        List<String> updated = removeMismatchedSwapPrimaryEntries(
                current, mappingId, currentPhysicalKeyId);
        if (!updated.equals(current)) {
            SWAP_PRIMARY_TARGETS.set(updated);
            SWAP_PRIMARY_TARGETS.save();
        }
    }

    static List<String> removeMismatchedSwapPrimaryEntries(
            List<String> entries, String mappingId, String currentPhysicalKeyId) {
        List<String> out = new ArrayList<>();
        if (entries == null) return out;
        for (String entry : entries) {
            int separator = entry == null ? -1 : entry.indexOf(SWAP_PRIMARY_SEPARATOR);
            if (separator <= 0 || separator == entry.length() - 1) continue;
            String physicalKeyId = entry.substring(0, separator);
            String storedMappingId = entry.substring(separator + 1);
            if (storedMappingId.equals(mappingId) && !physicalKeyId.equals(currentPhysicalKeyId)) continue;
            out.add(entry);
        }
        return out;
    }

    public static void setMember(String kmName, boolean enabled) {
        replaceMembers(updatedMembership(MEMBERS.get(), kmName, enabled));
    }

    public static void replaceMembers(List<String> members) {
        MEMBERS.set(updatedMembership(members, null, false));
        MEMBERS.save();
        invalidateRuntimeCaches();
    }

    public static void invalidateRuntimeCaches() {
        WheelConflictIndex.reset();
        LongPressWatcher.invalidateMemberCache();
    }

    static List<String> updatedMembership(List<String> members, String kmName, boolean enabled) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (members != null) {
            for (String id : members) {
                if (id != null && (enabled || !id.equals(kmName))) out.add(id);
            }
        }
        if (enabled && kmName != null) out.add(kmName);
        return new ArrayList<>(out);
    }

    public static void setBanned(String kmName, boolean banned) {
        List<String> list = new ArrayList<>(BANNED.get());
        if (banned && !list.contains(kmName)) list.add(kmName);
        if (!banned) list.remove(kmName);
        BANNED.set(list);
        BANNED.save();
    }
}
