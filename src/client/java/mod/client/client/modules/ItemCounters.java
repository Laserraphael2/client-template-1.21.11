package mod.client.client.modules;

import mod.client.client.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemCounters extends HudModule {
    private final int width;

    public ItemCounters() {
        this.x = 5;
        this.y = 111;
        this.width = resolveWidth();
    }

    @Override
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        int blocks = 0;
        int arrows = 0;
        int pearls = 0;
        int gaps = 0;
        int totems = 0;

        int size = client.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof BlockItem) {
                blocks += stack.getCount();
            }
            if (stack.is(Items.ARROW)) {
                arrows += stack.getCount();
            }
            if (stack.is(Items.ENDER_PEARL)) {
                pearls += stack.getCount();
            }
            if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                gaps += stack.getCount();
            }
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                totems += stack.getCount();
            }
        }

        String text = "B " + blocks + " A " + arrows + " P " + pearls + " G " + gaps + " T " + totems;
        context.drawString(client.font, text, x, y, RenderUtils.TEXT_COLOR, true);
    }

    @Override
    public String getName() {
        return "Item Counters";
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    private static int resolveWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return 190;
        }
        return client.font.width("B 9999 A 999 P 99 G 99 T 9");
    }
}
