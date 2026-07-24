package mod.client.client.screen;

import mod.client.client.ClientClient;
import mod.client.client.hud.HudManager;
import mod.client.client.modules.HudModule;
import mod.client.client.render.CrosshairCustomizer;
import mod.client.client.render.RenderUtils;
import mod.client.client.render.XenonTheme;
import mod.client.client.screen.panels.AboutPanel;
import mod.client.client.screen.panels.ConfigPanel;
import mod.client.client.screen.panels.ModulesPanel;
import mod.client.client.screen.panels.PerformancePanel;
import mod.client.client.screen.panels.PositionsPanel;
import mod.client.client.screen.panels.SettingsPanel;
import mod.client.client.util.KeyNameUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class XenonMenuScreen extends Screen {

    private static final int WIN_W = 872;
    private static final int WIN_H = 590;

    private static final int SIDEBAR_W = 60;
    private static final int HEADER_H = 52;
    private static final int POSITION_DONE_W = 140;
    private static final int POSITION_DONE_H = 26;
    private static final int MODULE_CARD_H = 96;
    private static final int MODULE_GAP = 5;
    private static final int SIDEBAR_ICON_SIZE = 16;
    private static final int MODULE_BADGE_SIZE = 26;
    private static final int EDGE_MARGIN = 14;
    private static final String[] FILTERS = {"All", "New", "HUD", "Hypixel", "PvP"};

    private int winX;
    private int winY;

    private float openAnim = 0.0f;

    private boolean draggingWindow;
    private int windowDragOffsetX;
    private int windowDragOffsetY;

    private HudModule draggingModule;
    private int moduleDragOffsetX;
    private int moduleDragOffsetY;

    private int draggingSlider; // 0=none, 1=red, 2=green, 3=blue
    private boolean searchFocused;
    private boolean confirmReset;
    private boolean wizardVisible;
    private boolean presetNameFocused;

    private HudModule awaitingKeybindModule;

    private String currentFilter = "All";
    private String searchQuery = "";
    private final List<HudModule> filteredModulesCache = new ArrayList<>();
    private String filteredModulesCacheFilter = "";
    private String filteredModulesCacheQuery = "";
    private boolean filteredModulesDirty = true;
    private int moduleGridScroll;

    private CrosshairCustomizer.CrosshairType crosshairType;
    private int crosshairRed;
    private int crosshairGreen;
    private int crosshairBlue;
    private boolean customCrosshairEnabled;
    private String presetName = "default";

    private enum Tab {
        MODULES,
        SETTINGS,
        POSITIONS,
        CHAT,
        PERFORMANCE,
        CONFIG,
        ABOUT
    }

    private Tab currentTab = Tab.MODULES;

    private final ModulesPanel modulesPanel = new ModulesPanel(this);
    private final SettingsPanel settingsPanel = new SettingsPanel(this);
    private final PositionsPanel positionsPanel = new PositionsPanel(this);
    private final PerformancePanel performancePanel = new PerformancePanel(this);
    private final ConfigPanel configPanel = new ConfigPanel(this);
    private final AboutPanel aboutPanel = new AboutPanel(this);

    public XenonMenuScreen() {
        super(Component.literal("XENON"));
        ClientClient client = ClientClient.getInstance();
        this.crosshairType = client.getCrosshairType();
        int color = client.getCrosshairColor();
        this.crosshairRed = (color >> 16) & 0xFF;
        this.crosshairGreen = (color >> 8) & 0xFF;
        this.crosshairBlue = color & 0xFF;
        this.customCrosshairEnabled = client.isCustomCrosshairEnabled();
        this.currentFilter = "All";
        this.searchQuery = "";
    }

    @Override
    protected void init() {
        ClientClient state = ClientClient.getInstance();
        if (state.getMenuX() >= 0 && state.getMenuY() >= 0) {
            this.winX = clamp(state.getMenuX(), 0, Math.max(0, this.width - WIN_W));
            this.winY = clamp(state.getMenuY(), 0, Math.max(0, this.height - WIN_H));
        } else {
            this.winX = (this.width - WIN_W) / 2;
            this.winY = (this.height - WIN_H) / 2;
        }

        wizardVisible = !state.isWizardCompleted();
        invalidateFilteredModules();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float partialTick) {
        openAnim = 1.0f;

        if (currentTab == Tab.POSITIONS) {
            positionsPanel.render(ctx, mouseX, mouseY, winX, winY, XenonTheme.fromId(ClientClient.getInstance().getThemeId()));
            super.render(ctx, mouseX, mouseY, partialTick);
            return;
        }

        ctx.fill(0, 0, this.width, this.height, applyAlpha(0x55000000, openAnim));
        renderWindow(ctx, mouseX, mouseY);
        super.render(ctx, mouseX, mouseY, partialTick);
    }

    private void renderWindow(GuiGraphics ctx, int mouseX, int mouseY) {
        XenonTheme theme = XenonTheme.fromId(ClientClient.getInstance().getThemeId());

        int drawX = winX;
        int drawY = winY + (int) ((1.0f - openAnim) * 10.0f);

        int contentBg = fadeAlpha(0xFF0E1320, 0.82f);
        int sidebarBg = fadeAlpha(0xFF0A1018, 0.88f);
        int headerBg = fadeAlpha(0xFF111829, 0.90f);

        RenderUtils.drawGlassPanel(ctx, drawX, drawY, WIN_W, WIN_H, 8, applyAlpha(contentBg, openAnim), applyAlpha(theme.accent, openAnim));
        RenderUtils.drawGlassPanel(ctx, drawX, drawY, SIDEBAR_W, WIN_H, 8, applyAlpha(sidebarBg, openAnim), applyAlpha(theme.accent, openAnim));
        RenderUtils.drawGlassPanel(ctx, drawX + SIDEBAR_W, drawY, WIN_W - SIDEBAR_W, HEADER_H, 0, applyAlpha(headerBg, openAnim), applyAlpha(theme.accent, openAnim));

        renderTopBar(ctx, mouseX, mouseY, drawX, drawY, theme);

        renderSidebar(ctx, mouseX, mouseY, drawX, drawY, theme);

        switch (currentTab) {
            case MODULES -> modulesPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case SETTINGS -> settingsPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case POSITIONS -> positionsPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case CHAT -> renderComingSoon(ctx, drawX, drawY, "Chat Options coming soon...");
            case PERFORMANCE -> performancePanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case CONFIG -> configPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case ABOUT -> aboutPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
        }

        if (wizardVisible) {
            renderFirstRunWizard(ctx, mouseX, mouseY, drawX, drawY, theme);
        }
    }

    public boolean isPositionEditorMode() {
        return currentTab == Tab.POSITIONS;
    }

    public void renderModulesPanel(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        renderModuleGrid(ctx, mouseX, mouseY, drawX, drawY, theme);
    }

    public void renderSettingsPanel(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        renderSettings(ctx, mouseX, mouseY, drawX, drawY, theme);
    }

    public void renderPositionsEditorPanel(GuiGraphics ctx, int mouseX, int mouseY) {
        renderPositionEditorScene(ctx, mouseX, mouseY);
    }

    public void renderPerformancePanel(GuiGraphics ctx, int drawX, int drawY) {
        renderPerformance(ctx, drawX, drawY);
    }

    public void renderConfigPanel(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        renderConfig(ctx, mouseX, mouseY, drawX, drawY, theme);
    }

    public void renderAboutPanel(GuiGraphics ctx, int drawX, int drawY, XenonTheme theme) {
        renderAbout(ctx, drawX, drawY, theme);
    }

    public boolean handlePositionsPanelClick(int absX, int absY) {
        int doneX = getPositionDoneX();
        int doneY = getPositionDoneY();
        if (inside(absX, absY, doneX, doneY, POSITION_DONE_W, POSITION_DONE_H)) {
            currentTab = Tab.MODULES;
            ClientClient.getHudManager().saveConfig();
            return true;
        }

        for (HudModule module : ClientClient.getHudManager().getModules()) {
            if (!module.isEnabled()) {
                continue;
            }
            int mw = Math.max(module.getScaledWidth(), 20);
            int mh = Math.max(module.getScaledHeight(), 10);
            if (inside(absX, absY, module.getX(), module.getY(), mw, mh)) {
                draggingModule = module;
                moduleDragOffsetX = absX - module.getX();
                moduleDragOffsetY = absY - module.getY();
                return true;
            }
        }

        return true;
    }

    public boolean handleModulesPanelClick(int mx, int my) {
        int startX = SIDEBAR_W + 12;
        int startY = HEADER_H + 12;
        int cardW = (WIN_W - SIDEBAR_W - 40) / 3;
        int cardH = MODULE_CARD_H;
        int gap = MODULE_GAP;
        List<HudModule> mods = getFilteredModules();
        int rowHeight = cardH + gap;
        int contentBottom = WIN_H - 30;
        int firstVisibleRow = Math.max(0, moduleGridScroll / rowHeight);
        int lastVisibleRow = Math.min(((mods.size() + 2) / 3) - 1, (moduleGridScroll + (contentBottom - startY) + EDGE_MARGIN) / rowHeight);

        for (int i = 0; i < mods.size(); i++) {
            int row = i / 3;
            if (row < firstVisibleRow || row > lastVisibleRow) {
                continue;
            }

            HudModule mod = mods.get(i);
            int cx = startX + (i % 3) * (cardW + gap);
            int cy = startY + (i / 3) * rowHeight - moduleGridScroll;

            if (cy + cardH < startY - EDGE_MARGIN || cy > contentBottom + EDGE_MARGIN) {
                continue;
            }

            if (!inside(mx, my, cx, cy, cardW, cardH)) {
                continue;
            }

            int toggleX = cx + cardW - 28;
            int toggleY = cy + cardH - 27;
            if (inside(mx, my, toggleX, toggleY, 18, 18)) {
                mod.setEnabled(!mod.isEnabled());
                ClientClient.getHudManager().saveConfig();
                return true;
            }

            int icon1X = cx + cardW - 52;
            int icon2X = cx + cardW - 70;
            int iconY = cy + cardH - 29;
            
            if (inside(mx, my, icon1X, iconY, 16, 16)) {
                return true;
            }
            
            if (inside(mx, my, icon2X, iconY, 16, 16)) {
                return true;
            }

            int keyButtonX = cx + 10;
            int keyButtonY = cy + 7;
            if (inside(mx, my, keyButtonX, keyButtonY, 40, 16)) {
                awaitingKeybindModule = mod;
                return true;
            }

            return true;
        }
        return true;
    }

    public boolean handleSettingsPanelClick(int mx, int my) {
        handleSettingsClick(mx, my);
        return true;
    }

    public boolean handleConfigPanelClick(int mx, int my) {
        handleConfigClick(mx, my);
        return true;
    }

    private void renderSidebar(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        renderSidebarTab(ctx, mouseX, mouseY, Tab.MODULES, 68, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.SETTINGS, 122, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.POSITIONS, 176, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.CHAT, 230, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.PERFORMANCE, 284, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.CONFIG, 338, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.ABOUT, 392, drawX, drawY, theme.accent);
    }

    private void renderSidebarTab(GuiGraphics ctx, int mouseX, int mouseY, Tab tab, int localY, int drawX, int drawY, int accent) {
        int boxX = drawX + 10;
        int y = drawY + localY;
        int boxW = 40;
        int boxH = 40;
        boolean active = currentTab == tab;
        boolean hover = inside(mouseX, mouseY, boxX, y, boxW, boxH);

        int bg = active ? 0xC0222D3F : (hover ? 0xB4161D2A : 0xA4121620);
        RenderUtils.drawGlassPanel(ctx, boxX, y, boxW, boxH, 8, bg, accent);
        RenderUtils.drawGlassHoverOverlay(ctx, boxX, y, boxW, boxH, hover || active, accent);

        if (active) {
            ctx.fill(boxX - 3, y + 9, boxX - 1, y + boxH - 9, accent);
        }

        int iconX = boxX + (boxW - SIDEBAR_ICON_SIZE) / 2;
        int iconY = y + (boxH - SIDEBAR_ICON_SIZE) / 2;
        renderSidebarBadge(ctx, tab, iconX, iconY, active, accent);
    }

    private void renderTopBar(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        int barX = drawX + SIDEBAR_W;
        int barY = drawY;
        int barW = WIN_W - SIDEBAR_W;

        int closeX = drawX + 12;
        int closeY = drawY + 8;
        RenderUtils.drawGlassPanel(ctx, closeX, closeY, 42, 34, 8, 0xB3131720, theme.accent);
        ctx.drawString(this.font, "X", closeX + 15, closeY + 10, 0xFFE7ECF7, true);

        RenderUtils.drawGlassPanel(ctx, barX + 54, barY + 10, 184, 30, 7, 0xB4151B28, theme.accent);
        drawSlantedHeader(ctx, barX + 54, barY + 10, 184, 30, 18, 0xB4151B28, 0xFF0B111B);
        ctx.drawString(this.font, "Med Menu", barX + 70, barY + 20, 0xFFF4F7FF, true);

        int fx = barX + 260;

        for (String filter : FILTERS) {
            boolean active = currentFilter.equals(filter);
            boolean hover = inside(mouseX, mouseY, fx, drawY + 10, 62, 30);
            int color = active ? 0xC0233044 : (hover ? 0xB41E2634 : 0x9E151C27);
            RenderUtils.drawGlassPanel(ctx, fx, drawY + 10, 62, 30, 7, color, theme.accent);
            ctx.drawString(this.font, filter, fx + (62 - this.font.width(filter)) / 2, drawY + 19, 0xFFF3F6FF, false);
            fx += 68;
        }

        int searchW = 160;
        int searchX = drawX + WIN_W - searchW - 12;
        RenderUtils.drawGlassPanel(ctx, searchX, drawY + 10, searchW, 30, 7, searchFocused ? 0xC0223041 : 0xB3131822, theme.accent);
        String text = searchQuery.isEmpty() ? "Search" : searchQuery;
        int color = searchQuery.isEmpty() ? 0xFF8A95A7 : 0xFFFFFFFF;
        ctx.drawString(this.font, text, searchX + 22, drawY + 19, color, false);
        ctx.drawString(this.font, "\u2022", searchX + 10, drawY + 18, 0xFF9DA6B5, false);

        String countText = "0%";
        ctx.drawString(this.font, countText, drawX + WIN_W / 2 - this.font.width(countText) / 2, drawY + WIN_H - 18, 0xFF9EA7B7, false);
    }

    private void renderModuleGrid(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        int startX = drawX + SIDEBAR_W + 12;
        int startY = drawY + HEADER_H + 12;
        int contentBottom = drawY + WIN_H - 30;
        int cardW = (WIN_W - SIDEBAR_W - 40) / 3;
        int cardH = MODULE_CARD_H;
        int gap = MODULE_GAP;
        int rowHeight = cardH + gap;

        List<HudModule> mods = getFilteredModules();
        if (mods.isEmpty()) {
            ctx.drawString(this.font, "No mods in this category", startX, startY + 6, RenderUtils.MUTED_COLOR, false);
            return;
        }

        int rows = (mods.size() + 2) / 3;
        int visibleHeight = contentBottom - startY;
        int maxScroll = Math.max(0, ((rows - 1) * rowHeight + cardH) - visibleHeight);
        moduleGridScroll = clamp(moduleGridScroll, 0, maxScroll);
        int firstVisibleRow = Math.max(0, moduleGridScroll / rowHeight);
        int lastVisibleRow = Math.min(rows - 1, (moduleGridScroll + visibleHeight) / rowHeight);

        for (int i = 0; i < mods.size(); i++) {
            int row = i / 3;
            if (row < firstVisibleRow || row > lastVisibleRow) {
                continue;
            }

            HudModule mod = mods.get(i);
            int cx = startX + (i % 3) * (cardW + gap);
            int cy = startY + (i / 3) * rowHeight - moduleGridScroll;

            if (cy + cardH < startY - EDGE_MARGIN || cy > contentBottom + EDGE_MARGIN) {
                continue;
            }

            boolean hover = inside(mouseX, mouseY, cx, cy, cardW, cardH);
            RenderUtils.drawModuleCard(ctx, cx, cy, cardW, cardH, hover, mod.isEnabled(), theme.accent);

            RenderUtils.drawSmallButton(ctx, cx + 10, cy + 7, 40, 16, 0xC0181F2B);
            ctx.drawString(this.font, "Key", cx + 18, cy + 11, 0xFFD8DCE7, false);
            
            int dotSize = 3;
            int dotX = cx + cardW - 10;
            int dotY = cy + 10;
            ctx.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, 0x88FFFFFF);

            String label = getModuleAbbreviation(mod.getName());
            int labelY = cy + 36;
            ctx.drawString(this.font, label, cx + (cardW - this.font.width(label)) / 2, labelY, 0xFFF4F7FF, true);

            int sepY = cy + cardH - 40;
            RenderUtils.drawSeparator(ctx, cx + 10, sepY, cardW - 20, 0x66394660);
            
            int nameY = cy + cardH - 30;
            ctx.drawString(this.font, mod.getName(), cx + 10, nameY, 0xFFE7ECF7, false);
            
            int toggleX = cx + cardW - 28;
            int toggleY = cy + cardH - 27;
            drawEnhancedToggle(ctx, toggleX, toggleY, mod.isEnabled(), theme.accent);
            
            int icon1X = cx + cardW - 52;
            int icon2X = cx + cardW - 70;
            int iconY = cy + cardH - 29;
            RenderUtils.drawSmallButton(ctx, icon1X, iconY, 16, 16, 0xA0202838);
            RenderUtils.drawSmallButton(ctx, icon2X, iconY, 16, 16, 0xA0202838);
            
            ctx.fill(icon1X + 5, iconY + 5, icon1X + 11, iconY + 7, 0xFFAAB7CC);
            ctx.fill(icon1X + 5, iconY + 9, icon1X + 11, iconY + 11, 0xFFAAB7CC);
            
            if (mod.isEnabled()) {
                ctx.fill(icon2X + 6, iconY + 6, icon2X + 10, iconY + 10, 0xFF43B581);
            } else {
                ctx.fill(icon2X + 6, iconY + 6, icon2X + 10, iconY + 10, 0xFF6A7590);
            }

            String keyText = mod.getKeybind() >= 0 ? KeyNameUtils.format(mod.getKeybind()) : "None";
            String status = (mod.isEnabled() ? "On" : "Orr") + " I " + keyText + " I " + formatScale(mod.getScale());
            ctx.drawString(this.font, status, cx + 10, cy + cardH - 14, 0xFF8A95A7, false);
        }

        if (awaitingKeybindModule != null) {
            ctx.drawString(this.font, "Press any key to bind. ESC clears.", drawX + 86, drawY + WIN_H - 18, theme.accent, true);
        }

        if (maxScroll > 0) {
            int trackX = drawX + WIN_W - 8;
            int trackY = startY;
            int trackH = contentBottom - startY;
            RenderUtils.drawRoundedRect(ctx, trackX, trackY, 3, trackH, 1, 0x331A1A1A);

            int thumbH = Math.max(18, (int) ((trackH / (float) (trackH + maxScroll)) * trackH));
            int thumbY = trackY + (int) ((moduleGridScroll / (float) maxScroll) * (trackH - thumbH));
            RenderUtils.drawRoundedRect(ctx, trackX, thumbY, 3, thumbH, 1, theme.accent);
        }
    }

    private void renderSettings(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        int x = drawX + 68;
        int y = drawY + 60;

        ctx.drawString(this.font, "Crosshair", x, y, 0xFFFFFFFF, true);
        y += 20;

        RenderUtils.drawRoundedRect(ctx, x, y, 120, 20, 0, customCrosshairEnabled ? theme.accent : 0xFF333333);
        ctx.drawString(this.font, customCrosshairEnabled ? "Custom: ON" : "Custom: OFF", x + 10, y + 6, 0xFFFFFFFF, false);

        int tx = x;
        for (CrosshairCustomizer.CrosshairType type : CrosshairCustomizer.CrosshairType.values()) {
            boolean active = type == crosshairType;
            boolean hover = inside(mouseX, mouseY, tx, y, 70, 20);
            RenderUtils.drawRoundedRect(ctx, tx, y, 70, 20, 0, active ? theme.accent : (hover ? 0xFF2A2A2A : 0xFF222222));
            ctx.drawString(this.font, type.name(), tx + 8, y + 6, 0xFFFFFFFF, false);
            tx += 76;
        }

        y += 32;
        renderColorSlider(ctx, "R", x, y, crosshairRed, 1, drawX, theme.accent);
        y += 28;
        renderColorSlider(ctx, "G", x, y, crosshairGreen, 2, drawX, theme.accent);
        y += 28;
        renderColorSlider(ctx, "B", x, y, crosshairBlue, 3, drawX, theme.accent);

        y += 36;
        ctx.drawString(this.font, "Preview", x, y, 0xFFFFFFFF, false);
        renderCrosshairPreview(ctx, x + 75, y + 18);

        y += 45;
        RenderUtils.drawSeparator(ctx, x, y, WIN_W - 90, RenderUtils.SEPARATOR_COLOR);
        y += 10;

        ctx.drawString(this.font, "Theme", x, y, 0xFFFFFFFF, false);
        int bx = x + 45;
        for (XenonTheme entry : XenonTheme.values()) {
            boolean active = entry.name().equalsIgnoreCase(ClientClient.getInstance().getThemeId());
            RenderUtils.drawRoundedRect(ctx, bx, y - 4, 60, 18, 0, active ? entry.accent : 0xFF2A2A2A);
            ctx.drawString(this.font, entry.name(), bx + 8, y + 1, 0xFFFFFFFF, false);
            bx += 66;
        }
    }

    private void renderPositionOverlays(GuiGraphics ctx, int mouseX, int mouseY) {
        for (HudModule module : ClientClient.getHudManager().getModules()) {
            if (!module.isEnabled()) {
                continue;
            }

            int mx = module.getX();
            int my = module.getY();
            int mw = Math.max(module.getScaledWidth(), 20);
            int mh = Math.max(module.getScaledHeight(), 10);

            boolean hover = inside(mouseX, mouseY, mx, my, mw, mh);
            int color = hover ? 0xFFFF5555 : 0xFFE53935;
            RenderUtils.drawRoundedRectOutline(ctx, mx, my, mw, mh, 0, color);
            RenderUtils.drawTextWithBackground(ctx, this.font, module.getName(), mx, my - 10, 0xFFFFFFFF, 0xCC000000, 2);
        }

        if (draggingModule != null) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            ctx.fill(centerX, 0, centerX + 1, this.height, 0x66FF5555);
            ctx.fill(0, centerY, this.width, centerY + 1, 0x66FF5555);

            int mx = draggingModule.getX();
            int my = draggingModule.getY();
            int mw = Math.max(draggingModule.getScaledWidth(), 20);
            int mh = Math.max(draggingModule.getScaledHeight(), 10);
            ctx.fill(mx + mw / 2, 0, mx + mw / 2 + 1, this.height, 0x44FFFFFF);
            ctx.fill(0, my + mh / 2, this.width, my + mh / 2 + 1, 0x44FFFFFF);
        }
    }

    private void renderPositionEditorScene(GuiGraphics ctx, int mouseX, int mouseY) {
        this.renderTransparentBackground(ctx);
        ctx.fill(0, 0, this.width, this.height, applyAlpha(0x66000000, openAnim));

        renderPositionOverlays(ctx, mouseX, mouseY);

        int panelW = 340;
        int panelH = 64;
        int panelX = (this.width - panelW) / 2;
        int panelY = 12;

        RenderUtils.drawRoundedRectWithBorder(ctx, panelX, panelY, panelW, panelH, 0, 0xBB0F0F0F, 0xFF2E2E2E);
        ctx.drawString(this.font, "Position Editor", panelX + 12, panelY + 10, 0xFFFFFFFF, false);
        ctx.drawString(this.font, "Drag enabled HUD modules. Press Done to return.", panelX + 12, panelY + 24, 0xFFBBBBBB, false);

        int doneX = getPositionDoneX();
        int doneY = getPositionDoneY();
        boolean doneHover = inside(mouseX, mouseY, doneX, doneY, POSITION_DONE_W, POSITION_DONE_H);
        RenderUtils.drawRoundedRect(ctx, doneX, doneY, POSITION_DONE_W, POSITION_DONE_H, 0, doneHover ? 0xFF4BC28D : 0xFF43B581);
        ctx.drawString(this.font, "Done", doneX + (POSITION_DONE_W - this.font.width("Done")) / 2, doneY + 9, 0xFFFFFFFF, false);
    }

    private int getPositionDoneX() {
        return (this.width - POSITION_DONE_W) / 2;
    }

    private int getPositionDoneY() {
        return 45;
    }

    private void renderPerformance(GuiGraphics ctx, int drawX, int drawY) {
        Minecraft client = Minecraft.getInstance();

        int fps = client.getFps();
        int ping = 0;
        ClientPacketListener connection = client.getConnection();
        if (connection != null && client.player != null) {
            var info = connection.getPlayerInfo(client.player.getUUID());
            ping = info != null ? info.getLatency() : 0;
        }

        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);
        float frameTime = fps > 0 ? 1000.0f / fps : 0.0f;

        int x = drawX + 68;
        int y = drawY + 60;

        ctx.drawString(this.font, "Performance", x, y, 0xFFFFFFFF, true);
        y += 22;
        ctx.drawString(this.font, "FPS: " + fps, x, y, 0xFFFFFFFF, false);
        y += 14;
        ctx.drawString(this.font, "Frame Time: " + String.format(Locale.ROOT, "%.2fms", frameTime), x, y, 0xFFFFFFFF, false);
        y += 14;
        ctx.drawString(this.font, "Ping: " + ping + "ms", x, y, 0xFFFFFFFF, false);
        y += 14;
        ctx.drawString(this.font, "Memory: " + usedMb + "MB / " + maxMb + "MB", x, y, 0xFFFFFFFF, false);

        y += 20;
        float memPct = maxMb > 0 ? usedMb / (float) maxMb : 0.0f;
        RenderUtils.drawProgressBar(ctx, x, y, 260, 8, memPct, 0xFF222222, 0xFF43B581);
    }

    private void renderConfig(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        int x = drawX + 68;
        int y = drawY + 60;

        ctx.drawString(this.font, "XENON Config", x, y, 0xFFFFFFFF, true);
        y += 24;

        boolean saveHover = inside(mouseX, mouseY, x, y, 180, 38);
        RenderUtils.drawRoundedRect(ctx, x, y, 180, 38, 0, saveHover ? theme.accentHover : theme.accent);
        ctx.drawString(this.font, "Save Config", x + 52, y + 14, 0xFFFFFFFF, false);

        y += 46;
        boolean resetHover = inside(mouseX, mouseY, x, y, 180, 38);
        RenderUtils.drawRoundedRect(ctx, x, y, 180, 38, 0, resetHover ? 0xFFB63B39 : RenderUtils.DANGER_COLOR);
        String resetText = confirmReset ? "Confirm Reset" : "Reset Positions";
        ctx.drawString(this.font, resetText, x + 45, y + 14, 0xFFFFFFFF, false);

        y += 56;
        ctx.drawString(this.font, "Path: .minecraft/config/client.json", x, y, RenderUtils.MUTED_COLOR, false);
        y += 14;
        ctx.drawString(this.font, "Backup: .minecraft/config/client.backup.json", x, y, RenderUtils.MUTED_COLOR, false);

        y += 20;
        ctx.drawString(this.font, "Preset", x, y, 0xFFFFFFFF, false);
        RenderUtils.drawRoundedRect(ctx, x + 48, y - 4, 120, 18, 4, presetNameFocused ? 0xFF303030 : 0xFF1C1C1C);
        ctx.drawString(this.font, presetName, x + 54, y + 1, 0xFFE7E7E7, false);
        RenderUtils.drawRoundedRect(ctx, x + 176, y - 4, 38, 18, 4, 0xFF2A2A2A);
        ctx.drawString(this.font, "Save", x + 186, y + 1, 0xFFFFFFFF, false);
        RenderUtils.drawRoundedRect(ctx, x + 218, y - 4, 38, 18, 4, 0xFF2A2A2A);
        ctx.drawString(this.font, "Load", x + 228, y + 1, 0xFFFFFFFF, false);
        RenderUtils.drawRoundedRect(ctx, x + 260, y - 4, 20, 18, 4, 0xFF2A2A2A);
        ctx.drawString(this.font, "-", x + 267, y + 1, 0xFFFFFFFF, false);

        y += 28;
        boolean light = ClientClient.getInstance().isLightModeEnabled();
        RenderUtils.drawRoundedRect(ctx, x, y, 180, 20, 4, light ? theme.accent : 0xFF2A2A2A);
        ctx.drawString(this.font, light ? "Light Mode: ON" : "Light Mode: OFF", x + 8, y + 6, 0xFFFFFFFF, false);

        int threshold = ClientClient.getInstance().getLightModeThresholdFps();
        RenderUtils.drawRoundedRect(ctx, x + 186, y, 110, 20, 4, 0xFF1C1C1C);
        ctx.drawString(this.font, "FPS<" + threshold, x + 196, y + 6, 0xFFBBBBBB, false);
        RenderUtils.drawRoundedRect(ctx, x + 302, y, 16, 20, 4, 0xFF2A2A2A);
        ctx.drawString(this.font, "-", x + 307, y + 6, 0xFFFFFFFF, false);
        RenderUtils.drawRoundedRect(ctx, x + 322, y, 16, 20, 4, 0xFF2A2A2A);
        ctx.drawString(this.font, "+", x + 327, y + 6, 0xFFFFFFFF, false);
    }

    private void renderAbout(GuiGraphics ctx, int drawX, int drawY, XenonTheme theme) {
        int x = drawX + 68;
        int y = drawY + 70;

        ctx.drawString(this.font, "XENON", x, y, theme.accent, true);
        y += 24;
        ctx.drawString(this.font, "XENON v1.0", x, y, 0xFFFFFFFF, false);
        y += 16;
        ctx.drawString(this.font, "Minecraft 1.21.11", x, y, RenderUtils.MUTED_COLOR, false);
        y += 20;
        RenderUtils.drawSeparator(ctx, x, y, 260, RenderUtils.SEPARATOR_COLOR);

        List<HudModule> modules = ClientClient.getHudManager().getModules();
        int enabled = 0;
        for (HudModule module : modules) {
            if (module.isEnabled()) {
                enabled++;
            }
        }

        y += 12;
        ctx.drawString(this.font, "Modules: " + enabled + "/" + modules.size(), x, y, 0xFFFFFFFF, false);
        y += 14;
        ctx.drawString(this.font, "Config autosave + backup active", x, y, 0xFFFFFFFF, false);
    }

    private void renderComingSoon(GuiGraphics ctx, int drawX, int drawY, String text) {
        int x = drawX + SIDEBAR_W + (WIN_W - SIDEBAR_W - this.font.width(text)) / 2;
        int y = drawY + WIN_H / 2;
        ctx.drawString(this.font, text, x, y, RenderUtils.MUTED_COLOR, false);
    }

    private void renderColorSlider(GuiGraphics ctx, String label, int x, int y, int value, int slider, int drawX, int accentColor) {
        ctx.drawString(this.font, label + ": " + value, x, y, 0xFFFFFFFF, false);
        int sliderX = drawX + 80;
        int sliderW = 220;
        RenderUtils.drawRoundedRect(ctx, sliderX, y + 4, sliderW, 10, 0, 0xFF222222);
        int fillW = (int) ((value / 255.0f) * sliderW);
        RenderUtils.drawRoundedRect(ctx, sliderX, y + 4, fillW, 10, 0, accentColor);
        int handleX = Math.max(sliderX, sliderX + fillW - 2);
        RenderUtils.drawRoundedRect(ctx, handleX, y + 2, 4, 14, 0, 0xFFFFFFFF);

        if (draggingSlider == slider) {
            ctx.drawString(this.font, "*", sliderX + sliderW + 8, y, accentColor, false);
        }
    }

    private void renderCrosshairPreview(GuiGraphics ctx, int centerX, int centerY) {
        if (!customCrosshairEnabled) {
            ctx.drawString(this.font, "Disabled", centerX - 20, centerY - 2, RenderUtils.MUTED_COLOR, false);
            return;
        }

        int color = (0xFF << 24) | (crosshairRed << 16) | (crosshairGreen << 8) | crosshairBlue;
        switch (crosshairType) {
            case VANILLA -> {
                ctx.fill(centerX - 6, centerY - 1, centerX - 2, centerY + 1, color);
                ctx.fill(centerX + 2, centerY - 1, centerX + 6, centerY + 1, color);
                ctx.fill(centerX - 1, centerY - 6, centerX + 1, centerY - 2, color);
                ctx.fill(centerX - 1, centerY + 2, centerX + 1, centerY + 6, color);
            }
            case DOT -> ctx.fill(centerX, centerY, centerX + 1, centerY + 1, color);
            case CROSS -> {
                ctx.fill(centerX - 5, centerY, centerX + 5, centerY + 1, color);
                ctx.fill(centerX, centerY - 5, centerX + 1, centerY + 5, color);
            }
            case CIRCLE -> {
                for (int angle = 0; angle < 360; angle += 15) {
                    int px = centerX + (int) (5 * Math.cos(Math.toRadians(angle)));
                    int py = centerY + (int) (5 * Math.sin(Math.toRadians(angle)));
                    ctx.fill(px, py, px + 1, py + 1, color);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int absX = (int) event.x();
        int absY = (int) event.y();

        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }

        if (currentTab == Tab.POSITIONS) {
            return positionsPanel.mouseClicked(absX, absY, event.button());
        }

        if (!inside(absX, absY, winX, winY, WIN_W, WIN_H)) {
            searchFocused = false;
            return true;
        }

        if (wizardVisible) {
            return handleFirstRunWizardClick(absX, absY);
        }

        int mx = absX - winX;
        int my = absY - winY;

        int closeX = 12;
        int closeY = 8;
        if (inside(mx, my, closeX, closeY, 42, 34)) {
            this.onClose();
            return true;
        }

        if (mx < SIDEBAR_W) {
            int boxX = 10;
            int boxW = 40;
            int boxH = 40;
            if (inside(mx, my, boxX, 68, boxW, boxH)) { currentTab = Tab.MODULES; return true; }
            if (inside(mx, my, boxX, 122, boxW, boxH)) { currentTab = Tab.SETTINGS; return true; }
            if (inside(mx, my, boxX, 176, boxW, boxH)) { currentTab = Tab.POSITIONS; return true; }
            if (inside(mx, my, boxX, 230, boxW, boxH)) { currentTab = Tab.CHAT; return true; }
            if (inside(mx, my, boxX, 284, boxW, boxH)) { currentTab = Tab.PERFORMANCE; return true; }
            if (inside(mx, my, boxX, 338, boxW, boxH)) { currentTab = Tab.CONFIG; return true; }
            if (inside(mx, my, boxX, 392, boxW, boxH)) { currentTab = Tab.ABOUT; return true; }
            return true;
        }

        if (my < HEADER_H) {
            int fx = SIDEBAR_W + 260;
            for (String f : FILTERS) {
                if (inside(mx, my, fx, 10, 62, 30)) {
                    currentFilter = f;
                    invalidateFilteredModules();
                    return true;
                }
                fx += 68;
            }

            int searchW = 160;
            int searchX = WIN_W - searchW - 12;
            if (inside(mx, my, searchX, 10, searchW, 30)) {
                searchFocused = true;
                return true;
            }

            searchFocused = false;
            draggingWindow = true;
            windowDragOffsetX = absX - winX;
            windowDragOffsetY = absY - winY;
            return true;
        }

        searchFocused = false;

        if (currentTab == Tab.MODULES) {
            return modulesPanel.mouseClicked(mx, my, event.button());
        }

        if (currentTab == Tab.SETTINGS) {
            return settingsPanel.mouseClicked(mx, my, event.button());
        }

        if (currentTab == Tab.CONFIG) {
            return configPanel.mouseClicked(mx, my, event.button());
        }

        if (currentTab == Tab.PERFORMANCE) {
            return true;
        }

        return true;
    }

    private void handleSettingsClick(int mx, int my) {
        int x = 68;
        int y = 60;

        if (inside(mx, my, x, y + 20, 120, 20)) {
            customCrosshairEnabled = !customCrosshairEnabled;
            applyCrosshairSettings();
            return;
        }

        int typeY = y + 54;
        int typeX = x;
        for (CrosshairCustomizer.CrosshairType type : CrosshairCustomizer.CrosshairType.values()) {
            if (inside(mx, my, typeX, typeY, 70, 20)) {
                crosshairType = type;
                applyCrosshairSettings();
                return;
            }
            typeX += 76;
        }

        int sliderX = 80;
        if (inside(mx, my, sliderX, y + 90, 220, 10)) {
            draggingSlider = 1;
            crosshairRed = valueFromSlider(mx);
            applyCrosshairSettings();
            return;
        }
        if (inside(mx, my, sliderX, y + 118, 220, 10)) {
            draggingSlider = 2;
            crosshairGreen = valueFromSlider(mx);
            applyCrosshairSettings();
            return;
        }
        if (inside(mx, my, sliderX, y + 146, 220, 10)) {
            draggingSlider = 3;
            crosshairBlue = valueFromSlider(mx);
            applyCrosshairSettings();
            return;
        }

        int buttonY = y + 201;
        if (inside(mx, my, x, buttonY, 140, 24)) {
            currentTab = Tab.CONFIG;
            return;
        }
        if (inside(mx, my, x + 150, buttonY, 140, 24)) {
            currentTab = Tab.ABOUT;
            return;
        }

        int themeY = y + 252;
        int bx = x + 45;
        for (XenonTheme entry : XenonTheme.values()) {
            if (inside(mx, my, bx, themeY - 4, 60, 18)) {
                ClientClient.getInstance().setThemeId(entry.name());
                ClientClient.getHudManager().saveConfig();
                return;
            }
            bx += 66;
        }
    }

    private void handleConfigClick(int mx, int my) {
        int x = 68;
        int y = 84;

        if (inside(mx, my, x, y, 180, 38)) {
            ClientClient.getHudManager().saveConfig();
            confirmReset = false;
            return;
        }

        if (inside(mx, my, x, y + 46, 180, 38)) {
            if (!confirmReset) {
                confirmReset = true;
                return;
            }
            ClientClient.getHudManager().resetPositions();
            ClientClient.getHudManager().saveConfig();
            confirmReset = false;
            toast("Positions reset");
            return;
        }

        int presetY = y + 122;
        if (inside(mx, my, x + 176, presetY - 4, 38, 18)) {
            if (ClientClient.getHudManager().savePreset(presetName)) {
                toast("Preset saved: " + presetName);
            }
            presetNameFocused = false;
            return;
        }
        if (inside(mx, my, x + 218, presetY - 4, 38, 18)) {
            if (ClientClient.getHudManager().loadPreset(presetName)) {
                toast("Preset loaded: " + presetName);
            }
            presetNameFocused = false;
            return;
        }
        if (inside(mx, my, x + 260, presetY - 4, 20, 18)) {
            if (ClientClient.getHudManager().deletePreset(presetName)) {
                toast("Preset deleted: " + presetName);
                presetName = "default";
            }
            presetNameFocused = false;
            return;
        }

        presetNameFocused = inside(mx, my, x + 48, presetY - 4, 120, 18);

        int lightY = presetY + 28;
        if (inside(mx, my, x, lightY, 180, 20)) {
            ClientClient state = ClientClient.getInstance();
            state.setLightModeEnabled(!state.isLightModeEnabled());
            ClientClient.getHudManager().saveConfig();
            toast("Light mode " + (state.isLightModeEnabled() ? "enabled" : "disabled"));
            return;
        }
        if (inside(mx, my, x + 302, lightY, 16, 20)) {
            ClientClient state = ClientClient.getInstance();
            state.setLightModeThresholdFps(state.getLightModeThresholdFps() - 5);
            ClientClient.getHudManager().saveConfig();
            return;
        }
        if (inside(mx, my, x + 322, lightY, 16, 20)) {
            ClientClient state = ClientClient.getInstance();
            state.setLightModeThresholdFps(state.getLightModeThresholdFps() + 5);
            ClientClient.getHudManager().saveConfig();
            return;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int absX = (int) event.x();
        int absY = (int) event.y();

        if (draggingWindow) {
            winX = clamp(absX - windowDragOffsetX, 0, Math.max(0, this.width - WIN_W));
            winY = clamp(absY - windowDragOffsetY, 0, Math.max(0, this.height - WIN_H));
            return true;
        }

        if (draggingModule != null) {
            int targetX = absX - moduleDragOffsetX;
            int targetY = absY - moduleDragOffsetY;
            int snappedX = snapX(targetX, draggingModule);
            int snappedY = snapY(targetY, draggingModule);
            draggingModule.setX(snappedX);
            draggingModule.setY(snappedY);
            return true;
        }

        if (draggingSlider > 0) {
            int sliderX = winX + 80;
            int sliderW = 220;
            float pct = Math.max(0.0f, Math.min(1.0f, (((float) absX) - sliderX) / sliderW));
            int val = (int) (pct * 255.0f);
            switch (draggingSlider) {
                case 1 -> crosshairRed = val;
                case 2 -> crosshairGreen = val;
                case 3 -> crosshairBlue = val;
                default -> {
                }
            }
            CrosshairCustomizer.setColor((crosshairRed << 16) | (crosshairGreen << 8) | crosshairBlue);
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab != Tab.MODULES) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int absX = (int) mouseX;
        int absY = (int) mouseY;
        if (!inside(absX, absY, winX + SIDEBAR_W, winY + HEADER_H, WIN_W - SIDEBAR_W, WIN_H - HEADER_H)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int cardH = MODULE_CARD_H;
        int gap = MODULE_GAP;
        int rowHeight = cardH + gap;
        int contentBottom = winY + WIN_H - 12;
        int startY = winY + HEADER_H + 8;
        int maxScroll = Math.max(0, ((getFilteredModules().size() + 2) / 3) * rowHeight - (contentBottom - startY));
        if (maxScroll <= 0) {
            return true;
        }

        moduleGridScroll = clamp(moduleGridScroll - (int) (verticalAmount * 20.0), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = false;

        if (draggingWindow) {
            draggingWindow = false;
            handled = true;
        }

        if (draggingModule != null) {
            draggingModule = null;
            ClientClient.getHudManager().saveConfig();
            handled = true;
        }

        if (draggingSlider > 0) {
            draggingSlider = 0;
            handled = true;
        }

        if (handled) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int key = keyEvent.key();

        if ((keyEvent.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_F) {
            searchFocused = true;
            return true;
        }

        if (awaitingKeybindModule != null) {
            if (key == 256) {
                awaitingKeybindModule.setKeybind(-1);
                toast(awaitingKeybindModule.getName() + " key cleared");
            } else {
                awaitingKeybindModule.setKeybind(key);
                toast(awaitingKeybindModule.getName() + " -> " + KeyNameUtils.format(key));
            }
            ClientClient.getHudManager().saveConfig();
            awaitingKeybindModule = null;
            return true;
        }

        if (searchFocused && key == 259) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                invalidateFilteredModules();
            }
            return true;
        }

        if (presetNameFocused && key == 259) {
            if (!presetName.isEmpty()) {
                presetName = presetName.substring(0, presetName.length() - 1);
            }
            return true;
        }

        if (presetNameFocused && key == 257) {
            presetNameFocused = false;
            return true;
        }

        if (searchFocused && key == 256) {
            if (!searchQuery.isEmpty()) {
                searchQuery = "";
                invalidateFilteredModules();
                return true;
            }
            searchFocused = false;
            return true;
        }

        if (key == 256) {
            onClose();
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused) {
            char c = (char) event.codepoint();
            if (!Character.isISOControl(c)) {
                searchQuery += c;
                invalidateFilteredModules();
            }
            return true;
        }

        if (presetNameFocused) {
            char c = (char) event.codepoint();
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                if (presetName.length() < 16) {
                    presetName += c;
                }
            }
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void removed() {
        applyCrosshairSettings();
        ClientClient state = ClientClient.getInstance();
        state.setCurrentFilter(currentFilter);
        state.setSearchQuery(searchQuery);
        state.setMenuX(winX);
        state.setMenuY(winY);
        ClientClient.getHudManager().saveConfig();
    }

    private List<HudModule> getFilteredModules() {
        String query = searchQuery.toLowerCase(Locale.ROOT);
        if (!filteredModulesDirty
            && filteredModulesCacheFilter.equals(currentFilter)
            && filteredModulesCacheQuery.equals(query)) {
            return filteredModulesCache;
        }

        HudManager manager = ClientClient.getHudManager();
        filteredModulesCache.clear();
        for (HudModule module : manager.getModules()) {
            if (!manager.matchesFilter(module, currentFilter)) {
                continue;
            }
            if (!query.isEmpty() && !module.getName().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            filteredModulesCache.add(module);
        }

        filteredModulesCacheFilter = currentFilter;
        filteredModulesCacheQuery = query;
        filteredModulesDirty = false;
        return filteredModulesCache;
    }

    private void invalidateFilteredModules() {
        filteredModulesDirty = true;
        moduleGridScroll = 0;
    }

    private String formatScale(float scale) {
        int centi = Math.round(scale * 100.0f);
        int whole = centi / 100;
        int frac = Math.abs(centi % 100);
        return whole + "." + (frac < 10 ? "0" : "") + frac + "x";
    }

    private String getModuleAbbreviation(String name) {
        return switch (name) {
            case "CPS Counter" -> "CPS";
            case "FPS & Ping" -> "FFS";
            case "Armor Status", "Armor Bars" -> "ARM";
            case "Keystrokes" -> "KEY";
            case "Sprint/Sneak Status" -> "MOV";
            case "Potion Status" -> "POT";
            case "Target Hud", "Target HUD" -> "TCT";
            case "Combo Counter" -> "CMBO";
            case "Direction + Coords" -> "DIR";
            case "Session Stats" -> "SES";
            case "Reach Display" -> "RCM";
            case "Memory Usage Display" -> "MEM";
            case "Speed Display" -> "SPD";
            case "Tps Display" -> "TPS";
            case "Compass Display" -> "CMP";
            default -> {
                String[] parts = name.split("[^A-Za-z0-9]+");
                StringBuilder out = new StringBuilder();
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        out.append(Character.toUpperCase(part.charAt(0)));
                        if (out.length() == 4) {
                            break;
                        }
                    }
                }
                yield out.length() == 0 ? "HUD" : out.toString();
            }
        };
    }

    private void renderSidebarBadge(GuiGraphics ctx, Tab tab, int x, int y, boolean active, int accent) {
        int bg = active ? 0xFF2A3344 : 0xFF1A2230;
        RenderUtils.drawRoundedRect(ctx, x, y, SIDEBAR_ICON_SIZE, SIDEBAR_ICON_SIZE, 4, bg);
        int fx = active ? accent : 0xFFDCE7FF;

        switch (tab) {
            case MODULES -> {
                ctx.fill(x + 4, y + 4, x + 12, y + 6, fx);
                ctx.fill(x + 4, y + 7, x + 10, y + 9, fx);
                ctx.fill(x + 4, y + 10, x + 8, y + 12, fx);
            }
            case SETTINGS -> {
                ctx.fill(x + 7, y + 3, x + 9, y + 13, fx);
                ctx.fill(x + 3, y + 7, x + 13, y + 9, fx);
                RenderUtils.drawRoundedRect(ctx, x + 5, y + 5, 6, 6, 2, 0x33FFFFFF);
            }
            case POSITIONS -> {
                ctx.fill(x + 7, y + 3, x + 9, y + 13, fx);
                ctx.fill(x + 3, y + 7, x + 13, y + 9, fx);
                ctx.fill(x + 6, y + 6, x + 10, y + 10, 0x66FFFFFF);
            }
            case CHAT -> {
                RenderUtils.drawRoundedRect(ctx, x + 3, y + 4, 10, 7, 3, fx);
                ctx.fill(x + 6, y + 11, x + 8, y + 13, fx);
            }
            case PERFORMANCE -> {
                ctx.fill(x + 4, y + 10, x + 12, y + 12, fx);
                ctx.fill(x + 5, y + 7, x + 7, y + 10, fx);
                ctx.fill(x + 8, y + 5, x + 10, y + 10, fx);
            }
            case CONFIG -> {
                RenderUtils.drawRoundedRect(ctx, x + 4, y + 4, 8, 8, 2, fx);
                ctx.fill(x + 5, y + 12, x + 11, y + 13, fx);
            }
            case ABOUT -> {
                ctx.fill(x + 7, y + 3, x + 9, y + 13, fx);
                ctx.fill(x + 3, y + 7, x + 13, y + 9, fx);
                ctx.fill(x + 5, y + 5, x + 11, y + 11, 0x44FFFFFF);
            }
        }
    }

    private void renderModuleBadge(GuiGraphics ctx, String name, int x, int y, int size, int accent) {
        RenderUtils.drawRoundedRect(ctx, x, y, size, size, 6, 0xFF1A2230);
        RenderUtils.drawRoundedRectOutline(ctx, x, y, size, size, 6, 0x66D8E5FF);

        int cx = x + size / 2;
        int cy = y + size / 2;
        int ink = 0xFFF1F4FF;
        int glow = accent;

        switch (name) {
            case "CPS Counter" -> {
                RenderUtils.drawRoundedRect(ctx, x + 5, y + 6, 7, 10, 3, glow);
                RenderUtils.drawRoundedRect(ctx, x + 14, y + 8, 7, 8, 3, ink);
                ctx.fill(x + 18, y + 5, x + 20, y + 7, glow);
                ctx.fill(x + 19, y + 4, x + 21, y + 6, glow);
                ctx.fill(x + 20, y + 5, x + 22, y + 7, glow);
            }
            case "CPS Graph", "Ping Graph" -> {
                ctx.fill(x + 5, y + size - 6, x + size - 5, y + size - 4, glow);
                ctx.fill(x + 5, y + size - 6, x + 7, y + 7, glow);
                ctx.fill(x + 8, y + 10, x + 11, y + 13, ink);
                ctx.fill(x + 12, y + 8, x + 15, y + 11, ink);
                ctx.fill(x + 16, y + 12, x + 19, y + 15, ink);
            }
            case "FPS & Ping" -> {
                RenderUtils.drawRoundedRect(ctx, x + 4, y + 5, 18, 12, 3, glow);
                ctx.fill(x + 7, y + 8, x + 19, y + 11, ink);
                ctx.fill(x + 20, y + 15, x + 25, y + 17, glow);
            }
            case "Armor Status", "Armor Bars" -> {
                ctx.fill(x + 6, y + 5, x + 20, y + 8, glow);
                ctx.fill(x + 5, y + 8, x + 21, y + 17, glow);
                ctx.fill(x + 8, y + 9, x + 18, y + 15, ink);
            }
            case "Keystrokes" -> {
                RenderUtils.drawRoundedRect(ctx, x + 5, y + 5, 6, 6, 2, glow);
                RenderUtils.drawRoundedRect(ctx, x + 12, y + 5, 6, 6, 2, glow);
                RenderUtils.drawRoundedRect(ctx, x + 5, y + 12, 6, 6, 2, glow);
                RenderUtils.drawRoundedRect(ctx, x + 12, y + 12, 6, 6, 2, ink);
            }
            case "Sprint/Sneak Status" -> {
                ctx.fill(x + 5, y + 10, x + 13, y + 12, glow);
                ctx.fill(x + 12, y + 7, x + 17, y + 9, glow);
                ctx.fill(x + 13, y + 6, x + 15, y + 10, glow);
            }
            case "Potion Status" -> {
                ctx.fill(x + 9, y + 4, x + 13, y + 6, glow);
                RenderUtils.drawRoundedRect(ctx, x + 7, y + 6, 8, 11, 3, glow);
                ctx.fill(x + 8, y + 9, x + 14, y + 11, ink);
            }
            case "Target HUD" -> {
                ctx.fill(x + 4, y + 9, x + 22, y + 11, glow);
                ctx.fill(x + 9, y + 4, x + 11, y + 22, glow);
                RenderUtils.drawRoundedRect(ctx, cx - 2, cy - 2, 4, 4, 2, ink);
            }
            case "Combo Counter" -> {
                ctx.fill(x + 6, y + 14, x + 10, y + 18, glow);
                ctx.fill(x + 11, y + 10, x + 15, y + 14, ink);
                ctx.fill(x + 16, y + 6, x + 20, y + 10, glow);
            }
            case "Direction + Coords" -> {
                ctx.fill(x + 12, y + 4, x + 14, y + 18, glow);
                ctx.fill(x + 8, y + 8, x + 18, y + 10, glow);
                ctx.fill(x + 10, y + 6, x + 14, y + 8, ink);
            }
            case "Session Stats" -> {
                ctx.fill(x + 5, y + 15, x + 8, y + 18, glow);
                ctx.fill(x + 9, y + 11, x + 12, y + 18, ink);
                ctx.fill(x + 13, y + 7, x + 16, y + 18, glow);
                ctx.fill(x + 17, y + 4, x + 20, y + 18, ink);
            }
            case "Reach Display" -> {
                ctx.fill(x + 4, y + 13, x + 21, y + 15, glow);
                ctx.fill(x + 9, y + 9, x + 11, y + 18, ink);
            }
            case "Item Counters" -> {
                RenderUtils.drawRoundedRect(ctx, x + 5, y + 6, 8, 8, 2, glow);
                RenderUtils.drawRoundedRect(ctx, x + 10, y + 10, 8, 8, 2, ink);
                RenderUtils.drawRoundedRect(ctx, x + 15, y + 6, 8, 8, 2, glow);
            }
            case "Minimap Radar" -> {
                RenderUtils.drawRoundedRect(ctx, x + 4, y + 4, 18, 18, 9, 0x22FFFFFF);
                ctx.fill(x + 6, y + 16, x + 19, y + 18, glow);
                ctx.fill(x + 14, y + 6, x + 16, y + 16, glow);
                ctx.fill(x + 10, y + 10, x + 12, y + 12, ink);
            }
            case "Speed" -> {
                ctx.fill(x + 6, y + 14, x + 16, y + 16, glow);
                ctx.fill(x + 12, y + 6, x + 14, y + 14, glow);
                ctx.fill(x + 14, y + 8, x + 18, y + 12, ink);
            }
            case "TPS" -> {
                RenderUtils.drawRoundedRect(ctx, x + 5, y + 5, 16, 16, 8, 0x22FFFFFF);
                ctx.fill(x + 12, y + 7, x + 14, y + 15, glow);
                ctx.fill(x + 12, y + 12, x + 17, y + 14, ink);
            }
            case "Memory" -> {
                RenderUtils.drawRoundedRect(ctx, x + 5, y + 7, 16, 12, 3, glow);
                ctx.fill(x + 8, y + 10, x + 18, y + 13, ink);
                ctx.fill(x + 10, y + 5, x + 14, y + 7, glow);
            }
            case "Compass" -> {
                RenderUtils.drawRoundedRect(ctx, x + 4, y + 4, 18, 18, 9, 0x22FFFFFF);
                ctx.fill(x + 12, y + 6, x + 14, y + 16, glow);
                ctx.fill(x + 9, y + 9, x + 15, y + 11, glow);
                ctx.fill(x + 13, y + 7, x + 17, y + 11, ink);
            }
            default -> {
                RenderUtils.drawRoundedRect(ctx, x + 5, y + 5, 16, 16, 5, glow);
                ctx.fill(x + 8, y + 8, x + 14, y + 14, ink);
            }
        }
    }

    private void renderFirstRunWizard(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        int panelW = 430;
        int panelH = 220;
        int panelX = drawX + (WIN_W - panelW) / 2;
        int panelY = drawY + (WIN_H - panelH) / 2;

        ctx.fill(drawX, drawY, drawX + WIN_W, drawY + WIN_H, 0x88000000);
        RenderUtils.drawRoundedRectWithBorder(ctx, panelX, panelY, panelW, panelH, 4, 0xEE111111, 0x66333333);
        ctx.drawString(this.font, "Quick Setup", panelX + 14, panelY + 12, 0xFFFFFFFF, false);
        ctx.drawString(this.font, "1) Pick theme  2) Enable PvP starter modules", panelX + 14, panelY + 28, RenderUtils.MUTED_COLOR, false);

        int bx = panelX + 14;
        int by = panelY + 50;
        for (XenonTheme entry : XenonTheme.values()) {
            boolean active = entry.name().equalsIgnoreCase(ClientClient.getInstance().getThemeId());
            RenderUtils.drawRoundedRect(ctx, bx, by, 64, 18, 3, active ? entry.accent : 0xFF282828);
            ctx.drawString(this.font, entry.name(), bx + 10, by + 5, 0xFFFFFFFF, false);
            bx += 70;
        }

        ctx.drawString(this.font, "Starter: CPS, FPS, Armor, Keystrokes, Reach", panelX + 14, panelY + 82, 0xFFE2E2E2, false);
        RenderUtils.drawRoundedRect(ctx, panelX + 14, panelY + 100, 162, 22, 4, theme.accent);
        ctx.drawString(this.font, "Enable Starter Pack", panelX + 23, panelY + 107, 0xFFFFFFFF, false);

        RenderUtils.drawRoundedRect(ctx, panelX + panelW - 150, panelY + panelH - 34, 132, 22, 4, 0xFF43B581);
        ctx.drawString(this.font, "Finish Setup", panelX + panelW - 116, panelY + panelH - 27, 0xFFFFFFFF, false);
    }

    private boolean handleFirstRunWizardClick(int absX, int absY) {
        int drawX = winX;
        int drawY = winY;
        int panelW = 430;
        int panelH = 220;
        int panelX = drawX + (WIN_W - panelW) / 2;
        int panelY = drawY + (WIN_H - panelH) / 2;

        int lx = absX;
        int ly = absY;

        int bx = panelX + 14;
        int by = panelY + 50;
        for (XenonTheme entry : XenonTheme.values()) {
            if (inside(lx, ly, bx, by, 64, 18)) {
                ClientClient.getInstance().setThemeId(entry.name());
                ClientClient.getHudManager().saveConfig();
                return true;
            }
            bx += 70;
        }

        if (inside(lx, ly, panelX + 14, panelY + 100, 162, 22)) {
            setModuleEnabled("CPS Counter", true);
            setModuleEnabled("FPS & Ping", true);
            setModuleEnabled("Armor Status", true);
            setModuleEnabled("Keystrokes", true);
            setModuleEnabled("Reach Display", true);
            ClientClient.getHudManager().saveConfig();
            toast("Starter modules enabled");
            return true;
        }

        if (inside(lx, ly, panelX + panelW - 150, panelY + panelH - 34, 132, 22)) {
            ClientClient.getInstance().setWizardCompleted(true);
            ClientClient.getHudManager().saveConfig();
            wizardVisible = false;
            toast("Setup complete");
            return true;
        }

        return true;
    }

    private void setModuleEnabled(String moduleName, boolean enabled) {
        for (HudModule module : ClientClient.getHudManager().getModules()) {
            if (module.getName().equals(moduleName)) {
                module.setEnabled(enabled);
            }
        }
    }

    private void toast(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.displayClientMessage(Component.literal("[XENON] " + message), true);
        }
    }

    private void drawModuleToggle(GuiGraphics ctx, int centerX, int centerY, boolean enabled, int accentColor) {
        int outer = enabled ? 0xFF3A3A3A : 0xFF2A2A2A;
        int inner = enabled ? accentColor : 0xFF1B1B1B;
        RenderUtils.drawRoundedRect(ctx, centerX - 8, centerY - 8, 16, 16, 8, outer);
        RenderUtils.drawRoundedRect(ctx, centerX - 6, centerY - 6, 12, 12, 6, inner);
    }

    private void drawEnhancedToggle(GuiGraphics ctx, int x, int y, boolean enabled, int accentColor) {
        int bgColor = enabled ? 0xC043B581 : 0xC0303846;
        RenderUtils.drawRoundedRect(ctx, x, y, 18, 18, 4, bgColor);
        
        int lightEdge = 0x26FFFFFF;
        ctx.fill(x, y, x + 18, y + 1, lightEdge);
        ctx.fill(x, y, x + 1, y + 18, lightEdge);
        
        int innerSize = 10;
        int innerX = x + (18 - innerSize) / 2;
        int innerY = y + (18 - innerSize) / 2;
        int innerColor = enabled ? 0xFFFFFFFF : 0xFF5A6578;
        RenderUtils.drawRoundedRect(ctx, innerX, innerY, innerSize, innerSize, 2, innerColor);
    }

    private void drawSlantedHeader(GuiGraphics ctx, int x, int y, int width, int height, int slant, int color, int cutColor) {
        RenderUtils.drawRoundedRect(ctx, x, y, width, height, 3, color);
        for (int dy = 0; dy < height; dy++) {
            int cut = (int) ((dy / (float) height) * slant);
            ctx.fill(x + width - slant + cut, y + dy, x + width, y + dy + 1, cutColor);
        }
    }

    private void applyCrosshairSettings() {
        ClientClient client = ClientClient.getInstance();
        client.setCustomCrosshairEnabled(customCrosshairEnabled);
        client.setCrosshairType(crosshairType);
        client.setCrosshairColor((crosshairRed << 16) | (crosshairGreen << 8) | crosshairBlue);
    }

    private int valueFromSlider(int localX) {
        int sliderX = 80;
        int sliderW = 220;
        float pct = Math.max(0.0f, Math.min(1.0f, ((float) (localX - sliderX)) / sliderW));
        return (int) (pct * 255.0f);
    }

    private int snapX(int x, HudModule module) {
        int snapDistance = 6;
        int grid = 4;
        int width = module.getScaledWidth();

        int out = x;
        out = Math.round(out / (float) grid) * grid;

        if (Math.abs(out) <= snapDistance) {
            out = 0;
        }

        int rightEdge = this.width - width;
        if (Math.abs(out - rightEdge) <= snapDistance) {
            out = rightEdge;
        }

        int center = (this.width - width) / 2;
        if (Math.abs(out - center) <= snapDistance) {
            out = center;
        }

        for (HudModule other : ClientClient.getHudManager().getModules()) {
            if (other == module || !other.isEnabled()) {
                continue;
            }
            if (Math.abs(out - other.getX()) <= snapDistance) {
                out = other.getX();
            }
        }

        return clamp(out, 0, Math.max(0, this.width - width));
    }

    private int snapY(int y, HudModule module) {
        int snapDistance = 6;
        int grid = 4;
        int height = module.getScaledHeight();

        int out = y;
        out = Math.round(out / (float) grid) * grid;

        if (Math.abs(out) <= snapDistance) {
            out = 0;
        }

        int bottomEdge = this.height - height;
        if (Math.abs(out - bottomEdge) <= snapDistance) {
            out = bottomEdge;
        }

        int center = (this.height - height) / 2;
        if (Math.abs(out - center) <= snapDistance) {
            out = center;
        }

        for (HudModule other : ClientClient.getHudManager().getModules()) {
            if (other == module || !other.isEnabled()) {
                continue;
            }
            if (Math.abs(out - other.getY()) <= snapDistance) {
                out = other.getY();
            }
        }

        return clamp(out, 0, Math.max(0, this.height - height));
    }

    private static int applyAlpha(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int fadeAlpha(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int outA = clamp((int) (a * factor), 0, 255);
        return (outA << 24) | (color & 0x00FFFFFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
