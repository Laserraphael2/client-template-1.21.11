package mod.client.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import mod.client.client.ClientClient;
import mod.client.client.config.ClientConfig;
import mod.client.client.screen.XenonMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import mod.client.client.modules.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class HudManager {
    private final List<HudModule> modules = new ArrayList<>();
    private final ClientConfig config = new ClientConfig();
    private final Map<String, Set<String>> moduleTags = new HashMap<>();
    private final Map<Integer, Boolean> keyState = new HashMap<>();
    
    public HudManager() {
        modules.add(new CPSCounter());
        modules.add(new FPSPingDisplay());
        modules.add(new ArmorStatus());
        modules.add(new ArmorBarAdvanced());
        modules.add(new KeystrokeOverlay());
        modules.add(new SprintSneakStatus());
        modules.add(new PotionStatusHud());
        modules.add(new TargetHud());
        modules.add(new ComboCounter());
        modules.add(new DirectionCoordinates());
        modules.add(new SpeedDisplay());
        modules.add(new TpsDisplay());
        modules.add(new MemoryUsageDisplay());
        modules.add(new CompassDisplay());
        modules.add(new SessionStats());
        modules.add(new ReachDisplay());
        modules.add(new ItemCounters());
        modules.add(new CpsGraph());
        modules.add(new PingGraph());
        modules.add(new MinimapLiteRadar());
        modules.add(new SpotifyNowPlayingModule());
        modules.add(new MaceEnchantmentHud());

        registerTags();
        config.load(modules, ClientClient.getInstance());
    }
    
    public void render(GuiGraphics context, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof XenonMenuScreen screen && screen.isPositionEditorMode()) {
            return;
        }

        for (HudModule module : modules) {
            if (module.isEnabled()) {
                if (!shouldRenderModule(client, module)) {
                    continue;
                }

                float scale = module.getScale();
                if (Math.abs(scale - 1.0f) < 0.001f) {
                    module.render(context, partialTick);
                    continue;
                }

                int oldX = module.getX();
                int oldY = module.getY();

                context.pose().pushMatrix();
                context.pose().translate(oldX, oldY);
                context.pose().scale(scale, scale);
                module.setX(0);
                module.setY(0);
                module.render(context, partialTick);
                module.setX(oldX);
                module.setY(oldY);
                context.pose().popMatrix();
            }
        }
    }
    
    public void tick(Minecraft client) {
        for (HudModule module : modules) {
            module.tick(client);
        }

        handleModuleKeybindToggles(client);
    }
    
    public List<HudModule> getModules() {
        return modules;
    }

    public <T extends HudModule> Optional<T> getModule(Class<T> type) {
        return modules.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }
    
    public void saveConfig() {
        config.save(modules, ClientClient.getInstance());
    }
    
    public void resetPositions() {
        config.resetPositions(modules);
    }

    public boolean hasTag(HudModule module, String tag) {
        if (module == null || tag == null) {
            return false;
        }
        Set<String> tags = moduleTags.get(module.getName());
        return tags != null && tags.contains(tag.toLowerCase());
    }

    public boolean matchesFilter(HudModule module, String filter) {
        if ("All".equalsIgnoreCase(filter)) {
            return true;
        }
        if ("HUD".equalsIgnoreCase(filter)) {
            return hasTag(module, "hud");
        }
        if ("New".equalsIgnoreCase(filter)) {
            return hasTag(module, "new");
        }
        if ("Hypixel".equalsIgnoreCase(filter)) {
            return hasTag(module, "hypixel");
        }
        if ("PvP".equalsIgnoreCase(filter)) {
            return hasTag(module, "pvp");
        }
        return false;
    }

    private void registerTags() {
        tag("CPS Counter", "hud", "pvp", "hypixel");
        tag("CPS Graph", "hud", "new", "pvp");
        tag("FPS & Ping", "hud", "hypixel");
        tag("Ping Graph", "hud", "new", "hypixel");
        tag("Armor Status", "hud", "pvp");
        tag("Armor Bars", "hud", "new", "pvp");
        tag("Keystrokes", "hud", "pvp", "hypixel");
        tag("Sprint/Sneak Status", "hud", "pvp");
        tag("Potion Status", "hud", "new", "pvp");
        tag("Target HUD", "hud", "new", "pvp", "hypixel");
        tag("Combo Counter", "hud", "new", "pvp", "hypixel");
        tag("Direction + Coords", "hud", "new", "hypixel");
        tag("Speed", "hud", "new", "pvp");
        tag("TPS", "hud", "new");
        tag("Memory", "hud", "new");
        tag("Compass", "hud", "new", "hypixel");
        tag("Session Stats", "hud", "new", "pvp");
        tag("Reach Display", "hud", "new", "pvp", "hypixel");
        tag("Item Counters", "hud", "new", "pvp", "hypixel");
        tag("Minimap Radar", "hud", "new");
        tag("Spotify Now Playing", "hud", "new");
        tag("Mace Enchantment", "hud", "new", "pvp");
    }

    private void tag(String moduleName, String... tags) {
        Set<String> set = moduleTags.computeIfAbsent(moduleName, key -> new HashSet<>());
        for (String tag : tags) {
            set.add(tag.toLowerCase());
        }
    }

    private void handleModuleKeybindToggles(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            return;
        }

        var window = client.getWindow();
        boolean changed = false;

        for (HudModule module : modules) {
            int key = module.getKeybind();
            if (key < 0) {
                continue;
            }

            boolean down = InputConstants.isKeyDown(window, key);
            boolean wasDown = keyState.getOrDefault(key, false);
            if (down && !wasDown) {
                module.setEnabled(!module.isEnabled());
                changed = true;
            }
            keyState.put(key, down);
        }

        if (changed) {
            saveConfig();
        }
    }

    public List<String> listPresets() {
        return config.listPresets();
    }

    public boolean savePreset(String presetName) {
        return config.savePreset(presetName, modules, ClientClient.getInstance());
    }

    public boolean loadPreset(String presetName) {
        return config.loadPreset(presetName, modules, ClientClient.getInstance());
    }

    public boolean deletePreset(String presetName) {
        return config.deletePreset(presetName);
    }

    private boolean shouldRenderModule(Minecraft client, HudModule module) {
        ClientClient instance = ClientClient.getInstance();
        if (instance == null || !instance.isLightModeEnabled()) {
            return true;
        }

        if (client == null || client.getFps() >= instance.getLightModeThresholdFps()) {
            return true;
        }

        return !(module instanceof MinimapLiteRadar
                || module instanceof TargetHud
                || module instanceof PotionStatusHud
                || module instanceof CpsGraph
                || module instanceof PingGraph);
    }
}
