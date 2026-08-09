package com.example.keywheel.mixin;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.input.WheelActionBridge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyMappingLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = KeyMappingLookup.class, remap = false)
public abstract class KeyMappingLookupMixin {
    @Redirect(
            method = {
                    "getAll(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Ljava/util/List;",
                    "get(Lcom/mojang/blaze3d/platform/InputConstants$Key;Lnet/minecraftforge/client/settings/KeyModifier;)Lnet/minecraft/client/KeyMapping;"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;isActiveAndMatches(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean keywheel$filterMatch(KeyMapping mapping, InputConstants.Key key) {
        return shouldMatchLockedInput(mapping.isActiveAndMatches(key),
                KeyWheelConfig.isLocked(mapping.getName()),
                WheelActionBridge.isForceAllowed(mapping));
    }

    @Unique
    private static boolean shouldMatchLockedInput(
            boolean originalMatch, boolean locked, boolean forceAllow) {
        return originalMatch && (!locked || forceAllow);
    }
}
