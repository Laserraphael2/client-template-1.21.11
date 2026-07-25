package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import mod.client.client.spotify.SpotifyService;
import mod.client.client.spotify.SpotifySnapshot;
import mod.client.client.spotify.SpotifyTrack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SpotifyNowPlayingModule extends HudModule {
    public SpotifyNowPlayingModule() {
        this.x = 5;
        this.y = 185;
    }

    @Override
    public void tick(Minecraft client) {
        // Network sync is handled by SpotifyService background scheduler.
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        SpotifySnapshot snapshot = SpotifyService.getInstance().getSnapshot();

        String line1 = "Spotify";
        String line2;

        if (!snapshot.authenticated()) {
            line2 = "Disconnected";
        } else {
            SpotifyTrack track = snapshot.track();
            if (track == null || track.title().isBlank()) {
                line2 = "No playback";
            } else {
                String state = track.playing() ? "Playing" : "Paused";
                line2 = state + ": " + track.title();
                if (!track.artists().isBlank()) {
                    line2 += " - " + track.artists();
                }
            }
        }

        int width = Math.max(92, Math.min(getWidth(), mc.font.width(line2) + 16));
        RenderUtils.drawGlassPanel(context, x, y, width, getHeight(), 6, 0xB8FFFFFF, 0xFF1ED760);
        context.drawString(mc.font, line1, x + 7, y + 6, 0xFF1ED760, true);
        context.drawString(mc.font, line2, x + 7, y + 18, RenderUtils.TEXT_COLOR, false);
    }

    @Override
    public String getName() {
        return "Spotify Now Playing";
    }

    @Override
    public int getWidth() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) {
            return 180;
        }
        return Math.max(180, mc.font.width("Playing: 123456789012345678901234567890") + 6);
    }

    @Override
    public int getHeight() {
        return 34;
    }
}
