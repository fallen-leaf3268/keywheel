package com.example.keywheel.input;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class PhysicalKeyState {
    private PhysicalKeyState() {}

    public static boolean isSupported(InputConstants.Type type) {
        return type == InputConstants.Type.KEYSYM || type == InputConstants.Type.MOUSE;
    }

    public static boolean isPressed(long window, InputConstants.Key key) {
        if (key == null) return false;
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
