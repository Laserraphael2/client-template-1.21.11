package mod.client.client.screen.panels;

import mod.client.client.render.XenonTheme;
import mod.client.client.screen.XenonMenuScreen;
import net.minecraft.client.gui.GuiGraphics;

public class SpotifyPanel implements ScreenPanel {
    private final XenonMenuScreen screen;

    public SpotifyPanel(XenonMenuScreen screen) {
        this.screen = screen;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, int winX, int winY, XenonTheme theme) {
        screen.renderSpotifyPanel(ctx, mouseX, mouseY, winX, winY, theme);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        return screen.handleSpotifyPanelClick((int) mx, (int) my);
    }
}
