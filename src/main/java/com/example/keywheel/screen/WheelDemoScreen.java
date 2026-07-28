package com.example.keywheel.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class WheelDemoScreen extends Screen {
    private static final int COUNT = 8;
    private static final float OUTER_R = 96f;
    private static final float INNER_R = OUTER_R * 0.2f;
    private static final double DEAD = INNER_R + 4;

    private final List<WheelEntry> entries = new ArrayList<>();

    public WheelDemoScreen() {
        super(Component.literal("WheelDemoScreen"));
        int base = WheelRenderer.rgba(255, 255, 255, 80);
        for (int i = 0; i < COUNT; i++) {
            entries.add(new WheelEntry(base, Component.literal("Slot " + i), null));
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);

        double cx = width / 2.0;
        double cy = height / 2.0;

        int hovered = WheelGeometry.indexFromMouse(mouseX, mouseY, cx, cy, entries.size(), DEAD);

        for (int i = 0; i < entries.size(); i++) {
            float startAngle = WheelGeometry.sectorStartAngle(i, entries.size());
            float arc = WheelGeometry.sectorArc(entries.size());
            int col = entries.get(i).color();
            int alpha = i == hovered ? 200 : 120;
            int r = (col >>> 16) & 0xFF;
            int g = (col >>> 8) & 0xFF;
            int b = col & 0xFF;
            int colorWithAlpha = WheelRenderer.rgba(r, g, b, alpha);
            WheelRenderer.renderSector(gg, cx, cy, OUTER_R, INNER_R, startAngle, arc, colorWithAlpha);
        }

        WheelRenderer.renderOutlineRing(gg, cx, cy, OUTER_R,
                WheelRenderer.rgba(255, 255, 255, 200), 2.0f);
        WheelRenderer.renderOutlineRing(gg, cx, cy, INNER_R,
                WheelRenderer.rgba(255, 255, 255, 120), 2.0f);

        gg.drawCenteredString(this.font, "Wheel Demo (hovered=" + hovered + ")", width / 2, 8, 0xFFFFFFFF);
        if (hovered >= 0) {
            WheelEntry e = entries.get(hovered);
            gg.drawCenteredString(this.font, e.label().getString(), width / 2, height - 20, 0xFFEEEEEE);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
