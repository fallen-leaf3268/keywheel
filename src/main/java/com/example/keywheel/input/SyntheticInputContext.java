package com.example.keywheel.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class SyntheticInputContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private SyntheticInputContext() {}

    public static AutoCloseable begin(KeyMapping target, InputConstants.Key key) {
        if (target == null || key == null || CURRENT.get() != null) {
            throw new IllegalStateException("Invalid synthetic input scope");
        }
        State state = new State(target, key);
        CURRENT.set(state);
        return new Scope(state);
    }

    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    public static boolean allows(KeyMapping mapping) {
        State state = CURRENT.get();
        return state == null || state.target() == mapping;
    }

    public static boolean shouldMask(KeyMapping mapping, InputConstants.Key actualKey) {
        State state = CURRENT.get();
        return state != null
                && state.target() != mapping
                && state.key().equals(actualKey);
    }

    private record State(KeyMapping target, InputConstants.Key key) {}

    private static final class Scope implements AutoCloseable {
        private final State state;
        private boolean closed;

        private Scope(State state) {
            this.state = state;
        }

        @Override
        public void close() {
            if (closed) return;
            if (CURRENT.get() != state) throw new IllegalStateException("Synthetic input scope mismatch");
            CURRENT.remove();
            closed = true;
        }
    }
}
