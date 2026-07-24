package mod.client.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public abstract class HudModule {
    protected int x, y;
    protected boolean enabled = true;
    protected float scale = 1.0f;
    protected int keybind = -1;
    
    public abstract void render(GuiGraphics context, float partialTick);
    
    public void tick(Minecraft client) {}
    
    public abstract String getName();
    
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = Math.max(0.75f, Math.min(2.0f, scale)); }
    public int getKeybind() { return keybind; }
    public void setKeybind(int keybind) { this.keybind = keybind; }
    public int getScaledWidth() { return Math.max(1, (int) (getWidth() * scale)); }
    public int getScaledHeight() { return Math.max(1, (int) (getHeight() * scale)); }
    
    public abstract int getWidth();
    public abstract int getHeight();
}
