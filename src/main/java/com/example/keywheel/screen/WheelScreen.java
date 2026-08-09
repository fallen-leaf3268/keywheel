package com.example.keywheel.screen;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.input.ActionExecutor;
import com.example.keywheel.input.LongPressWatcher;
import com.example.keywheel.input.PhysicalKeyState;
import com.example.keywheel.input.WheelActionBridge;
import com.example.keywheel.widget.SwapModeWidget;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WheelScreen extends Screen {
    private static final float OUTER_R = 96f;
    private static final float INNER_R = OUTER_R * 0.2f;
    private static final double DEAD = INNER_R + 4;

    private static final int TOP_BANNER_BG = 0x80000000;

    private final List<WheelEntry> entries;
    private final List<ItemStack> entryIcons;
    private final String tapFunctionLabel;
    private final String releaseHint;
    @Nullable
    final Screen previousScreen;
    private final InputConstants.Key triggerKey;
    private int hoveredIndex = -1;
    private final SwapModeWidget swapWidget;
    private final net.minecraft.client.gui.components.Button configButton;

    public WheelScreen(List<KeyMapping> targets, @Nullable Screen previous) {
        super(Component.translatable("key.keywheel.title"));
        this.previousScreen = previous;
        this.triggerKey = targets.isEmpty() ? null : targets.get(0).getKey();
        this.entries = new ArrayList<>(targets.size());
        this.entryIcons = new ArrayList<>(targets.size());
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
            entryIcons.add(resolveIcon(km));
        }
        this.tapFunctionLabel = findTapFunctionLabel(triggerKey);
        this.releaseHint = I18n.get("key.keywheel.release_to_select");
        this.swapWidget = new SwapModeWidget(58, 56, triggerKey);
        this.configButton = Button.builder(
                Component.translatable("key.keywheel.config"),
                b -> {
                    LongPressWatcher.suppressUntilRelease();
                    WheelActionBridge.clearForceAllow();
                    if (WheelScreen.this.minecraft != null)
                        WheelScreen.this.minecraft.setScreen(new WheelConfigScreen(triggerKey));
                })
                .bounds(12, 30, 100, 18)
                .build();
        addRenderableWidget(configButton);
    }

    public boolean tickSelectOnRelease() {
        if (triggerKey == null) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (PhysicalKeyState.isPressed(window, triggerKey)) return false;
        int idx = hoveredIndex;
        if (idx >= 0 && idx < entries.size()) {
            WheelEntry e = entries.get(idx);
            Object tag = e.tag();
            if (tag instanceof KeyMapping km) {
                if (swapWidget.isOn()) {
                    SwapModeWidget.swapMembers(km);
                    return true;
                }
                ActionExecutor.runWheelAction(km);
            }
        }
        return true;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        WheelRenderer.drawWheelBackground(gg, width, height);
        configButton.render(gg, mouseX, mouseY, partialTick);
        double cx = width / 2.0;
        double cy = height / 2.0;

        hoveredIndex = WheelGeometry.indexFromMouse(mouseX, mouseY, cx, cy, entries.size(), DEAD, OUTER_R);

        double dist = Math.sqrt((mouseX - cx) * (mouseX - cx) + (mouseY - cy) * (mouseY - cy));

        int n = entries.size();
        WheelRenderer.drawWheelSectors(gg, cx, cy, OUTER_R, INNER_R, n, hoveredIndex, DEAD, dist);

        for (int i = 0; i < n; i++) {
            ItemStack icon = entryIcons.get(i);
            if (!icon.isEmpty()) {
                float midAngle = WheelGeometry.sectorStartAngle(i, n) + WheelGeometry.sectorArc(n) / 2f;
                double rad = Math.toRadians(midAngle);
                double midR = (OUTER_R + INNER_R) / 2;
                int ix = (int)(cx + Math.cos(rad) * midR - 8);
                int iy = (int)(cy + Math.sin(rad) * midR - 8);
                gg.renderFakeItem(icon, ix, iy);
            }
        }

        int hintW = font.width(releaseHint);
        int hintX = width / 2 - hintW / 2 - 4;
        int hintY = 4;
        gg.fill(hintX, hintY, hintX + hintW + 8, hintY + 14, TOP_BANNER_BG);
        gg.drawCenteredString(this.font, releaseHint, width / 2, hintY + 3, 0xFFFFFFFF);

        swapWidget.render(gg, mouseX, mouseY, partialTick);
        if (swapWidget.isOn() && tapFunctionLabel != null) {
            int tw = this.font.width(tapFunctionLabel);
            gg.drawString(this.font, tapFunctionLabel, width / 2 - tw / 2, swapWidget.getY() + 24, 0xFFEEEEEE);
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
            int idx = WheelGeometry.indexFromMouse(mouseX, mouseY, cx, cy, entries.size(), DEAD, OUTER_R);
            if (idx >= 0 && idx < entries.size()) {
                WheelEntry e = entries.get(idx);
                Object tag = e.tag();
                if (tag instanceof KeyMapping km) {
                    if (swapWidget.isOn()) {
                        if (SwapModeWidget.swapMembers(km)) {
                            onClose();
                            return super.mouseReleased(mouseX, mouseY, button);
                        }
                        return true;
                    }
                    ActionExecutor.runWheelAction(km);
                }
            }
        }
        onClose();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    static ItemStack resolveIcon(KeyMapping mapping) {
        if (mapping == null) return ItemStack.EMPTY;
        String iconId = KeyWheelConfig.getIcon(mapping.getName());
        ResourceLocation location = iconId == null ? null : ResourceLocation.tryParse(iconId);
        if (location == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(location);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static String findTapFunctionLabel(InputConstants.Key key) {
        if (key == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return null;
        Set<String> members = new HashSet<>(KeyWheelConfig.MEMBERS.get());
        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getKey().equals(key) && !members.contains(km.getName())
                    && !KeyWheelConfig.isBanned(km.getName())) {
                return I18n.get(km.getName());
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
