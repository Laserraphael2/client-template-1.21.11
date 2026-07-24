package mod.client.client.screen.panels;

import mod.client.client.render.XenonTheme;
import net.minecraft.client.gui.GuiGraphics;

public interface ScreenPanel {
    void render(GuiGraphics ctx, int mouseX, int mouseY, int winX, int winY, XenonTheme theme);

    default boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    default boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return false;
    }

    default boolean keyPressed(int key, int scanCode, int modifiers) {
        return false;
    }

    default boolean charTyped(char c, int modifiers) {
        return false;
    }
}
