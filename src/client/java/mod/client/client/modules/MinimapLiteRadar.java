package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class MinimapLiteRadar extends HudModule {
    private static final int SIZE = 74;
    private static final int RADIUS = 30;
    private static final double RANGE = 48.0;

    public MinimapLiteRadar() {
        this.x = 5;
        this.y = 182;
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        int cx = x + SIZE / 2;
        int cy = y + SIZE / 2;

        RenderUtils.drawRoundedRect(context, x, y, SIZE, SIZE, 4, 0xAA0F0F0F);
        RenderUtils.drawRoundedRectOutline(context, x, y, SIZE, SIZE, 4, 0x55353535);
        context.fill(cx - 1, y + 4, cx + 1, y + SIZE - 4, 0x332A2A2A);
        context.fill(x + 4, cy - 1, x + SIZE - 4, cy + 1, 0x332A2A2A);

        context.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFF5555);

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player) || entity == client.player) {
                continue;
            }

            double dx = entity.getX() - client.player.getX();
            double dz = entity.getZ() - client.player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > RANGE) {
                continue;
            }

            int px = cx + (int) Math.round((dx / RANGE) * RADIUS);
            int py = cy + (int) Math.round((dz / RANGE) * RADIUS);
            context.fill(px - 1, py - 1, px + 1, py + 1, 0xFF43B581);
        }

        context.drawString(client.font, "Radar", x + 4, y + 4, RenderUtils.TEXT_COLOR, false);
    }

    @Override
    public String getName() {
        return "Minimap Radar";
    }

    @Override
    public int getWidth() {
        return SIZE;
    }

    @Override
    public int getHeight() {
        return SIZE;
    }
}
