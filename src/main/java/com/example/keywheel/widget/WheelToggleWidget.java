package com.example.keywheel.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class WheelToggleWidget extends AbstractWidget {
    public boolean on;
    public Runnable onToggle;

    public WheelToggleWidget(int x, int y, boolean on) {
        super(x, y, 22, 11, Component.empty());
        this.on = on;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && this.isMouseOver(mx, my)) {
            this.on = !this.on;
            if (onToggle != null) onToggle.run();
            return true;
        }
        return false;
    }

    @Override
    public void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partial) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hovered = isMouseOver(mouseX, mouseY);

        SwapModeWidget.renderSwitch(gg, x, y, w, h, on, hovered);

        if (hovered) {
            gg.renderTooltip(net.minecraft.client.Minecraft.getInstance().font,
                    Component.translatable("key.keywheel.wheel_toggle_tooltip"), mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean isFocused() { return false; }
    @Override public void setFocused(boolean focused) {}
}
