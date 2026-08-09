package com.example.keywheel.mixin;

import com.example.keywheel.input.ActionExecutor;
import com.example.keywheel.input.WheelActionBridge;
import com.example.keywheel.screen.WheelConflictIndex;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyMapping.class)
public abstract class KeyMappingSetAllMixin {
    @Redirect(
            method = "setAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V")
    )
    private static void keywheel$filterSetDown(KeyMapping mapping, boolean down) {
        if (keywheel$shouldPreserveHeldState(ActionExecutor.isHolding(mapping))) return;
        boolean wheelKey = WheelConflictIndex.wheelKeys().contains(mapping.getKey());
        if (!WheelActionBridge.isForceAllowed(mapping)
                && Minecraft.getInstance().screen == null && wheelKey) {
            mapping.setDown(false);
            return;
        }
        mapping.setDown(down);
    }

    @Unique
    private static boolean keywheel$shouldPreserveHeldState(boolean holding) {
        return holding;
    }
}
