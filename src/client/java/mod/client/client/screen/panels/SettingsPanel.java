package mod.client.client.screen.panels;

import mod.client.client.render.XenonTheme;
import mod.client.client.screen.XenonMenuScreen;
import net.minecraft.client.gui.GuiGraphics;

public class SettingsPanel implements ScreenPanel {
    private final XenonMenuScreen screen;

    public SettingsPanel(XenonMenuScreen screen) {
        this.screen = screen;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, int winX, int winY, XenonTheme theme) {
        screen.renderSettingsPanel(ctx, mouseX, mouseY, winX, winY, theme);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        return screen.handleSettingsPanelClick((int) mx, (int) my);
    }
}
