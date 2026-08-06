package com.example.keywheel.widget;

import com.example.keywheel.config.KeyWheelConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SwapModeWidget extends AbstractWidget {
    private final String physicalKeyId;

    public SwapModeWidget(int x, int y, InputConstants.Key physicalKey) {
        super(x, y, 30, 14, Component.empty());
        this.physicalKeyId = physicalKey == null ? null : physicalKey.getName();
    }

    public boolean isOn() {
        return KeyWheelConfig.isSwapMode(physicalKeyId);
    }

    private void toggle() {
        KeyWheelConfig.setSwapMode(physicalKeyId, !isOn());
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
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean on = isOn();
        boolean hovered = this.isHovered;

        renderSwitch(gg, x, y, w, h, on, hovered);

        String textLabel = Component.translatable("key.keywheel.swap_mode").getString();
        gg.drawString(Minecraft.getInstance().font, textLabel, 12, y + 3, 0xFFEEEEEE);
    }

    static void renderSwitch(GuiGraphics gg, int x, int y, int w, int h, boolean on, boolean hovered) {
        gg.fill(x, y, x + w, y + h, 0xFF2B2B2B);
        gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, frameColor(hovered));
        gg.fill(x + 2, y + 2, x + w - 2, y + h - 2, trackColor(on));
        gg.fill(x + 2, y + 2, x + w - 2, y + 3, on ? 0xFF69A069 : 0xFF606060);

        int knobD = h - 4;
        int knobX = on ? x + w - knobD - 2 : x + 2;
        int knobY = y + 2;
        int knobTop = knobTopColor(hovered);
        int knobBottom = hovered ? 0xFF888888 : 0xFF787878;
        int knobHighlight = hovered ? 0xFFE8E8E8 : 0xFFD8D8D8;
        gg.fillGradient(knobX, knobY, knobX + knobD, knobY + knobD, knobTop, knobBottom);
        gg.fill(knobX, knobY, knobX + knobD, knobY + 1, knobHighlight);
        gg.fill(knobX, knobY, knobX + 1, knobY + knobD, knobHighlight);
        gg.fill(knobX, knobY + knobD - 1, knobX + knobD, knobY + knobD, 0xFF4A4A4A);
        gg.fill(knobX + knobD - 1, knobY, knobX + knobD, knobY + knobD, 0xFF4A4A4A);
    }

    private static int trackColor(boolean on) {
        return on ? 0xFF4F8A4F : 0xFF4A4A4A;
    }

    private static int frameColor(boolean hovered) {
        return hovered ? 0xFFA0A0A0 : 0xFF777777;
    }

    private static int knobTopColor(boolean hovered) {
        return hovered ? 0xFFD8D8D8 : 0xFFC8C8C8;
    }

    public static boolean swapMembers(KeyMapping target) {
        List<String> members = KeyWheelConfig.MEMBERS.get();
        if (members == null || members.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return false;
        if (target == null || !members.contains(target.getName())) return false;
        KeyMapping firstNonMember = null;
        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getKey().equals(target.getKey()) && !members.contains(km.getName())
                    && !KeyWheelConfig.isBanned(km.getName())) {
                firstNonMember = km; break;
            }
        }
        if (firstNonMember == null) return false;
        List<String> out = new ArrayList<>();
        for (String id : members) {
            if (id != null && !id.equals(target.getName())) out.add(id);
        }
        out.add(firstNonMember.getName());
        KeyWheelConfig.replaceMembers(out);
        KeyWheelConfig.setSwapPrimary(target.getKey().getName(), target.getName());
        return true;
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput o) {}
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean isFocused() { return false; }
    @Override public void setFocused(boolean focused) {}
}
