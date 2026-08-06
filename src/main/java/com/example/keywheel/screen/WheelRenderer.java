package com.example.keywheel.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

public final class WheelRenderer {
    private WheelRenderer() {}

    public static final int SECTOR_EVEN = 0x80606060;
    public static final int SECTOR_ODD = 0x80808080;
    public static final int SECTOR_SELECTED = 0x80E0E0E0;
    public static final int SECTOR_LAST_ODD = 0x80A0A0A0;
    public static final int CANCEL_ZONE = 0x80000000;
    public static final int CANCEL_ZONE_HOVER = 0x80B04232;
    public static final int OUTLINE_COLOR = 0x80FFFFFF;
    public static final int SEPARATOR_COLOR = 0x60FFFFFF;
    public static final int DIMMED_BACKGROUND = 0x60000000;

    public static void drawWheelBackground(GuiGraphics gg, int screenW, int screenH) {
        gg.fill(0, 0, screenW, screenH, DIMMED_BACKGROUND);
    }

    public static void drawWheelSectors(GuiGraphics gg, double cx, double cy, double outerR, double innerR,
                                         int sectorCount, int hoveredIndex, double deadZone, double mouseDist) {
        boolean inDeadZone = mouseDist < deadZone;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        PoseStack poseStack = gg.pose();
        poseStack.pushPose();
        poseStack.translate(cx, cy, 0);
        Matrix4f pose = poseStack.last().pose();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float arc = WheelGeometry.sectorArc(sectorCount);
        boolean drawSeparators = shouldDrawSeparators(sectorCount);
        for (int i = 0; i < sectorCount; i++) {
            float startAngle = WheelGeometry.sectorStartAngle(i, sectorCount);
            int color;
            if (i == hoveredIndex) {
                color = SECTOR_SELECTED;
            } else if (sectorCount % 2 != 0 && i == sectorCount - 1) {
                color = SECTOR_LAST_ODD;
            } else {
                color = (i % 2 == 0) ? SECTOR_EVEN : SECTOR_ODD;
            }
            appendSector(buf, pose, outerR, innerR, startAngle, arc, color);
            if (drawSeparators) {
                appendSeparator(buf, pose, outerR, innerR, startAngle, SEPARATOR_COLOR);
            }
        }
        int cancelColor = inDeadZone ? CANCEL_ZONE_HOVER : OUTLINE_COLOR;
        appendRing(buf, pose, innerR, cancelColor, 2.0f);
        appendRing(buf, pose, outerR, OUTLINE_COLOR, 2.0f);

        tess.end();
        poseStack.popPose();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    static boolean shouldDrawSeparators(int sectorCount) {
        return sectorCount > 1;
    }

    private static void appendSector(BufferBuilder buf, Matrix4f pose, double outerR, double innerR,
                                     float startAngle, float arc, int rgba) {
        float a = (rgba >>> 24) & 0xFF;
        float r = (rgba >>> 16) & 0xFF;
        float g = (rgba >>> 8) & 0xFF;
        float b = rgba & 0xFF;

        int segments = Math.max(8, (int)(arc / 6.0f) + 1);
        double startRad = Math.toRadians(startAngle);
        double prevCos = Math.cos(startRad);
        double prevSin = Math.sin(startRad);
        for (int i = 1; i <= segments; i++) {
            double ang = Math.toRadians(startAngle + arc * i / segments);
            double cos = Math.cos(ang);
            double sin = Math.sin(ang);
            double ox0 = prevCos * outerR, oy0 = prevSin * outerR;
            double ix0 = prevCos * innerR, iy0 = prevSin * innerR;
            double ox1 = cos * outerR, oy1 = sin * outerR;
            double ix1 = cos * innerR, iy1 = sin * innerR;
            buf.vertex(pose, (float) ox0, (float) oy0, 0f).color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
            buf.vertex(pose, (float) ox1, (float) oy1, 0f).color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
            buf.vertex(pose, (float) ix1, (float) iy1, 0f).color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
            buf.vertex(pose, (float) ix0, (float) iy0, 0f).color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
            prevCos = cos;
            prevSin = sin;
        }
    }

    private static void appendRing(BufferBuilder buf, Matrix4f pose, double radius, int rgba, float thickness) {
        float a = (rgba >>> 24) & 0xFF;
        float r = (rgba >>> 16) & 0xFF;
        float g = (rgba >>> 8) & 0xFF;
        float b = rgba & 0xFF;
        int segments = 64;

        for (int i = 0; i < segments; i++) {
            double angle0 = (i / (double) segments) * Math.PI * 2;
            double angle1 = ((i + 1) / (double) segments) * Math.PI * 2;
            double cos0 = Math.cos(angle0), sin0 = Math.sin(angle0);
            double cos1 = Math.cos(angle1), sin1 = Math.sin(angle1);
            buf.vertex(pose, (float)(cos0 * radius), (float)(sin0 * radius), 0f)
                    .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
            buf.vertex(pose, (float)(cos1 * radius), (float)(sin1 * radius), 0f)
                    .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
            buf.vertex(pose, (float)(cos1 * (radius - thickness)), (float)(sin1 * (radius - thickness)), 0f)
                    .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
            buf.vertex(pose, (float)(cos0 * (radius - thickness)), (float)(sin0 * (radius - thickness)), 0f)
                    .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        }
    }

    private static void appendSeparator(BufferBuilder buf, Matrix4f pose, double outerR,
                                        double innerR, float angle, int rgba) {
        float a = (rgba >>> 24) & 0xFF;
        float r = (rgba >>> 16) & 0xFF;
        float g = (rgba >>> 8) & 0xFF;
        float b = rgba & 0xFF;

        double ang = Math.toRadians(angle);
        double cos = Math.cos(ang);
        double sin = Math.sin(ang);
        double hw = 0.8;
        double innerStop = innerR;
        double outerStop = outerR;
        double px = -sin * hw;
        double py = cos * hw;

        buf.vertex(pose, (float)(cos * outerStop + px), (float)(sin * outerStop + py), 0f)
                .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        buf.vertex(pose, (float)(cos * outerStop - px), (float)(sin * outerStop - py), 0f)
                .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        buf.vertex(pose, (float)(cos * innerStop - px), (float)(sin * innerStop - py), 0f)
                .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        buf.vertex(pose, (float)(cos * innerStop + px), (float)(sin * innerStop + py), 0f)
                .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
    }

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
