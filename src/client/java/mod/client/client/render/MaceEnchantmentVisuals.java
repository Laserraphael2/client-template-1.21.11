package mod.client.client.render;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public final class MaceEnchantmentVisuals {
    private static final int BREACH_COLOR = 0xFFFF5A45;
    private static final int BREACH_DARK = 0xCC5E1712;
    private static final int DENSITY_COLOR = 0xFF39D8FF;
    private static final int DENSITY_DARK = 0xCC073D55;

    private MaceEnchantmentVisuals() {
    }

    public static void registerTooltipColors() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> colorEnchantmentLines(lines));
    }

    public static void renderHeldIndicator(GuiGraphics context, int x, int y) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }

        ItemStack stack = client.player.getMainHandItem();
        Visual visual = resolve(stack);
        if (visual == Visual.NONE) {
            stack = client.player.getOffhandItem();
            visual = resolve(stack);
        }
        if (visual == Visual.NONE) {
            return;
        }

        int level = getLevel(stack, visual);
        String text = visual.label + " " + toRoman(level);
        int width = client.font.width(text) + 22;

        RenderUtils.drawGlassPanel(context, x, y, width, 18, 6, 0xB8FFFFFF, visual.color);
        RenderUtils.drawRoundedRect(context, x + 4, y + 4, 10, 10, 3, visual.darkColor);
        context.drawString(client.font, visual.badge, x + 6, y + 5, visual.color, true);
        context.drawString(client.font, text, x + 18, y + 5, visual.color, true);
    }

    public static void renderItemOverlay(GuiGraphics context, ItemStack stack, int x, int y) {
        Visual visual = resolve(stack);
        if (visual == Visual.NONE) {
            return;
        }

        long now = System.currentTimeMillis();
        int phase = (int) ((now / (visual == Visual.BREACH ? 55L : 90L)) % 12L);
        int pulse = 30 + (int) (18.0 * (0.5 + 0.5 * Math.sin(now / 180.0)));
        int glintColor = (pulse << 24) | (visual.color & 0x00FFFFFF);

        for (int row = 1; row < 15; row += visual == Visual.BREACH ? 4 : 3) {
            int stripeX = x + Math.floorMod(phase + row, 14);
            context.fill(stripeX, y + row, Math.min(x + 15, stripeX + 3), y + row + 1, glintColor);
        }

        RenderUtils.drawRoundedRect(context, x, y, 7, 7, 2, visual.darkColor);
        context.drawString(Minecraft.getInstance().font, visual.badge, x + 1, y, visual.color, true);
    }

    private static void colorEnchantmentLines(List<Component> lines) {
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            if (!(line.getContents() instanceof TranslatableContents contents)) {
                continue;
            }

            if ("enchantment.minecraft.breach".equals(contents.getKey())) {
                lines.set(i, ((MutableComponent) line.copy()).withColor(BREACH_COLOR));
            } else if ("enchantment.minecraft.density".equals(contents.getKey())) {
                lines.set(i, ((MutableComponent) line.copy()).withColor(DENSITY_COLOR));
            }
        }
    }

    private static Visual resolve(ItemStack stack) {
        if (!stack.is(Items.MACE)) {
            return Visual.NONE;
        }

        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(Enchantments.BREACH)) {
                return Visual.BREACH;
            }
            if (entry.getKey().is(Enchantments.DENSITY)) {
                return Visual.DENSITY;
            }
        }
        return Visual.NONE;
    }

    private static int getLevel(ItemStack stack, Visual visual) {
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(visual == Visual.BREACH ? Enchantments.BREACH : Enchantments.DENSITY)) {
                return entry.getIntValue();
            }
        }
        return 1;
    }

    private static String toRoman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(level);
        };
    }

    private enum Visual {
        NONE("", "", 0, 0),
        BREACH("B", "BREACH", BREACH_COLOR, BREACH_DARK),
        DENSITY("D", "DENSITY", DENSITY_COLOR, DENSITY_DARK);

        private final String badge;
        private final String label;
        private final int color;
        private final int darkColor;

        Visual(String badge, String label, int color, int darkColor) {
            this.badge = badge;
            this.label = label;
            this.color = color;
            this.darkColor = darkColor;
        }
    }
}