package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class FPSPingDisplay extends HudModule {
    private int fps = 0;
    private int ping = 0;
    private final int width;
    
    public FPSPingDisplay() {
        this.x = 5;
        this.y = 20;
        this.width = resolveWidth();
    }
    
    @Override
    public void tick(Minecraft client) {
        fps = client.getFps();
        
        ClientPacketListener networkHandler = client.getConnection();
        if (networkHandler != null && client.player != null) {
            var playerInfo = networkHandler.getPlayerInfo(client.player.getUUID());
            ping = playerInfo != null ? playerInfo.getLatency() : 0;
        }
    }
    
    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        String text = fps + "  FPS  " + ping + "ms";
        context.drawString(client.font, text, x, y, RenderUtils.TEXT_COLOR, true);
    }
    
    @Override
    public String getName() {
        return "FPS & Ping";
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
        return client.font.width("999  FPS  999ms");
    }
}
