package mod.client.client.state;

import mod.client.client.render.XenonTheme;

public class UIState {
    private String currentFilter = "All";
    private String searchQuery = "";
    private String themeId = "BLACK";
    private int menuX = -1;
    private int menuY = -1;
    private boolean wizardCompleted = false;
    private boolean lightModeEnabled = false;
    private int lightModeThresholdFps = 45;
    private boolean spotifyEnabled = true;
    private boolean spotifyHudEnabled = true;
    private String spotifyClientId = "";
    private int spotifyRefreshIntervalMs = 1000;
    private boolean spotifyCompactView = false;

    public String getCurrentFilter() {
        return currentFilter;
    }

    public void setCurrentFilter(String currentFilter) {
        this.currentFilter = currentFilter;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = XenonTheme.fromId(themeId).name();
    }

    public int getMenuX() {
        return menuX;
    }

    public void setMenuX(int menuX) {
        this.menuX = menuX;
    }

    public int getMenuY() {
        return menuY;
    }

    public void setMenuY(int menuY) {
        this.menuY = menuY;
    }

    public boolean isWizardCompleted() {
        return wizardCompleted;
    }

    public void setWizardCompleted(boolean wizardCompleted) {
        this.wizardCompleted = wizardCompleted;
    }

    public boolean isLightModeEnabled() {
        return lightModeEnabled;
    }

    public void setLightModeEnabled(boolean lightModeEnabled) {
        this.lightModeEnabled = lightModeEnabled;
    }

    public int getLightModeThresholdFps() {
        return lightModeThresholdFps;
    }

    public void setLightModeThresholdFps(int lightModeThresholdFps) {
        this.lightModeThresholdFps = Math.max(20, Math.min(240, lightModeThresholdFps));
    }

    public boolean isSpotifyEnabled() {
        return spotifyEnabled;
    }

    public void setSpotifyEnabled(boolean spotifyEnabled) {
        this.spotifyEnabled = spotifyEnabled;
    }

    public boolean isSpotifyHudEnabled() {
        return spotifyHudEnabled;
    }

    public void setSpotifyHudEnabled(boolean spotifyHudEnabled) {
        this.spotifyHudEnabled = spotifyHudEnabled;
    }

    public String getSpotifyClientId() {
        return spotifyClientId;
    }

    public void setSpotifyClientId(String spotifyClientId) {
        this.spotifyClientId = spotifyClientId == null ? "" : spotifyClientId.trim();
    }

    public int getSpotifyRefreshIntervalMs() {
        return spotifyRefreshIntervalMs;
    }

    public void setSpotifyRefreshIntervalMs(int spotifyRefreshIntervalMs) {
        this.spotifyRefreshIntervalMs = Math.max(750, Math.min(5000, spotifyRefreshIntervalMs));
    }

    public boolean isSpotifyCompactView() {
        return spotifyCompactView;
    }

    public void setSpotifyCompactView(boolean spotifyCompactView) {
        this.spotifyCompactView = spotifyCompactView;
    }
}
