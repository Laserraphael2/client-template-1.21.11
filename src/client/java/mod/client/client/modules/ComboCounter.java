package mod.client.client.modules;

import mod.client.client.ClientClient;
import mod.client.client.render.RenderUtils;
import mod.client.client.render.XenonTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ComboCounter extends HudModule {
    private static int combo = 0;
    private final int width;
    private long lastHitTime = 0;
    private static final long COMBO_RESET_TIME = 3000; // 3 seconds without hit = reset
    
    private float lastHealth = 20.0f; // Track player health to detect damage
    
    public ComboCounter() {
        this.x = 5;
        this.y = 180;
        this.width = resolveWidth();
    }
    
    public void onHit() {
        combo++;
        lastHitTime = System.currentTimeMillis();
    }

    public void onPlayerHurt() {
        combo = 0;
        lastHitTime = 0;
    }

    public int getCombo() {
        return combo;
    }
    
    @Override
    public void tick(Minecraft client) {
        if (client.player == null) return;
        
        float currentHealth = client.player.getHealth();
        if (currentHealth < lastHealth && combo > 0) {
            combo = 0;
            lastHitTime = 0;
        }
        lastHealth = currentHealth;

        if (System.currentTimeMillis() - lastHitTime > COMBO_RESET_TIME && combo > 0) {
            combo = 0;
        }
    }
    
    @Override
    public void render(GuiGraphics context, float partialTick) {
        if (combo == 0) return;

        Minecraft client = Minecraft.getInstance();

        String text = combo + "x";
        int color;
        if (combo >= 10) {
            color = XenonTheme.fromId(ClientClient.getInstance().getThemeId()).accent;
        } else if (combo >= 5) {
            color = 0xFFFFAA00;
        } else {
            color = RenderUtils.TEXT_COLOR;
        }

        context.drawString(client.font, text, x, y, color, true);
    }
    
    @Override
    public String getName() {
        return "Combo Counter";
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return 10;
    }

    private static int resolveWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return 24;
        }
        return client.font.width("99x");
    }
}
