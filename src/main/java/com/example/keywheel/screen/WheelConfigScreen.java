package com.example.keywheel.screen;

import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.input.LongPressWatcher;
import com.example.keywheel.mixin.ScreenRenderablesAccessor;
import com.example.keywheel.widget.FunctionLockWidget;
import com.example.keywheel.widget.FunctionHoldWidget;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WheelConfigScreen extends Screen {
    private static final int COLS = 9;
    private static final int CELL = 18;
    private static final int MARGIN = 6;
    private static final int NAV_H = CELL;
    private static final int SEARCH_H = 20;
    private static final float OUTER_R = 96f;
    private static final float INNER_R = OUTER_R * 0.2f;
    private static final double DEAD = 24;

    private final InputConstants.Key physicalKey;
    private final List<KeyMapping> allMappings = new ArrayList<>();
    private final List<Item> allItems = new ArrayList<>();
    private final Set<String> memberIds = new HashSet<>();
    private final Set<String> bannedIds = new HashSet<>();
    private final List<KeyMapping> cachedWheelMembers = new ArrayList<>();
    private final Map<Item, String> itemSearchText = new IdentityHashMap<>();
    private final Map<Item, ItemStack> itemStacks = new IdentityHashMap<>();
    private final Map<String, ItemStack> wheelIcons = new HashMap<>();
    private List<Item> filteredItems = new ArrayList<>();
    private int page = 0;
    private String selected = null;
    private EditBox searchBox;
    private int hoveredConfigSector = -1;

    public WheelConfigScreen(InputConstants.Key physicalKey) {
        super(Component.translatable("key.keywheel.config_title"));
        this.physicalKey = physicalKey;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) for (KeyMapping km : mc.options.keyMappings) if (km.getKey().equals(physicalKey)) allMappings.add(km);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            allItems.add(item);
            String id = BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase(Locale.ROOT);
            String label = item.getDescription().getString().toLowerCase(Locale.ROOT);
            itemSearchText.put(item, label + '\n' + id);
            itemStacks.put(item, new ItemStack(item));
        }
        allItems.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        filteredItems = new ArrayList<>(allItems);
        refreshConfigCache();
    }

    private int panelX() { return width - COLS * CELL - MARGIN * 2; }
    private int panelW() { return COLS * CELL + MARGIN * 2; }
    private int rows() { return Math.max(4, (height - 100) / CELL); }
    private int totalPages() { return Math.max(1, (filteredItems.size() + COLS * rows() - 1) / (COLS * rows())); }
    private int countPerPage() { return COLS * rows(); }

    private boolean isInWheel(KeyMapping km) {
        return memberIds.contains(km.getName());
    }

    private List<KeyMapping> wheelMembers() {
        return cachedWheelMembers;
    }

    private void refreshConfigCache() {
        List<String> members = KeyWheelConfig.MEMBERS.get();
        bannedIds.clear();
        for (String id : KeyWheelConfig.BANNED.get()) if (id != null) bannedIds.add(id);

        List<String> sanitized = new ArrayList<>();
        boolean changed = false;
        for (String id : members) {
            if (id != null && bannedIds.contains(id)) {
                changed = true;
            } else {
                sanitized.add(id);
            }
        }
        if (changed) {
            KeyWheelConfig.replaceMembers(sanitized);
            members = sanitized;
        }

        memberIds.clear();
        for (String id : members) if (id != null) memberIds.add(id);
        cachedWheelMembers.clear();
        wheelIcons.clear();
        for (KeyMapping km : allMappings) {
            if (bannedIds.contains(km.getName()) || !memberIds.contains(km.getName())) continue;
            cachedWheelMembers.add(km);
            wheelIcons.put(km.getName(), WheelScreen.resolveIcon(km));
        }
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 100, height - 26, 200, 20).build());

        if (selected != null) {
            String kmName = selected;
            boolean fnInW = memberIds.contains(kmName);
            if (fnInW) {
                addRenderableWidget(Button.builder(Component.translatable("key.keywheel.function_lock"), b ->
                        KeyWheelConfig.setLocked(kmName, !KeyWheelConfig.isLocked(kmName)))
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                                "key.keywheel.function_lock_tooltip")))
                        .bounds(10, 56, 80, 18).build());
                addRenderableWidget(new FunctionLockWidget(92, 58, kmName));
                addRenderableWidget(Button.builder(Component.translatable("key.keywheel.function_hold"), b ->
                        KeyWheelConfig.setHoldEnabled(kmName, !KeyWheelConfig.isHoldEnabled(kmName)))
                        .bounds(10, 78, 80, 18).build());
                addRenderableWidget(new FunctionHoldWidget(92, 80, kmName));
                addRenderableWidget(Button.builder(Component.translatable("key.keywheel.remove_from_wheel"), b -> {
                    KeyWheelConfig.setMember(kmName, false);
                    refreshConfigCache();
                    rebuildWidgets();
                }).bounds(10, 100, 80, 18).build());
            }
            addRenderableWidget(Button.builder(Component.translatable("key.keywheel.clear_icon"), b -> {
                KeyWheelConfig.setIcon(kmName, null);
                refreshConfigCache();
                rebuildWidgets();
            }).bounds(10, fnInW ? 122 : 56, 80, 18).build());
            int pl = panelX(), pw = panelW(), navY = 44 - NAV_H - 2;
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { if (page > 0) { page--; rebuildWidgets(); } }).bounds(pl + MARGIN, navY, CELL, NAV_H).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { if (page < totalPages() - 1) { page++; rebuildWidgets(); } }).bounds(pl + pw - MARGIN - CELL, navY, CELL, NAV_H).build());
            int srY = 44 + rows() * CELL + MARGIN;
            searchBox = new EditBox(font, pl + MARGIN, srY, pw - MARGIN * 2, SEARCH_H, Component.empty());
            searchBox.setMaxLength(128); searchBox.setResponder(s -> { page = 0; filterItems(); });
            addRenderableWidget(searchBox);
        }

        List<KeyMapping> nonWheel = new ArrayList<>();
        for (KeyMapping km : allMappings) if (!isInWheel(km)) nonWheel.add(km);
        int by = height - 30 - nonWheel.size() * 20;
        for (int i = 0; i < nonWheel.size(); i++) {
            KeyMapping nkm = nonWheel.get(i);
            String bl = net.minecraft.client.resources.language.I18n.get(nkm.getName());
            if (bl.length() > 12) bl = bl.substring(0, 11) + "..";
            boolean fnIb = bannedIds.contains(nkm.getName());
            final boolean fIb = fnIb;
            addRenderableWidget(Button.builder(Component.literal(fIb ? "🚫" : "☐"), b -> {
                KeyWheelConfig.setBanned(nkm.getName(), !fIb);
                refreshConfigCache();
                rebuildWidgets();
            }).bounds(10, by + i * 20, 22, 18)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                    fIb ? "key.keywheel.tooltip_lock" : "key.keywheel.tooltip_unlock")))
            .build());
            addRenderableWidget(Button.builder(Component.literal(bl), b -> {
                if (fIb) return;
                KeyWheelConfig.setMember(nkm.getName(), true);
                refreshConfigCache();
                rebuildWidgets();
            }).bounds(34, by + i * 20, 138, 18)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                    fIb ? "key.keywheel.tooltip_unlock_then_add" : "key.keywheel.tooltip_add_to_wheel")))
            .build());
        }
    }

    private void filterItems() {
        String text = searchBox != null ? searchBox.getValue() : "";
        filteredItems.clear();
        if (text.isEmpty()) { filteredItems.addAll(allItems); return; }
        String lower = text.toLowerCase(Locale.ROOT);
        for (Item item : allItems) {
            if (itemSearchText.get(item).contains(lower)) filteredItems.add(item);
        }
        if (page >= totalPages()) page = totalPages() - 1;
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (selected != null) {
            if (delta < 0 && page < totalPages() - 1) page++; else if (delta > 0 && page > 0) page--;
            rebuildWidgets(); return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        double cx = width / 2.0, cy = height / 2.0;
        List<KeyMapping> members = wheelMembers();
        int n = members.size();
        if (n > 0) {
            double dist = Math.sqrt((mx-cx)*(mx-cx) + (my-cy)*(my-cy));
            int index = WheelGeometry.indexFromMouse(mx, my, cx, cy, n, DEAD, OUTER_R);
            if (dist >= INNER_R && dist <= OUTER_R && index >= 0 && index < n) {
                String nm = members.get(index).getName();
                if (shouldSelectSector(button) && !nm.equals(selected)) {
                    selected = nm;
                    page = 0;
                    filteredItems = new ArrayList<>(allItems);
                    rebuildWidgets();
                }
                if (shouldSelectSector(button)) return true;
            }
        }
        if (selected != null && button == 0) {
            int pl = panelX(), gy = 44, r = rows(), start = page * countPerPage();
            for (int row = 0; row < r; row++) {
                for (int col = 0; col < COLS; col++) {
                    int idx = start + row * COLS + col;
                    if (idx >= filteredItems.size()) break;
                    int x = pl + MARGIN + col * CELL, y = gy + row * CELL;
                    if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) {
                        KeyWheelConfig.setIcon(selected, BuiltInRegistries.ITEM.getKey(filteredItems.get(idx)).toString());
                        refreshConfigCache();
                        rebuildWidgets();
                        return true;
                    }
                }
            }
        }
        boolean handled = super.mouseClicked(mx, my, button);
        if (shouldClearButtonFocus(handled, getFocused() instanceof Button)) {
            setFocused(null);
        }
        return handled;
    }

    @Override
    public void render(GuiGraphics gg, int mx, int my, float p) {
        WheelRenderer.drawWheelBackground(gg, width, height);
        for (Renderable r : ((ScreenRenderablesAccessor) this).keywheel$getRenderables()) {
            r.render(gg, mx, my, p);
        }
        gg.drawCenteredString(font, Component.translatable("key.keywheel.config_title"), width / 2, 8, 0xFFFFFFFF);
        if (selected != null) {
            String label = net.minecraft.client.resources.language.I18n.get(selected);
            if (label.length() > 16) label = label.substring(0, 15) + "..";
            gg.drawString(font, label, 10, 40, 0xFFFFFFFF);
        }
        List<KeyMapping> members = wheelMembers();
        int n = members.size();
        double cx = width / 2.0, cy = height / 2.0;
        hoveredConfigSector = (n > 0) ? WheelGeometry.indexFromMouse(mx, my, cx, cy, n, DEAD, OUTER_R) : -1;
        if (n > 0) {
            double dist = Math.sqrt((mx-cx)*(mx-cx) + (my-cy)*(my-cy));
            WheelRenderer.drawWheelSectors(gg, cx, cy, OUTER_R, INNER_R, n, hoveredConfigSector, DEAD, dist);
            if (hoveredConfigSector >= 0) {
                gg.drawCenteredString(font, net.minecraft.client.resources.language.I18n.get(members.get(hoveredConfigSector).getName()),
                        (int)cx, (int)(cy - OUTER_R - 14), 0xFFEEEEEE);
            }
            for (int i = 0; i < n; i++) {
                KeyMapping km = members.get(i);
                ItemStack icon = wheelIcons.get(km.getName());
                if (icon != null && !icon.isEmpty()) {
                    float sa = WheelGeometry.sectorStartAngle(i, n), arc = WheelGeometry.sectorArc(n);
                    float mid = sa + arc / 2f;
                    double rad = Math.toRadians(mid), midR = (OUTER_R + INNER_R) / 2;
                    gg.renderFakeItem(icon, (int)(cx + Math.cos(rad)*midR - 8), (int)(cy + Math.sin(rad)*midR - 8));
                }
            }
        } else {
            gg.drawCenteredString(font, Component.translatable("key.keywheel.empty_config_hint"),
                    (int)cx, (int)(cy - 10), 0xFF888888);
        }
        if (selected != null) {
            int pl = panelX(), pw = panelW(), r = rows(), gy = 44;
            String pi = (page + 1) + "/" + totalPages();
            int piw = font.width(pi), navCY = 44 - NAV_H - 2 + (NAV_H - 8) / 2;
            gg.drawString(font, pi, pl + (pw - piw) / 2, navCY, 0xFFFFFFFF);
            int s = page * countPerPage();
            for (int row = 0; row < r; row++)
                for (int col = 0; col < COLS; col++) {
                    int idx = s + row * COLS + col; if (idx >= filteredItems.size()) break;
                    int x = pl + MARGIN + col * CELL, y = gy + row * CELL;
                    ItemStack stack = itemStacks.get(filteredItems.get(idx));
                    if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) { gg.fill(x, y, x + CELL, y + CELL, 0x80FFFFFF); gg.renderTooltip(font, stack, mx, my); }
                    gg.renderFakeItem(stack, x, y);
                }
        }
    }

    @Override public void onClose() {
        if (minecraft != null) {
            LongPressWatcher.STATE.memberTargets.clear(); LongPressWatcher.STATE.nonMemberTargets.clear();
            for (KeyMapping km : allMappings) {
                if (memberIds.contains(km.getName())) LongPressWatcher.STATE.memberTargets.add(km);
                else LongPressWatcher.STATE.nonMemberTargets.add(km);
            }
            minecraft.setScreen(new WheelScreen(LongPressWatcher.STATE.memberTargets,
                    minecraft.screen instanceof WheelScreen ws ? ws.previousScreen : null));
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    static boolean shouldSelectSector(int button) { return button == 0; }

    static boolean shouldClearButtonFocus(boolean handled, boolean buttonFocused) {
        return handled && buttonFocused;
    }
}
