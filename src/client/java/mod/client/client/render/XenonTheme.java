package mod.client.client.render;

public enum XenonTheme {
    BLACK(0xFFA8B3C7, 0xFFD5DCE8, 0xFF11151C, 0xFF090C11),
    WHITE(0xFFF3F6FA, 0xFFFFFFFF, 0xFF242A33, 0xFF171C23),
    BLUE(0xFF4D8DFF, 0xFF72A7FF, 0xFF101A2C, 0xFF0A1120),
    GREEN(0xFF38C985, 0xFF65DCA3, 0xFF101E19, 0xFF09150F);

    public final int accent;
    public final int accentHover;
    public final int contentBg;
    public final int sidebarBg;

    XenonTheme(int accent, int accentHover, int contentBg, int sidebarBg) {
        this.accent = accent;
        this.accentHover = accentHover;
        this.contentBg = contentBg;
        this.sidebarBg = sidebarBg;
    }

    public static XenonTheme fromId(String id) {
        if (id != null) {
            if (id.equalsIgnoreCase("ICE") || id.equalsIgnoreCase("ARCTIC")) {
                return BLUE;
            }
            if (id.equalsIgnoreCase("CARBON")) {
                return BLACK;
            }
            if (id.equalsIgnoreCase("RED")) {
                return GREEN;
            }
        }
        for (XenonTheme theme : values()) {
            if (theme.name().equalsIgnoreCase(id)) {
                return theme;
            }
        }
        return BLACK;
    }
}
