package com.example.keywheel.mixin;

import com.example.keywheel.screen.WheelConflictIndex;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class KeyMappingSetKeyMixin {
    @Inject(method = "setKey", at = @At("TAIL"))
    private void keywheel$onSetKey(InputConstants.Key key, CallbackInfo ci) {
        WheelConflictIndex.markDirty();
    }

    @Inject(method = "resetMapping", at = @At("TAIL"))
    private static void keywheel$afterResetMapping(CallbackInfo ci) {
        WheelConflictIndex.flushDirty();
    }
}
