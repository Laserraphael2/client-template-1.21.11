package mod.client.client.spotify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class SpotifyAuthStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private File getAuthFile() {
        Minecraft mc = Minecraft.getInstance();
        File gameDir = (mc != null) ? mc.gameDirectory : new File(System.getProperty("user.dir"));
        return new File(gameDir, "config/spotify-auth.json");
    }

    public AuthData load() {
        File file = getAuthFile();
        if (!file.exists()) {
            return new AuthData();
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return new AuthData();
            }
            AuthData data = new AuthData();
            if (root.has("clientId")) data.clientId = root.get("clientId").getAsString();
            if (root.has("accessToken")) data.accessToken = root.get("accessToken").getAsString();
            if (root.has("refreshToken")) data.refreshToken = root.get("refreshToken").getAsString();
            if (root.has("expiresAtMs")) data.expiresAtMs = root.get("expiresAtMs").getAsLong();
            return data;
        } catch (Exception ignored) {
            return new AuthData();
        }
    }

    public void save(AuthData data) {
        File file = getAuthFile();
        try {
            file.getParentFile().mkdirs();
            JsonObject root = new JsonObject();
            root.addProperty("clientId", safe(data.clientId));
            root.addProperty("accessToken", safe(data.accessToken));
            root.addProperty("refreshToken", safe(data.refreshToken));
            root.addProperty("expiresAtMs", data.expiresAtMs);
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public void clear() {
        File file = getAuthFile();
        if (file.exists()) {
            file.delete();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class AuthData {
        public String clientId = "";
        public String accessToken = "";
        public String refreshToken = "";
        public long expiresAtMs = 0L;
    }
}
