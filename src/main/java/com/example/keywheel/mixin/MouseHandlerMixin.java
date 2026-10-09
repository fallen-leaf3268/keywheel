package com.example.keywheel.mixin;

import com.example.keywheel.input.ActionExecutor;
import com.example.keywheel.input.SyntheticInputContext;
import com.example.keywheel.input.LongPressWatcher;
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
        if (SyntheticInputContext.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (window != mc.getWindow().getWindow()) return;
        ActionExecutor.releaseHeldOnInput(action);
        InputConstants.Key inputKey = InputConstants.Type.MOUSE.getOrCreate(button);
        if (LongPressWatcher.recordPhysicalInput(inputKey, action)
                || action == GLFW.GLFW_RELEASE && ActionExecutor.isHoldingKey(inputKey)) {
            ci.cancel();
        }
    }
}
