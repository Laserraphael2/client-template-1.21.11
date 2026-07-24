package mod.client.client.render;

public enum XenonTheme {
    RED(0xFFE53935, 0xFFEF5350, 0xFF141414, 0xFF0D0D0D),
    CARBON(0xFF8EA0B1, 0xFFA9B8C7, 0xFF10131A, 0xFF0A0D12),
    ICE(0xFF89B6FF, 0xFFA8CCFF, 0xFF0F1420, 0xFF090D15);

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
        for (XenonTheme theme : values()) {
            if (theme.name().equalsIgnoreCase(id)) {
                return theme;
            }
        }
        return RED;
    }
}
