package com.example.keywheel.mixin;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.input.SyntheticInputContext;
import com.example.keywheel.input.WheelActionBridge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingClickMixin {
    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
    private void keywheel$guardSetDown(boolean down, CallbackInfo ci) {
        KeyMapping mapping = (KeyMapping) (Object) this;
        if (down && !SyntheticInputContext.allows(mapping)) {
            mapping.setDown(false);
            ci.cancel();
            return;
        }
        boolean locked = KeyWheelConfig.isLocked(mapping.getName());
        boolean forceAllowed = WheelActionBridge.isForceAllowed(mapping);
        boolean blocked = shouldBlockLockedInput(locked, down, forceAllowed);
        if (blocked) {
            mapping.setDown(false);
            ci.cancel();
        }
    }

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void keywheel$blockMatches(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
        KeyMapping mapping = (KeyMapping) (Object) this;
        if (!SyntheticInputContext.allows(mapping) || isLockedOutsideWheel(mapping)) cir.setReturnValue(false);
    }

    @Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
    private void keywheel$blockMatchesMouse(int button, CallbackInfoReturnable<Boolean> cir) {
        KeyMapping mapping = (KeyMapping) (Object) this;
        if (!SyntheticInputContext.allows(mapping) || isLockedOutsideWheel(mapping)) cir.setReturnValue(false);
    }

    @Inject(method = "getKey", at = @At("RETURN"), cancellable = true, remap = false)
    private void keywheel$maskSyntheticKey(CallbackInfoReturnable<InputConstants.Key> cir) {
        KeyMapping mapping = (KeyMapping) (Object) this;
        if (SyntheticInputContext.shouldMask(mapping, cir.getReturnValue())) {
            cir.setReturnValue(InputConstants.UNKNOWN);
        }
    }

    @Unique
    private static boolean isLockedOutsideWheel(KeyMapping mapping) {
        return KeyWheelConfig.isLocked(mapping.getName())
                && !WheelActionBridge.isForceAllowed(mapping);
    }

    @Unique
    private static boolean shouldBlockLockedInput(boolean locked, boolean down, boolean forceAllow) {
        return locked && down && !forceAllow;
    }
}