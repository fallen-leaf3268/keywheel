package com.example.keywheel.mixin;

import com.example.keywheel.input.WheelActionBridge;
import com.example.keywheel.screen.WheelConflictIndex;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyMapping.class)
public abstract class KeyMappingSetAllMixin {
    @Redirect(
            method = "setAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V")
    )
    private static void keywheel$filterSetDown(KeyMapping mapping, boolean down) {
        boolean wheelKey = WheelConflictIndex.wheelKeys().contains(mapping.getKey());
        if (!WheelActionBridge.hasForceAllow() && Minecraft.getInstance().screen == null && wheelKey) {
            mapping.setDown(false);
            return;
        }
        mapping.setDown(down);
    }
}
