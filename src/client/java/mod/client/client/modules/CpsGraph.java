package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import mod.client.client.util.SessionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.Deque;

public class CpsGraph extends HudModule {
    private static final int GRAPH_W = 90;
    private static final int GRAPH_H = 24;

    private final Deque<Integer> history = new ArrayDeque<>();
    private int lastClicks;

    public CpsGraph() {
        this.x = 5;
        this.y = 152;
    }

    @Override
    public void tick(Minecraft client) {
        int total = SessionTracker.getLeftClicks() + SessionTracker.getRightClicks();
        int cps = Math.max(0, total - lastClicks);
        lastClicks = total;

        if (history.size() >= 45) {
            history.removeFirst();
        }
        history.addLast(cps);
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        RenderUtils.drawRoundedRect(context, x, y, GRAPH_W, GRAPH_H, 3, 0xAA0F0F0F);
        RenderUtils.drawRoundedRectOutline(context, x, y, GRAPH_W, GRAPH_H, 3, 0x552F2F2F);

        int idx = 0;
        int max = 1;
        for (int v : history) {
            if (v > max) {
                max = v;
            }
        }

        for (int v : history) {
            int px = x + 2 + idx * 2;
            int bar = Math.max(1, (int) ((v / (float) max) * (GRAPH_H - 8)));
            context.fill(px, y + GRAPH_H - 3 - bar, px + 1, y + GRAPH_H - 3, 0xFFFF5555);
            idx++;
        }

        int current = history.peekLast() != null ? history.peekLast() : 0;
        context.drawString(client.font, "CPS " + current, x + 4, y + 3, RenderUtils.TEXT_COLOR, false);
    }

    @Override
    public String getName() {
        return "CPS Graph";
    }

    @Override
    public int getWidth() {
        return GRAPH_W;
    }

    @Override
    public int getHeight() {
        return GRAPH_H;
    }
}
