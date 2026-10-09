package com.example.keywheel.input;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class PhysicalKeyState {
    private PhysicalKeyState() {}

    public static boolean isSupported(InputConstants.Type type) {
        return type == InputConstants.Type.KEYSYM || type == InputConstants.Type.MOUSE;
    }

    public static boolean isSupported(InputConstants.Key key) {
        if (key == null) return false;
        int value = key.getValue();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return value >= GLFW.GLFW_KEY_SPACE && value <= GLFW.GLFW_KEY_LAST;
        }
        return key.getType() == InputConstants.Type.MOUSE
                && value >= GLFW.GLFW_MOUSE_BUTTON_1 && value <= GLFW.GLFW_MOUSE_BUTTON_LAST;
    }

    public static boolean isPressed(long window, InputConstants.Key key) {
        if (!isSupported(key)) return false;
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
