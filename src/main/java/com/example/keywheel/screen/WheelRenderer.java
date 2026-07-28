package com.example.keywheel.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;

public final class WheelRenderer {
    private static final float INNER_RATIO = 0.45f;

    private WheelRenderer() {}

    public static void renderSector(GuiGraphics gg, double cx, double cy, double outerR,
                                     double innerR, float startAngle, float arc, int rgba) {
        float a = (rgba >>> 24) & 0xFF;
        float r = (rgba >>> 16) & 0xFF;
        float g = (rgba >>> 8) & 0xFF;
        float b = rgba & 0xFF;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        PoseStack pose = gg.pose();

        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        pose.pushPose();
        pose.translate(cx, cy, 0);

        int segments = Math.max(8, (int)(arc / 6.0f) + 1);
        for (int i = 0; i <= segments; i++) {
            double ang = Math.toRadians(startAngle + arc * i / segments);
            double cos = Math.cos(ang);
            double sin = Math.sin(ang);
            buf.vertex(pose.last().pose(), (float)(cos * outerR), (float)(sin * outerR), 0f)
                    .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
            buf.vertex(pose.last().pose(), (float)(cos * innerR), (float)(sin * innerR), 0f)
                    .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        }
        tess.end();
        pose.popPose();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void renderOutlineRing(GuiGraphics gg, double cx, double cy, double radius, int rgba, float thickness) {
        float a = (rgba >>> 24) & 0xFF;
        float r = (rgba >>> 16) & 0xFF;
        float g = (rgba >>> 8) & 0xFF;
        float b = rgba & 0xFF;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        PoseStack pose = gg.pose();

        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        pose.pushPose();
        pose.translate(cx, cy, 0);
        int segments = 64;

        for (int i = 0; i <= segments; i++) {
            double ang = (i / (double) segments) * Math.PI * 2;
            double cos = Math.cos(ang);
            double sin = Math.sin(ang);
            buf.vertex(pose.last().pose(), (float)(cos * radius), (float)(sin * radius), 0f)
                    .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
            buf.vertex(pose.last().pose(), (float)(cos * (radius - thickness)), (float)(sin * (radius - thickness)), 0f)
                    .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        }
        tess.end();
        pose.popPose();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void renderSeparator(GuiGraphics gg, double cx, double cy, double outerR,
                                        double innerR, float angle, int rgba) {
        float a = (rgba >>> 24) & 0xFF;
        float r = (rgba >>> 16) & 0xFF;
        float g = (rgba >>> 8) & 0xFF;
        float b = rgba & 0xFF;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        PoseStack pose = gg.pose();

        double ang = Math.toRadians(angle);
        double cos = Math.cos(ang);
        double sin = Math.sin(ang);
        double hw = 0.8;
        // perpendicular offset: rotate by 90 degrees
        double px = -sin * hw;
        double py = cos * hw;

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        pose.pushPose();
        pose.translate(cx, cy, 0);
        buf.vertex(pose.last().pose(), (float)(cos * outerR + px), (float)(sin * outerR + py), 0f)
                .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        buf.vertex(pose.last().pose(), (float)(cos * outerR - px), (float)(sin * outerR - py), 0f)
                .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        buf.vertex(pose.last().pose(), (float)(cos * innerR - px), (float)(sin * innerR - py), 0f)
                .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        buf.vertex(pose.last().pose(), (float)(cos * innerR + px), (float)(sin * innerR + py), 0f)
                .color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        tess.end();
        pose.popPose();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
