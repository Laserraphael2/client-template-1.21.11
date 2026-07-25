package mod.client.client.render;

import net.minecraft.client.gui.GuiGraphics;

public class CrosshairCustomizer {
    public static final int CUSTOM_GRID_SIZE = 11;
    private static final int CUSTOM_GRID_CELLS = CUSTOM_GRID_SIZE * CUSTOM_GRID_SIZE;
    private static final String DEFAULT_CUSTOM_PATTERN = "000001000000000100000000010000000001000000000100000111110111110001000000000100000000010000000001000000000100000";
    private static boolean enabled = false;
    private static int color = 0xFFFFFF;
    private static float scale = 1.0f;
    private static CrosshairType type = CrosshairType.VANILLA;
    private static String customPattern = normalizePattern(DEFAULT_CUSTOM_PATTERN);
    
    public enum CrosshairType {
        VANILLA,
        DOT,
        CROSS,
        CIRCLE,
        DRAWN
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setEnabled(boolean enabled) {
        CrosshairCustomizer.enabled = enabled;
    }
    
    public static int getColor() {
        return color;
    }
    
    public static void setColor(int color) {
        CrosshairCustomizer.color = color;
    }
    
    public static float getScale() {
        return scale;
    }
    
    public static void setScale(float scale) {
        CrosshairCustomizer.scale = scale;
    }
    
    public static CrosshairType getType() {
        return type;
    }
    
    public static void setType(CrosshairType type) {
        CrosshairCustomizer.type = type;
    }

    public static String getCustomPattern() {
        return customPattern;
    }

    public static void setCustomPattern(String pattern) {
        customPattern = normalizePattern(pattern);
    }

    public static boolean isCustomPixelSet(int column, int row) {
        if (column < 0 || row < 0 || column >= CUSTOM_GRID_SIZE || row >= CUSTOM_GRID_SIZE) {
            return false;
        }
        return customPattern.charAt(row * CUSTOM_GRID_SIZE + column) == '1';
    }

    public static void setCustomPixel(int column, int row, boolean active) {
        if (column < 0 || row < 0 || column >= CUSTOM_GRID_SIZE || row >= CUSTOM_GRID_SIZE) {
            return;
        }
        int index = row * CUSTOM_GRID_SIZE + column;
        if ((customPattern.charAt(index) == '1') == active) {
            return;
        }
        char[] pixels = customPattern.toCharArray();
        pixels[index] = active ? '1' : '0';
        customPattern = new String(pixels);
    }

    public static void clearCustomPattern() {
        customPattern = "0".repeat(CUSTOM_GRID_CELLS);
    }
    
    /**
     * Renders the custom crosshair at the center of the screen
     */
    public static void render(GuiGraphics context, int screenWidth, int screenHeight) {
        if (!enabled) return;
        
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        int fullColor = (255 << 24) | (color & 0xFFFFFF);
        
        switch (type) {
            case VANILLA -> {
                // Classic + shape
                context.fill(centerX - 7, centerY - 1, centerX - 2, centerY + 1, fullColor);
                context.fill(centerX + 2, centerY - 1, centerX + 7, centerY + 1, fullColor);
                context.fill(centerX - 1, centerY - 7, centerX + 1, centerY - 2, fullColor);
                context.fill(centerX - 1, centerY + 2, centerX + 1, centerY + 7, fullColor);
            }
            case DOT -> {
                // Single pixel dot
                context.fill(centerX, centerY, centerX + 1, centerY + 1, fullColor);
            }
            case CROSS -> {
                // Thin + without gaps
                context.fill(centerX - 5, centerY, centerX + 5, centerY + 1, fullColor);
                context.fill(centerX, centerY - 5, centerX + 1, centerY + 5, fullColor);
            }
            case CIRCLE -> {
                for (int angle = 0; angle < 360; angle += 15) {
                    int px = centerX + (int)(5 * Math.cos(Math.toRadians(angle)));
                    int py = centerY + (int)(5 * Math.sin(Math.toRadians(angle)));
                    context.fill(px, py, px + 1, py + 1, fullColor);
                }
            }
            case DRAWN -> {
                int originX = centerX - CUSTOM_GRID_SIZE / 2;
                int originY = centerY - CUSTOM_GRID_SIZE / 2;
                for (int row = 0; row < CUSTOM_GRID_SIZE; row++) {
                    for (int column = 0; column < CUSTOM_GRID_SIZE; column++) {
                        if (isCustomPixelSet(column, row)) {
                            context.fill(originX + column, originY + row, originX + column + 1, originY + row + 1, fullColor);
                        }
                    }
                }
            }
        }
    }

    private static String normalizePattern(String pattern) {
        String source = pattern == null ? "" : pattern;
        StringBuilder normalized = new StringBuilder(CUSTOM_GRID_CELLS);
        for (int i = 0; i < CUSTOM_GRID_CELLS; i++) {
            normalized.append(i < source.length() && source.charAt(i) == '1' ? '1' : '0');
        }
        return normalized.toString();
    }
}
