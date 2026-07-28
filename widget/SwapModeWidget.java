package com.example.keywheel.widget;

import com.example.keywheel.config.KeyWheelConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SwapModeWidget extends AbstractWidget {
    public boolean on;

    public SwapModeWidget(int x, int y) {
        super(x, y, 16, 16, Component.empty());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && this.isMouseOver(mx, my)) {
            this.on = !this.on;
            return true;
        }
        return false;
    }

    @Override
    public void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partial) {
        int x = this.getX(), y = this.getY(), s = this.getWidth();
        boolean hovered = this.isMouseOver(mouseX, mouseY);
        int bg = on ? 0xFF6FD66F : 0xFF404040;
        int border = hovered ? 0xFFFFFFFF : 0xFFA0A0A0;
        gg.fill(x, y, x + s, y + s, bg);
        gg.renderOutline(x, y, s, s, border);
        if (on) {
            gg.fill(x + 3, y + 3, x + s - 3, y + s - 3, 0xFFFFFFFF);
        }
    }

    public static String swapMembers(Component selectedLabel) {
        List<String> members = new ArrayList<>(KeyWheelConfig.MEMBERS.get());
        if (members == null || members.isEmpty()) return null;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.options == null) return null;
        net.minecraft.client.KeyMapping target = null;
        for (net.minecraft.client.KeyMapping km : mc.options.keyMappings) {
            String key = km.getName();
            String translated = net.minecraft.client.resources.language.I18n.get(key, new Object[0]);
            if (translated.equals(selectedLabel.getString()) || key.equals(selectedLabel.getString())) {
                if (members.contains(key)) { target = km; break; }
            }
        }
        if (target == null) return null;
        net.minecraft.client.KeyMapping firstNonMember = null;
        for (net.minecraft.client.KeyMapping km : mc.options.keyMappings) {
            if (km.getKey().equals(target.getKey()) && !members.contains(km.getName())) {
                firstNonMember = km; break;
            }
        }
        if (firstNonMember == null) return null;
        List<String> out = new ArrayList<>();
        for (String id : members) {
            if (id != null && !id.equals(target.getName())) out.add(id);
        }
        out.add(firstNonMember.getName());
        KeyWheelConfig.MEMBERS.set(out);
        KeyWheelConfig.MEMBERS.save();
        return firstNonMember.getName();
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput o) {}
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean isFocused() { return false; }
    @Override public void setFocused(boolean focused) {}
}
