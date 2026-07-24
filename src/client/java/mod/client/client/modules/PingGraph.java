package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.ArrayDeque;
import java.util.Deque;

public class PingGraph extends HudModule {
    private static final int GRAPH_W = 90;
    private static final int GRAPH_H = 24;
    private final Deque<Integer> history = new ArrayDeque<>();

    public PingGraph() {
        this.x = 100;
        this.y = 152;
    }

    @Override
    public void tick(Minecraft client) {
        int ping = 0;
        ClientPacketListener network = client.getConnection();
        if (network != null && client.player != null) {
            var info = network.getPlayerInfo(client.player.getUUID());
            ping = info != null ? info.getLatency() : 0;
        }

        if (history.size() >= 45) {
            history.removeFirst();
        }
        history.addLast(Math.max(0, ping));
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        RenderUtils.drawRoundedRect(context, x, y, GRAPH_W, GRAPH_H, 3, 0xAA0F0F0F);
        RenderUtils.drawRoundedRectOutline(context, x, y, GRAPH_W, GRAPH_H, 3, 0x552F2F2F);

        int max = 1;
        int min = Integer.MAX_VALUE;
        int sum = 0;
        for (int value : history) {
            if (value > max) {
                max = value;
            }
            if (value < min) {
                min = value;
            }
            sum += value;
        }

        int idx = 0;
        for (int value : history) {
            int px = x + 2 + idx * 2;
            int bar = Math.max(1, (int) ((value / (float) max) * (GRAPH_H - 8)));
            context.fill(px, y + GRAPH_H - 3 - bar, px + 1, y + GRAPH_H - 3, 0xFF43B581);
            idx++;
        }

        int avg = history.isEmpty() ? 0 : (sum / history.size());
        int jitter = history.isEmpty() ? 0 : Math.max(0, max - min);
        context.drawString(client.font, avg + "ms j" + jitter, x + 4, y + 3, RenderUtils.TEXT_COLOR, false);
    }

    @Override
    public String getName() {
        return "Ping Graph";
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
