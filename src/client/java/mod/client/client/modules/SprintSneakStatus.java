package mod.client.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SprintSneakStatus extends HudModule {
    private final int width;
    
    public SprintSneakStatus() {
        this.x = 5;
        this.y = 153;
        this.width = resolveWidth();
    }
    
    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        int offsetY = 0;

        if (client.player.isSprinting()) {
            context.drawString(client.font, "SPRINT", x, y + offsetY, 0xFF55FF55, true);
            offsetY += 12;
        }

        if (client.player.isCrouching()) {
            context.drawString(client.font, "SNEAK", x, y + offsetY, 0xFFFFAA00, true);
        }
    }
    
    @Override
    public String getName() {
        return "Sprint/Sneak Status";
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return 20;
    }

    private static int resolveWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return 40;
        }
        return client.font.width("SPRINT");
    }
}
