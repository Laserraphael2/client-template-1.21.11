package mod.client.client.modules;

import mod.client.client.render.MaceEnchantmentVisuals;
import net.minecraft.client.gui.GuiGraphics;

public class MaceEnchantmentHud extends HudModule {
    public MaceEnchantmentHud() {
        this.x = 8;
        this.y = 120;
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        MaceEnchantmentVisuals.renderHeldIndicator(context, x, y);
    }

    @Override
    public String getName() {
        return "Mace Enchantment";
    }

    @Override
    public int getWidth() {
        return 100;
    }

    @Override
    public int getHeight() {
        return 18;
    }
}