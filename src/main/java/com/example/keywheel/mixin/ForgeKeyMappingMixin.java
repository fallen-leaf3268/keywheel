package com.example.keywheel.mixin;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.input.WheelActionBridge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.extensions.IForgeKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = IForgeKeyMapping.class, remap = false)
public interface ForgeKeyMappingMixin {
    /**
     * @author Lenovo
     * @reason Block wheel-locked mappings in inventory and other direct Forge matching paths.
     */
    @Overwrite(remap = false)
    default boolean isActiveAndMatches(InputConstants.Key key) {
        if ((Object) this instanceof KeyMapping mapping
                && KeyWheelConfig.isLocked(mapping.getName())
                && !WheelActionBridge.isForceAllowed(mapping)) {
            return false;
        }
        IForgeKeyMapping forgeMapping = (IForgeKeyMapping) this;
        return key != InputConstants.UNKNOWN
                && key.equals(forgeMapping.getKey())
                && forgeMapping.getKeyConflictContext().isActive()
                && forgeMapping.getKeyModifier().isActive(forgeMapping.getKeyConflictContext());
    }
}
