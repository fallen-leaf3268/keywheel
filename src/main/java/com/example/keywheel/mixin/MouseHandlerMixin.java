package com.example.keywheel.mixin;

import com.example.keywheel.input.ActionExecutor;
import com.example.keywheel.screen.WheelConflictIndex;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void keywheel$onMousePress(long window, int button, int action, int mods, CallbackInfo ci) {
        ActionExecutor.releaseHeldOnInput(action);
        if (action != GLFW.GLFW_PRESS) return;
        if (Minecraft.getInstance().screen != null) return;
        InputConstants.Key inputKey = InputConstants.Type.MOUSE.getOrCreate(button);
        if (WheelConflictIndex.wheelKeys().contains(inputKey)) {
            ci.cancel();
        }
    }
}
