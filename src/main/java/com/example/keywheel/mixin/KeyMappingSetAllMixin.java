package com.example.keywheel.mixin;

import com.example.keywheel.input.WheelActionBridge;
import com.example.keywheel.screen.WheelConflictIndex;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class KeyMappingSetAllMixin {
    @Inject(method = "setAll", at = @At("HEAD"), cancellable = true)
    private static void keywheel$guardSetAll(CallbackInfo ci) {
        if (WheelActionBridge.hasForceAllow()) return;
        ci.cancel();
    }
}
