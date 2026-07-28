package com.example.keywheel.widget;

import com.example.keywheel.config.KeyWheelConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * 20x20 自定义勾选 widget —— 完全在普通包里，不在 mixin 包，
 * 避开 mixin 0.8.5 的 "all classes in mixin package are treated as mixins" 规则。
 */
public class WheelToggleWidget extends AbstractWidget {
    public boolean on;
    public Runnable onToggle;

    public WheelToggleWidget(int x, int y, boolean on) {
        super(x, y, 20, 20, Component.empty());
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
        int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();
        boolean hovered = this.isMouseOver(mouseX, mouseY);
        int bg = on ? 0xFF6FD66F : 0xFF303030;
        int border = hovered ? 0xFFFFFFFF : 0xFF808080;
        gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        gg.renderOutline(x, y, w, h, border);
        if (on) {
            gg.renderOutline(x + 4, y + 4, w - 8, h - 8, 0xFFFFFFFF);
        }
        if (hovered) {
            gg.renderTooltip(net.minecraft.client.Minecraft.getInstance().font,
                    Component.translatable("key.keywheel.wheel_toggle_tooltip"), mouseX, mouseY);
        }
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
