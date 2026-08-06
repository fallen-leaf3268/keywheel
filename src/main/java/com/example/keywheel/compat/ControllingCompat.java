package com.example.keywheel.compat;

import com.example.keywheel.KeyWheel;
import com.example.keywheel.config.KeyWheelConfig;
import com.example.keywheel.mixin.KeyEntryAccessor;
import com.example.keywheel.screen.WheelConflictIndex;
import com.example.keywheel.widget.WheelToggleWidget;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ControllingCompat {

    private static final Map<String, WheelToggleWidget> WIDGETS = new HashMap<>();
    private static boolean registered = false;
    private static boolean renderFailureLogged = false;
    private static boolean listenerFailureLogged = false;

    public static void init() {
        if (registered) return;
        registered = true;
        try {
            Class<?> cls = Class.forName("com.blamejared.controlling.api.events.KeyEntryRenderEvent");
            Method getEntry = cls.getMethod("getEntry");
            Method getGuiGraphics = cls.getMethod("getGuiGraphics");
            Method getX = cls.getMethod("getX");
            Method getY = cls.getMethod("getY");
            Method getMouseX = cls.getMethod("getMouseX");
            Method getMouseY = cls.getMethod("getMouseY");
            Method getPartialTicks = cls.getMethod("getPartialTicks");

            Consumer<Event> renderHandler = event -> {
                try {
                    Object entry = getEntry.invoke(event);
                    KeyMapping key = getKey(entry);
                    if (key == null) return;
                    if (!WheelConflictIndex.contains(key.getKey()) || key.isUnbound()) return;
                    WheelToggleWidget w = getWidget(key);
                    w.setX((int) getX.invoke(event) + 75);
                    w.setY((int) getY.invoke(event) + 5);
                    w.render((net.minecraft.client.gui.GuiGraphics) getGuiGraphics.invoke(event),
                            (int) getMouseX.invoke(event), (int) getMouseY.invoke(event),
                            (float) getPartialTicks.invoke(event));
                } catch (Exception e) {
                    if (!renderFailureLogged) {
                        renderFailureLogged = true;
                        KeyWheel.LOG.warn("Controlling render compatibility failed", e);
                    }
                }
            };
            IEventBus bus = MinecraftForge.EVENT_BUS;
            bus.addListener(EventPriority.NORMAL, false, (Class) cls, (Consumer) renderHandler);

            Class<?> listenersCls = Class.forName("com.blamejared.controlling.api.events.KeyEntryListenersEvent");
            Method lGetEntry = listenersCls.getMethod("getEntry");
            Method lGetListeners = listenersCls.getMethod("getListeners");

            Consumer<Event> listenersHandler = event -> {
                try {
                    Object entry = lGetEntry.invoke(event);
                    KeyMapping key = getKey(entry);
                    if (key == null) return;
                    if (!WheelConflictIndex.contains(key.getKey()) || key.isUnbound()) return;
                    ((List) lGetListeners.invoke(event)).add(getWidget(key));
                } catch (Exception e) {
                    if (!listenerFailureLogged) {
                        listenerFailureLogged = true;
                        KeyWheel.LOG.warn("Controlling listener compatibility failed", e);
                    }
                }
            };
            bus.addListener(EventPriority.NORMAL, false, (Class) listenersCls, (Consumer) listenersHandler);
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            KeyWheel.LOG.warn("Controlling compatibility initialization failed", e);
        }
    }

    private static Field keyField;

    private static KeyMapping getKey(Object entry) {
        if (entry instanceof KeyEntryAccessor a) return a.getKey();
        try {
            if (keyField == null) {
                keyField = entry.getClass().getDeclaredField("key");
                keyField.setAccessible(true);
            }
            Object obj = keyField.get(entry);
            if (obj instanceof KeyMapping km) return km;
        } catch (Exception ignored) {}
        return null;
    }

    private static WheelToggleWidget getWidget(KeyMapping key) {
        WheelToggleWidget widget = WIDGETS.computeIfAbsent(key.getName(), k -> {
            boolean on = isEnabled(key);
            WheelToggleWidget w = new WheelToggleWidget(0, 0, on);
            w.onToggle = () -> persist(key, w);
            return w;
        });
        widget.on = isEnabled(key);
        return widget;
    }

    private static boolean isEnabled(KeyMapping key) {
        return KeyWheelConfig.isMember(key.getName());
    }

    private static void persist(KeyMapping key, WheelToggleWidget widget) {
        KeyWheelConfig.setMember(key.getName(), widget.on);
    }
}
