package com.example.keywheel.input;

import com.example.keywheel.screen.WheelGeometry;
import com.example.keywheel.screen.WheelConflictIndex;
import com.mojang.blaze3d.platform.InputConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public final class KeyWheelRegressionTest {
    private KeyWheelRegressionTest() {}

    public static void main(String[] args) throws Exception {
        require(PhysicalKeyState.isSupported(InputConstants.Type.KEYSYM), "keyboard keys must be supported");
        require(PhysicalKeyState.isSupported(InputConstants.Type.MOUSE), "mouse buttons must be supported");
        require(!PhysicalKeyState.isSupported(InputConstants.Type.SCANCODE), "scancodes must not be exposed as supported");
        require(WheelGeometry.indexFromMouse(100, 50, 50, 50, 4, 20, 50) == -1,
                "mouse outside the visible wheel must not select a sector");
        require(WheelGeometry.indexFromMouse(99, 50, 50, 50, 4, 20, 50) >= 0,
                "mouse inside the visible wheel must select a sector");
        require(styleColor("trackColor", false) == 0xFF4A4A4A, "off track must blend with the vanilla button gray");
        require(styleColor("trackColor", true) == 0xFF4F8A4F, "on track must retain a clear green state");
        require(styleColor("frameColor", false) == 0xFF777777, "normal frame must use a subdued vanilla edge");
        require(styleColor("frameColor", true) == 0xFFA0A0A0, "hovered frame must avoid a stark white outline");
        require(styleColor("knobTopColor", false) == 0xFFC8C8C8, "normal knob highlight must be subdued");
        require(styleColor("knobTopColor", true) == 0xFFD8D8D8, "hovered knob highlight must remain soft");
        requireSharedSwitchRenderer();
        require(updatedMembership(List.of("a", "a", "b"), "a", false).equals(List.of("b")),
                "member removal must remove duplicates in one pass");
        require(updatedMembership(List.of("a"), "b", true).equals(List.of("a", "b")),
                "member addition must preserve order");
        require(updatedSwapModeKeys(List.of("key.keyboard.g"), "key.keyboard.h", true)
                        .equals(List.of("key.keyboard.g", "key.keyboard.h")),
                "enabling one physical key must preserve independent key states");
        require(updatedSwapModeKeys(List.of("key.keyboard.g", "key.keyboard.g", "key.keyboard.h"),
                        "key.keyboard.g", false).equals(List.of("key.keyboard.h")),
                "disabling a physical key must remove only that key and any duplicates");
        Class.forName("com.example.keywheel.widget.SwapModeWidget")
                .getDeclaredConstructor(int.class, int.class, InputConstants.Key.class);
        requireField("com.example.keywheel.screen.WheelScreen", "entryIcons");
        requireField("com.example.keywheel.screen.WheelScreen", "tapFunctionLabel");
        requireField("com.example.keywheel.screen.WheelConfigScreen", "memberIds");
        requireField("com.example.keywheel.screen.WheelConfigScreen", "itemSearchText");
        requireDeclaredMethod("com.example.keywheel.screen.WheelRenderer", "appendSector");
        requireDeclaredMethod("com.example.keywheel.screen.WheelRenderer", "appendRing");
        requireDeclaredMethod("com.example.keywheel.input.ActionExecutor", "execute");
        require(!shouldDrawSeparators(1), "a single sector must render without a separator");
        require(shouldDrawSeparators(2), "multiple sectors must retain separators");
        requireWheelKeyCacheReset();
        requireMemberCacheInvalidation();
        requireSwapPrimaryPersistenceHelpers();
        requireCurrentConflictEligibility();
        String english = resource("assets/keywheel/lang/en_us.json");
        for (String key : new String[]{
                "key.keywheel.config",
                "key.keywheel.config_title",
                "key.keywheel.clear_icon",
                "key.keywheel.remove_from_wheel",
                "key.keywheel.empty_config_hint"
        }) {
            require(english.contains("\"" + key + "\""), "missing English translation: " + key);
        }
    }

    private static String resource(String path) throws IOException {
        try (InputStream stream = KeyWheelRegressionTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new AssertionError("missing resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int styleColor(String method, boolean state) throws Exception {
        Class<?> widget = Class.forName("com.example.keywheel.widget.SwapModeWidget");
        var m = widget.getDeclaredMethod(method, boolean.class);
        m.setAccessible(true);
        return (int) m.invoke(null, state);
    }

    private static void requireSharedSwitchRenderer() throws Exception {
        Class<?> widget = Class.forName("com.example.keywheel.widget.SwapModeWidget");
        Class<?> graphics = Class.forName("net.minecraft.client.gui.GuiGraphics");
        widget.getDeclaredMethod("renderSwitch", graphics, int.class, int.class, int.class, int.class,
                boolean.class, boolean.class);
    }

    @SuppressWarnings("unchecked")
    private static List<String> updatedMembership(List<String> members, String id, boolean enabled) throws Exception {
        Class<?> config = Class.forName("com.example.keywheel.config.KeyWheelConfig");
        var method = config.getDeclaredMethod("updatedMembership", List.class, String.class, boolean.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(null, members, id, enabled);
    }

    @SuppressWarnings("unchecked")
    private static List<String> updatedSwapModeKeys(List<String> keys, String id, boolean enabled) throws Exception {
        Class<?> config = Class.forName("com.example.keywheel.config.KeyWheelConfig");
        var method = config.getDeclaredMethod("updatedSwapModeKeys", List.class, String.class, boolean.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(null, keys, id, enabled);
    }

    private static void requireField(String className, String fieldName) throws Exception {
        Class.forName(className).getDeclaredField(fieldName);
    }

    private static void requireDeclaredMethod(String className, String methodName) throws Exception {
        for (var method : Class.forName(className).getDeclaredMethods()) {
            if (method.getName().equals(methodName)) return;
        }
        throw new NoSuchMethodException(className + "." + methodName);
    }

    private static boolean shouldDrawSeparators(int sectorCount) throws Exception {
        Class<?> renderer = Class.forName("com.example.keywheel.screen.WheelRenderer");
        var method = renderer.getDeclaredMethod("shouldDrawSeparators", int.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, sectorCount);
    }

    private static void requireWheelKeyCacheReset() throws Exception {
        var cache = WheelConflictIndex.class.getDeclaredField("wheelKeysCache");
        var stamp = WheelConflictIndex.class.getDeclaredField("wheelKeysStamp");
        cache.setAccessible(true);
        stamp.setAccessible(true);
        cache.set(null, Set.of(InputConstants.Type.KEYSYM.getOrCreate(71)));
        stamp.setLong(null, 42L);
        WheelConflictIndex.reset();
        require(((Set<?>) cache.get(null)).isEmpty(), "reset must clear the wheel key cache immediately");
        require(stamp.getLong(null) == 0L, "reset must clear the wheel key cache timestamp");
    }

    private static void requireMemberCacheInvalidation() throws Exception {
        var cache = LongPressWatcher.class.getDeclaredField("cachedIds");
        var stamp = LongPressWatcher.class.getDeclaredField("cacheStamp");
        cache.setAccessible(true);
        stamp.setAccessible(true);
        cache.set(null, List.of("key.example.cached"));
        stamp.setLong(null, 42L);
        java.lang.reflect.Method invalidate;
        try {
            invalidate = LongPressWatcher.class.getDeclaredMethod("invalidateMemberCache");
        } catch (NoSuchMethodException e) {
            throw new AssertionError("member changes need an explicit cache invalidation entry point", e);
        }
        invalidate.setAccessible(true);
        invalidate.invoke(null);
        require(((List<?>) cache.get(null)).isEmpty(), "member invalidation must clear cached member ids");
        require(stamp.getLong(null) == 0L, "member invalidation must clear its timestamp");
    }

    @SuppressWarnings("unchecked")
    private static void requireSwapPrimaryPersistenceHelpers() throws Exception {
        Class<?> config = Class.forName("com.example.keywheel.config.KeyWheelConfig");
        java.lang.reflect.Method update;
        java.lang.reflect.Method find;
        java.lang.reflect.Method removeMismatched;
        try {
            update = config.getDeclaredMethod("updatedSwapPrimaryEntries",
                    List.class, String.class, String.class);
            find = config.getDeclaredMethod("findSwapPrimary", List.class, String.class);
            removeMismatched = config.getDeclaredMethod("removeMismatchedSwapPrimaryEntries",
                    List.class, String.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("swap primary targets need persistent list helpers", e);
        }
        update.setAccessible(true);
        find.setAccessible(true);
        removeMismatched.setAccessible(true);
        List<String> stored = List.of(
                "key.keyboard.g|key.example.old",
                "malformed",
                "key.keyboard.h|key.example.keep",
                "key.keyboard.g|key.example.duplicate");
        List<String> replaced = (List<String>) update.invoke(null, stored,
                "key.keyboard.g", "key.example.new");
        require(replaced.equals(List.of(
                        "key.keyboard.h|key.example.keep",
                        "key.keyboard.g|key.example.new")),
                "updating one physical key must replace duplicates and preserve other keys");
        require("key.example.new".equals(find.invoke(null, replaced, "key.keyboard.g")),
                "stored swap primary must be found by physical key");
        List<String> removed = (List<String>) update.invoke(null, replaced, "key.keyboard.g", null);
        require(removed.equals(List.of("key.keyboard.h|key.example.keep")),
                "clearing one swap primary must preserve other physical keys");
        require(find.invoke(null, List.of("malformed"), "key.keyboard.g") == null,
                "malformed swap primary entries must be ignored");
        List<String> rebound = (List<String>) removeMismatched.invoke(null,
                List.of("key.keyboard.g|key.example.target", "key.keyboard.h|key.example.keep"),
                "key.example.target", "key.keyboard.h");
        require(rebound.equals(List.of("key.keyboard.h|key.example.keep")),
                "rebinding a persisted primary must remove its stale physical-key entry");
    }

    private static void requireCurrentConflictEligibility() throws Exception {
        Class<?> index = Class.forName("com.example.keywheel.screen.WheelConflictIndex");
        java.lang.reflect.Method method;
        try {
            method = index.getDeclaredMethod("shouldIncludeWheelKey", boolean.class, boolean.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("wheel interception must require a current key conflict", e);
        }
        method.setAccessible(true);
        require((boolean) method.invoke(null, true, true),
                "a current member on a conflicting key must remain eligible");
        require(!(boolean) method.invoke(null, true, false),
                "a member whose conflict disappeared must not remain intercepted");
        require(!(boolean) method.invoke(null, false, true),
                "a conflicting key without wheel members must not be intercepted");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
