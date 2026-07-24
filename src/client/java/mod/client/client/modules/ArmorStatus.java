package mod.client.client.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class ArmorStatus extends HudModule {
    private static final int SLOT_SPACING = 20;
    private static final int ITEM_DRAW_SIZE = 16;
    private static final int MAX_ARMOR_SLOTS = 4;
    
    public ArmorStatus() {
        this.x = 5;
        this.y = 35;
    }
    
    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        ItemStack[] stacks = new ItemStack[] {
            client.player.getInventory().getItem(38),
            client.player.getInventory().getItem(39),
            client.player.getInventory().getItem(37),
            client.player.getInventory().getItem(36)
        };

        boolean hasRenderable = false;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                hasRenderable = true;
                break;
            }
        }

        if (!hasRenderable) {
            return;
        }

        int offsetX = 0;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }

            int drawX = x + offsetX;
            context.renderItem(stack, drawX, y);

            int durability = stack.getMaxDamage() - stack.getDamageValue();
            int maxDurability = Math.max(1, stack.getMaxDamage());
            float pct = durability / (float) maxDurability;
            int color;
            if (pct > 0.75f) {
                color = 0xFF55FF55;
            } else if (pct > 0.50f) {
                color = 0xFFB7FF55;
            } else if (pct > 0.25f) {
                color = 0xFFFFAA00;
            } else {
                color = 0xFFFF5555;
            }

            // Always show live remaining durability as integer value.
            String durText = Integer.toString(Math.max(0, durability));
            int textWidth = client.font.width(durText);
            int textX = drawX + (ITEM_DRAW_SIZE / 2) - (textWidth / 2);
            context.drawString(client.font, durText, textX, y + 18, color, true);

            offsetX += SLOT_SPACING;
        }
    }
    
    @Override
    public String getName() {
        return "Armor Status";
    }
    
    @Override
    public int getWidth() {
        return MAX_ARMOR_SLOTS * SLOT_SPACING;
    }
    
    @Override
    public int getHeight() {
        return 28;
    }
}
