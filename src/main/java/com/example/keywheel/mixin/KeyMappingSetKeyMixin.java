package com.example.keywheel.mixin;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.screen.WheelConflictIndex;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(KeyMapping.class)
public abstract class KeyMappingSetKeyMixin {
    @Inject(method = "setKey", at = @At("TAIL"))
    private void keywheel$onSetKey(InputConstants.Key key, CallbackInfo ci) {
        KeyMapping self = (KeyMapping)(Object) this;
        WheelConflictIndex.reset();
        List<String> members = KeyWheelConfig.MEMBERS.get();
        if (members == null || !members.contains(self.getName())) return;
        boolean shouldRemove = self.isUnbound() || !WheelConflictIndex.contains(key);
        if (shouldRemove) {
            List<String> out = new ArrayList<>();
            for (String id : members) {
                if (id != null && !id.equals(self.getName())) out.add(id);
            }
            KeyWheelConfig.MEMBERS.set(out);
            KeyWheelConfig.MEMBERS.save();
        }
    }
}
