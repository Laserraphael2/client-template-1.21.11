package mod.client.client.render;

import net.minecraft.client.gui.GuiGraphics;

public class CrosshairCustomizer {
    private static boolean enabled = false;
    private static int color = 0xFFFFFF;
    private static float scale = 1.0f;
    private static CrosshairType type = CrosshairType.VANILLA;
    
    public enum CrosshairType {
        VANILLA,
        DOT,
        CROSS,
        CIRCLE
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
                // Simple circle approximation
                for (int angle = 0; angle < 360; angle += 15) {
                    int px = centerX + (int)(5 * Math.cos(Math.toRadians(angle)));
                    int py = centerY + (int)(5 * Math.sin(Math.toRadians(angle)));
                    context.fill(px, py, px + 1, py + 1, fullColor);
                }
            }
        }
    }
}
