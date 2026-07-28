package com.example.keywheel.mixin;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.screen.WheelConflictIndex;
import com.example.keywheel.widget.WheelToggleWidget;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyEntryWheelMixin {
    @Shadow
    private KeyMapping key;
    @Shadow
    private Button resetButton;
    @Shadow
    private Button changeButton;

    private WheelToggleWidget wheelWidget = null;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void keywheel$afterInit(KeyBindsList parent, KeyMapping km,
                                   Component name, CallbackInfo ci) {
        keywheel$refreshWidget();
    }

    @Inject(method = "children", at = @At("RETURN"), cancellable = true)
    private void keywheel$appendWidget(CallbackInfoReturnable<List<? extends GuiEventListener>> cir) {
        keywheel$refreshWidget();
        if (wheelWidget == null) return;
        List<GuiEventListener> mutable = new ArrayList<>(cir.getReturnValue());
        mutable.add(wheelWidget);
        cir.setReturnValue(mutable);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void keywheel$headRender(GuiGraphics gg, int idx, int top, int left, int width,
                                     int height, int mouseX, int mouseY, boolean hovering,
                                     float partial, CallbackInfo ci) {
        keywheel$refreshWidget();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void keywheel$tailRender(GuiGraphics gg, int idx, int top, int left, int width,
                                     int height, int mouseX, int mouseY, boolean hovering,
                                     float partial, CallbackInfo ci) {
        if (wheelWidget == null) return;
        wheelWidget.setX(changeButton.getX() - 30);
        wheelWidget.setY(resetButton.getY());
        wheelWidget.render(gg, mouseX, mouseY, partial);
    }

    private void keywheel$refreshWidget() {
        boolean show = WheelConflictIndex.contains(key.getKey()) && !key.isUnbound();
        if (wheelWidget == null && show) {
            boolean on = false;
            for (String id : KeyWheelConfig.MEMBERS.get()) {
                if (id != null && id.equals(key.getName())) { on = true; break; }
            }
            wheelWidget = new WheelToggleWidget(0, 0, on);
            wheelWidget.onToggle = this::keywheel$persist;
        } else if (wheelWidget != null && !show) {
            wheelWidget = null;
        } else if (wheelWidget != null) {
            boolean on = false;
            for (String id : KeyWheelConfig.MEMBERS.get()) {
                if (id != null && id.equals(key.getName())) { on = true; break; }
            }
            wheelWidget.on = on;
        }
    }

    private void keywheel$persist() {
        if (wheelWidget == null) return;
        boolean on = wheelWidget.on;
        List<String> out = new ArrayList<>();
        for (String id : KeyWheelConfig.MEMBERS.get()) {
            if (id == null) continue;
            if (!id.equals(key.getName())) out.add(id);
        }
        if (on) out.add(key.getName());
        KeyWheelConfig.MEMBERS.set(out);
        KeyWheelConfig.MEMBERS.save();
    }
}
