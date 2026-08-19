package com.example.keywheel.input;

import com.example.keywheel.KeyWheel;
import com.example.keywheel.mixin.MouseHandlerInvoker;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class SyntheticInputReplayer {
    private SyntheticInputReplayer() {}

    public static boolean supports(InputConstants.Key key) {
        return key != null
                && !InputConstants.UNKNOWN.equals(key)
                && (key.getType() == InputConstants.Type.KEYSYM
                || key.getType() == InputConstants.Type.MOUSE);
    }

    public static boolean replay(KeyMapping target, InputConstants.Key key, int action) {
        if (target == null || !supports(key)) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return false;
        long window = mc.getWindow().getWindow();
        try (AutoCloseable ignored = SyntheticInputContext.begin(target, key)) {
            WheelActionBridge.addForceAllow(target);
            if (key.getType() == InputConstants.Type.KEYSYM) {
                mc.keyboardHandler.keyPress(window, key.getValue(), 0, action, modifierMask(target));
            } else {
                ((MouseHandlerInvoker) mc.mouseHandler)
                        .keywheel$invokeOnPress(window, key.getValue(), action, modifierMask(target));
            }
            return true;
        } catch (Exception exception) {
            KeyWheel.LOG.error("Failed to replay selected wheel input {}", target.getName(), exception);
            return true;
        } finally {
            WheelActionBridge.clearForceAllow();
        }
    }

    private static int modifierMask(KeyMapping target) {
        KeyModifier modifier = target.getKeyModifier();
        if (modifier == KeyModifier.CONTROL) return GLFW.GLFW_MOD_CONTROL;
        if (modifier == KeyModifier.SHIFT) return GLFW.GLFW_MOD_SHIFT;
        if (modifier == KeyModifier.ALT) return GLFW.GLFW_MOD_ALT;
        return 0;
    }
}