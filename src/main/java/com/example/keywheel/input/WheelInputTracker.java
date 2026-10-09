package com.example.keywheel.input;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class WheelInputTracker {
    private final Map<InputConstants.Key, HeldKeyState> pressed = new LinkedHashMap<>();
    private final List<HeldKeyState> released = new ArrayList<>();

    HeldKeyState press(InputConstants.Key key) {
        if (pressed.containsKey(key)) return null;
        HeldKeyState state = new HeldKeyState();
        state.physicalKey = key;
        pressed.put(key, state);
        return state;
    }

    void release(InputConstants.Key key) {
        HeldKeyState state = pressed.remove(key);
        if (state != null) released.add(state);
    }

    void discard(InputConstants.Key key) {
        pressed.remove(key);
    }

    List<HeldKeyState> drainReleased() {
        List<HeldKeyState> out = new ArrayList<>(released);
        released.clear();
        return out;
    }

    List<HeldKeyState> heldStates() {
        return new ArrayList<>(pressed.values());
    }

    Set<InputConstants.Key> pressedKeys() {
        return Set.copyOf(pressed.keySet());
    }

    void cancel() {
        pressed.clear();
        released.clear();
    }
}
