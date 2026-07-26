package mod.client.client.modules;

import net.minecraft.client.gui.GuiGraphics;

public class ItemBeam extends HudModule {
    private static boolean active = true;

    @Override
    public void render(GuiGraphics context, float partialTick) {
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        active = enabled;
    }

    public static boolean isActive() {
        return active;
    }

    @Override
    public String getName() {
        return "Item Beams";
    }

    @Override
    public int getWidth() {
        return 1;
    }

    @Override
    public int getHeight() {
        return 1;
    }
}