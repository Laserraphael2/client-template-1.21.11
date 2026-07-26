package mod.client.client.render;

import mod.client.shield.ShieldPatternData;

public final class ShieldPatternCustomizer {
    private static String pattern = ShieldPatternData.EMPTY_PATTERN;
    private static int color = ShieldPatternData.DEFAULT_COLOR;

    private ShieldPatternCustomizer() {
    }

    public static String getPattern() {
        return pattern;
    }

    public static void setPattern(String pattern) {
        ShieldPatternCustomizer.pattern = ShieldPatternData.isValidPattern(pattern)
                ? pattern
                : ShieldPatternData.EMPTY_PATTERN;
    }

    public static int getColor() {
        return color;
    }

    public static void setColor(int color) {
        ShieldPatternCustomizer.color = color & 0xFFFFFF;
    }

    public static ShieldPatternData.Pattern getDesign() {
        return new ShieldPatternData.Pattern(pattern, color);
    }
}