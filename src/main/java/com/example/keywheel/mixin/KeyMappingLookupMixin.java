package com.example.keywheel.mixin;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.input.SyntheticInputContext;
import com.example.keywheel.input.WheelActionBridge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyMappingLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = KeyMappingLookup.class, remap = false)
public abstract class KeyMappingLookupMixin {
    @Inject(method = "getAll(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void keywheel$selectSyntheticMappings(InputConstants.Key key,
                                                  CallbackInfoReturnable<List<KeyMapping>> cir) {
        if (!SyntheticInputContext.isActive()) return;
        cir.setReturnValue(SyntheticInputContext.key().equals(key)
                ? List.of(SyntheticInputContext.target()) : List.of());
    }

    @Inject(method = "get(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Lnet/minecraft/client/KeyMapping;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void keywheel$selectSyntheticMapping(InputConstants.Key key,
                                                 CallbackInfoReturnable<KeyMapping> cir) {
        if (!SyntheticInputContext.isActive()) return;
        cir.setReturnValue(SyntheticInputContext.key().equals(key) ? SyntheticInputContext.target() : null);
    }

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
        return SyntheticInputContext.allows(mapping)
                && shouldMatchLockedInput(mapping.isActiveAndMatches(key),
                KeyWheelConfig.isLocked(mapping.getName()),
                WheelActionBridge.isForceAllowed(mapping));
    }

    @Unique
    private static boolean shouldMatchLockedInput(
            boolean originalMatch, boolean locked, boolean forceAllow) {
        return originalMatch && (!locked || forceAllow);
    }
}
