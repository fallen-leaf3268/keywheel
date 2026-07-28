package com.example.keywheel.widget;

import com.example.keywheel.config.KeyWheelConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SwapModeWidget extends AbstractWidget {

    public SwapModeWidget(int x, int y) {
        super(x, y, 16, 16, Component.empty());
        KeyWheelConfig.SWAP_MODE.save();
    }

    public boolean isOn() {
        return KeyWheelConfig.SWAP_MODE.get();
    }

    private void toggle() {
        boolean v = !KeyWheelConfig.SWAP_MODE.get();
        KeyWheelConfig.SWAP_MODE.set(v);
        KeyWheelConfig.SWAP_MODE.save();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && this.isMouseOver(mx, my) && this.active) {
            toggle();
            return true;
        }
        return false;
    }

    @Override
    public void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partial) {
        int x = this.getX(), y = this.getY(), s = this.getWidth();
        boolean hovered = this.isMouseOver(mouseX, mouseY);
        boolean on = isOn();
        int bg = on ? 0xFF6FD66F : 0xFF404040;
        int border = hovered ? 0xFFFFFFFF : 0xFFA0A0A0;
        gg.fill(x, y, x + s, y + s, bg);
        gg.renderOutline(x, y, s, s, border);
        if (on) {
            gg.fill(x + 3, y + 3, x + s - 3, y + s - 3, 0xFFFFFFFF);
        }
    }

    public static boolean swapMembers(Component selectedLabel) {
        List<String> members = new ArrayList<>(KeyWheelConfig.MEMBERS.get());
        if (members == null || members.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return false;
        String selectedKey = selectedLabel.getString();
        KeyMapping target = null;
        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getName().equals(selectedKey)) { target = km; break; }
            String translated = net.minecraft.client.resources.language.I18n.get(km.getName(), new Object[0]);
            if (translated.equals(selectedKey)) { target = km; break; }
        }
        if (target == null || !members.contains(target.getName())) return false;
        KeyMapping firstNonMember = null;
        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getKey().equals(target.getKey()) && !members.contains(km.getName())) {
                firstNonMember = km; break;
            }
        }
        List<String> out = new ArrayList<>();
        for (String id : members) {
            if (id != null && !id.equals(target.getName())) out.add(id);
        }
        if (firstNonMember != null) {
            out.add(firstNonMember.getName());
        }
        KeyWheelConfig.MEMBERS.set(out);
        KeyWheelConfig.MEMBERS.save();
        return true;
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput o) {}
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean isFocused() { return false; }
    @Override public void setFocused(boolean focused) {}
}
