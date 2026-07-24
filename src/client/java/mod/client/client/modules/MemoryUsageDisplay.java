package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class MemoryUsageDisplay extends HudModule {
    private long usedMb;
    private long maxMb;
    private final int width;

    public MemoryUsageDisplay() {
        this.x = 5;
        this.y = 102;
        this.width = resolveWidth();
    }

    @Override
    public void tick(Minecraft client) {
        Runtime runtime = Runtime.getRuntime();
        usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        maxMb = runtime.maxMemory() / (1024L * 1024L);
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        String text = "MEM " + usedMb + "MB/" + maxMb + "MB";
        context.drawString(client.font, text, x, y, RenderUtils.TEXT_COLOR, true);

        int barY = y + 10;
        float progress = maxMb > 0 ? (usedMb / (float) maxMb) : 0.0f;
        RenderUtils.drawProgressBar(context, x, barY, 110, 5, progress, 0xFF2A2A2A, 0xFF43B581);
    }

    @Override
    public String getName() {
        return "Memory";
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return 17;
    }

    private static int resolveWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return 112;
        }
        return Math.max(112, client.font.width("MEM 1024MB/1024MB"));
    }
}
