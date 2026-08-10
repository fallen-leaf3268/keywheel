package com.example.keywheel.input;

import com.example.keywheel.screen.WheelGeometry;
import com.example.keywheel.screen.WheelConflictIndex;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
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
        requireLockedInputIsolation();
        requireLockedInputEntryPoints();
        requireSyntheticInputIsolation();
        requireLockedMembershipCleanup();
        requireLockedStateClearEntryPoint();
        requireForgeDirectMatchMixin();
        requireTargetSpecificForceAllow();
        requireFunctionLockWidget();
        requireFunctionLockTooltip();
        requireHeldMembershipCleanup();
        requireHeldInputReleaseEntryPoints();
        requireNoHeldDiagnostics();
        requireHeldSetAllPreservation();
        requireFunctionHoldWidget();
        requireButtonFocusPolicy();
        String english = resource("assets/keywheel/lang/en_us.json");
        for (String key : new String[]{
                "key.keywheel.config",
                "key.keywheel.config_title",
                "key.keywheel.clear_icon",
                "key.keywheel.remove_from_wheel",
                "key.keywheel.empty_config_hint",
                "key.keywheel.function_hold"
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

    private static byte[] resourceBytes(String path) throws IOException {
        try (InputStream stream = KeyWheelRegressionTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new AssertionError("missing resource: " + path);
            return stream.readAllBytes();
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

    @SuppressWarnings("unchecked")
    private static void requireLockedInputIsolation() throws Exception {
        Class<?> clickMixin = Class.forName("com.example.keywheel.mixin.KeyMappingClickMixin");
        Class<?> lookupMixin = Class.forName("com.example.keywheel.mixin.KeyMappingLookupMixin");
        java.lang.reflect.Method matchPolicy;
        java.lang.reflect.Method lockedOutsideWheel;
        java.lang.reflect.Method shouldBlock;
        try {
            matchPolicy = lookupMixin.getDeclaredMethod("shouldMatchLockedInput",
                    boolean.class, boolean.class, boolean.class);
            lockedOutsideWheel = clickMixin.getDeclaredMethod("isLockedOutsideWheel", KeyMapping.class);
            shouldBlock = clickMixin.getDeclaredMethod("shouldBlockLockedInput",
                    boolean.class, boolean.class, boolean.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("locked input must be filtered per mapping instead of per physical key", e);
        }
        matchPolicy.setAccessible(true);
        lockedOutsideWheel.setAccessible(true);
        shouldBlock.setAccessible(true);
        requirePrivateStaticMixinHelper(matchPolicy);
        requirePrivateStaticMixinHelper(lockedOutsideWheel);
        requirePrivateStaticMixinHelper(shouldBlock);

        boolean lockedModifierMatch = (boolean) matchPolicy.invoke(null, true, true, false);
        boolean normalFallbackMatch = (boolean) matchPolicy.invoke(null, true, false, false);
        require(!lockedModifierMatch && normalFallbackMatch,
                "a locked modifier binding C must not prevent normal A/B fallback on the same key");
        require((boolean) shouldBlock.invoke(null, true, true, false),
                "a locked press outside the wheel must be blocked");
        require(!(boolean) shouldBlock.invoke(null, true, false, false),
                "a locked release must remain allowed to prevent stuck keys");
        require(!(boolean) shouldBlock.invoke(null, false, true, false),
                "an unlocked mapping must remain allowed");
        require(!(boolean) shouldBlock.invoke(null, true, true, true),
                "wheel execution must be able to force-allow the selected locked mapping");
    }

    private static void requirePrivateStaticMixinHelper(java.lang.reflect.Method method) {
        int modifiers = method.getModifiers();
        require(Modifier.isPrivate(modifiers) && Modifier.isStatic(modifiers),
                method.getName() + " must be private static for Mixin 0.8.5");
    }

    private static void requireLockedInputEntryPoints() throws Exception {
        requireDeclaredMethod("com.example.keywheel.mixin.KeyMappingClickMixin", "keywheel$guardSetDown");
        requireDeclaredMethod("com.example.keywheel.mixin.KeyMappingClickMixin", "keywheel$blockMatches");
        requireDeclaredMethod("com.example.keywheel.mixin.KeyMappingClickMixin", "keywheel$blockMatchesMouse");
        requireDeclaredMethod("com.example.keywheel.mixin.KeyMappingLookupMixin", "keywheel$filterMatch");
    }

    private static void requireSyntheticInputIsolation() throws Exception {
        KeyMapping target = new KeyMapping("key.keywheel.replay.target", 71, "key.categories.misc");
        KeyMapping sameKey = new KeyMapping("key.keywheel.replay.other", 71, "key.categories.misc");
        KeyMapping otherKey = new KeyMapping("key.keywheel.replay.unrelated", 72, "key.categories.misc");
        Class<?> context = Class.forName("com.example.keywheel.input.SyntheticInputContext");
        var begin = context.getDeclaredMethod("begin", KeyMapping.class, InputConstants.Key.class);
        var active = context.getDeclaredMethod("isActive");
        var allows = context.getDeclaredMethod("allows", KeyMapping.class);
        var shouldMask = context.getDeclaredMethod("shouldMask", KeyMapping.class, InputConstants.Key.class);
        AutoCloseable scope = (AutoCloseable) begin.invoke(null, target, target.getKey());
        try {
            require((boolean) active.invoke(null), "synthetic replay context must be active inside its scope");
            require((boolean) allows.invoke(null, target), "synthetic replay must allow its target");
            require(!(boolean) allows.invoke(null, sameKey), "synthetic replay must isolate a same-key non-target");
            require((boolean) shouldMask.invoke(null, sameKey, sameKey.getKey()),
                    "same-key non-target key must be masked");
            require(!(boolean) shouldMask.invoke(null, otherKey, otherKey.getKey()),
                    "unrelated keys must not be masked");
        } finally {
            scope.close();
        }
        require(!(boolean) active.invoke(null), "synthetic replay context must always clear");
        requireDeclaredMethod("com.example.keywheel.mixin.KeyMappingClickMixin", "keywheel$maskSyntheticKey");
    }

    @SuppressWarnings("unchecked")
    private static void requireLockedMembershipCleanup() throws Exception {
        Class<?> config = Class.forName("com.example.keywheel.config.KeyWheelConfig");
        var method = config.getDeclaredMethod("retainedLockedMembers", List.class, List.class);
        method.setAccessible(true);
        List<String> retained = (List<String>) method.invoke(null,
                List.of("locked-c", "locked-c", "stale", "wheel-b"),
                List.of("wheel-b", "locked-c"));
        require(retained.equals(List.of("locked-c", "wheel-b")),
                "locked ids must be unique and restricted to current wheel members");
        List<String> removed = (List<String>) method.invoke(null,
                retained, List.of("wheel-b"));
        require(removed.equals(List.of("wheel-b")),
                "removing a member must also remove its persistent lock");
        requireField("com.example.keywheel.config.KeyWheelConfig", "lockedIdsCache");
    }

    private static void requireLockedStateClearEntryPoint() throws Exception {
        Class.forName("com.example.keywheel.input.ActionExecutor")
                .getDeclaredMethod("clear", KeyMapping.class);
    }

    private static void requireForgeDirectMatchMixin() throws Exception {
        requireDeclaredMethod("com.example.keywheel.mixin.KeyMappingLookupMixin", "keywheel$filterMatch");
        Class<?> forgeMixin = Class.forName("com.example.keywheel.mixin.ForgeKeyMappingMixin");
        var directMatch = forgeMixin.getDeclaredMethod("isActiveAndMatches", InputConstants.Key.class);
        require(directMatch.isDefault() && Modifier.isPublic(directMatch.getModifiers()),
                "the Forge direct matching guard must replace the public default method");
        String mixins = resource("keywheel.mixins.json");
        require(mixins.contains("\"KeyMappingLookupMixin\""),
                "Forge lookup filtering must use a scoped lookup mixin");
        require(mixins.contains("\"ForgeKeyMappingMixin\""),
                "inventory UI direct matching must use the Forge mapping guard");
    }

    private static void requireTargetSpecificForceAllow() {
        KeyMapping allowed = new KeyMapping("key.keywheel.test.allowed", -1, "key.categories.misc");
        KeyMapping other = new KeyMapping("key.keywheel.test.other", -1, "key.categories.misc");
        try {
            WheelActionBridge.addForceAllow(allowed);
            require(WheelActionBridge.isForceAllowed(allowed),
                    "the selected wheel target must be force-allowed");
            require(!WheelActionBridge.isForceAllowed(other),
                    "force-allowing C must not allow another locked mapping");
        } finally {
            WheelActionBridge.clearForceAllow();
        }
    }

    private static void requireFunctionLockWidget() throws Exception {
        Class.forName("com.example.keywheel.widget.FunctionLockWidget")
                .getDeclaredConstructor(int.class, int.class, String.class);
        Class<?> screen = Class.forName("com.example.keywheel.screen.WheelConfigScreen");
        var selectPolicy = screen.getDeclaredMethod("shouldSelectSector", int.class);
        selectPolicy.setAccessible(true);
        require((boolean) selectPolicy.invoke(null, 0),
                "left click must select a wheel function for lock configuration");
        require(!(boolean) selectPolicy.invoke(null, 1),
                "right click must no longer lock or select a wheel function");
        require(!containsBytes(resourceBytes("com/example/keywheel/screen/WheelConfigScreen.class"),
                        "🔒".getBytes(StandardCharsets.UTF_8)),
                "the wheel preview must no longer render an orange lock icon");
        byte[] labelKey = "key.keywheel.function_lock".getBytes(StandardCharsets.UTF_8);
        require(containsBytes(resourceBytes("com/example/keywheel/screen/WheelConfigScreen.class"), labelKey),
                "the function lock label must be rendered by a vanilla button on the config screen");
        require(!containsUtf8Constant(resourceBytes("com/example/keywheel/widget/FunctionLockWidget.class"),
                        "key.keywheel.function_lock"),
                "the switch must not draw a second plain-text function lock label");
        String english = resource("assets/keywheel/lang/en_us.json");
        require(english.contains("\"key.keywheel.function_lock\""),
                "missing function lock slider translation");
    }

    private static void requireFunctionLockTooltip() throws Exception {
        byte[] tooltipKey = "key.keywheel.function_lock_tooltip".getBytes(StandardCharsets.UTF_8);
        require(containsBytes(resourceBytes("com/example/keywheel/screen/WheelConfigScreen.class"), tooltipKey),
                "the function lock button must use the lock behavior tooltip");
        require(containsBytes(resourceBytes("com/example/keywheel/widget/FunctionLockWidget.class"), tooltipKey),
                "the function lock switch must use the same lock behavior tooltip");
        String chinese = resource("assets/keywheel/lang/zh_cn.json");
        String english = resource("assets/keywheel/lang/en_us.json");
        require(chinese.contains("\"key.keywheel.function_lock_tooltip\": \"锁定后，该功能只会通过轮盘触发\""),
                "missing exact Chinese function lock tooltip");
        require(english.contains("\"key.keywheel.function_lock_tooltip\": \"When locked, this action can only be triggered from the wheel.\""),
                "missing exact English function lock tooltip");
    }

    @SuppressWarnings("unchecked")
    private static void requireHeldMembershipCleanup() throws Exception {
        Class<?> config = Class.forName("com.example.keywheel.config.KeyWheelConfig");
        requireField("com.example.keywheel.config.KeyWheelConfig", "HOLD_ENABLED");
        var method = config.getDeclaredMethod("retainedHeldMembers", List.class, List.class);
        method.setAccessible(true);
        List<String> retained = (List<String>) method.invoke(null,
                List.of("held-c", "held-c", "stale", "wheel-b"),
                List.of("wheel-b", "held-c"));
        require(retained.equals(List.of("held-c", "wheel-b")),
                "held ids must be unique and restricted to current wheel members");
        List<String> removed = (List<String>) method.invoke(null,
                retained, List.of("wheel-b"));
        require(removed.equals(List.of("wheel-b")),
                "removing a member must also remove its persistent hold setting");
    }

    private static void requireHeldInputReleaseEntryPoints() throws Exception {
        Class<?> executor = Class.forName("com.example.keywheel.input.ActionExecutor");
        var release = executor.getDeclaredMethod("releaseHeldOnInput", int.class);
        var policy = executor.getDeclaredMethod("shouldReleaseHeldInput", int.class);
        policy.setAccessible(true);
        require(Modifier.isPublic(release.getModifiers()) && Modifier.isStatic(release.getModifiers()),
                "input handlers need one public held-action release entry point");
        require((boolean) policy.invoke(null, 1),
                "a new input press must release held wheel actions");
        require(!(boolean) policy.invoke(null, 0),
                "an input release must not release held wheel actions");
        require(!(boolean) policy.invoke(null, 2),
                "a keyboard repeat must not release held wheel actions");
        requireDeclaredMethod("com.example.keywheel.input.ActionExecutor", "releaseHeld");
        byte[] keyboard = resourceBytes("com/example/keywheel/mixin/KeyboardHandlerMixin.class");
        byte[] mouse = resourceBytes("com/example/keywheel/mixin/MouseHandlerMixin.class");
        byte[] entryPoint = "releaseHeldOnInput".getBytes(StandardCharsets.UTF_8);
        require(containsBytes(keyboard, entryPoint),
                "keyboard presses must release held wheel actions before normal handling");
        require(containsBytes(mouse, entryPoint),
                "mouse presses must release held wheel actions before normal handling");
        require(containsBytes(resourceBytes("com/example/keywheel/screen/WheelScreen.class"),
                        "runWheelAction".getBytes(StandardCharsets.UTF_8)),
                "wheel selection must use the hold-aware execution path");
        require(!containsBytes(resourceBytes("com/example/keywheel/input/LongPressWatcher.class"),
                        "runWheelAction".getBytes(StandardCharsets.UTF_8)),
                "short-press primary actions must remain one-shot actions");
    }

    private static void requireNoHeldDiagnostics() throws Exception {
        Class<?> executor = Class.forName("com.example.keywheel.input.ActionExecutor");
        executor.getDeclaredMethod("isHolding", KeyMapping.class);
        for (var method : executor.getDeclaredMethods()) {
            require(!method.getName().equals("tickHeldDiagnostics"),
                    "temporary held tick diagnostics must be removed");
            require(!method.getName().equals("takeSetAllDiagnostic"),
                    "temporary setAll diagnostics must be removed");
            require(!(method.getName().equals("releaseHeldOnInput") && method.getParameterCount() == 2),
                    "diagnostic release reasons must be removed");
        }
        byte[] marker = "[KEYWHEEL HOLD]".getBytes(StandardCharsets.UTF_8);
        for (String classPath : new String[]{
                "com/example/keywheel/input/ActionExecutor.class",
                "com/example/keywheel/input/LongPressWatcher.class",
                "com/example/keywheel/mixin/KeyMappingClickMixin.class",
                "com/example/keywheel/mixin/KeyMappingSetAllMixin.class",
                "com/example/keywheel/mixin/KeyboardHandlerMixin.class",
                "com/example/keywheel/mixin/MouseHandlerMixin.class"
        }) {
            require(!containsBytes(resourceBytes(classPath), marker),
                    "temporary held diagnostic logging must be absent from " + classPath);
        }
    }

    private static void requireHeldSetAllPreservation() throws Exception {
        Class<?> mixin = Class.forName("com.example.keywheel.mixin.KeyMappingSetAllMixin");
        var policy = mixin.getDeclaredMethod("keywheel$shouldPreserveHeldState", boolean.class);
        policy.setAccessible(true);
        requirePrivateStaticMixinHelper(policy);
        require((boolean) policy.invoke(null, true),
                "setAll must preserve a wheel action that is currently being held");
        require(!(boolean) policy.invoke(null, false),
                "setAll must keep synchronizing mappings that are not being held");
    }

    private static void requireFunctionHoldWidget() throws Exception {
        Class.forName("com.example.keywheel.widget.FunctionHoldWidget")
                .getDeclaredConstructor(int.class, int.class, String.class);
        byte[] labelKey = "key.keywheel.function_hold".getBytes(StandardCharsets.UTF_8);
        require(containsBytes(resourceBytes("com/example/keywheel/screen/WheelConfigScreen.class"), labelKey),
                "the function hold label must be rendered by a vanilla button on the config screen");
        require(!containsUtf8Constant(resourceBytes("com/example/keywheel/widget/FunctionHoldWidget.class"),
                        "key.keywheel.function_hold"),
                "the hold switch must not draw a second plain-text label");
    }

    private static boolean containsUtf8Constant(byte[] classBytes, String value) {
        byte[] text = value.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i <= classBytes.length - text.length - 3; i++) {
            if (classBytes[i] != 1
                    || (classBytes[i + 1] & 0xFF) != text.length >>> 8
                    || (classBytes[i + 2] & 0xFF) != (text.length & 0xFF)) continue;
            boolean matches = true;
            for (int j = 0; j < text.length; j++) {
                if (classBytes[i + j + 3] != text[j]) {
                    matches = false;
                    break;
                }
            }
            if (matches) return true;
        }
        return false;
    }

    private static boolean containsBytes(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static void requireButtonFocusPolicy() throws Exception {
        Class<?> screen = Class.forName("com.example.keywheel.screen.WheelConfigScreen");
        var policy = screen.getDeclaredMethod("shouldClearButtonFocus", boolean.class, boolean.class);
        policy.setAccessible(true);
        require((boolean) policy.invoke(null, true, true),
                "a mouse-handled button must release focus after clicking");
        require(!(boolean) policy.invoke(null, true, false),
                "search boxes and other focused controls must retain focus");
        require(!(boolean) policy.invoke(null, false, true),
                "an unhandled mouse click must not alter focus");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
