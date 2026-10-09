package com.example.keywheel.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.List;

public final class HeldKeyState {
    public InputConstants.Key physicalKey = null;
    public int ticksHeld = 0;
    public boolean thresholdReached = false;
    public final List<KeyMapping> memberTargets = new ArrayList<>();
    public final List<KeyMapping> nonMemberTargets = new ArrayList<>();

    public void reset() {
        physicalKey = null;
        ticksHeld = 0;
        thresholdReached = false;
        memberTargets.clear();
        nonMemberTargets.clear();
    }

    public boolean isActive() {
        return physicalKey != null;
    }

    public void copyFrom(HeldKeyState state) {
        reset();
        if (state == null) return;
        physicalKey = state.physicalKey;
        ticksHeld = state.ticksHeld;
        thresholdReached = state.thresholdReached;
        memberTargets.addAll(state.memberTargets);
        nonMemberTargets.addAll(state.nonMemberTargets);
    }
}
