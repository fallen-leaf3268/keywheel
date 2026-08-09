package com.example.keywheel.widget;

import com.example.keywheel.config.KeyWheelConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class FunctionHoldWidget extends AbstractWidget {
    private final String mappingId;

    public FunctionHoldWidget(int x, int y, String mappingId) {
        super(x, y, 30, 14, Component.empty());
        this.mappingId = mappingId;
    }

    public boolean isOn() {
        return KeyWheelConfig.isHoldEnabled(mappingId);
    }

    private void toggle() {
        KeyWheelConfig.setHoldEnabled(mappingId, !isOn());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && isMouseOver(mx, my) && active) {
            toggle();
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partial) {
        SwapModeWidget.renderSwitch(gg, getX(), getY(), getWidth(), getHeight(), isOn(), isHovered);
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    @Override
    public boolean isFocused() { return false; }

    @Override
    public void setFocused(boolean focused) {}
}
