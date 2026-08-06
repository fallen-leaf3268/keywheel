package com.example.keywheel.screen;

public final class WheelGeometry {
    private WheelGeometry() {}

    public static final float RAD2DEG = (float)(180.0 / Math.PI);

    public static int indexFromMouse(double mouseX, double mouseY, double cx, double cy,
                                     int sectorCount, double deadZone, double outerRadius) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distSq = dx * dx + dy * dy;
        if (distSq < deadZone * deadZone) return -1;
        if (distSq >= outerRadius * outerRadius) return -1;
        if (sectorCount <= 0) return -1;

        double angle = Math.atan2(dy, dx) + Math.PI / 2.0 + (Math.PI * 2.0 / sectorCount) / 2.0;
        if (angle < 0) angle += Math.PI * 2.0;
        if (angle >= Math.PI * 2.0) angle -= Math.PI * 2.0;
        int idx = (int)Math.floor(angle / (Math.PI * 2.0) * sectorCount);
        if (idx >= sectorCount) idx = 0;
        return idx;
    }

    public static float sectorStartAngle(int index, int sectorCount) {
        return -90f - 360f / sectorCount / 2f + (360f / sectorCount) * index;
    }

    public static float sectorArc(int sectorCount) {
        return 360f / sectorCount;
    }
}
