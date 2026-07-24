package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public class SpeedDisplay extends HudModule {
    private double bps;
    private final int width;

    public SpeedDisplay() {
        this.x = 5;
        this.y = 78;
        this.width = resolveWidth();
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null) {
            bps = 0.0;
            return;
        }

        var velocity = client.player.getDeltaMovement();
        double horizontal = Math.sqrt((velocity.x * velocity.x) + (velocity.z * velocity.z));
        bps = horizontal * 20.0;
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        String text = "BPS " + String.format(Locale.ROOT, "%.2f", bps);
        context.drawString(client.font, text, x, y, RenderUtils.TEXT_COLOR, true);
    }

    @Override
    public String getName() {
        return "Speed";
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
            return 75;
        }
        return client.font.width("BPS 99.99");
    }
}
