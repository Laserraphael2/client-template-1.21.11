package mod.client.client.screen;

import mod.client.client.ClientClient;
import mod.client.client.hud.HudManager;
import mod.client.client.modules.HudModule;
import mod.client.client.render.CrosshairCustomizer;
import mod.client.client.render.RenderUtils;
import mod.client.client.render.XenonTheme;
import mod.client.client.spotify.SpotifyDevice;
import mod.client.client.spotify.SpotifyPlaylist;
import mod.client.client.spotify.SpotifySearchTrack;
import mod.client.client.spotify.SpotifyService;
import mod.client.client.spotify.SpotifySnapshot;
import mod.client.client.spotify.SpotifyTrack;
import mod.client.client.screen.panels.AboutPanel;
import mod.client.client.screen.panels.ConfigPanel;
import mod.client.client.screen.panels.ModulesPanel;
import mod.client.client.screen.panels.PerformancePanel;
import mod.client.client.screen.panels.PositionsPanel;
import mod.client.client.screen.panels.SettingsPanel;
import mod.client.client.screen.panels.SpotifyPanel;
import mod.client.client.util.KeyNameUtils;
import mod.client.shield.ShieldPatternData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class XenonMenuScreen extends Screen {

    private static final int WIN_W = 760;
    private static final int WIN_H = 500;
    private static final float MIN_UI_SCALE = 0.4f;
    private static final float MAX_UI_SCALE = 1.0f;
    private static final float UI_SCALE_STEP = 0.1f;
    private static final long CLICK_ANIMATION_MS = 240L;

    private static final int SIDEBAR_W = 60;
    private static final int HEADER_H = 46;
    private static final int POSITION_DONE_W = 140;
    private static final int POSITION_DONE_H = 26;
    private static final int MODULE_CARD_H = 84;
    private static final int MODULE_GAP = 6;
    private static final int SIDEBAR_ICON_SIZE = 16;
    private static final int MODULE_BADGE_SIZE = 26;
    private static final int EDGE_MARGIN = 14;
    private static final int SPOTIFY_APP_X = 68;
    private static final int SPOTIFY_APP_Y = 56;
    private static final int SPOTIFY_LEFT_W = 300;
    private static final int CROSSHAIR_EDITOR_X = 480;
    private static final int CROSSHAIR_EDITOR_Y = 166;
    private static final int CROSSHAIR_EDITOR_CELL = 12;
    private static final int SHIELD_EDITOR_X = 84;
    private static final int SHIELD_EDITOR_Y = 92;
    private static final int SHIELD_EDITOR_CELL = 18;
    private static final String[] FILTERS = {"All", "New", "HUD", "Hypixel", "PvP"};

    private int winX;
    private int winY;
    private float uiScale = 0.5f;
    private int clickAnimX;
    private int clickAnimY;
    private long clickAnimStartedAt;

    private float openAnim = 0.0f;

    private boolean draggingWindow;
    private int windowDragOffsetX;
    private int windowDragOffsetY;

    private HudModule draggingModule;
    private int moduleDragOffsetX;
    private int moduleDragOffsetY;

    private int draggingSlider; // 0=none, 1=red, 2=green, 3=blue
    private boolean drawingCrosshair;
    private boolean crosshairDrawValue;
    private boolean drawingShield;
    private boolean shieldDrawValue;
    private boolean searchFocused;
    private boolean confirmReset;
    private boolean wizardVisible;
    private boolean presetNameFocused;
    private boolean spotifyClientIdFocused;
    private boolean spotifyRefreshFocused;
    private boolean spotifyDeviceSearchFocused;
    private boolean spotifySearchFocused;

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
    private String shieldPattern = ShieldPatternData.EMPTY_PATTERN;
    private int shieldRed = (ShieldPatternData.DEFAULT_COLOR >> 16) & 0xFF;
    private int shieldGreen = (ShieldPatternData.DEFAULT_COLOR >> 8) & 0xFF;
    private int shieldBlue = ShieldPatternData.DEFAULT_COLOR & 0xFF;
    private String presetName = "default";
    private String spotifyClientIdInput = "";
    private String spotifyRefreshInput = "1000";
    private String spotifyDeviceSearchQuery = "";
    private int spotifyDeviceScroll;
    private int spotifyDeviceMaxScroll;
    private boolean spotifyCompactView;
    private String spotifyAlbumArtKey = "";
    private final int[] spotifyAlbumArtPixels = new int[64];
    private boolean spotifyAlbumArtReady;
    private boolean spotifyVolumeDragging;
    private int spotifyLiveVolume = 50;
    private String spotifySearchInput = "";
    private int spotifySearchScroll;
    private int spotifySearchMaxScroll;
    private int spotifyLibraryScroll;
    private int spotifyLibraryMaxScroll;

    private enum SpotifyView {
        HOME,
        SEARCH,
        LIBRARY
    }

    private SpotifyView spotifyView = SpotifyView.HOME;

    private enum Tab {
        MODULES,
        SETTINGS,
        POSITIONS,
        SHIELD,
        SPOTIFY,
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
    private final SpotifyPanel spotifyPanel = new SpotifyPanel(this);

    public XenonMenuScreen() {
        super(Component.literal("XENON"));
        ClientClient client = ClientClient.getInstance();
        this.crosshairType = client.getCrosshairType();
        int color = client.getCrosshairColor();
        this.crosshairRed = (color >> 16) & 0xFF;
        this.crosshairGreen = (color >> 8) & 0xFF;
        this.crosshairBlue = color & 0xFF;
        this.customCrosshairEnabled = client.isCustomCrosshairEnabled();
        this.shieldPattern = client.getShieldPattern();
        int shieldColor = client.getShieldColor();
        this.shieldRed = (shieldColor >> 16) & 0xFF;
        this.shieldGreen = (shieldColor >> 8) & 0xFF;
        this.shieldBlue = shieldColor & 0xFF;
        this.spotifyClientIdInput = client.getSpotifyClientId();
        this.spotifyRefreshInput = Integer.toString(client.getSpotifyRefreshIntervalMs());
        this.spotifyCompactView = client.isSpotifyCompactView();
        this.currentFilter = "All";
        this.searchQuery = "";
    }

    @Override
    protected void init() {
        ClientClient state = ClientClient.getInstance();
        int logicalWidth = Math.round(this.width / uiScale);
        int logicalHeight = Math.round(this.height / uiScale);
        if (state.getMenuX() >= 0 && state.getMenuY() >= 0) {
            this.winX = clamp(state.getMenuX(), 0, Math.max(0, logicalWidth - WIN_W));
            this.winY = clamp(state.getMenuY(), 0, Math.max(0, logicalHeight - WIN_H));
        } else {
            this.winX = (logicalWidth - WIN_W) / 2;
            this.winY = (logicalHeight - WIN_H) / 2;
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

        ctx.fill(0, 0, this.width, this.height, applyAlpha(0x44000000, openAnim));
        ctx.pose().pushMatrix();
        ctx.pose().scale(uiScale, uiScale);
        renderWindow(ctx, toLogical(mouseX), toLogical(mouseY));
        ctx.pose().popMatrix();
        super.render(ctx, mouseX, mouseY, partialTick);
    }

    private void renderWindow(GuiGraphics ctx, int mouseX, int mouseY) {
        XenonTheme theme = XenonTheme.fromId(ClientClient.getInstance().getThemeId());

        int drawX = winX;
        int drawY = winY + (int) ((1.0f - openAnim) * 10.0f);

        int contentBg = 0xE8F4FCFF;
        int sidebarBg = 0xDEEEFAFF;
        int headerBg = 0xE0F0FBFF;

        RenderUtils.drawGlassPanel(ctx, drawX, drawY, WIN_W, WIN_H, 8, applyAlpha(contentBg, openAnim), applyAlpha(theme.accent, openAnim));
        RenderUtils.drawGlassPanel(ctx, drawX, drawY, SIDEBAR_W, WIN_H, 8, applyAlpha(sidebarBg, openAnim), applyAlpha(theme.accent, openAnim));
        RenderUtils.drawGlassPanel(ctx, drawX + SIDEBAR_W, drawY, WIN_W - SIDEBAR_W, HEADER_H, 0, applyAlpha(headerBg, openAnim), applyAlpha(theme.accent, openAnim));

        renderTopBar(ctx, mouseX, mouseY, drawX, drawY, theme);

        renderSidebar(ctx, mouseX, mouseY, drawX, drawY, theme);

        switch (currentTab) {
            case MODULES -> modulesPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case SETTINGS -> settingsPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case POSITIONS -> positionsPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case SHIELD -> renderShieldEditor(ctx, mouseX, mouseY, drawX, drawY, theme);
            case SPOTIFY -> spotifyPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case PERFORMANCE -> performancePanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case CONFIG -> configPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
            case ABOUT -> aboutPanel.render(ctx, mouseX, mouseY, drawX, drawY, theme);
        }

        renderClickAnimation(ctx, theme.accent);

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

    public void renderSpotifyPanel(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        renderSpotify(ctx, mouseX, mouseY, drawX, drawY, theme);
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

    public boolean handleSettingsPanelClick(int mx, int my, int button) {
        if (button == 0 || button == 1) {
            handleSettingsClick(mx, my, button);
        }
        return true;
    }

    public boolean handleConfigPanelClick(int mx, int my) {
        handleConfigClick(mx, my);
        return true;
    }

    public boolean handleSpotifyPanelClick(int mx, int my) {
        handleSpotifyClick(mx, my);
        return true;
    }

    private void renderSidebar(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        renderSidebarTab(ctx, mouseX, mouseY, Tab.MODULES, 68, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.SETTINGS, 122, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.POSITIONS, 176, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.SHIELD, 230, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.SPOTIFY, 284, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.PERFORMANCE, 338, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.CONFIG, 392, drawX, drawY, theme.accent);
        renderSidebarTab(ctx, mouseX, mouseY, Tab.ABOUT, 446, drawX, drawY, theme.accent);
    }

    private void renderSidebarTab(GuiGraphics ctx, int mouseX, int mouseY, Tab tab, int localY, int drawX, int drawY, int accent) {
        int boxX = drawX + 10;
        int y = drawY + localY;
        int boxW = 40;
        int boxH = 40;
        boolean active = currentTab == tab;
        boolean hover = inside(mouseX, mouseY, boxX, y, boxW, boxH);

        int bg = active ? 0xF0FCFFFF : (hover ? 0xECF8FEFF : 0xE5F5FDFF);
        RenderUtils.drawGlassPanel(ctx, boxX, y, boxW, boxH, 8, bg, accent);
        RenderUtils.drawGlassHoverOverlay(ctx, boxX, y, boxW, boxH, hover || active, accent);

        if (active) {
            RenderUtils.drawRoundedRect(ctx, boxX - 3, y + 9, 2, boxH - 18, 1, accent);
        }

        int iconX = boxX + (boxW - SIDEBAR_ICON_SIZE) / 2;
        int iconY = y + (boxH - SIDEBAR_ICON_SIZE) / 2;
        renderSidebarBadge(ctx, tab, iconX, iconY, active, accent);
    }

    private void renderTopBar(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        int barX = drawX + SIDEBAR_W;
        int barY = drawY;

        int closeX = drawX + 12;
        int closeY = drawY + 8;
        RenderUtils.drawGlassPanel(ctx, closeX, closeY, 42, 30, 8, 0xEEFAFFFF, theme.accent);
        ctx.drawString(this.font, "X", closeX + 15, closeY + 10, RenderUtils.TEXT_COLOR, true);

        RenderUtils.drawGlassPanel(ctx, barX + 54, barY + 10, 146, 26, 6, 0xEEFAFFFF, theme.accent);
        ctx.drawString(this.font, "Menu", barX + 69, barY + 18, RenderUtils.TEXT_COLOR, true);

        int filterW = 56;
        int filterH = 26;
        int filterStep = 60;
        int fx = barX + 212;

        for (String filter : FILTERS) {
            boolean active = currentFilter.equals(filter);
            boolean hover = inside(mouseX, mouseY, fx, drawY + 10, filterW, filterH);
            int color = active ? 0xF5FCFFFF : (hover ? 0xF0FAFFFF : 0xECF8FEFF);
            RenderUtils.drawGlassPanel(ctx, fx, drawY + 10, filterW, filterH, 6, color, theme.accent);
            ctx.drawString(this.font, filter, fx + (filterW - this.font.width(filter)) / 2, drawY + 18, RenderUtils.TEXT_COLOR, false);
            fx += filterStep;
        }

        int searchW = 146;
        int searchX = drawX + WIN_W - searchW - 12;
        RenderUtils.drawGlassPanel(ctx, searchX, drawY + 10, searchW, 26, 6, searchFocused ? 0xF5FCFFFF : 0xECF8FEFF, theme.accent);
        String text = searchQuery.isEmpty() ? "Search" : searchQuery;
        int color = searchQuery.isEmpty() ? RenderUtils.MUTED_COLOR : RenderUtils.TEXT_COLOR;
        ctx.drawString(this.font, text, searchX + 18, drawY + 18, color, false);

    }

    private void renderModuleGrid(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        int startX = drawX + SIDEBAR_W + 12;
        int startY = drawY + HEADER_H + 10;
        int contentBottom = drawY + WIN_H - 22;
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

            RenderUtils.drawSmallButton(ctx, cx + 10, cy + 7, 40, 16, 0xECF8FEFF);
            ctx.drawString(this.font, "Key", cx + 18, cy + 11, RenderUtils.TEXT_COLOR, false);
            
            int dotSize = 3;
            int dotX = cx + cardW - 10;
            int dotY = cy + 10;
            ctx.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, 0x88FFFFFF);

            String label = getModuleAbbreviation(mod.getName());
            int labelY = cy + 36;
            ctx.drawString(this.font, label, cx + (cardW - this.font.width(label)) / 2, labelY, RenderUtils.TEXT_COLOR, true);

            int sepY = cy + cardH - 40;
            RenderUtils.drawSeparator(ctx, cx + 10, sepY, cardW - 20, 0x3000D9FF);
            
            int nameY = cy + cardH - 30;
            ctx.drawString(this.font, mod.getName(), cx + 10, nameY, RenderUtils.TEXT_COLOR, false);
            
            int toggleX = cx + cardW - 28;
            int toggleY = cy + cardH - 27;
            drawEnhancedToggle(ctx, toggleX, toggleY, mod.isEnabled(), theme.accent);
            
            int icon1X = cx + cardW - 52;
            int icon2X = cx + cardW - 70;
            int iconY = cy + cardH - 29;
            RenderUtils.drawSmallButton(ctx, icon1X, iconY, 16, 16, 0xECF8FEFF);
            RenderUtils.drawSmallButton(ctx, icon2X, iconY, 16, 16, 0xECF8FEFF);
            
            ctx.fill(icon1X + 5, iconY + 5, icon1X + 11, iconY + 7, RenderUtils.TEXT_COLOR);
            ctx.fill(icon1X + 5, iconY + 9, icon1X + 11, iconY + 11, RenderUtils.TEXT_COLOR);
            
            if (mod.isEnabled()) {
                ctx.fill(icon2X + 6, iconY + 6, icon2X + 10, iconY + 10, theme.accent);
            } else {
                ctx.fill(icon2X + 6, iconY + 6, icon2X + 10, iconY + 10, RenderUtils.MUTED_COLOR);
            }

            String keyText = mod.getKeybind() >= 0 ? KeyNameUtils.format(mod.getKeybind()) : "None";
            String status = (mod.isEnabled() ? "On" : "Off") + " | " + keyText + " | " + formatScale(mod.getScale());
            ctx.drawString(this.font, status, cx + 10, cy + cardH - 13, RenderUtils.MUTED_COLOR, false);
        }

        if (awaitingKeybindModule != null) {
            ctx.drawString(this.font, "Press any key to bind. ESC clears.", drawX + 86, drawY + WIN_H - 18, 0xFF00D9FF, true);
        }

        if (maxScroll > 0) {
            int trackX = drawX + WIN_W - 8;
            int trackY = startY;
            int trackH = contentBottom - startY;
            RenderUtils.drawRoundedRect(ctx, trackX, trackY, 3, trackH, 1, 0x20B0D8F0);

            int thumbH = Math.max(18, (int) ((trackH / (float) (trackH + maxScroll)) * trackH));
            int thumbY = trackY + (int) ((moduleGridScroll / (float) maxScroll) * (trackH - thumbH));
            RenderUtils.drawRoundedRect(ctx, trackX, thumbY, 3, thumbH, 1, 0xFF00D9FF);
        }
    }

    private void renderSettings(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        int x = drawX + 76;
        int y = drawY + 62;

        ctx.drawString(this.font, "Crosshair Editor", x, y, RenderUtils.TEXT_COLOR, true);
        RenderUtils.drawGlassPanel(ctx, x, y + 18, 140, 24, 7, 0xD8FFFFFF, theme.accent);
        if (customCrosshairEnabled) {
            RenderUtils.drawRoundedRect(ctx, x + 4, y + 22, 4, 16, 2, theme.accent);
        }
        ctx.drawString(this.font, customCrosshairEnabled ? "Crosshair enabled" : "Crosshair disabled", x + 14, y + 26,
            customCrosshairEnabled ? RenderUtils.TEXT_COLOR : RenderUtils.MUTED_COLOR, false);

        int typeY = y + 52;
        int tx = x;
        for (CrosshairCustomizer.CrosshairType type : CrosshairCustomizer.CrosshairType.values()) {
            boolean active = type == crosshairType;
            boolean hover = inside(mouseX, mouseY, tx, typeY, 78, 24);
            RenderUtils.drawGlassPanel(ctx, tx, typeY, 78, 24, 6, active || hover ? 0xE8FFFFFF : 0xC8FFFFFF, active ? theme.accent : 0xFF607080);
            ctx.drawString(this.font, type.name(), tx + (78 - this.font.width(type.name())) / 2, typeY + 8,
                active ? theme.accent : RenderUtils.TEXT_COLOR, false);
            tx += 84;
        }

        renderColorSlider(ctx, "R", x, y + 96, crosshairRed, 1, theme.accent);
        renderColorSlider(ctx, "G", x, y + 128, crosshairGreen, 2, theme.accent);
        renderColorSlider(ctx, "B", x, y + 160, crosshairBlue, 3, theme.accent);

        int previewX = x + 322;
        int previewY = y + 128;
        RenderUtils.drawGlassPanel(ctx, previewX, previewY, 74, 74, 10, 0xCCFFFFFF, theme.accent);
        ctx.drawString(this.font, "Preview", previewX + 18, previewY + 8, RenderUtils.MUTED_COLOR, false);
        renderCrosshairPreview(ctx, previewX + 37, previewY + 44);

        int editorX = drawX + CROSSHAIR_EDITOR_X;
        int editorY = drawY + CROSSHAIR_EDITOR_Y;
        int gridSize = CrosshairCustomizer.CUSTOM_GRID_SIZE * CROSSHAIR_EDITOR_CELL;
        ctx.drawString(this.font, "Draw your own", editorX, editorY - 16, RenderUtils.TEXT_COLOR, true);
        RenderUtils.drawGlassPanel(ctx, editorX - 5, editorY - 5, gridSize + 10, gridSize + 10, 9, 0xD4FFFFFF, theme.accent);
        ctx.fill(editorX, editorY, editorX + gridSize, editorY + gridSize, 0xA806090D);
        for (int row = 0; row < CrosshairCustomizer.CUSTOM_GRID_SIZE; row++) {
            for (int column = 0; column < CrosshairCustomizer.CUSTOM_GRID_SIZE; column++) {
                if (CrosshairCustomizer.isCustomPixelSet(column, row)) {
                    int cellX = editorX + column * CROSSHAIR_EDITOR_CELL;
                    int cellY = editorY + row * CROSSHAIR_EDITOR_CELL;
                    RenderUtils.drawRoundedRect(ctx, cellX + 2, cellY + 2, CROSSHAIR_EDITOR_CELL - 3, CROSSHAIR_EDITOR_CELL - 3, 3,
                        0xFF000000 | (crosshairRed << 16) | (crosshairGreen << 8) | crosshairBlue);
                }
            }
        }
        for (int line = 1; line < CrosshairCustomizer.CUSTOM_GRID_SIZE; line++) {
            int offset = line * CROSSHAIR_EDITOR_CELL;
            ctx.fill(editorX + offset, editorY, editorX + offset + 1, editorY + gridSize, 0x1839D8FF);
            ctx.fill(editorX, editorY + offset, editorX + gridSize, editorY + offset + 1, 0x1839D8FF);
        }
        RenderUtils.drawGlassPanel(ctx, editorX, editorY + gridSize + 10, 64, 22, 6, 0xCCFFFFFF, theme.accent);
        ctx.drawString(this.font, "Clear", editorX + 18, editorY + gridSize + 17, RenderUtils.TEXT_COLOR, false);
        ctx.drawString(this.font, "LMB draw  RMB erase", editorX + 70, editorY + gridSize + 17, RenderUtils.MUTED_COLOR, false);

        int themeY = drawY + 360;
        RenderUtils.drawSeparator(ctx, x, themeY - 12, WIN_W - 106, 0x2839D8FF);
        ctx.drawString(this.font, "Theme", x, themeY, RenderUtils.TEXT_COLOR, false);
        int bx = x + 48;
        for (XenonTheme entry : XenonTheme.values()) {
            boolean active = entry.name().equalsIgnoreCase(ClientClient.getInstance().getThemeId());
            RenderUtils.drawGlassPanel(ctx, bx, themeY - 5, 68, 22, 6, 0xD8FFFFFF, active ? entry.accent : 0xFF607080);
            ctx.drawString(this.font, entry.name(), bx + (68 - this.font.width(entry.name())) / 2, themeY + 2,
                active ? entry.accent : RenderUtils.TEXT_COLOR, false);
            bx += 74;
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

    private void renderSpotify(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        SpotifyService service = ClientClient.getSpotifyService();
        SpotifySnapshot snapshot = service.getSnapshot();
        SpotifyTrack track = snapshot.track();
        List<SpotifyDevice> filteredDevices = getFilteredSpotifyDevices(snapshot);

        updateSpotifyAlbumArtThumbnail(track);

        int activeVolume = getActiveSpotifyVolume(snapshot);
        spotifyLiveVolume = activeVolume;

        int appX = drawX + SPOTIFY_APP_X;
        int appY = drawY + SPOTIFY_APP_Y;
        int appW = WIN_W - SIDEBAR_W - 80;
        int appH = WIN_H - 74;
        int gap = 12;
        int leftW = SPOTIFY_LEFT_W;
        int rightX = appX + leftW + gap;
        int rightW = appW - leftW - gap;

        int spotifyGreen = 0xFF1ED760;
        RenderUtils.drawGlassPanel(ctx, appX, appY, appW, appH, 10, 0xECF8FEFF, spotifyGreen);
        ctx.drawString(this.font, "Spotify", appX + 14, appY + 11, RenderUtils.TEXT_COLOR, true);
        ctx.drawString(this.font, snapshot.status(), appX + 66, appY + 11, snapshot.authenticated() ? spotifyGreen : RenderUtils.MUTED_COLOR, false);

        if (!snapshot.authenticated()) {
            renderSpotifySetup(ctx, mouseX, mouseY, appX, appY, appW, appH, snapshot, spotifyGreen);
            return;
        }

        int hudX = appX + appW - 164;
        drawSpotifyControlButton(ctx, mouseX, mouseY, hudX, appY + 7, 76, 22,
            ClientClient.getInstance().isSpotifyHudEnabled() ? "HUD On" : "HUD Off");
        drawSpotifyDangerButton(ctx, mouseX, mouseY, appX + appW - 82, appY + 7, 70, 22, "Logout");

        int leftX = appX + 12;
        int sectionY = appY + 38;
        int statusY = sectionY;
        int statusH = 232;
        RenderUtils.drawGlassPanel(ctx, leftX, statusY, leftW - 12, statusH, 8, 0xEEF9FDFF, 0xFF00D9FF);
        ctx.drawString(this.font, "Now Playing", leftX + 10, statusY + 9, RenderUtils.TEXT_COLOR, false);

        String trackTitle = track == null || track.title().isBlank() ? "No active playback" : track.title();
        String trackArtist = track == null ? "Connect to Spotify and start music" : (track.artists().isBlank() ? "Unknown artist" : track.artists());
        String trackAlbum = track == null ? "" : track.album();
        String stateText = snapshot.status();
        if (track != null) {
            stateText = (track.playing() ? "Playing" : "Paused") + " | " + snapshot.status();
        }

        int coverSize = 112;
        int coverX = leftX + (leftW - 12 - coverSize) / 2;
        int coverY = statusY + 26;
        RenderUtils.drawGlassPanel(ctx, coverX, coverY, coverSize, coverSize, 6, 0xF4FCFFFF, 0xFF00D9FF);
        if (spotifyAlbumArtReady) {
            int cell = Math.max(1, (coverSize - 8) / 8);
            int px = coverX + 4;
            int py = coverY + 4;
            int index = 0;
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    int color = spotifyAlbumArtPixels[index++];
                    ctx.fill(px + (col * cell), py + (row * cell), px + ((col + 1) * cell), py + ((row + 1) * cell), color);
                }
            }
        } else {
            ctx.drawString(this.font, "No cover", coverX + 33, coverY + 52, RenderUtils.MUTED_COLOR, false);
        }

        int textY = coverY + coverSize + 10;
        ctx.drawString(this.font, cropText(trackTitle, 38), leftX + 10, textY, RenderUtils.TEXT_COLOR, true);
        ctx.drawString(this.font, cropText(trackArtist, 40), leftX + 10, textY + 14, RenderUtils.MUTED_COLOR, false);
        if (!trackAlbum.isBlank()) {
            ctx.drawString(this.font, cropText(trackAlbum, 40), leftX + 10, textY + 27, RenderUtils.MUTED_COLOR, false);
        }

        int durationMs = track == null ? 0 : track.durationMs();
        int animatedProgressMs = track == null ? 0 : track.progressMs();
        if (track != null && track.playing()) {
            long elapsed = Math.max(0L, System.currentTimeMillis() - snapshot.lastUpdatedMs());
            animatedProgressMs += (int) elapsed;
        }
        if (durationMs > 0) {
            animatedProgressMs = Math.min(durationMs, animatedProgressMs);
        }

        float progress = 0.0f;
        String timing = "00:00 / 00:00";
        if (durationMs > 0) {
            progress = Math.max(0.0f, Math.min(1.0f, animatedProgressMs / (float) durationMs));
            timing = formatSpotifyTime(animatedProgressMs) + " / " + formatSpotifyTime(durationMs);
        }
        int progressY = statusY + 190;
        RenderUtils.drawProgressBar(ctx, leftX + 10, progressY, leftW - 32, 8, progress, 0x30B0D8F0, 0xFF00D9FF);
        ctx.drawString(this.font, timing, leftX + 10, progressY + 12, RenderUtils.MUTED_COLOR, false);
        ctx.drawString(this.font, cropText(stateText, 24), leftX + 166, progressY + 12, RenderUtils.MUTED_COLOR, false);

        int volLabelY = statusY + statusH + 12;
        ctx.drawString(this.font, spotifyLiveVolume + "%", leftX + leftW - 42, volLabelY + 3, RenderUtils.MUTED_COLOR, false);
        int volX = leftX + 42;
        int volW = leftW - 96;
        RenderUtils.drawRoundedRect(ctx, volX, volLabelY + 4, volW, 8, 4, 0x20B0D8F0);
        int volFill = (int) ((spotifyLiveVolume / 100.0f) * volW);
        RenderUtils.drawRoundedRect(ctx, volX, volLabelY + 4, Math.max(2, volFill), 8, 4, 0xFF00D9FF);
        int volHandle = volX + Math.max(0, Math.min(volW - 4, volFill - 2));
        RenderUtils.drawRoundedRect(ctx, volHandle, volLabelY + 2, 4, 12, 2, 0xFFF4FFFF);

        int controlsY = volLabelY + 24;
        drawSpotifyControlButton(ctx, mouseX, mouseY, leftX + 54, controlsY, 46, 34, "|<");
        drawSpotifyControlButton(ctx, mouseX, mouseY, leftX + 108, controlsY - 4, 60, 42, track != null && track.playing() ? "||" : ">");
        drawSpotifyControlButton(ctx, mouseX, mouseY, leftX + 176, controlsY, 46, 34, ">|");

        int rightPanelX = rightX;
        int rightPanelY = sectionY;
        int rightPanelW = rightW - 12;
        int rightPanelH = appH - 40;
        RenderUtils.drawGlassPanel(ctx, rightPanelX, rightPanelY, rightPanelW, rightPanelH, 8, 0xEEF9FDFF, 0xFF00D9FF);
        ctx.drawString(this.font, "Browse", rightPanelX + 10, rightPanelY + 9, RenderUtils.TEXT_COLOR, false);

        int navY = rightPanelY + 24;
        int navW = 82;
        int navH = 22;
        drawSpotifyViewButton(ctx, mouseX, mouseY, rightPanelX + 10, navY, navW, navH, "Home", spotifyView == SpotifyView.HOME);
        drawSpotifyViewButton(ctx, mouseX, mouseY, rightPanelX + 98, navY, navW, navH, "Search", spotifyView == SpotifyView.SEARCH);
        drawSpotifyViewButton(ctx, mouseX, mouseY, rightPanelX + 186, navY, navW, navH, "Library", spotifyView == SpotifyView.LIBRARY);

        int contentX = rightPanelX + 10;
        int contentY = navY + 30;
        int contentW = rightPanelW - 20;
        int contentH = rightPanelH - 40 - navH;

        if (spotifyView == SpotifyView.HOME) {
            renderSpotifyHomeContent(ctx, mouseX, mouseY, snapshot, filteredDevices, contentX, contentY, contentW, contentH);
        } else if (spotifyView == SpotifyView.SEARCH) {
            renderSpotifySearchContent(ctx, mouseX, mouseY, service, contentX, contentY, contentW, contentH);
        } else {
            renderSpotifyLibraryContent(ctx, mouseX, mouseY, service, contentX, contentY, contentW, contentH);
        }
    }

    private void renderSpotifySetup(GuiGraphics ctx, int mouseX, int mouseY, int appX, int appY, int appW, int appH,
                                    SpotifySnapshot snapshot, int spotifyGreen) {
        int panelW = 360;
        int panelH = 184;
        int panelX = appX + (appW - panelW) / 2;
        int panelY = appY + (appH - panelH) / 2;
        RenderUtils.drawGlassPanel(ctx, panelX, panelY, panelW, panelH, 8, 0xD8FFFFFF, spotifyGreen);

        ctx.drawString(this.font, "Connect Spotify", panelX + 18, panelY + 18, RenderUtils.TEXT_COLOR, true);
        ctx.drawString(this.font, "Enter your own Client ID if the default app rejects you.", panelX + 18, panelY + 38, RenderUtils.MUTED_COLOR, false);

        int inputY = panelY + 58;
        RenderUtils.drawGlassPanel(ctx, panelX + 18, inputY, panelW - 36, 24, 6,
            spotifyClientIdFocused ? 0xF6FCFFFF : 0xECF8FEFF, spotifyGreen);
        String clientIdText = spotifyClientIdInput.isBlank() ? "Spotify Client ID (optional)" : spotifyClientIdInput;
        int clientIdColor = spotifyClientIdInput.isBlank() ? RenderUtils.MUTED_COLOR : RenderUtils.TEXT_COLOR;
        ctx.drawString(this.font, cropText(clientIdText, 48), panelX + 26, inputY + 8, clientIdColor, false);

        boolean connecting = snapshot.connecting();
        drawSpotifyPrimaryButton(ctx, mouseX, mouseY, panelX + 18, panelY + 94, panelW - 36, 32,
            connecting ? "Finish login in browser" : "Connect with Spotify", spotifyGreen);
        ctx.drawString(this.font, cropText(snapshot.status(), 48), panelX + 18, panelY + 140, RenderUtils.MUTED_COLOR, false);
        ctx.drawString(this.font, "Redirect: http://127.0.0.1:8888/callback", panelX + 18, panelY + 158, RenderUtils.MUTED_COLOR, false);
    }

    private void drawSpotifyPrimaryButton(GuiGraphics ctx, int mouseX, int mouseY, int x, int y, int w, int h, String text, int color) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        RenderUtils.drawRoundedRect(ctx, x, y, w, h, h / 2, hover ? 0xFF35E778 : color);
        ctx.drawString(this.font, text, x + (w - this.font.width(text)) / 2, y + 12, 0xFF07130B, true);
    }

    private void drawSpotifyViewButton(GuiGraphics ctx, int mouseX, int mouseY, int x, int y, int w, int h, String text, boolean active) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        int bg = active ? 0xF4FCFFFF : (hover ? 0xF0FAFFFF : 0xECF8FEFF);
        RenderUtils.drawGlassPanel(ctx, x, y, w, h, 6, bg, 0xFF00D9FF);
        ctx.drawString(this.font, text, x + (w - this.font.width(text)) / 2, y + 7, RenderUtils.TEXT_COLOR, false);
    }

    private void renderSpotifyHomeContent(GuiGraphics ctx, int mouseX, int mouseY, SpotifySnapshot snapshot, List<SpotifyDevice> filteredDevices, int x, int y, int w, int h) {
        int artSize = 84;
        RenderUtils.drawGlassPanel(ctx, x, y, artSize, artSize, 8, 0xF4FCFFFF, 0xFF00D9FF);
        if (spotifyAlbumArtReady) {
            int cell = Math.max(1, (artSize - 12) / 8);
            int px = x + 6;
            int py = y + 6;
            int index = 0;
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    int color = spotifyAlbumArtPixels[index++];
                    ctx.fill(px + (col * cell), py + (row * cell), px + ((col + 1) * cell), py + ((row + 1) * cell), color);
                }
            }
        } else {
            ctx.drawString(this.font, "ART", x + artSize / 2 - 10, y + artSize / 2 - 4, RenderUtils.MUTED_COLOR, false);
        }

        int searchX = x + artSize + 10;
        int searchW = Math.max(90, w - artSize - 10);
        RenderUtils.drawGlassPanel(ctx, searchX, y, searchW, 22, 6, spotifyDeviceSearchFocused ? 0xF6FCFFFF : 0xECF8FEFF, 0xFF00D9FF);
        String searchText = spotifyDeviceSearchQuery.isEmpty() ? "Search devices" : spotifyDeviceSearchQuery;
        int searchColor = spotifyDeviceSearchQuery.isEmpty() ? RenderUtils.MUTED_COLOR : RenderUtils.TEXT_COLOR;
        ctx.drawString(this.font, cropText(searchText, 18), searchX + 8, y + 7, searchColor, false);

        ctx.drawString(this.font, "Devices: " + filteredDevices.size(), searchX + 2, y + 30, RenderUtils.MUTED_COLOR, false);
        if (snapshot.activeDeviceId() != null && !snapshot.activeDeviceId().isBlank()) {
            ctx.drawString(this.font, "Active playback device detected", searchX + 2, y + 42, RenderUtils.MUTED_COLOR, false);
        }

        int deviceListY = y + artSize + 8;
        int deviceListH = Math.max(24, h - artSize - 14);
        int rowH = 26;
        int totalRows = filteredDevices.size();
        int visibleRows = Math.max(1, deviceListH / rowH);
        spotifyDeviceMaxScroll = Math.max(0, totalRows - visibleRows);
        spotifyDeviceScroll = clamp(spotifyDeviceScroll, 0, spotifyDeviceMaxScroll);

        if (filteredDevices.isEmpty()) {
            ctx.drawString(this.font, "No available Spotify devices", x, deviceListY + 8, RenderUtils.MUTED_COLOR, false);
            return;
        }

        int start = spotifyDeviceScroll;
        int end = Math.min(totalRows, start + visibleRows);
        int rowY = deviceListY;
        for (int i = start; i < end; i++) {
            SpotifyDevice device = filteredDevices.get(i);
            boolean active = device.id().equals(snapshot.activeDeviceId()) || device.active();
            boolean hover = inside(mouseX, mouseY, x, rowY, w, 22);
            int bg = active ? 0xF4FCFFFF : (hover ? 0xF0FAFFFF : 0xECF8FEFF);
            RenderUtils.drawGlassPanel(ctx, x, rowY, w, 22, 6, bg, 0xFF00D9FF);

            String label = cropText(device.name() + " (" + device.type() + ")", 24);
            ctx.drawString(this.font, label, x + 8, rowY + 7, RenderUtils.TEXT_COLOR, false);
            ctx.drawString(this.font, Math.max(0, device.volumePercent()) + "%", x + w - 38, rowY + 7, RenderUtils.MUTED_COLOR, false);
            if (active) {
                ctx.drawString(this.font, "ACTIVE", x + w - 86, rowY + 7, 0xFF00A8D0, false);
            }
            rowY += rowH;
        }
    }

    private void renderSpotifySearchContent(GuiGraphics ctx, int mouseX, int mouseY, SpotifyService service, int x, int y, int w, int h) {
        RenderUtils.drawGlassPanel(ctx, x, y, w - 74, 22, 6, spotifySearchFocused ? 0xF6FCFFFF : 0xECF8FEFF, 0xFF00D9FF);
        String searchText = spotifySearchInput.isEmpty() ? "Search songs, artists, albums" : spotifySearchInput;
        int searchColor = spotifySearchInput.isEmpty() ? RenderUtils.MUTED_COLOR : RenderUtils.TEXT_COLOR;
        ctx.drawString(this.font, cropText(searchText, 34), x + 8, y + 7, searchColor, false);

        drawSpotifyControlButton(ctx, mouseX, mouseY, x + w - 68, y, 68, 22, "Search");
        ctx.drawString(this.font, "Query: " + cropText(service.getLastSearchQuery(), 30), x, y + 30, RenderUtils.MUTED_COLOR, false);

        List<SpotifySearchTrack> results = service.getSearchResults();
        int listY = y + 46;
        int rowH = 28;
        int visibleRows = Math.max(1, (h - 52) / rowH);
        spotifySearchMaxScroll = Math.max(0, results.size() - visibleRows);
        spotifySearchScroll = clamp(spotifySearchScroll, 0, spotifySearchMaxScroll);

        if (results.isEmpty()) {
            ctx.drawString(this.font, "No search results yet", x, listY + 8, RenderUtils.MUTED_COLOR, false);
            return;
        }

        int start = spotifySearchScroll;
        int end = Math.min(results.size(), start + visibleRows);
        int rowY = listY;
        for (int i = start; i < end; i++) {
            SpotifySearchTrack track = results.get(i);
            boolean hover = inside(mouseX, mouseY, x, rowY, w, 24);
            RenderUtils.drawGlassPanel(ctx, x, rowY, w, 24, 6, hover ? 0xF0FAFFFF : 0xECF8FEFF, 0xFF00D9FF);
            ctx.drawString(this.font, cropText(track.title(), 26), x + 8, rowY + 6, RenderUtils.TEXT_COLOR, false);
            ctx.drawString(this.font, cropText(track.artists(), 18), x + 152, rowY + 6, RenderUtils.MUTED_COLOR, false);
            ctx.drawString(this.font, formatSpotifyTime(track.durationMs()), x + w - 90, rowY + 6, RenderUtils.MUTED_COLOR, false);
            ctx.drawString(this.font, "PLAY", x + w - 34, rowY + 6, 0xFF00A8D0, false);
            rowY += rowH;
        }
    }

    private void renderSpotifyLibraryContent(GuiGraphics ctx, int mouseX, int mouseY, SpotifyService service, int x, int y, int w, int h) {
        drawSpotifyControlButton(ctx, mouseX, mouseY, x + w - 72, y, 72, 22, "Reload");
        ctx.drawString(this.font, cropText(service.getPlaylistsStatus(), 40), x, y + 7, RenderUtils.MUTED_COLOR, false);

        List<SpotifyPlaylist> items = service.getPlaylists();
        int listY = y + 32;
        int rowH = 28;
        int visibleRows = Math.max(1, (h - 36) / rowH);
        spotifyLibraryMaxScroll = Math.max(0, items.size() - visibleRows);
        spotifyLibraryScroll = clamp(spotifyLibraryScroll, 0, spotifyLibraryMaxScroll);

        if (items.isEmpty()) {
            ctx.drawString(this.font, "No playlists loaded", x, listY + 8, RenderUtils.MUTED_COLOR, false);
            return;
        }

        int start = spotifyLibraryScroll;
        int end = Math.min(items.size(), start + visibleRows);
        int rowY = listY;
        for (int i = start; i < end; i++) {
            SpotifyPlaylist playlist = items.get(i);
            boolean hover = inside(mouseX, mouseY, x, rowY, w, 24);
            RenderUtils.drawGlassPanel(ctx, x, rowY, w, 24, 6, hover ? 0xF0FAFFFF : 0xECF8FEFF, 0xFF00D9FF);
            ctx.drawString(this.font, cropText(playlist.name(), 24), x + 8, rowY + 6, RenderUtils.TEXT_COLOR, false);
            ctx.drawString(this.font, cropText(playlist.owner(), 16), x + 150, rowY + 6, RenderUtils.MUTED_COLOR, false);
            ctx.drawString(this.font, playlist.tracksCount() + " tracks", x + w - 98, rowY + 6, RenderUtils.MUTED_COLOR, false);
            ctx.drawString(this.font, "PLAY", x + w - 34, rowY + 6, 0xFF00A8D0, false);
            rowY += rowH;
        }
    }

    private void drawSpotifyControlButton(GuiGraphics ctx, int mouseX, int mouseY, int x, int y, int w, int h, String text) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        RenderUtils.drawGlassPanel(ctx, x, y, w, h, 6, hover ? 0xF4FCFFFF : 0xECF8FEFF, 0xFF00D9FF);
        ctx.drawString(this.font, text, x + (w - this.font.width(text)) / 2, y + 7, RenderUtils.TEXT_COLOR, false);
    }

    private void drawSpotifyDangerButton(GuiGraphics ctx, int mouseX, int mouseY, int x, int y, int w, int h, String text) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        RenderUtils.drawGlassPanel(ctx, x, y, w, h, 6, hover ? 0xFFFBEFEF : 0xFFF8EAEA, 0xFFD87474);
        ctx.drawString(this.font, text, x + (w - this.font.width(text)) / 2, y + 7, 0xFF8A1E1E, false);
    }

    private void handleSpotifyClick(int mx, int my) {
        int x = SPOTIFY_APP_X;
        int y = SPOTIFY_APP_Y;
        int appW = WIN_W - SIDEBAR_W - 80;
        int appH = WIN_H - 74;
        int gap = 12;
        int leftW = SPOTIFY_LEFT_W;
        int rightX = x + leftW + gap;
        int rightW = appW - leftW - gap;

        SpotifyService service = ClientClient.getSpotifyService();
        SpotifySnapshot snapshot = service.getSnapshot();

        if (!snapshot.authenticated()) {
            int panelW = 360;
            int panelH = 184;
            int panelX = x + (appW - panelW) / 2;
            int panelY = y + (appH - panelH) / 2;

            if (inside(mx, my, panelX + 18, panelY + 58, panelW - 36, 24)) {
                spotifyClientIdFocused = true;
                return;
            }

            if (inside(mx, my, panelX + 18, panelY + 94, panelW - 36, 32)) {
                ClientClient.getInstance().setSpotifyClientId(spotifyClientIdInput);
                service.setClientId(spotifyClientIdInput);
                spotifyClientIdInput = service.getClientId();
                ClientClient.getInstance().setSpotifyClientId(spotifyClientIdInput);
                ClientClient.getHudManager().saveConfig();
                spotifyClientIdFocused = false;
                service.beginLogin();
                return;
            }

            spotifyClientIdFocused = false;
            return;
        }

        int hudX = x + appW - 164;
        if (inside(mx, my, hudX, y + 7, 76, 22)) {
            boolean next = !ClientClient.getInstance().isSpotifyHudEnabled();
            ClientClient.getInstance().setSpotifyHudEnabled(next);
            for (HudModule module : ClientClient.getHudManager().getModules()) {
                if ("Spotify Now Playing".equals(module.getName())) {
                    module.setEnabled(next);
                    break;
                }
            }
            ClientClient.getHudManager().saveConfig();
            return;
        }
        if (inside(mx, my, x + appW - 82, y + 7, 70, 22)) {
            service.logout();
            spotifyView = SpotifyView.HOME;
            return;
        }

        int sectionY = y + 38;
        int leftX = x + 12;
        int statusY = sectionY;
        int statusH = 232;
        int volLabelY = statusY + statusH + 12;
        int volX = leftX + 42;
        int volW = leftW - 96;
        if (inside(mx, my, volX, volLabelY + 2, volW, 12)) {
            spotifyClientIdFocused = false;
            spotifyRefreshFocused = false;
            spotifyDeviceSearchFocused = false;
            spotifyVolumeDragging = true;
            applySpotifyVolumeFromMouse(mx, volX, volW);
            return;
        }

        int controlsY = volLabelY + 24;
        if (inside(mx, my, leftX + 54, controlsY, 46, 34)) {
            service.previousTrack();
            return;
        }
        if (inside(mx, my, leftX + 108, controlsY - 4, 60, 42)) {
            service.togglePlayPause();
            return;
        }
        if (inside(mx, my, leftX + 176, controlsY, 46, 34)) {
            service.nextTrack();
            return;
        }

        int rightPanelX = rightX;
        int rightPanelY = sectionY;
        int rightPanelW = rightW - 12;
        int rightPanelH = appH - 40;
        int navY = rightPanelY + 24;

        if (inside(mx, my, rightPanelX + 10, navY, 82, 22)) {
            spotifyView = SpotifyView.HOME;
            spotifySearchFocused = false;
            return;
        }
        if (inside(mx, my, rightPanelX + 98, navY, 82, 22)) {
            spotifyView = SpotifyView.SEARCH;
            spotifyDeviceSearchFocused = false;
            return;
        }
        if (inside(mx, my, rightPanelX + 186, navY, 82, 22)) {
            spotifyView = SpotifyView.LIBRARY;
            spotifyDeviceSearchFocused = false;
            spotifySearchFocused = false;
            service.loadUserPlaylists();
            return;
        }

        int contentX = rightPanelX + 10;
        int contentY = navY + 30;
        int contentW = rightPanelW - 20;
        int contentH = rightPanelH - 40 - 22;

        if (spotifyView == SpotifyView.HOME) {
            int artSize = 84;
            int searchX = contentX + artSize + 10;
            int searchW = Math.max(90, contentW - artSize - 10);
            if (inside(mx, my, searchX, contentY, searchW, 22)) {
                spotifyDeviceSearchFocused = true;
                spotifySearchFocused = false;
                return;
            }

            List<SpotifyDevice> filteredDevices = getFilteredSpotifyDevices(service.getSnapshot());
            int deviceListY = contentY + artSize + 8;
            int deviceListH = Math.max(24, contentH - artSize - 14);
            int rowH = 26;
            int visibleRows = Math.max(1, deviceListH / rowH);
            int start = spotifyDeviceScroll;
            int end = Math.min(filteredDevices.size(), start + visibleRows);
            int rowY = deviceListY;
            for (int i = start; i < end; i++) {
                SpotifyDevice device = filteredDevices.get(i);
                if (inside(mx, my, contentX, rowY, contentW, 22)) {
                    service.transferPlayback(device.id());
                    return;
                }
                rowY += rowH;
            }
        } else if (spotifyView == SpotifyView.SEARCH) {
            if (inside(mx, my, contentX, contentY, contentW - 74, 22)) {
                spotifySearchFocused = true;
                spotifyDeviceSearchFocused = false;
                return;
            }
            if (inside(mx, my, contentX + contentW - 68, contentY, 68, 22)) {
                spotifySearchFocused = false;
                service.searchTracks(spotifySearchInput);
                return;
            }

            List<SpotifySearchTrack> results = service.getSearchResults();
            int listY = contentY + 46;
            int rowH = 28;
            int visibleRows = Math.max(1, (contentH - 52) / rowH);
            int start = spotifySearchScroll;
            int end = Math.min(results.size(), start + visibleRows);
            int rowY = listY;
            for (int i = start; i < end; i++) {
                SpotifySearchTrack result = results.get(i);
                if (inside(mx, my, contentX, rowY, contentW, 24)) {
                    service.playTrack(result.uri());
                    return;
                }
                rowY += rowH;
            }
        } else {
            if (inside(mx, my, contentX + contentW - 72, contentY, 72, 22)) {
                service.loadUserPlaylists();
                return;
            }

            List<SpotifyPlaylist> items = service.getPlaylists();
            int listY = contentY + 32;
            int rowH = 28;
            int visibleRows = Math.max(1, (contentH - 36) / rowH);
            int start = spotifyLibraryScroll;
            int end = Math.min(items.size(), start + visibleRows);
            int rowY = listY;
            for (int i = start; i < end; i++) {
                SpotifyPlaylist playlist = items.get(i);
                if (inside(mx, my, contentX, rowY, contentW, 24)) {
                    service.playPlaylist(playlist.uri());
                    return;
                }
                rowY += rowH;
            }
        }

        spotifyClientIdFocused = false;
        spotifyRefreshFocused = false;
        spotifyDeviceSearchFocused = false;
        spotifySearchFocused = false;
        spotifyVolumeDragging = false;
    }

    private void adjustSpotifyVolume(int delta) {
        SpotifySnapshot snapshot = ClientClient.getSpotifyService().getSnapshot();
        int current = getActiveSpotifyVolume(snapshot);
        ClientClient.getSpotifyService().setVolume(Math.max(0, Math.min(100, current + delta)));
    }

    private int getActiveSpotifyVolume(SpotifySnapshot snapshot) {
        if (snapshot == null) {
            return spotifyLiveVolume;
        }
        int fallback = spotifyLiveVolume;
        String activeId = snapshot.activeDeviceId();
        for (SpotifyDevice device : snapshot.devices()) {
            if ((activeId != null && activeId.equals(device.id())) || device.active()) {
                return Math.max(0, Math.min(100, device.volumePercent()));
            }
            fallback = Math.max(0, Math.min(100, device.volumePercent()));
        }
        return fallback;
    }

    private void applySpotifyVolumeFromMouse(int localMouseX, int sliderX, int sliderW) {
        float pct = Math.max(0.0f, Math.min(1.0f, (localMouseX - sliderX) / (float) sliderW));
        int vol = Math.max(0, Math.min(100, Math.round(pct * 100.0f)));
        spotifyLiveVolume = vol;
        ClientClient.getSpotifyService().setVolume(vol);
    }

    private void applySpotifyRefreshInterval() {
        int value;
        try {
            value = Integer.parseInt(spotifyRefreshInput);
        } catch (NumberFormatException ignored) {
            value = 1000;
        }
        value = Math.max(750, Math.min(5000, value));
        spotifyRefreshInput = Integer.toString(value);
        ClientClient.getInstance().setSpotifyRefreshIntervalMs(value);
        ClientClient.getSpotifyService().setRefreshIntervalMs(value);
        ClientClient.getHudManager().saveConfig();
    }

    private String cropText(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private String formatSpotifyTime(int millis) {
        int totalSeconds = Math.max(0, millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    private List<SpotifyDevice> getFilteredSpotifyDevices(SpotifySnapshot snapshot) {
        String query = spotifyDeviceSearchQuery.toLowerCase(Locale.ROOT).trim();
        if (query.isEmpty()) {
            return snapshot.devices();
        }

        List<SpotifyDevice> out = new ArrayList<>();
        for (SpotifyDevice device : snapshot.devices()) {
            String name = device.name() == null ? "" : device.name().toLowerCase(Locale.ROOT);
            String type = device.type() == null ? "" : device.type().toLowerCase(Locale.ROOT);
            if (name.contains(query) || type.contains(query)) {
                out.add(device);
            }
        }
        return out;
    }

    private void updateSpotifyAlbumArtThumbnail(SpotifyTrack track) {
        String artUrl = (track == null || track.albumArtUrl() == null) ? "" : track.albumArtUrl();
        if (artUrl.isBlank()) {
            spotifyAlbumArtReady = false;
            spotifyAlbumArtKey = "";
            return;
        }

        if (artUrl.equals(spotifyAlbumArtKey) && spotifyAlbumArtReady) {
            return;
        }

        byte[] bytes = ClientClient.getSpotifyService().getAlbumArtBytes(artUrl);
        if (bytes == null || bytes.length == 0) {
            if (!artUrl.equals(spotifyAlbumArtKey)) {
                spotifyAlbumArtReady = false;
                spotifyAlbumArtKey = artUrl;
            }
            return;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return;
            }

            int width = Math.max(1, image.getWidth());
            int height = Math.max(1, image.getHeight());
            int idx = 0;
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    int px = Math.min(width - 1, (int) ((col / 7.0f) * (width - 1)));
                    int py = Math.min(height - 1, (int) ((row / 7.0f) * (height - 1)));
                    int rgb = image.getRGB(px, py);
                    spotifyAlbumArtPixels[idx++] = 0xFF000000 | (rgb & 0x00FFFFFF);
                }
            }
            spotifyAlbumArtReady = true;
            spotifyAlbumArtKey = artUrl;
        } catch (Exception ignored) {
        }
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

    private void renderShieldEditor(GuiGraphics ctx, int mouseX, int mouseY, int drawX, int drawY, XenonTheme theme) {
        int gridX = drawX + SHIELD_EDITOR_X;
        int gridY = drawY + SHIELD_EDITOR_Y;
        int gridSize = ShieldPatternData.GRID_SIZE * SHIELD_EDITOR_CELL;
        int color = 0xFF000000 | shieldColor();

        ctx.drawString(this.font, "Shield Painter", drawX + 76, drawY + 62, RenderUtils.TEXT_COLOR, true);
        ctx.drawString(this.font, "Left click draws, right click erases", drawX + 76, drawY + 76, RenderUtils.MUTED_COLOR, false);
        RenderUtils.drawGlassPanel(ctx, gridX - 8, gridY - 8, gridSize + 16, gridSize + 16, 8, 0xEAF7FCFF, theme.accent);
        for (int row = 0; row < ShieldPatternData.GRID_SIZE; row++) {
            for (int column = 0; column < ShieldPatternData.GRID_SIZE; column++) {
                int x = gridX + column * SHIELD_EDITOR_CELL;
                int y = gridY + row * SHIELD_EDITOR_CELL;
                boolean active = shieldPattern.charAt(row * ShieldPatternData.GRID_SIZE + column) == '1';
                ctx.fill(x, y, x + SHIELD_EDITOR_CELL - 1, y + SHIELD_EDITOR_CELL - 1,
                        active ? color : 0xBFE7F1F7);
            }
        }

        int controlsX = drawX + 414;
        int controlsY = drawY + 92;
        RenderUtils.drawGlassPanel(ctx, controlsX - 12, controlsY - 12, 318, 318, 8, 0xEAF7FCFF, theme.accent);
        ctx.drawString(this.font, "Paint color", controlsX, controlsY, RenderUtils.TEXT_COLOR, true);
        renderShieldColorSlider(ctx, "R", controlsX, controlsY + 28, shieldRed, 4, 0xFFE05A5A);
        renderShieldColorSlider(ctx, "G", controlsX, controlsY + 62, shieldGreen, 5, 0xFF55C878);
        renderShieldColorSlider(ctx, "B", controlsX, controlsY + 96, shieldBlue, 6, 0xFF4A8FE7);

        ctx.drawString(this.font, "Preview", controlsX, controlsY + 136, RenderUtils.TEXT_COLOR, true);
        int previewX = controlsX + 12;
        int previewY = controlsY + 154;
        RenderUtils.drawRoundedRect(ctx, previewX, previewY, 92, 126, 10, 0xFF6B4B32);
        for (int row = 0; row < ShieldPatternData.GRID_SIZE; row++) {
            for (int column = 0; column < ShieldPatternData.GRID_SIZE; column++) {
                if (shieldPattern.charAt(row * ShieldPatternData.GRID_SIZE + column) == '1') {
                    int x = previewX + 6 + column * 5;
                    int y = previewY + 7 + row * 7;
                    ctx.fill(x, y, x + 5, y + 7, color);
                }
            }
        }

        drawShieldEditorButton(ctx, mouseX, mouseY, controlsX + 124, controlsY + 154, 164, 28, "Load saved design", theme.accent);
        drawShieldEditorButton(ctx, mouseX, mouseY, controlsX + 124, controlsY + 192, 164, 28, "Clear canvas", 0xFFD87474);
        ctx.drawString(this.font, "Client-side: visible only to you", controlsX + 124, controlsY + 232, RenderUtils.MUTED_COLOR, false);
        drawShieldEditorButton(ctx, mouseX, mouseY, controlsX + 124, controlsY + 252, 164, 34, "Apply to held shield", 0xFF35B86B);
    }

    private void renderShieldColorSlider(GuiGraphics ctx, String label, int x, int y, int value, int slider, int color) {
        ctx.drawString(this.font, label + ": " + value, x, y, RenderUtils.TEXT_COLOR, false);
        int sliderX = x + 38;
        int sliderW = 220;
        RenderUtils.drawRoundedRect(ctx, sliderX, y + 3, sliderW, 10, 5, 0xA8080B10);
        int fillW = (int) ((value / 255.0F) * sliderW);
        RenderUtils.drawRoundedRect(ctx, sliderX, y + 3, fillW, 10, 5, color);
        RenderUtils.drawRoundedRect(ctx, Math.max(sliderX, sliderX + fillW - 2), y, 5, 16, 2, 0xFFFFFFFF);
        if (draggingSlider == slider) {
            ctx.drawString(this.font, "*", sliderX + sliderW + 8, y, color, false);
        }
    }

    private void drawShieldEditorButton(GuiGraphics ctx, int mouseX, int mouseY, int x, int y, int w, int h,
                                        String text, int accent) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        RenderUtils.drawGlassPanel(ctx, x, y, w, h, 6, hover ? 0xF7FFFFFF : 0xECF8FEFF, accent);
        ctx.drawString(this.font, text, x + (w - this.font.width(text)) / 2, y + (h - 8) / 2,
                RenderUtils.TEXT_COLOR, false);
    }

    private int shieldColor() {
        return (shieldRed << 16) | (shieldGreen << 8) | shieldBlue;
    }

    private void renderColorSlider(GuiGraphics ctx, String label, int x, int y, int value, int slider, int accentColor) {
        ctx.drawString(this.font, label + ": " + value, x, y, RenderUtils.TEXT_COLOR, false);
        int sliderX = x + 38;
        int sliderW = 220;
        RenderUtils.drawRoundedRect(ctx, sliderX, y + 3, sliderW, 10, 5, 0xA8080B10);
        int fillW = (int) ((value / 255.0f) * sliderW);
        RenderUtils.drawRoundedRect(ctx, sliderX, y + 3, fillW, 10, 5, accentColor);
        int handleX = Math.max(sliderX, sliderX + fillW - 2);
        RenderUtils.drawRoundedRect(ctx, handleX, y, 5, 16, 2, 0xFFFFFFFF);

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
            case DRAWN -> {
                int originX = centerX - CrosshairCustomizer.CUSTOM_GRID_SIZE / 2;
                int originY = centerY - CrosshairCustomizer.CUSTOM_GRID_SIZE / 2;
                for (int row = 0; row < CrosshairCustomizer.CUSTOM_GRID_SIZE; row++) {
                    for (int column = 0; column < CrosshairCustomizer.CUSTOM_GRID_SIZE; column++) {
                        if (CrosshairCustomizer.isCustomPixelSet(column, row)) {
                            ctx.fill(originX + column, originY + row, originX + column + 1, originY + row + 1, color);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int screenX = (int) event.x();
        int screenY = (int) event.y();

        if (currentTab == Tab.POSITIONS) {
            if (event.button() != 0) {
                return super.mouseClicked(event, doubleClick);
            }
            return positionsPanel.mouseClicked(screenX, screenY, event.button());
        }

        int absX = toLogical(screenX);
        int absY = toLogical(screenY);

        if (!inside(absX, absY, winX, winY, WIN_W, WIN_H)) {
            searchFocused = false;
            return true;
        }

        clickAnimX = absX;
        clickAnimY = absY;
        clickAnimStartedAt = System.currentTimeMillis();

        if (wizardVisible) {
            return handleFirstRunWizardClick(absX, absY);
        }

        int mx = absX - winX;
        int my = absY - winY;

        if (event.button() != 0) {
            if (currentTab == Tab.SETTINGS && event.button() == 1) {
                return settingsPanel.mouseClicked(mx, my, event.button());
            }
            if (currentTab == Tab.SHIELD && event.button() == 1) {
                handleShieldEditorClick(mx, my, false);
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

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
            if (inside(mx, my, boxX, 230, boxW, boxH)) { currentTab = Tab.SHIELD; return true; }
            if (inside(mx, my, boxX, 284, boxW, boxH)) { currentTab = Tab.SPOTIFY; return true; }
            if (inside(mx, my, boxX, 338, boxW, boxH)) { currentTab = Tab.PERFORMANCE; return true; }
            if (inside(mx, my, boxX, 392, boxW, boxH)) { currentTab = Tab.CONFIG; return true; }
            if (inside(mx, my, boxX, 446, boxW, boxH)) { currentTab = Tab.ABOUT; return true; }
            return true;
        }

        if (my < HEADER_H) {
            int filterW = 56;
            int filterH = 26;
            int filterStep = 60;
            int fx = SIDEBAR_W + 212;
            for (String f : FILTERS) {
                if (inside(mx, my, fx, 10, filterW, filterH)) {
                    currentFilter = f;
                    invalidateFilteredModules();
                    return true;
                }
                fx += filterStep;
            }

            int searchW = 146;
            int searchX = WIN_W - searchW - 12;
            if (inside(mx, my, searchX, 10, searchW, 26)) {
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

        if (currentTab == Tab.SHIELD) {
            handleShieldEditorClick(mx, my, true);
            return true;
        }

        if (currentTab == Tab.CONFIG) {
            return configPanel.mouseClicked(mx, my, event.button());
        }

        if (currentTab == Tab.SPOTIFY) {
            return spotifyPanel.mouseClicked(mx, my, event.button());
        }

        if (currentTab == Tab.PERFORMANCE) {
            return true;
        }

        return true;
    }

    private void handleShieldEditorClick(int mx, int my, boolean drawValue) {
        if (applyShieldCanvasInput(mx, my, drawValue)) {
            drawingShield = true;
            shieldDrawValue = drawValue;
            return;
        }

        int controlsX = 414;
        int controlsY = 92;
        int sliderX = controlsX + 38;
        if (inside(mx, my, sliderX, controlsY + 28, 220, 16)) {
            draggingSlider = 4;
            applyShieldSlider(mx, sliderX, 4);
            return;
        }
        if (inside(mx, my, sliderX, controlsY + 62, 220, 16)) {
            draggingSlider = 5;
            applyShieldSlider(mx, sliderX, 5);
            return;
        }
        if (inside(mx, my, sliderX, controlsY + 96, 220, 16)) {
            draggingSlider = 6;
            applyShieldSlider(mx, sliderX, 6);
            return;
        }
        if (!drawValue) {
            return;
        }
        if (inside(mx, my, controlsX + 124, controlsY + 154, 164, 28)) {
            loadHeldShieldPattern();
        } else if (inside(mx, my, controlsX + 124, controlsY + 192, 164, 28)) {
            shieldPattern = ShieldPatternData.EMPTY_PATTERN;
        } else if (inside(mx, my, controlsX + 124, controlsY + 252, 164, 34)) {
            applyPatternToHeldShield();
        }
    }

    private boolean applyShieldCanvasInput(int mx, int my, boolean active) {
        int size = ShieldPatternData.GRID_SIZE * SHIELD_EDITOR_CELL;
        if (!inside(mx, my, SHIELD_EDITOR_X, SHIELD_EDITOR_Y, size, size)) {
            return false;
        }
        int column = (mx - SHIELD_EDITOR_X) / SHIELD_EDITOR_CELL;
        int row = (my - SHIELD_EDITOR_Y) / SHIELD_EDITOR_CELL;
        int index = row * ShieldPatternData.GRID_SIZE + column;
        char[] pixels = shieldPattern.toCharArray();
        pixels[index] = active ? '1' : '0';
        shieldPattern = new String(pixels);
        return true;
    }

    private void applyShieldSlider(int mouseX, int sliderX, int slider) {
        int value = clamp((int) (((mouseX - sliderX) / 220.0F) * 255.0F), 0, 255);
        switch (slider) {
            case 4 -> shieldRed = value;
            case 5 -> shieldGreen = value;
            case 6 -> shieldBlue = value;
            default -> {
            }
        }
    }

    private ItemStack heldShield() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        if (client.player.getMainHandItem().is(Items.SHIELD)) {
            return client.player.getMainHandItem();
        }
        if (client.player.getOffhandItem().is(Items.SHIELD)) {
            return client.player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private void loadHeldShieldPattern() {
        ClientClient client = ClientClient.getInstance();
        shieldPattern = client.getShieldPattern();
        int color = client.getShieldColor();
        shieldRed = (color >> 16) & 0xFF;
        shieldGreen = (color >> 8) & 0xFF;
        shieldBlue = color & 0xFF;
        toast("Saved local design loaded");
    }

    private void applyPatternToHeldShield() {
        Minecraft client = Minecraft.getInstance();
        ItemStack shield = heldShield();
        if (client.player == null || shield.isEmpty()) {
            toast("Hold a shield in either hand");
            return;
        }
        ClientClient state = ClientClient.getInstance();
        state.setShieldPattern(shieldPattern);
        state.setShieldColor(shieldColor());
        ClientClient.getHudManager().saveConfig();
        toast("Local shield design saved");
    }

    private void handleSettingsClick(int mx, int my, int button) {
        if (applyCrosshairCanvasInput(mx, my, button == 0)) {
            drawingCrosshair = true;
            crosshairDrawValue = button == 0;
            crosshairType = CrosshairCustomizer.CrosshairType.DRAWN;
            customCrosshairEnabled = true;
            applyCrosshairSettings();
            return;
        }

        if (button != 0) {
            return;
        }

        int x = 76;
        int y = 62;

        if (inside(mx, my, x, y + 18, 140, 24)) {
            customCrosshairEnabled = !customCrosshairEnabled;
            applyCrosshairSettings();
            return;
        }

        int typeY = y + 52;
        int typeX = x;
        for (CrosshairCustomizer.CrosshairType type : CrosshairCustomizer.CrosshairType.values()) {
            if (inside(mx, my, typeX, typeY, 78, 24)) {
                crosshairType = type;
                applyCrosshairSettings();
                return;
            }
            typeX += 84;
        }

        int sliderX = x + 38;
        if (inside(mx, my, sliderX, y + 96, 220, 16)) {
            draggingSlider = 1;
            crosshairRed = valueFromSlider(mx, sliderX);
            applyCrosshairSettings();
            return;
        }
        if (inside(mx, my, sliderX, y + 128, 220, 16)) {
            draggingSlider = 2;
            crosshairGreen = valueFromSlider(mx, sliderX);
            applyCrosshairSettings();
            return;
        }
        if (inside(mx, my, sliderX, y + 160, 220, 16)) {
            draggingSlider = 3;
            crosshairBlue = valueFromSlider(mx, sliderX);
            applyCrosshairSettings();
            return;
        }

        int gridSize = CrosshairCustomizer.CUSTOM_GRID_SIZE * CROSSHAIR_EDITOR_CELL;
        if (inside(mx, my, CROSSHAIR_EDITOR_X, CROSSHAIR_EDITOR_Y + gridSize + 10, 64, 22)) {
            CrosshairCustomizer.clearCustomPattern();
            crosshairType = CrosshairCustomizer.CrosshairType.DRAWN;
            applyCrosshairSettings();
            ClientClient.getHudManager().saveConfig();
            return;
        }

        int themeY = 360;
        int bx = x + 48;
        for (XenonTheme entry : XenonTheme.values()) {
            if (inside(mx, my, bx, themeY - 5, 68, 22)) {
                ClientClient.getInstance().setThemeId(entry.name());
                ClientClient.getHudManager().saveConfig();
                return;
            }
            bx += 74;
        }
    }

    private boolean applyCrosshairCanvasInput(int mx, int my, boolean active) {
        int gridSize = CrosshairCustomizer.CUSTOM_GRID_SIZE * CROSSHAIR_EDITOR_CELL;
        if (!inside(mx, my, CROSSHAIR_EDITOR_X, CROSSHAIR_EDITOR_Y, gridSize, gridSize)) {
            return false;
        }

        int column = (mx - CROSSHAIR_EDITOR_X) / CROSSHAIR_EDITOR_CELL;
        int row = (my - CROSSHAIR_EDITOR_Y) / CROSSHAIR_EDITOR_CELL;
        CrosshairCustomizer.setCustomPixel(column, row, active);
        return true;
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
        int screenX = (int) event.x();
        int screenY = (int) event.y();

        if (draggingWindow) {
            int absX = toLogical(screenX);
            int absY = toLogical(screenY);
            int logicalWidth = Math.round(this.width / uiScale);
            int logicalHeight = Math.round(this.height / uiScale);
            winX = clamp(absX - windowDragOffsetX, 0, Math.max(0, logicalWidth - WIN_W));
            winY = clamp(absY - windowDragOffsetY, 0, Math.max(0, logicalHeight - WIN_H));
            return true;
        }

        if (draggingModule != null) {
            int targetX = screenX - moduleDragOffsetX;
            int targetY = screenY - moduleDragOffsetY;
            int snappedX = snapX(targetX, draggingModule);
            int snappedY = snapY(targetY, draggingModule);
            draggingModule.setX(snappedX);
            draggingModule.setY(snappedY);
            return true;
        }

        if (draggingSlider > 0) {
            int absX = toLogical(screenX);
            if (currentTab == Tab.SHIELD && draggingSlider >= 4) {
                applyShieldSlider(absX - winX, 452, draggingSlider);
                return true;
            }
            int sliderX = winX + 114;
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

        if (drawingCrosshair) {
            int mx = toLogical(screenX) - winX;
            int my = toLogical(screenY) - winY;
            applyCrosshairCanvasInput(mx, my, crosshairDrawValue);
            return true;
        }

        if (drawingShield) {
            int mx = toLogical(screenX) - winX;
            int my = toLogical(screenY) - winY;
            applyShieldCanvasInput(mx, my, shieldDrawValue);
            return true;
        }

        if (currentTab == Tab.SPOTIFY && spotifyVolumeDragging) {
            int absX = toLogical(screenX);
            int mx = absX - winX;
            int leftX = SPOTIFY_APP_X + 12;
            int volX = leftX + 42;
            int volW = SPOTIFY_LEFT_W - 96;
            applySpotifyVolumeFromMouse(mx, volX, volW);
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int logicalMouseX = toLogical(mouseX);
        int logicalMouseY = toLogical(mouseY);
        boolean overResizeArea = inside(logicalMouseX, logicalMouseY, winX, winY, WIN_W, WIN_H)
            && (logicalMouseX < winX + SIDEBAR_W || logicalMouseY < winY + HEADER_H);
        if (currentTab != Tab.POSITIONS && overResizeArea && verticalAmount != 0.0) {
            resizeMenu(verticalAmount);
            return true;
        }

        if (currentTab == Tab.SPOTIFY) {
            int absX = logicalMouseX;
            int absY = logicalMouseY;

            int appX = winX + SPOTIFY_APP_X;
            int appW = WIN_W - SIDEBAR_W - 80;
            int appH = WIN_H - 74;
            int gap = 12;
            int leftW = SPOTIFY_LEFT_W;
            int rightX = appX + leftW + gap;
            int rightW = appW - leftW - gap;
            int sectionY = (winY + SPOTIFY_APP_Y) + 38;
            int rightPanelX = rightX;
            int rightPanelY = sectionY;
            int rightPanelW = rightW - 12;
            int rightPanelH = appH - 40;
            int navY = rightPanelY + 24;
            int contentX = rightPanelX + 10;
            int contentY = navY + 30;
            int contentW = rightPanelW - 20;
            int contentH = rightPanelH - 40 - 22;

            if (spotifyView == SpotifyView.HOME) {
                int artSize = 84;
                int listY = contentY + artSize + 8;
                int listH = Math.max(24, contentH - artSize - 14);
                if (inside(absX, absY, contentX, listY, contentW + 8, listH)) {
                    if (spotifyDeviceMaxScroll <= 0) {
                        return true;
                    }
                    spotifyDeviceScroll = clamp(spotifyDeviceScroll - (int) Math.signum(verticalAmount), 0, spotifyDeviceMaxScroll);
                    return true;
                }
            } else if (spotifyView == SpotifyView.SEARCH) {
                int listY = contentY + 46;
                int listH = Math.max(24, contentH - 52);
                if (inside(absX, absY, contentX, listY, contentW + 8, listH)) {
                    if (spotifySearchMaxScroll <= 0) {
                        return true;
                    }
                    spotifySearchScroll = clamp(spotifySearchScroll - (int) Math.signum(verticalAmount), 0, spotifySearchMaxScroll);
                    return true;
                }
            } else {
                int listY = contentY + 32;
                int listH = Math.max(24, contentH - 36);
                if (inside(absX, absY, contentX, listY, contentW + 8, listH)) {
                    if (spotifyLibraryMaxScroll <= 0) {
                        return true;
                    }
                    spotifyLibraryScroll = clamp(spotifyLibraryScroll - (int) Math.signum(verticalAmount), 0, spotifyLibraryMaxScroll);
                    return true;
                }
            }

            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (currentTab != Tab.MODULES) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int absX = logicalMouseX;
        int absY = logicalMouseY;
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

        if (drawingCrosshair) {
            drawingCrosshair = false;
            ClientClient.getHudManager().saveConfig();
            handled = true;
        }

        if (drawingShield) {
            drawingShield = false;
            handled = true;
        }

        if (spotifyVolumeDragging) {
            spotifyVolumeDragging = false;
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
        boolean ctrlDown = (keyEvent.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;

        if (ctrlDown && key == GLFW.GLFW_KEY_F) {
            searchFocused = true;
            return true;
        }

        if (ctrlDown && key == GLFW.GLFW_KEY_V) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip == null) {
                clip = "";
            }
            clip = clip.replace("\r", "").replace("\n", "");

            if (spotifyClientIdFocused) {
                spotifyClientIdInput = clip.length() > 128 ? clip.substring(0, 128) : clip;
                return true;
            }

            if (spotifyRefreshFocused) {
                StringBuilder digits = new StringBuilder();
                for (int i = 0; i < clip.length(); i++) {
                    char c = clip.charAt(i);
                    if (Character.isDigit(c)) {
                        digits.append(c);
                    }
                    if (digits.length() >= 5) {
                        break;
                    }
                }
                if (!digits.isEmpty()) {
                    spotifyRefreshInput = digits.toString();
                }
                return true;
            }

            if (spotifyDeviceSearchFocused) {
                spotifyDeviceSearchQuery = clip.length() > 24 ? clip.substring(0, 24) : clip;
                spotifyDeviceScroll = 0;
                return true;
            }

            if (spotifySearchFocused) {
                spotifySearchInput = clip.length() > 64 ? clip.substring(0, 64) : clip;
                return true;
            }

            if (searchFocused) {
                searchQuery = clip.length() > 64 ? clip.substring(0, 64) : clip;
                invalidateFilteredModules();
                return true;
            }

            if (presetNameFocused) {
                StringBuilder name = new StringBuilder();
                for (int i = 0; i < clip.length(); i++) {
                    char c = clip.charAt(i);
                    if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                        name.append(c);
                    }
                    if (name.length() >= 16) {
                        break;
                    }
                }
                if (!name.isEmpty()) {
                    presetName = name.toString();
                }
                return true;
            }
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

        if (spotifyClientIdFocused && key == 259) {
            if (!spotifyClientIdInput.isEmpty()) {
                spotifyClientIdInput = spotifyClientIdInput.substring(0, spotifyClientIdInput.length() - 1);
            }
            return true;
        }

        if (spotifyRefreshFocused && key == 259) {
            if (!spotifyRefreshInput.isEmpty()) {
                spotifyRefreshInput = spotifyRefreshInput.substring(0, spotifyRefreshInput.length() - 1);
            }
            return true;
        }

        if (spotifyDeviceSearchFocused && key == 259) {
            if (!spotifyDeviceSearchQuery.isEmpty()) {
                spotifyDeviceSearchQuery = spotifyDeviceSearchQuery.substring(0, spotifyDeviceSearchQuery.length() - 1);
                spotifyDeviceScroll = 0;
            }
            return true;
        }

        if (spotifySearchFocused && key == 259) {
            if (!spotifySearchInput.isEmpty()) {
                spotifySearchInput = spotifySearchInput.substring(0, spotifySearchInput.length() - 1);
            }
            return true;
        }

        if (spotifyRefreshFocused && key == 257) {
            applySpotifyRefreshInterval();
            spotifyRefreshFocused = false;
            return true;
        }

        if (spotifyClientIdFocused && key == 257) {
            ClientClient.getInstance().setSpotifyClientId(spotifyClientIdInput);
            ClientClient.getSpotifyService().setClientId(spotifyClientIdInput);
            ClientClient.getHudManager().saveConfig();
            spotifyClientIdFocused = false;
            return true;
        }

        if (spotifyDeviceSearchFocused && key == 257) {
            spotifyDeviceSearchFocused = false;
            return true;
        }

        if (spotifySearchFocused && key == 257) {
            ClientClient.getSpotifyService().searchTracks(spotifySearchInput);
            spotifySearchFocused = false;
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

        if (spotifyClientIdFocused && key == 256) {
            spotifyClientIdFocused = false;
            return true;
        }

        if (spotifyRefreshFocused && key == 256) {
            spotifyRefreshFocused = false;
            return true;
        }

        if (spotifyDeviceSearchFocused && key == 256) {
            if (!spotifyDeviceSearchQuery.isEmpty()) {
                spotifyDeviceSearchQuery = "";
                spotifyDeviceScroll = 0;
            }
            spotifyDeviceSearchFocused = false;
            return true;
        }

        if (spotifySearchFocused && key == 256) {
            if (!spotifySearchInput.isEmpty()) {
                spotifySearchInput = "";
                return true;
            }
            spotifySearchFocused = false;
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

        if (spotifyClientIdFocused) {
            char c = (char) event.codepoint();
            if (!Character.isISOControl(c) && spotifyClientIdInput.length() < 128) {
                spotifyClientIdInput += c;
            }
            return true;
        }

        if (spotifyRefreshFocused) {
            char c = (char) event.codepoint();
            if (Character.isDigit(c) && spotifyRefreshInput.length() < 5) {
                spotifyRefreshInput += c;
            }
            return true;
        }

        if (spotifyDeviceSearchFocused) {
            char c = (char) event.codepoint();
            if (!Character.isISOControl(c) && spotifyDeviceSearchQuery.length() < 24) {
                spotifyDeviceSearchQuery += c;
                spotifyDeviceScroll = 0;
            }
            return true;
        }

        if (spotifySearchFocused) {
            char c = (char) event.codepoint();
            if (!Character.isISOControl(c) && spotifySearchInput.length() < 64) {
                spotifySearchInput += c;
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
        state.setSpotifyCompactView(spotifyCompactView);
        state.setSpotifyClientId(spotifyClientIdInput);
        ClientClient.getSpotifyService().setClientId(spotifyClientIdInput);
        applySpotifyRefreshInterval();
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
            case SHIELD -> {
                RenderUtils.drawRoundedRect(ctx, x + 4, y + 3, 8, 10, 3, fx);
                ctx.fill(x + 6, y + 12, x + 10, y + 14, fx);
            }
            case SPOTIFY -> {
                RenderUtils.drawRoundedRect(ctx, x + 3, y + 4, 10, 10, 5, fx);
                ctx.fill(x + 6, y + 7, x + 10, y + 8, 0x44FFFFFF);
                ctx.fill(x + 7, y + 9, x + 11, y + 10, 0x44FFFFFF);
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
        int bgColor = enabled ? accentColor : 0xFFD0E8F5;
        RenderUtils.drawRoundedRect(ctx, x, y, 18, 18, 4, bgColor);
        
        int lightEdge = 0x26FFFFFF;
        ctx.fill(x, y, x + 18, y + 1, lightEdge);
        ctx.fill(x, y, x + 1, y + 18, lightEdge);
        
        int innerSize = 10;
        int innerX = x + (18 - innerSize) / 2;
        int innerY = y + (18 - innerSize) / 2;
        int innerColor = enabled ? 0xFFFFFFFF : 0xFF7A9AB0;
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

    private int valueFromSlider(int localX, int sliderX) {
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

    private int toLogical(double coordinate) {
        return (int) (coordinate / uiScale);
    }

    private void resizeMenu(double scrollAmount) {
        float nextScale = uiScale + (float) Math.signum(scrollAmount) * UI_SCALE_STEP;
        nextScale = Math.max(MIN_UI_SCALE, Math.min(MAX_UI_SCALE, nextScale));
        if (Math.abs(nextScale - uiScale) < 0.001f) {
            return;
        }

        float screenCenterX = (winX + WIN_W / 2.0f) * uiScale;
        float screenCenterY = (winY + WIN_H / 2.0f) * uiScale;
        uiScale = nextScale;

        int logicalWidth = Math.round(this.width / uiScale);
        int logicalHeight = Math.round(this.height / uiScale);
        winX = clamp(Math.round(screenCenterX / uiScale - WIN_W / 2.0f), 0, Math.max(0, logicalWidth - WIN_W));
        winY = clamp(Math.round(screenCenterY / uiScale - WIN_H / 2.0f), 0, Math.max(0, logicalHeight - WIN_H));
    }

    private void renderClickAnimation(GuiGraphics ctx, int accentColor) {
        if (clickAnimStartedAt == 0L) {
            return;
        }

        float progress = (System.currentTimeMillis() - clickAnimStartedAt) / (float) CLICK_ANIMATION_MS;
        if (progress >= 1.0f) {
            clickAnimStartedAt = 0L;
            return;
        }

        RenderUtils.drawClickPulse(ctx, clickAnimX, clickAnimY, Math.max(0.0f, progress), accentColor);
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
