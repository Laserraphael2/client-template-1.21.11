package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;

public class TargetHud extends HudModule {
    private LivingEntity currentTarget = null;
    private long lastTargetTime = 0;
    private static final long TARGET_FADE_TIME = 2000; // 2 seconds fade
    private float displayedHealth = 0.0f;
    
    public TargetHud() {
        this.x = 400;
        this.y = 300;
    }
    
    @Override
    public void tick(Minecraft client) {
        if (client.crosshairPickEntity != null && client.crosshairPickEntity instanceof LivingEntity living) {
            currentTarget = living;
            lastTargetTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastTargetTime > TARGET_FADE_TIME) {
            currentTarget = null;
        }
    }
    
    @Override
    public void render(GuiGraphics context, float partialTick) {
        if (currentTarget == null || !currentTarget.isAlive()) {
            currentTarget = null;
            return;
        }
        
        Minecraft client = Minecraft.getInstance();
        
        // Get target info
        String name = currentTarget.getName().getString();
        float health = currentTarget.getHealth();
        float maxHealth = currentTarget.getMaxHealth();
        float healthPercent = health / maxHealth;
        displayedHealth += (health - displayedHealth) * 0.22f;
        float displayedPercent = Math.max(0.0f, Math.min(1.0f, displayedHealth / Math.max(1.0f, maxHealth)));
        float distance = client.player != null ? client.player.distanceTo(currentTarget) : 0.0f;

        int nameWidth = client.font.width(name);
        int boxWidth = Math.max(152, nameWidth + 34);
        int boxHeight = 36;

        RenderUtils.drawRoundedRect(context, x, y, boxWidth, boxHeight, 3, 0xB0101010);
        RenderUtils.drawRoundedRectOutline(context, x, y, boxWidth, boxHeight, 3, 0x55333333);
        context.drawString(client.font, name, x + 6, y + 6, RenderUtils.TEXT_COLOR, true);
        String meta = Math.round(health) + "/" + Math.round(maxHealth) + " HP  " + String.format(java.util.Locale.ROOT, "%.1f", distance) + "m";
        context.drawString(client.font, meta, x + 6, y + 16, 0xFFB0B0B0, false);

        int barX = x + 6;
        int barY = y + boxHeight - 8;
        int barWidth = boxWidth - 12;
        int barHeight = 4;

        int healthColor;
        if (healthPercent > 0.6f) {
            healthColor = 0xFF55FF55;
        } else if (healthPercent > 0.3f) {
            healthColor = 0xFFFFAA00;
        } else {
            healthColor = 0xFFFF5555;
        }

        RenderUtils.drawProgressBar(context, barX, barY, barWidth, barHeight, displayedPercent, 0x80000000, healthColor);
    }
    
    @Override
    public String getName() {
        return "Target HUD";
    }
    
    @Override
    public int getWidth() {
        return 150;
    }
    
    @Override
    public int getHeight() {
        return 36;
    }
}
