package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class CompassDisplay extends HudModule {
    private final int width;

    public CompassDisplay() {
        this.x = 5;
        this.y = 124;
        this.width = resolveWidth();
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        float yaw = (client.player.getYRot() % 360 + 360) % 360;
        int heading = (int) yaw;
        String cardinal = toCardinal(yaw);
        String text = "DIR " + cardinal + " " + heading + "°";
        context.drawString(client.font, text, x, y, RenderUtils.TEXT_COLOR, true);
    }

    @Override
    public String getName() {
        return "Compass";
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    private static String toCardinal(float yaw) {
        // Normalize to 8-way compass.
        int idx = Math.round(yaw / 45.0f) & 7;
        return switch (idx) {
            case 0 -> "S";
            case 1 -> "SW";
            case 2 -> "W";
            case 3 -> "NW";
            case 4 -> "N";
            case 5 -> "NE";
            case 6 -> "E";
            default -> "SE";
        };
    }

    private static int resolveWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return 80;
        }
        return client.font.width("DIR NW 359°");
    }
}
