package mod.client.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import mod.client.client.ClientClient;
import mod.client.client.modules.HudModule;
import mod.client.client.state.UIState;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("Xenon");

    private File getConfigFile() {
        Minecraft mc = Minecraft.getInstance();
        File gameDir = (mc != null) ? mc.gameDirectory : new File(System.getProperty("user.dir"));
        return new File(gameDir, "config/client.json");
    }

    private File getBackupFile() {
        Minecraft mc = Minecraft.getInstance();
        File gameDir = (mc != null) ? mc.gameDirectory : new File(System.getProperty("user.dir"));
        return new File(gameDir, "config/client.backup.json");
    }

    private File getPresetsDirectory() {
        Minecraft mc = Minecraft.getInstance();
        File gameDir = (mc != null) ? mc.gameDirectory : new File(System.getProperty("user.dir"));
        return new File(gameDir, "config/client-presets");
    }

    private File getPresetFile(String presetName) {
        String safeName = presetName == null ? "default" : presetName.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        if (safeName.isEmpty()) {
            safeName = "default";
        }
        return new File(getPresetsDirectory(), safeName + ".json");
    }

    public void save(List<HudModule> moduleList, ClientClient clientState) {
        File configFile = getConfigFile();
        try {
            configFile.getParentFile().mkdirs();

            if (configFile.exists()) {
                Files.copy(configFile.toPath(), getBackupFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            JsonObject root = createRoot(moduleList, clientState);

            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(root, writer);
            }
            LOGGER.info("Config saved: {}", configFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public void load(List<HudModule> moduleList, ClientClient clientState) {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }

            applyRoot(root, moduleList, clientState);

            LOGGER.info("Config loaded");
        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    public void resetPositions(List<HudModule> moduleList) {
        int yOffset = 5;
        for (HudModule module : moduleList) {
            module.setX(5);
            module.setY(yOffset);
            module.setScale(1.0f);
            yOffset += module.getHeight() + 5;
        }
    }

    public boolean savePreset(String presetName, List<HudModule> moduleList, ClientClient clientState) {
        File presetFile = getPresetFile(presetName);
        try {
            presetFile.getParentFile().mkdirs();
            JsonObject root = createRoot(moduleList, clientState);
            try (FileWriter writer = new FileWriter(presetFile)) {
                GSON.toJson(root, writer);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to save preset {}", presetName, e);
            return false;
        }
    }

    public boolean loadPreset(String presetName, List<HudModule> moduleList, ClientClient clientState) {
        File presetFile = getPresetFile(presetName);
        if (!presetFile.exists()) {
            return false;
        }

        try (FileReader reader = new FileReader(presetFile)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return false;
            }
            applyRoot(root, moduleList, clientState);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to load preset {}", presetName, e);
            return false;
        }
    }

    public boolean deletePreset(String presetName) {
        File presetFile = getPresetFile(presetName);
        return presetFile.exists() && presetFile.delete();
    }

    public List<String> listPresets() {
        File dir = getPresetsDirectory();
        List<String> out = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) {
            return out;
        }

        File[] files = dir.listFiles((parent, name) -> name.endsWith(".json"));
        if (files == null) {
            return out;
        }

        for (File file : files) {
            String name = file.getName();
            out.add(name.substring(0, name.length() - 5));
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    private JsonObject createRoot(List<HudModule> moduleList, ClientClient clientState) {
        JsonObject root = new JsonObject();
        JsonObject modulesJson = new JsonObject();
        for (HudModule module : moduleList) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("x", module.getX());
            moduleJson.addProperty("y", module.getY());
            moduleJson.addProperty("enabled", module.isEnabled());
            moduleJson.addProperty("scale", module.getScale());
            moduleJson.addProperty("keybind", module.getKeybind());
            modulesJson.add(module.getName(), moduleJson);
        }
        root.add("modules", modulesJson);

        UIState uiState = clientState.getUiState();
        JsonObject ui = new JsonObject();
        ui.addProperty("filter", uiState.getCurrentFilter());
        ui.addProperty("search", uiState.getSearchQuery());
        ui.addProperty("theme", uiState.getThemeId());
        ui.addProperty("menuX", uiState.getMenuX());
        ui.addProperty("menuY", uiState.getMenuY());
        ui.addProperty("wizardCompleted", uiState.isWizardCompleted());
        ui.addProperty("lightModeEnabled", uiState.isLightModeEnabled());
        ui.addProperty("lightModeThresholdFps", uiState.getLightModeThresholdFps());
        ui.addProperty("crosshairEnabled", clientState.isCustomCrosshairEnabled());
        ui.addProperty("crosshairType", clientState.getCrosshairType().name());
        ui.addProperty("crosshairColor", clientState.getCrosshairColor());
        root.add("ui", ui);
        return root;
    }

    private void applyRoot(JsonObject root, List<HudModule> moduleList, ClientClient clientState) {
        if (root.has("modules")) {
            JsonObject modulesJson = root.getAsJsonObject("modules");
            for (HudModule module : moduleList) {
                String name = module.getName();
                if (!modulesJson.has(name)) {
                    continue;
                }
                JsonObject moduleJson = modulesJson.getAsJsonObject(name);
                if (moduleJson.has("x")) module.setX(moduleJson.get("x").getAsInt());
                if (moduleJson.has("y")) module.setY(moduleJson.get("y").getAsInt());
                if (moduleJson.has("enabled")) module.setEnabled(moduleJson.get("enabled").getAsBoolean());
                if (moduleJson.has("scale")) module.setScale(moduleJson.get("scale").getAsFloat());
                if (moduleJson.has("keybind")) module.setKeybind(moduleJson.get("keybind").getAsInt());
            }
        }

        if (root.has("ui")) {
            JsonObject ui = root.getAsJsonObject("ui");
            UIState uiState = clientState.getUiState();
            if (ui.has("filter")) uiState.setCurrentFilter(ui.get("filter").getAsString());
            if (ui.has("search")) uiState.setSearchQuery(ui.get("search").getAsString());
            if (ui.has("theme")) uiState.setThemeId(ui.get("theme").getAsString());
            if (ui.has("menuX")) uiState.setMenuX(ui.get("menuX").getAsInt());
            if (ui.has("menuY")) uiState.setMenuY(ui.get("menuY").getAsInt());
            if (ui.has("wizardCompleted")) uiState.setWizardCompleted(ui.get("wizardCompleted").getAsBoolean());
            if (ui.has("lightModeEnabled")) uiState.setLightModeEnabled(ui.get("lightModeEnabled").getAsBoolean());
            if (ui.has("lightModeThresholdFps")) uiState.setLightModeThresholdFps(ui.get("lightModeThresholdFps").getAsInt());
            if (ui.has("crosshairEnabled")) clientState.setCustomCrosshairEnabled(ui.get("crosshairEnabled").getAsBoolean());
            if (ui.has("crosshairColor")) clientState.setCrosshairColor(ui.get("crosshairColor").getAsInt());
            if (ui.has("crosshairType")) {
                try {
                    clientState.setCrosshairType(mod.client.client.render.CrosshairCustomizer.CrosshairType.valueOf(ui.get("crosshairType").getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
