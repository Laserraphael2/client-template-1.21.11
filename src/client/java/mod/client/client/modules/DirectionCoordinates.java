package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class DirectionCoordinates extends HudModule {
    private final int width;

    public DirectionCoordinates() {
        this.x = 5;
        this.y = 66;
        this.width = resolveWidth();
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        int px = (int) Math.floor(client.player.getX());
        int py = (int) Math.floor(client.player.getY());
        int pz = (int) Math.floor(client.player.getZ());
        Direction dir = client.player.getDirection();

        String text = dir.getName().toUpperCase() + " " + px + " " + py + " " + pz;
        if (client.player.level().dimension() == Level.NETHER) {
            text += " [O " + (px * 8) + " " + (pz * 8) + "]";
        } else {
            text += " [N " + (px / 8) + " " + (pz / 8) + "]";
        }

        context.drawString(client.font, text, x, y, RenderUtils.TEXT_COLOR, true);
    }

    @Override
    public String getName() {
        return "Direction + Coords";
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
            return 200;
        }
        return client.font.width("SOUTH -12345 255 -12345 [N -1543 -1543]");
    }
}
