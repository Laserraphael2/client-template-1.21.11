package mod.client.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class RenderUtils {
    public static final int TEXT_COLOR = 0xFFFFFFFF;
    public static final int SEPARATOR_COLOR = 0xFF2A2A2A;
    public static final int MUTED_COLOR = 0xFF555555;
    public static final int SUCCESS_COLOR = 0xFF43B581;
    public static final int DANGER_COLOR = 0xFFE53935;
    public static final int HOVER_COLOR = 0xFF222222;

    private static final int GLASS_LIGHT_BORDER = 0x26FFFFFF;
    private static final int GLASS_DARK_BORDER = 0x22000000;

    public static void drawRoundedRect(GuiGraphics context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }

        context.fill(x, y, x + width, y + height, color);
    }

    public static void drawRoundedRectWithBorder(GuiGraphics context, int x, int y, int width, int height, int radius, int bgColor, int borderColor) {
        drawRoundedRect(context, x, y, width, height, radius, bgColor);
        drawRoundedRectOutline(context, x, y, width, height, radius, borderColor);
    }

    public static void drawGlassPanel(GuiGraphics context, int x, int y, int width, int height, int radius, int baseColor, int accentColor) {
        drawVerticalGradient(context, x, y, width, height, blend(baseColor, 0x12FFFFFF), blend(baseColor, 0x22000000), 4);
        drawHorizontalGradient(context, x, y, width, height, blend(baseColor, 0x10FFFFFF), blend(baseColor, 0x08000000), 4);
        drawRoundedRect(context, x, y, width, height, radius, baseColor);
        drawGlassBorder(context, x, y, width, height, radius, accentColor);
    }

    public static void drawGlassBorder(GuiGraphics context, int x, int y, int width, int height, int radius, int accentColor) {
        drawRoundedRectOutline(context, x, y, width, height, radius, GLASS_LIGHT_BORDER);
        context.fill(x, y, x + width, y + 1, GLASS_LIGHT_BORDER);
        context.fill(x, y, x + 1, y + height, GLASS_LIGHT_BORDER);
        context.fill(x, y + height - 1, x + width, y + height, GLASS_DARK_BORDER);
        context.fill(x + width - 1, y, x + width, y + height, GLASS_DARK_BORDER);

        int innerAccent = blend(accentColor, 0x2AFFFFFF);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, innerAccent);
    }

    public static void drawGlassHoverOverlay(GuiGraphics context, int x, int y, int width, int height, boolean hover, int baseAccentColor) {
        int alpha = hover ? 0x26 : 0x12;
        int overlay = (alpha << 24) | (baseAccentColor & 0x00FFFFFF);
        context.fill(x, y, x + width, y + height, overlay);
    }

    public static void drawModuleCard(GuiGraphics context, int x, int y, int width, int height, boolean hover, boolean enabled, int accentColor) {
        int cardBg = hover ? 0xC8192232 : 0xBE141D2A;
        drawRoundedRect(context, x, y, width, height, 8, cardBg);
        
        int lightEdge = 0x33FFFFFF;
        int darkEdge = 0x22000000;
        context.fill(x, y, x + width, y + 1, lightEdge);
        context.fill(x, y, x + 1, y + height, lightEdge);
        context.fill(x, y + height - 1, x + width, y + height, darkEdge);
        context.fill(x + width - 1, y, x + width, y + height, darkEdge);
        
        if (enabled) {
            drawGreenGlow(context, x, y, width, height);
        }
    }

    public static void drawGreenGlow(GuiGraphics context, int x, int y, int width, int height) {
        int glowColor = 0x4043B581;
        context.fill(x - 1, y - 1, x + width + 1, y, glowColor);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, glowColor);
        context.fill(x - 1, y, x, y + height, glowColor);
        context.fill(x + width, y, x + width + 1, y + height, glowColor);
        
        int innerGlow = 0x2643B581;
        context.fill(x, y, x + width, y + 1, innerGlow);
        context.fill(x, y, x + 1, y + height, innerGlow);
    }

    public static void drawSmallButton(GuiGraphics context, int x, int y, int width, int height, int bgColor) {
        drawRoundedRect(context, x, y, width, height, 4, bgColor);
        int lightEdge = 0x26FFFFFF;
        context.fill(x, y, x + width, y + 1, lightEdge);
        context.fill(x, y, x + 1, y + height, lightEdge);
    }

    public static void drawVerticalGradient(GuiGraphics context, int x, int y, int width, int height, int topColor, int bottomColor, int slices) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int steps = Math.max(1, slices);
        for (int i = 0; i < steps; i++) {
            float t0 = i / (float) steps;
            float t1 = (i + 1) / (float) steps;
            int sliceY0 = y + Math.round(height * t0);
            int sliceY1 = y + Math.round(height * t1);
            int color = lerpColor(topColor, bottomColor, (t0 + t1) * 0.5f);
            context.fill(x, sliceY0, x + width, Math.max(sliceY0 + 1, sliceY1), color);
        }
    }

    public static void drawHorizontalGradient(GuiGraphics context, int x, int y, int width, int height, int leftColor, int rightColor, int slices) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int steps = Math.max(1, slices);
        for (int i = 0; i < steps; i++) {
            float t0 = i / (float) steps;
            float t1 = (i + 1) / (float) steps;
            int sliceX0 = x + Math.round(width * t0);
            int sliceX1 = x + Math.round(width * t1);
            int color = lerpColor(leftColor, rightColor, (t0 + t1) * 0.5f);
            context.fill(sliceX0, y, Math.max(sliceX0 + 1, sliceX1), y + height, color);
        }
    }

    public static void drawRoundedRectOutline(GuiGraphics context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 1 || height <= 1) {
            return;
        }

        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static void drawProgressBar(GuiGraphics context, int x, int y, int width, int height, float progress, int bgColor, int fillColor) {
        progress = Math.max(0, Math.min(1, progress));

        drawRoundedRect(context, x, y, width, height, 2, bgColor);

        int fillWidth = (int) (width * progress);
        if (fillWidth > 0) {
            drawRoundedRect(context, x, y, fillWidth, height, 2, fillColor);
        }
    }

    public static void drawTextWithBackground(GuiGraphics context, Font font, String text, int x, int y, int textColor, int bgColor, int padding) {
        int textWidth = font.width(text);
        context.fill(x - padding, y - padding, x + textWidth + padding, y + 10 + padding, bgColor);
        context.drawString(font, text, x, y, textColor, true);
    }

    public static void drawSeparator(GuiGraphics ctx, int x, int y, int width, int color) {
        ctx.fill(x, y, x + width, y + 1, color);
    }

    public static void drawToggleSwitch(GuiGraphics ctx, int x, int y, boolean enabled, int enabledColor) {
        int background = enabled ? enabledColor : 0xFF333333;
        int dotColor = enabled ? 0xFFFFFFFF : 0xFF9A9A9A;
        drawRoundedRect(ctx, x, y, 32, 16, 8, background);

        int dotX = enabled ? x + 20 : x + 3;
        int dotY = y + 2;
        drawRoundedRect(ctx, dotX, dotY, 10, 12, 5, dotColor);
    }

    private static int blend(int color, int overlay) {
        int alpha = (overlay >>> 24) & 0xFF;
        float factor = alpha / 255.0f;
        int a = (int) (((color >>> 24) & 0xFF) * (1.0f - factor) + ((overlay >>> 24) & 0xFF) * factor);
        int r = (int) (((color >>> 16) & 0xFF) * (1.0f - factor) + ((overlay >>> 16) & 0xFF) * factor);
        int g = (int) (((color >>> 8) & 0xFF) * (1.0f - factor) + ((overlay >>> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * (1.0f - factor) + (overlay & 0xFF) * factor);
        return (clamp(a, 0, 255) << 24) | (clamp(r, 0, 255) << 16) | (clamp(g, 0, 255) << 8) | clamp(b, 0, 255);
    }

    private static int lerpColor(int startColor, int endColor, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a = lerp((startColor >>> 24) & 0xFF, (endColor >>> 24) & 0xFF, t);
        int r = lerp((startColor >>> 16) & 0xFF, (endColor >>> 16) & 0xFF, t);
        int g = lerp((startColor >>> 8) & 0xFF, (endColor >>> 8) & 0xFF, t);
        int b = lerp(startColor & 0xFF, endColor & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int start, int end, float t) {
        return Math.round(start + ((end - start) * t));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
