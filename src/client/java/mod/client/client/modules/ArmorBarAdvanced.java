package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class ArmorBarAdvanced extends HudModule {
    private static final int SLOT_SPACING = 20;
    private static final int BAR_W = 16;
    private static final int BAR_H = 2;

    public ArmorBarAdvanced() {
        this.x = 5;
        this.y = 126;
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        ItemStack[] stacks = new ItemStack[] {
                client.player.getInventory().getItem(38),
                client.player.getInventory().getItem(39),
                client.player.getInventory().getItem(37),
                client.player.getInventory().getItem(36)
        };

        int offsetX = 0;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                offsetX += SLOT_SPACING;
                continue;
            }

            int drawX = x + offsetX;
            context.renderItem(stack, drawX, y);

            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            float pct = Math.max(0.0f, Math.min(1.0f, remaining / (float) Math.max(1, stack.getMaxDamage())));
            int color = pct > 0.66f ? 0xFF55FF55 : (pct > 0.33f ? 0xFFFFAA00 : 0xFFFF5555);
            RenderUtils.drawProgressBar(context, drawX, y + 17, BAR_W, BAR_H, pct, 0xAA111111, color);

            offsetX += SLOT_SPACING;
        }
    }

    @Override
    public String getName() {
        return "Armor Bars";
    }

    @Override
    public int getWidth() {
        return SLOT_SPACING * 4;
    }

    @Override
    public int getHeight() {
        return 21;
    }
}
