package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class KeystrokeOverlay extends HudModule {
    private final KeyButton wKey, aKey, sKey, dKey, spaceKey, lmbKey, rmbKey;
    
    public KeystrokeOverlay() {
        this.x = 5;
        this.y = 70;
        
        wKey = new KeyButton(GLFW.GLFW_KEY_W, 20, 0);
        aKey = new KeyButton(GLFW.GLFW_KEY_A, 0, 20);
        sKey = new KeyButton(GLFW.GLFW_KEY_S, 20, 20);
        dKey = new KeyButton(GLFW.GLFW_KEY_D, 40, 20);
        spaceKey = new KeyButton(GLFW.GLFW_KEY_SPACE, 0, 40, 60, 18);
        lmbKey = new KeyButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0, 60, 28, 18);
        rmbKey = new KeyButton(GLFW.GLFW_MOUSE_BUTTON_RIGHT, 32, 60, 28, 18);
    }
    
    @Override
    public void render(GuiGraphics context, float partialTick) {
        wKey.render(context, x, y);
        aKey.render(context, x, y);
        sKey.render(context, x, y);
        dKey.render(context, x, y);
        spaceKey.render(context, x, y);
        lmbKey.render(context, x, y);
        rmbKey.render(context, x, y);
    }
    
    @Override
    public String getName() {
        return "Keystrokes";
    }
    
    @Override
    public int getWidth() {
        return 60;
    }
    
    @Override
    public int getHeight() {
        return 78;
    }
    
    public void onLeftClick() { lmbKey.press(); }
    public void onRightClick() { rmbKey.press(); }
    public void onKeyPress(int key) {
        if (key == GLFW.GLFW_KEY_W) wKey.press();
        else if (key == GLFW.GLFW_KEY_A) aKey.press();
        else if (key == GLFW.GLFW_KEY_S) sKey.press();
        else if (key == GLFW.GLFW_KEY_D) dKey.press();
        else if (key == GLFW.GLFW_KEY_SPACE) spaceKey.press();
    }
    
    private static class KeyButton {
        private final int key;
        private final int offsetX, offsetY, width, height;
        private long pressTime = 0;
        
        KeyButton(int key, int offsetX, int offsetY) {
            this(key, offsetX, offsetY, 18, 18);
        }
        
        KeyButton(int key, int offsetX, int offsetY, int width, int height) {
            this.key = key;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.width = width;
            this.height = height;
        }
        
        void press() {
            pressTime = System.currentTimeMillis();
        }
        
        void render(GuiGraphics context, int baseX, int baseY) {
            boolean isPressed = System.currentTimeMillis() - pressTime < 100;
            
            int x = baseX + offsetX;
            int y = baseY + offsetY;
            int color = isPressed ? 0x99FFFFFF : 0x33000000;
            
            RenderUtils.drawRoundedRect(context, x, y, width, height, 3, color);
            RenderUtils.drawRoundedRectOutline(context, x, y, width, height, 3, 0x66FFFFFF);
            
            Minecraft client = Minecraft.getInstance();
            String label = getLabel();
            int textX = x + (width - client.font.width(label)) / 2;
            int textY = y + (height - 8) / 2;
            int textColor = isPressed ? 0xFF000000 : 0xFFFFFFFF;
            context.drawString(client.font, label, textX, textY, textColor, false);
        }
        
        String getLabel() {
            return switch (key) {
                case GLFW.GLFW_KEY_W -> "W";
                case GLFW.GLFW_KEY_A -> "A";
                case GLFW.GLFW_KEY_S -> "S";
                case GLFW.GLFW_KEY_D -> "D";
                case GLFW.GLFW_KEY_SPACE -> "---";
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "LMB";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "RMB";
                default -> "?";
            };
        }
    }
}
