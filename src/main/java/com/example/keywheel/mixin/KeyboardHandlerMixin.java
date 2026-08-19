package com.example.keywheel.mixin;

import com.example.keywheel.input.ActionExecutor;
import com.example.keywheel.input.SyntheticInputContext;
import com.example.keywheel.screen.WheelConflictIndex;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void keywheel$onKeyPress(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        if (SyntheticInputContext.isActive()) return;
        ActionExecutor.releaseHeldOnInput(action);
        if (action != 1 && action != 2) return;
        if (Minecraft.getInstance().screen != null) return;
        InputConstants.Key inputKey = InputConstants.getKey(key, scancode);
        if (WheelConflictIndex.wheelKeys().contains(inputKey)) {
            ci.cancel();
        }
    }
}