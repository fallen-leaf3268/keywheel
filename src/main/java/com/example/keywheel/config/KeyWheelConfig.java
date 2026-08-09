package com.example.keywheel.config;

import com.example.keywheel.input.LongPressWatcher;
import com.example.keywheel.input.ActionExecutor;
import com.example.keywheel.screen.WheelConflictIndex;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public final class KeyWheelConfig {
    private static final char SWAP_PRIMARY_SEPARATOR = '|';
    private static volatile Set<String> lockedIdsCache = Set.of();
    private static volatile List<String> lockedIdsSource;
    private static volatile List<String> lockedMembersSource;

    private KeyWheelConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue HELD_TICKS_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<List<String>> SWAP_MODE_KEYS;
    public static final ForgeConfigSpec.ConfigValue<List<String>> SWAP_PRIMARY_TARGETS;
    public static final ForgeConfigSpec.ConfigValue<List<String>> MEMBERS;
    public static final ForgeConfigSpec.ConfigValue<List<String>> ICONS;
    public static final ForgeConfigSpec.ConfigValue<List<String>> BANNED;
    public static final ForgeConfigSpec.ConfigValue<List<String>> LOCKED;
    public static final ForgeConfigSpec.ConfigValue<List<String>> HOLD_ENABLED;

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
        LOCKED = (ForgeConfigSpec.ConfigValue<List<String>>)
                (ForgeConfigSpec.ConfigValue<?>) b.defineListAllowEmpty("locked", ArrayList::new, s -> s instanceof String);
        HOLD_ENABLED = (ForgeConfigSpec.ConfigValue<List<String>>)
                (ForgeConfigSpec.ConfigValue<?>) b.defineListAllowEmpty("hold_enabled", ArrayList::new, s -> s instanceof String);
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
        List<String> normalizedMembers = updatedMembership(members, null, false);
        MEMBERS.set(normalizedMembers);
        MEMBERS.save();
        replaceLocked(retainedLockedMembers(LOCKED.get(), normalizedMembers));
        replaceHeld(retainedHeldMembers(HOLD_ENABLED.get(), normalizedMembers));
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
        if (banned) {
            setLocked(kmName, false);
            setHoldEnabled(kmName, false);
        }
    }

    public static boolean isLocked(String kmName) {
        return kmName != null && lockedIds().contains(kmName);
    }

    public static void setLocked(String kmName, boolean locked) {
        if (kmName == null) return;
        List<String> list = updatedMembership(LOCKED.get(), kmName,
                locked && isMember(kmName));
        replaceLocked(retainedLockedMembers(list, MEMBERS.get()));
        if (locked && isLocked(kmName)) clearMappingState(kmName);
    }

    static List<String> retainedLockedMembers(List<String> locked, List<String> members) {
        Set<String> memberIds = members == null ? Set.of() : new LinkedHashSet<>(members);
        LinkedHashSet<String> retained = new LinkedHashSet<>();
        if (locked != null) {
            for (String id : locked) {
                if (id != null && memberIds.contains(id)) retained.add(id);
            }
        }
        return new ArrayList<>(retained);
    }

    public static boolean isHoldEnabled(String kmName) {
        return kmName != null && isMember(kmName) && HOLD_ENABLED.get().contains(kmName);
    }

    public static void setHoldEnabled(String kmName, boolean enabled) {
        if (kmName == null) return;
        List<String> updated = updatedMembership(HOLD_ENABLED.get(), kmName,
                enabled && isMember(kmName));
        replaceHeld(retainedHeldMembers(updated, MEMBERS.get()));
        if (!enabled) clearHeldMappingState(kmName);
    }

    static List<String> retainedHeldMembers(List<String> held, List<String> members) {
        Set<String> memberIds = members == null ? Set.of() : new LinkedHashSet<>(members);
        LinkedHashSet<String> retained = new LinkedHashSet<>();
        if (held != null) {
            for (String id : held) {
                if (id != null && memberIds.contains(id)) retained.add(id);
            }
        }
        return new ArrayList<>(retained);
    }

    public static void invalidateLockedCache() {
        synchronized (KeyWheelConfig.class) {
            lockedIdsSource = null;
            lockedMembersSource = null;
            lockedIdsCache = Set.of();
        }
    }

    private static Set<String> lockedIds() {
        List<String> locked = LOCKED.get();
        List<String> members = MEMBERS.get();
        if (locked != lockedIdsSource || members != lockedMembersSource) {
            synchronized (KeyWheelConfig.class) {
                if (locked != lockedIdsSource || members != lockedMembersSource) {
                    lockedIdsCache = Set.copyOf(retainedLockedMembers(locked, members));
                    lockedIdsSource = locked;
                    lockedMembersSource = members;
                }
            }
        }
        return lockedIdsCache;
    }

    private static void replaceLocked(List<String> locked) {
        if (!locked.equals(LOCKED.get())) {
            LOCKED.set(locked);
            LOCKED.save();
        }
        invalidateLockedCache();
    }

    private static void replaceHeld(List<String> held) {
        List<String> previous = HOLD_ENABLED.get();
        if (!held.equals(previous)) {
            HOLD_ENABLED.set(held);
            HOLD_ENABLED.save();
        }
        if (previous != null) {
            for (String id : previous) {
                if (id != null && !held.contains(id)) clearHeldMappingState(id);
            }
        }
    }

    private static void clearMappingState(String kmName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) return;
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (kmName.equals(mapping.getName())) ActionExecutor.clear(mapping);
        }
    }

    private static void clearHeldMappingState(String kmName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) return;
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (kmName.equals(mapping.getName())) ActionExecutor.clearHeld(mapping);
        }
    }
}
