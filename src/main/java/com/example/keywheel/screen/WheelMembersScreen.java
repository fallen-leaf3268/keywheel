package com.example.keywheel.screen;

import com.example.keywheel.KeyWheel;
import com.example.keywheel.config.KeyWheelConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WheelMembersScreen extends Screen {
    private final @Nullable Screen previous;

    private static final int TITLE_H = 22;
    private static final int ROW_H = 22;
    private static final int CHECK_SIZE = 20;
    private static final int LIST_TOP = 40;
    private static final int LIST_BOTTOM_OFFSET = 56;
    private static final int ROW_LEFT = 12;

    private record Row(KeyMapping km, int absY) {}

    private final List<Row> rows = new ArrayList<>();
    private final List<Checkbox> boxes = new ArrayList<>();
    private int groupTitleY = 0;
    private String groupTitle = "";
    private int scroll = 0;
    private int totalContentHeight = 0;

    public WheelMembersScreen(@Nullable Screen previous) {
        super(Component.literal("Wheel Members"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        KeyMapping[] all = Minecraft.getInstance().options.keyMappings;
        Set<String> enabledIds = new HashSet<>();
        for (String id : KeyWheelConfig.MEMBERS.get()) {
            if (id != null) enabledIds.add(id);
        }

        Map<InputConstants.Key, List<KeyMapping>> byPhysicalKey = new HashMap<>();
        for (KeyMapping km : all) {
            if (km.getCategory().equals("key.categories.keywheel")) continue;
            byPhysicalKey.computeIfAbsent(km.getKey(), k -> new ArrayList<>()).add(km);
        }

        List<Map.Entry<InputConstants.Key, List<KeyMapping>>> conflictGroups = new ArrayList<>();
        for (Map.Entry<InputConstants.Key, List<KeyMapping>> e : byPhysicalKey.entrySet()) {
            if (e.getValue().size() >= 2) {
                conflictGroups.add(e);
            }
        }
        conflictGroups.sort(Comparator.comparing(e -> e.getKey().getName()));

        int y = LIST_TOP;
        for (Map.Entry<InputConstants.Key, List<KeyMapping>> g : conflictGroups) {
            y += TITLE_H;
            for (KeyMapping km : g.getValue()) {
                km.getName();
                int checkY = y - scroll;
                Checkbox cb = new Checkbox(
                        ROW_LEFT + 16,
                        checkY,
                        CHECK_SIZE, CHECK_SIZE,
                        Component.empty(),
                        enabledIds.contains(km.getName()),
                        true
                );
                boxes.add(cb);
                rows.add(new Row(km, y));
                y += ROW_H;
            }
            y += 4;
        }
        totalContentHeight = y - LIST_TOP;

        for (Checkbox cb : boxes) {
            addRenderableWidget(cb);
        }

        addRenderableWidget(Button.builder(
                Component.literal("Done"),
                b -> saveAndClose()
        ).bounds(width - 110, height - 30, 96, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                b -> onClose()
        ).bounds(width - 210, height - 30, 96, 20).build());
    }

    private void saveAndClose() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).selected()) {
                out.add(rows.get(i).km().getName());
            }
        }
        KeyWheelConfig.MEMBERS.set(out);
        KeyWheelConfig.MEMBERS.save();
        if (previous instanceof KeyBindsScreen) {
            onClose();
        } else if (previous != null) {
            Minecraft.getInstance().setScreen(previous);
        } else {
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float pt) {
        renderBackground(gg);
        gg.drawCenteredString(this.font, "Wheel Members", width / 2, 10, 0xFFFFFFFF);
        gg.drawCenteredString(this.font,
                "Only keys with conflicts (same physical key bound multiple times) are shown.",
                width / 2, 24, 0xFFBFBFBF);

        if (rows.isEmpty()) {
            gg.drawCenteredString(this.font, "No conflicts found - nothing to set up.",
                    width / 2, height / 2, 0xFF888888);
            super.render(gg, mouseX, mouseY, pt);
            return;
        }

        int listBottom = height - LIST_BOTTOM_OFFSET;

        gg.drawString(this.font, "Wheel", ROW_LEFT + 20, LIST_TOP - 12, 0xFFD0D0D0);
        gg.drawString(this.font, "KeyBinding", ROW_LEFT + 80, LIST_TOP - 12, 0xFFD0D0D0);

        Map<InputConstants.Key, List<KeyMapping>> grouped = new HashMap<>();
        for (Row r : rows) {
            grouped.computeIfAbsent(r.km().getKey(), k -> new ArrayList<>()).add(r.km());
        }

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int y = r.absY() - scroll;
            if (y < LIST_TOP - 4 || y > listBottom - ROW_H) continue;

            InputConstants.Key pk = r.km().getKey();
            List<KeyMapping> groupMates = grouped.getOrDefault(pk, List.of());
            if (groupMates.size() >= 2 && groupMates.get(0) == r.km()) {
                gg.drawString(this.font, "[ " + pk.getName() + " ]",
                        ROW_LEFT + 4, y - TITLE_H + 4, 0xFFFFD060);
                gg.fill(ROW_LEFT, y - 4, width - 12, y - 3, 0x40FFD060);
            }

            gg.drawString(this.font, r.km().getName(),
                    ROW_LEFT + 80, y + 6, 0xFFEEEEEE);
            gg.drawString(this.font, "(" + r.km().getCategory() + ")",
                    ROW_LEFT + 80 + this.font.width(r.km().getName()) + 6, y + 6, 0xFF888888);
        }

        super.render(gg, mouseX, mouseY, pt);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int listBottom = height - LIST_BOTTOM_OFFSET;
        int visibleH = listBottom - LIST_TOP;
        int max = Math.max(0, totalContentHeight - visibleH);
        scroll = (int) Math.max(0, Math.min(max, scroll - delta * 12));
        for (int i = 0; i < boxes.size(); i++) {
            boxes.get(i).setY(rows.get(i).absY() - scroll);
        }
        return true;
    }

    @Override
    public void onClose() {
        if (previous != null) {
            Minecraft.getInstance().setScreen(previous);
        } else {
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
