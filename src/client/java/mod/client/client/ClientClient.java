package mod.client.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import mod.client.client.hud.HudManager;
import mod.client.client.render.CrosshairCustomizer;
import mod.client.client.state.UIState;

public class ClientClient implements ClientModInitializer {
    private static ClientClient instance;
    private static HudManager hudManager;
    private final UIState uiState = new UIState();
    
    @Override
    public void onInitializeClient() {
        instance = this;
        hudManager = new HudManager();
        KeyBindings.register();
        
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            hudManager.render(context, tickDelta.getGameTimeDeltaTicks());
        });
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            hudManager.tick(client);
        });
    }
    
    public static ClientClient getInstance() {
        return instance;
    }
    
    public static HudManager getHudManager() {
        return hudManager;
    }

    public UIState getUiState() {
        return uiState;
    }
    
    // Crosshair settings - delegates to CrosshairCustomizer
    public CrosshairCustomizer.CrosshairType getCrosshairType() {
        return CrosshairCustomizer.getType();
    }
    
    public void setCrosshairType(CrosshairCustomizer.CrosshairType type) {
        CrosshairCustomizer.setType(type);
    }
    
    public int getCrosshairColor() {
        return CrosshairCustomizer.getColor();
    }
    
    public void setCrosshairColor(int color) {
        CrosshairCustomizer.setColor(color);
    }
    
    public boolean isCustomCrosshairEnabled() {
        return CrosshairCustomizer.isEnabled();
    }
    
    public void setCustomCrosshairEnabled(boolean enabled) {
        CrosshairCustomizer.setEnabled(enabled);
    }

    public String getCurrentFilter() {
        return uiState.getCurrentFilter();
    }

    public void setCurrentFilter(String currentFilter) {
        uiState.setCurrentFilter(currentFilter);
    }

    public String getSearchQuery() {
        return uiState.getSearchQuery();
    }

    public void setSearchQuery(String searchQuery) {
        uiState.setSearchQuery(searchQuery);
    }

    public String getThemeId() {
        return uiState.getThemeId();
    }

    public void setThemeId(String themeId) {
        uiState.setThemeId(themeId);
    }

    public int getMenuX() {
        return uiState.getMenuX();
    }

    public void setMenuX(int menuX) {
        uiState.setMenuX(menuX);
    }

    public int getMenuY() {
        return uiState.getMenuY();
    }

    public void setMenuY(int menuY) {
        uiState.setMenuY(menuY);
    }

    public boolean isWizardCompleted() {
        return uiState.isWizardCompleted();
    }

    public void setWizardCompleted(boolean wizardCompleted) {
        uiState.setWizardCompleted(wizardCompleted);
    }

    public boolean isLightModeEnabled() {
        return uiState.isLightModeEnabled();
    }

    public void setLightModeEnabled(boolean enabled) {
        uiState.setLightModeEnabled(enabled);
    }

    public int getLightModeThresholdFps() {
        return uiState.getLightModeThresholdFps();
    }

    public void setLightModeThresholdFps(int fps) {
        uiState.setLightModeThresholdFps(fps);
    }
}