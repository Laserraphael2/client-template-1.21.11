package mod.client.client.state;

public class UIState {
    private String currentFilter = "All";
    private String searchQuery = "";
    private String themeId = "ICE";
    private int menuX = -1;
    private int menuY = -1;
    private boolean wizardCompleted = false;
    private boolean lightModeEnabled = false;
    private int lightModeThresholdFps = 45;

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
        this.themeId = themeId;
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
}
