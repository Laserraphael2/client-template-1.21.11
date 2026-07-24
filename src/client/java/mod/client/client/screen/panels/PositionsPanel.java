package mod.client.client.screen.panels;

import mod.client.client.render.XenonTheme;
import mod.client.client.screen.XenonMenuScreen;
import net.minecraft.client.gui.GuiGraphics;

public class PositionsPanel implements ScreenPanel {
    private final XenonMenuScreen screen;

    public PositionsPanel(XenonMenuScreen screen) {
        this.screen = screen;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, int winX, int winY, XenonTheme theme) {
        screen.renderPositionsEditorPanel(ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        return screen.handlePositionsPanelClick((int) mx, (int) my);
    }
}
