package com.example.keywheel.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingClickCountAccessor {
    @Accessor("clickCount")
    int keywheel$getClickCount();

    @Accessor("clickCount")
    void keywheel$setClickCount(int value);
}
