package mod.client.client.screen.panels;

import mod.client.client.render.XenonTheme;
import mod.client.client.screen.XenonMenuScreen;
import net.minecraft.client.gui.GuiGraphics;

public class AboutPanel implements ScreenPanel {
    private final XenonMenuScreen screen;

    public AboutPanel(XenonMenuScreen screen) {
        this.screen = screen;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, int winX, int winY, XenonTheme theme) {
        screen.renderAboutPanel(ctx, winX, winY, theme);
    }
}
