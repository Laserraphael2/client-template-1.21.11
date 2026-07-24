package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public class TpsDisplay extends HudModule {
    private long lastGameTime = -1L;
    private long lastUpdateNanos = -1L;
    private double tps = 20.0;
    private final int width;

    public TpsDisplay() {
        this.x = 5;
        this.y = 90;
        this.width = resolveWidth();
    }

    @Override
    public void tick(Minecraft client) {
        if (client.level == null) {
            return;
        }

        long gameTime = client.level.getGameTime();
        long now = System.nanoTime();
        if (lastGameTime < 0L || lastUpdateNanos < 0L) {
            lastGameTime = gameTime;
            lastUpdateNanos = now;
            return;
        }

        long passedTicks = gameTime - lastGameTime;
        if (passedTicks <= 0L) {
            return;
        }

        double elapsedMs = (now - lastUpdateNanos) / 1_000_000.0;
        double mspt = elapsedMs / passedTicks;
        if (mspt > 0.0) {
            tps = Math.min(20.0, 1000.0 / mspt);
        }

        lastGameTime = gameTime;
        lastUpdateNanos = now;
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        String text = "TPS " + String.format(Locale.ROOT, "%.2f", tps);
        context.drawString(client.font, text, x, y, RenderUtils.TEXT_COLOR, true);
    }

    @Override
    public String getName() {
        return "TPS";
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
            return 68;
        }
        return client.font.width("TPS 20.00");
    }
}
