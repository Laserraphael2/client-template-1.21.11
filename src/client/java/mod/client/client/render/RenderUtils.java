package mod.client.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class RenderUtils {
    public static final int TEXT_COLOR = 0xFFFFFFFF;
    public static final int SEPARATOR_COLOR = 0xFF2A2A2A;
    public static final int MUTED_COLOR = 0xFF9AA4B2;
    public static final int SUCCESS_COLOR = 0xFF43B581;
    public static final int DANGER_COLOR = 0xFFE53935;
    public static final int HOVER_COLOR = 0xFF222222;

    private static final int GLASS_LIGHT_BORDER = 0x18FFFFFF;
    private static final int GLASS_DARK_BORDER = 0x18000000;
    private static final int MAX_CACHED_RADIUS = 32;
    private static final double[][] ROUNDED_INSETS = createRoundedInsetCache();
    private static XenonTheme activeTheme = XenonTheme.BLACK;

    public static void setTheme(XenonTheme theme) {
        activeTheme = theme == null ? XenonTheme.BLACK : theme;
    }

    public static void drawRoundedRect(GuiGraphics context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        color = resolveThemeColor(color);

        int corner = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (corner == 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x, y + corner, x + width, y + height - corner, color);
        for (int row = 0; row < corner; row++) {
            double exactInset = roundedInsetExact(row, corner);
            int inset = (int) Math.ceil(exactInset);
            context.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            context.fill(x + inset, y + height - row - 1, x + width - inset, y + height - row, color);

            float edgeCoverage = (float) (inset - exactInset);
            if (edgeCoverage > 0.01f && inset > 0) {
                int edgeColor = scaleAlpha(color, edgeCoverage);
                context.fill(x + inset - 1, y + row, x + inset, y + row + 1, edgeColor);
                context.fill(x + width - inset, y + row, x + width - inset + 1, y + row + 1, edgeColor);
                context.fill(x + inset - 1, y + height - row - 1, x + inset, y + height - row, edgeColor);
                context.fill(x + width - inset, y + height - row - 1, x + width - inset + 1, y + height - row, edgeColor);
            }
        }
    }

    public static void drawRoundedRectWithBorder(GuiGraphics context, int x, int y, int width, int height, int radius, int bgColor, int borderColor) {
        drawRoundedRect(context, x, y, width, height, radius, bgColor);
        drawRoundedRectOutline(context, x, y, width, height, radius, borderColor);
    }

    public static void drawGlassPanel(GuiGraphics context, int x, int y, int width, int height, int radius, int baseColor, int accentColor) {
        if (width >= 80 && height >= 40) {
            drawRoundedRect(context, x + 1, y + 2, width, height, radius, 0x10000000);
        }
        int glassAlpha = Math.max(0x48, Math.round(((baseColor >>> 24) & 0xFF) * 0.72f));
        int baseRgb = baseColor & 0x00FFFFFF;
        if (((baseRgb >> 16) & 0xFF) > 220 && ((baseRgb >> 8) & 0xFF) > 220 && (baseRgb & 0xFF) > 220) {
            baseRgb = activeTheme.contentBg & 0x00FFFFFF;
        }
        drawRoundedRect(context, x, y, width, height, radius, (glassAlpha << 24) | baseRgb);
        drawGlassBorder(context, x, y, width, height, radius, activeTheme.accent);
    }

    public static void drawGlassBorder(GuiGraphics context, int x, int y, int width, int height, int radius, int accentColor) {
        int accentLine = (0x32 << 24) | (accentColor & 0x00FFFFFF);
        int lineInset = Math.max(2, Math.min(radius, width / 4));
        if (width >= 80 && height >= 32) {
            drawRoundedRectOutline(context, x, y, width, height, radius, GLASS_LIGHT_BORDER);
        }
        context.fill(x + lineInset, y, x + width - lineInset, y + 1, accentLine);
        if (height >= 32) {
            context.fill(x + lineInset, y + height - 1, x + width - lineInset, y + height, GLASS_DARK_BORDER);
        }
    }

    public static void drawGlassHoverOverlay(GuiGraphics context, int x, int y, int width, int height, boolean hover, int baseAccentColor) {
        int alpha = hover ? 0x12 : 0x08;
        int overlay = (alpha << 24) | (baseAccentColor & 0x00FFFFFF);
        context.fill(x, y, x + width, y + height, overlay);
    }

    public static void drawClickPulse(GuiGraphics context, int centerX, int centerY, float progress, int accentColor) {
        float eased = 1.0f - (1.0f - progress) * (1.0f - progress);
        int radius = 4 + Math.round(18.0f * eased);
        int alpha = Math.round(54.0f * (1.0f - progress));
        int color = (alpha << 24) | (accentColor & 0x00FFFFFF);
        drawRoundedRect(context, centerX - radius, centerY - radius, radius * 2, radius * 2, radius, color);
    }

    public static void drawModuleCard(GuiGraphics context, int x, int y, int width, int height, boolean hover, boolean enabled, int accentColor) {
        int cardBg = hover ? 0xD8FFFFFF : 0xB8FFFFFF;
        drawGlassPanel(context, x, y, width, height, 8, cardBg, accentColor);
        
        if (enabled) {
            drawGreenGlow(context, x, y, width, height, accentColor);
        }
    }

    public static void drawGreenGlow(GuiGraphics context, int x, int y, int width, int height, int accentColor) {
        int glowColor = 0x30000000 | (accentColor & 0x00FFFFFF);
        context.fill(x - 1, y - 1, x + width + 1, y, glowColor);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, glowColor);
        context.fill(x - 1, y, x, y + height, glowColor);
        context.fill(x + width, y, x + width + 1, y + height, glowColor);
        
        int innerGlow = 0x20000000 | (accentColor & 0x00FFFFFF);
        context.fill(x, y, x + width, y + 1, innerGlow);
        context.fill(x, y, x + 1, y + height, innerGlow);
    }

    public static void drawSmallButton(GuiGraphics context, int x, int y, int width, int height, int bgColor) {
        drawRoundedRect(context, x, y, width, height, 4, 0x7610141B);
        int lightEdge = 0x16FFFFFF;
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

        int corner = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (corner == 0) {
            context.fill(x, y, x + width, y + 1, color);
            context.fill(x, y + height - 1, x + width, y + height, color);
            context.fill(x, y + 1, x + 1, y + height - 1, color);
            context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
            return;
        }

        context.fill(x, y + corner, x + 1, y + height - corner, color);
        context.fill(x + width - 1, y + corner, x + width, y + height - corner, color);

        int previousInset = corner;
        for (int row = 0; row < corner; row++) {
            int inset = roundedInset(row, height, corner);
            if (row == 0) {
                context.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
                context.fill(x + inset, y + height - row - 1, x + width - inset, y + height - row, color);
            } else {
                int thickness = Math.max(1, Math.abs(previousInset - inset));
                context.fill(x + inset, y + row, x + inset + thickness, y + row + 1, color);
                context.fill(x + width - inset - thickness, y + row, x + width - inset, y + row + 1, color);
                context.fill(x + inset, y + height - row - 1, x + inset + thickness, y + height - row, color);
                context.fill(x + width - inset - thickness, y + height - row - 1, x + width - inset, y + height - row, color);
            }
            previousInset = inset;
        }
    }

    public static void drawProgressBar(GuiGraphics context, int x, int y, int width, int height, float progress, int bgColor, int fillColor) {
        progress = Math.max(0, Math.min(1, progress));

        drawRoundedRect(context, x, y, width, height, 2, bgColor);

        int fillWidth = (int) (width * progress);
        if (fillWidth > 0) {
            drawRoundedRect(context, x, y, fillWidth, height, 2, resolveThemeColor(fillColor));
        }
    }

    private static int resolveThemeColor(int color) {
        int rgb = color & 0x00FFFFFF;
        if (rgb == 0x0000D9FF || rgb == 0x0039D8FF || rgb == 0x0000E5FF || rgb == 0x0089B6FF) {
            return (color & 0xFF000000) | (activeTheme.accent & 0x00FFFFFF);
        }
        return color;
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

    private static int roundedInset(int row, int height, int radius) {
        int distance = Math.min(row, height - 1 - row);
        return (int) Math.ceil(roundedInsetExact(distance, radius));
    }

    private static double roundedInsetExact(int row, int radius) {
        if (row >= radius) {
            return 0.0;
        }

        if (radius <= MAX_CACHED_RADIUS) {
            return ROUNDED_INSETS[radius][row];
        }

        double vertical = radius - row - 0.5;
        return Math.max(0.0, radius - Math.sqrt((radius * radius) - (vertical * vertical)));
    }

    private static double[][] createRoundedInsetCache() {
        double[][] cache = new double[MAX_CACHED_RADIUS + 1][];
        cache[0] = new double[0];
        for (int radius = 1; radius <= MAX_CACHED_RADIUS; radius++) {
            cache[radius] = new double[radius];
            for (int row = 0; row < radius; row++) {
                double vertical = radius - row - 0.5;
                cache[radius][row] = Math.max(0.0, radius - Math.sqrt((radius * radius) - (vertical * vertical)));
            }
        }
        return cache;
    }

    private static int scaleAlpha(int color, float factor) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * factor);
        return (clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
