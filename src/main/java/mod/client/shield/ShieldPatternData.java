package mod.client.shield;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ShieldPatternData {
    public static final int GRID_SIZE = 16;
    public static final int PATTERN_LENGTH = GRID_SIZE * GRID_SIZE;
    public static final String EMPTY_PATTERN = "0".repeat(PATTERN_LENGTH);
    public static final int DEFAULT_COLOR = 0x00D9FF;
    private static final String PATTERN_KEY = "XenonShieldPattern";
    private static final String COLOR_KEY = "XenonShieldColor";

    private ShieldPatternData() {
    }

    public static Pattern read(ItemStack stack) {
        return read(stack.getComponents());
    }

    public static Pattern read(DataComponentMap components) {
        CustomData customData = components.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return Pattern.EMPTY;
        }
        CompoundTag tag = customData.copyTag();
        String pattern = tag.getStringOr(PATTERN_KEY, EMPTY_PATTERN);
        int color = tag.getIntOr(COLOR_KEY, DEFAULT_COLOR) & 0xFFFFFF;
        return isValidPattern(pattern) ? new Pattern(pattern, color) : Pattern.EMPTY;
    }

    public static void write(ItemStack stack, String pattern, int color) {
        if (!isValidPattern(pattern)) {
            throw new IllegalArgumentException("Invalid shield pattern");
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(PATTERN_KEY, pattern);
            tag.putInt(COLOR_KEY, color & 0xFFFFFF);
        });
    }

    public static boolean isValidPattern(String pattern) {
        if (pattern == null || pattern.length() != PATTERN_LENGTH) {
            return false;
        }
        for (int i = 0; i < pattern.length(); i++) {
            char value = pattern.charAt(i);
            if (value != '0' && value != '1') {
                return false;
            }
        }
        return true;
    }

    public record Pattern(String pixels, int color) {
        public static final Pattern EMPTY = new Pattern(EMPTY_PATTERN, DEFAULT_COLOR);

        public boolean isEmpty() {
            return pixels.indexOf('1') < 0;
        }
    }
}