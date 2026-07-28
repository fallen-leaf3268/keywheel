package com.example.keywheel.screen;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.input.ActionExecutor;
import com.example.keywheel.input.LongPressWatcher;
import com.example.keywheel.input.WheelActionBridge;
import com.example.keywheel.widget.SwapModeWidget;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class WheelScreen extends Screen {
    private static final float OUTER_R = 96f;
    private static final float INNER_R = OUTER_R * 0.2f;
    private static final double DEAD = INNER_R + 4;

    private final List<WheelEntry> entries;
    @Nullable
    private final Screen previousScreen;
    private final InputConstants.Key triggerKey;
    private int hoveredIndex = -1;
    private final SwapModeWidget swapWidget;

    public WheelScreen(List<KeyMapping> targets, @Nullable Screen previous) {
        super(Component.translatable("key.keywheel.title"));
        this.previousScreen = previous;
        this.entries = new ArrayList<>(targets.size());
        int baseCol = WheelRenderer.rgba(255, 255, 255, 80);
        for (int i = 0; i < targets.size(); i++) {
            KeyMapping km = targets.get(i);
            Component label;
            if (km == null) {
                label = Component.literal("<empty>");
            } else {
                String key = km.getName();
                String translated = net.minecraft.client.resources.language.I18n.get(key);
                if (translated != null && !translated.equals(key)) {
                    label = Component.literal(translated);
                } else {
                    label = Component.literal(key);
                }
            }
            entries.add(new WheelEntry(baseCol, label, km));
        }
        this.triggerKey = targets.isEmpty() ? null : targets.get(0).getKey();
        this.swapWidget = new SwapModeWidget(12, 30);
    }

    public boolean tickSelectOnRelease() {
        if (triggerKey == null) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (triggerKey.getType() == InputConstants.Type.KEYSYM) {
            if (GLFW.glfwGetKey(window, triggerKey.getValue()) == GLFW.GLFW_PRESS) {
                return false;
            }
        }
        int idx = hoveredIndex;
        if (idx >= 0 && idx < entries.size()) {
            WheelEntry e = entries.get(idx);
            Object tag = e.tag();
            if (tag instanceof KeyMapping km) {
                if (swapWidget.isOn()) {
                    if (SwapModeWidget.swapMembers(e.label())) {
                        onClose();
                        return true;
                    }
                    return true;
                }
                ActionExecutor.run(km);
            }
        }
        onClose();
        return true;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);
        double cx = width / 2.0;
        double cy = height / 2.0;

        hoveredIndex = WheelGeometry.indexFromMouse(mouseX, mouseY, cx, cy, entries.size(), DEAD);

        int sepColor = WheelRenderer.rgba(200, 200, 200, 120);
        for (int i = 0; i < entries.size(); i++) {
            float startAngle = WheelGeometry.sectorStartAngle(i, entries.size());
            float arc = WheelGeometry.sectorArc(entries.size());
            int alpha = i == hoveredIndex ? 180 : 100;
            int color = WheelRenderer.rgba(200, 200, 200, alpha);
            WheelRenderer.renderSector(gg, cx, cy, OUTER_R, INNER_R, startAngle, arc, color);
            WheelRenderer.renderSeparator(gg, cx, cy, OUTER_R, INNER_R, startAngle, sepColor);
        }

        WheelRenderer.renderOutlineRing(gg, cx, cy, OUTER_R,
                WheelRenderer.rgba(255, 255, 255, 160), 2.0f);
        WheelRenderer.renderOutlineRing(gg, cx, cy, INNER_R,
                WheelRenderer.rgba(255, 255, 255, 160), 2.0f);

        gg.drawCenteredString(this.font,
                net.minecraft.client.resources.language.I18n.get("key.keywheel.release_to_select"),
                width / 2, 8, 0xFFFFFFFF);
        swapWidget.render(gg, mouseX, mouseY, partialTick);
        gg.drawString(this.font,
                net.minecraft.client.resources.language.I18n.get("key.keywheel.swap_mode"),
                swapWidget.getX() + 20, swapWidget.getY() + 4, 0xFFCCCCCC);
        if (swapWidget.isOn() && !entries.isEmpty()) {
            String tapLabel = getTapFunctionLabel();
            if (tapLabel != null) {
                int tw = this.font.width(tapLabel);
                gg.drawString(this.font, tapLabel, width / 2 - tw / 2, swapWidget.getY() + 24, 0xFFEEEEEE);
            }
        }
        if (hoveredIndex >= 0) {
            WheelEntry e = entries.get(hoveredIndex);
            gg.drawCenteredString(this.font, e.label().getString(), width / 2, (int)(cy - OUTER_R - 14), 0xFFEEEEEE);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (swapWidget.isMouseOver(mouseX, mouseY)) {
            return swapWidget.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (swapWidget.isMouseOver(mouseX, mouseY)) return true;
        if (button == 0) {
            LongPressWatcher.suppressUntilRelease();
            double cx = width / 2.0;
            double cy = height / 2.0;
            int idx = WheelGeometry.indexFromMouse(mouseX, mouseY, cx, cy, entries.size(), DEAD);
            if (idx >= 0 && idx < entries.size()) {
                WheelEntry e = entries.get(idx);
                Object tag = e.tag();
                if (tag instanceof KeyMapping km) {
                    if (swapWidget.isOn()) {
                        if (SwapModeWidget.swapMembers(e.label())) {
                            onClose();
                            return super.mouseReleased(mouseX, mouseY, button);
                        }
                        return true;
                    }
                    ActionExecutor.run(km);
                }
            }
        }
        onClose();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private String getTapFunctionLabel() {
        if (entries.isEmpty()) return null;
        InputConstants.Key key = entries.get(0).tag() instanceof KeyMapping km ? km.getKey() : null;
        if (key == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return null;
        java.util.List<String> members = com.example.keywheel.config.KeyWheelConfig.MEMBERS.get();
        if (members == null) members = java.util.List.of();
        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getKey().equals(key) && !members.contains(km.getName())) {
                return net.minecraft.client.resources.language.I18n.get(km.getName());
            }
        }
        return null;
    }

    @Override
    public void onClose() {
        LongPressWatcher.STATE.reset();
        WheelActionBridge.clearForceAllow();
        if (minecraft != null) {
            if (previousScreen != null) {
                minecraft.setScreen(previousScreen);
            } else {
                minecraft.setScreen(null);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
