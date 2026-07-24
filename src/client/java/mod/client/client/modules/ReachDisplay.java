package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import mod.client.client.util.SessionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public class ReachDisplay extends HudModule {
    private final int width;

    public ReachDisplay() {
        this.x = 5;
        this.y = 96;
        this.width = resolveWidth();
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        float reach = SessionTracker.getLastReach();
        long age = SessionTracker.getLastReachAgeMs();

        String text;
        int color;
        if (reach <= 0.0f || age > 5000) {
            text = "Reach --";
            color = RenderUtils.MUTED_COLOR;
        } else {
            text = "Reach " + String.format(Locale.ROOT, "%.2f", reach);
            color = reach > 3.0f ? 0xFFFFAA00 : RenderUtils.TEXT_COLOR;
        }

        context.drawString(client.font, text, x, y, color, true);
    }

    @Override
    public String getName() {
        return "Reach Display";
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
            return 80;
        }
        return client.font.width("Reach 0.00");
    }
}
