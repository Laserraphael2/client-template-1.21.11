package mod.client.client.spotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpServer;
import mod.client.client.ClientClient;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpotifyService {
    private static final Logger LOGGER = LoggerFactory.getLogger("Xenon-Spotify");
    private static final String AUTHORIZE_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String API_BASE = "https://api.spotify.com/v1";
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final String BUNDLED_CLIENT_ID = "fb57aabad8f24190ae8bc3a2551a3af4";
    private static final String SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing user-read-private playlist-read-private playlist-read-collaborative";

    private static final SpotifyService INSTANCE = new SpotifyService();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(15))
            .build();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "xenon-spotify");
        t.setDaemon(true);
        return t;
    });
    private final SpotifyAuthStore authStore = new SpotifyAuthStore();
    private final Object lock = new Object();
    private final Map<String, byte[]> albumArtCache = new ConcurrentHashMap<>();

    private volatile SpotifySnapshot snapshot = SpotifySnapshot.disconnected("Spotify disconnected", 1000);

    private String clientId = "";
    private String accessToken = "";
    private String refreshToken = "";
    private long expiresAtMs = 0L;

    private int refreshIntervalMs = 1000;
    private long nextAllowedPollMs = 0L;
    private int devicesRefreshCounter = 0;

    private volatile List<SpotifySearchTrack> searchResults = List.of();
    private volatile String lastSearchQuery = "";
    private volatile List<SpotifyPlaylist> playlists = List.of();
    private volatile String playlistsStatus = "Library not loaded";

    private HttpServer callbackServer;
    private String pendingState = "";
    private String pendingVerifier = "";

    private boolean initialized;

    private SpotifyService() {
    }

    public static SpotifyService getInstance() {
        return INSTANCE;
    }

    public void initializeFromState(ClientClient state) {
        synchronized (lock) {
            if (initialized) {
                return;
            }
            initialized = true;
        }

        this.refreshIntervalMs = clampRefreshInterval(state.getSpotifyRefreshIntervalMs());

        SpotifyAuthStore.AuthData auth = authStore.load();
        String configuredClientId = normalizeClientId(state.getSpotifyClientId());
        this.clientId = isLikelyClientId(configuredClientId) ? configuredClientId : BUNDLED_CLIENT_ID;
        boolean compatibleSession = this.clientId.equals(normalizeClientId(auth.clientId));
        state.setSpotifyClientId(this.clientId);
        if (compatibleSession) {
            this.accessToken = safe(auth.accessToken);
            this.refreshToken = safe(auth.refreshToken);
            this.expiresAtMs = auth.expiresAtMs;
        } else {
            this.accessToken = "";
            this.refreshToken = "";
            this.expiresAtMs = 0L;
            persistAuth();
        }

        setStatus(this.accessToken.isEmpty() ? "Spotify disconnected" : "Spotify ready", false, !this.accessToken.isEmpty());

        scheduler.scheduleAtFixedRate(this::safePoll, 500L, 500L, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        synchronized (lock) {
            stopCallbackServer();
            scheduler.shutdownNow();
        }
    }

    public SpotifySnapshot getSnapshot() {
        return snapshot;
    }

    public void setClientId(String clientId) {
        synchronized (lock) {
            String nextClientId = normalizeClientId(clientId);
            if (nextClientId.isBlank()) {
                nextClientId = BUNDLED_CLIENT_ID;
            }
            if (nextClientId.equals(this.clientId)) {
                return;
            }
            this.clientId = nextClientId;
            accessToken = "";
            refreshToken = "";
            expiresAtMs = 0L;
            pendingState = "";
            pendingVerifier = "";
            stopCallbackServer();
            persistAuth();
        }
        setStatus("Spotify disconnected", false, false);
    }

    public String getClientId() {
        synchronized (lock) {
            return clientId;
        }
    }

    public void setRefreshIntervalMs(int refreshIntervalMs) {
        synchronized (lock) {
            this.refreshIntervalMs = clampRefreshInterval(refreshIntervalMs);
            snapshot = new SpotifySnapshot(
                    snapshot.authenticated(),
                    snapshot.connecting(),
                    snapshot.status(),
                    snapshot.track(),
                    snapshot.devices(),
                    snapshot.activeDeviceId(),
                    this.refreshIntervalMs,
                    System.currentTimeMillis()
            );
        }
    }

    public int getRefreshIntervalMs() {
        synchronized (lock) {
            return refreshIntervalMs;
        }
    }

    public void beginLogin() {
        String localClientId;
        synchronized (lock) {
            localClientId = clientId;
        }

        if (localClientId.isBlank()) {
            setStatus("Set Spotify Client ID first", false, false);
            return;
        }

        if (!isLikelyClientId(localClientId)) {
            setStatus("Invalid Client ID format", false, false);
            return;
        }

        try {
            String stateToken = randomUrlSafe(24);
            String verifier = randomUrlSafe(64);
            String challenge = sha256Base64Url(verifier);

            synchronized (lock) {
                pendingState = stateToken;
                pendingVerifier = verifier;
                startCallbackServer();
            }

            HttpUrl url = HttpUrl.parse(AUTHORIZE_URL).newBuilder()
                    .addQueryParameter("response_type", "code")
                    .addQueryParameter("client_id", localClientId)
                    .addQueryParameter("scope", SCOPES)
                    .addQueryParameter("redirect_uri", REDIRECT_URI)
                    .addQueryParameter("state", stateToken)
                    .addQueryParameter("code_challenge_method", "S256")
                    .addQueryParameter("code_challenge", challenge)
                    .build();

            String loginUrl = url.toString();
            if (openBrowser(loginUrl)) {
                setStatus("Complete Spotify login in browser", true, false);
            } else {
                setStatus("Browser open failed. Copy URL from log.", false, false);
                LOGGER.info("Spotify login URL: {}", loginUrl);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to start Spotify login", e);
            setStatus("Failed to start login", false, false);
        }
    }

    public void logout() {
        synchronized (lock) {
            accessToken = "";
            refreshToken = "";
            expiresAtMs = 0L;
            pendingState = "";
            pendingVerifier = "";
            stopCallbackServer();
            persistAuth();
        }
        albumArtCache.clear();
        setStatus("Spotify disconnected", false, false);
    }

    public byte[] getAlbumArtBytes(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return albumArtCache.get(imageUrl);
    }

    public void togglePlayPause() {
        scheduler.execute(() -> {
            SpotifyTrack track = snapshot.track();
            boolean currentlyPlaying = track != null && track.playing();
            if (currentlyPlaying) {
                postNoBody("/me/player/pause");
            } else {
                putNoBody("/me/player/play");
            }
            pollNowPlaying();
        });
    }

    public void nextTrack() {
        scheduler.execute(() -> {
            postNoBody("/me/player/next");
            pollNowPlaying();
        });
    }

    public void previousTrack() {
        scheduler.execute(() -> {
            postNoBody("/me/player/previous");
            pollNowPlaying();
        });
    }

    public void setVolume(int volumePercent) {
        int clamped = Math.max(0, Math.min(100, volumePercent));
        scheduler.execute(() -> {
            requestWithBody("PUT", "/me/player/volume?volume_percent=" + clamped, null);
            pollDevices();
        });
    }

    public void transferPlayback(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }

        scheduler.execute(() -> {
            JsonObject root = new JsonObject();
            JsonArray ids = new JsonArray();
            ids.add(deviceId);
            root.add("device_ids", ids);
            root.addProperty("play", true);
            requestWithBody("PUT", "/me/player", RequestBody.create(root.toString(), MediaType.parse("application/json")));
            pollNowPlaying();
            pollDevices();
        });
    }

    public void searchTracks(String query) {
        String safeQuery = safe(query);
        scheduler.execute(() -> runTrackSearch(safeQuery));
    }

    public List<SpotifySearchTrack> getSearchResults() {
        return searchResults;
    }

    public String getLastSearchQuery() {
        return lastSearchQuery;
    }

    public void loadUserPlaylists() {
        scheduler.execute(this::pollPlaylists);
    }

    public List<SpotifyPlaylist> getPlaylists() {
        return playlists;
    }

    public String getPlaylistsStatus() {
        return playlistsStatus;
    }

    public void playTrack(String trackUri) {
        if (trackUri == null || trackUri.isBlank()) {
            return;
        }

        scheduler.execute(() -> {
            JsonObject root = new JsonObject();
            JsonArray uris = new JsonArray();
            uris.add(trackUri);
            root.add("uris", uris);
            requestWithBody("PUT", "/me/player/play", RequestBody.create(root.toString(), MediaType.parse("application/json")));
            pollNowPlaying();
        });
    }

    public void playPlaylist(String playlistUri) {
        if (playlistUri == null || playlistUri.isBlank()) {
            return;
        }

        scheduler.execute(() -> {
            JsonObject root = new JsonObject();
            root.addProperty("context_uri", playlistUri);
            requestWithBody("PUT", "/me/player/play", RequestBody.create(root.toString(), MediaType.parse("application/json")));
            pollNowPlaying();
        });
    }

    private void safePoll() {
        try {
            pollTick();
        } catch (Exception e) {
            LOGGER.debug("Spotify poll failed", e);
            setStatus("Spotify temporary error", false, !accessToken.isBlank());
        }
    }

    private void pollTick() {
        long now = System.currentTimeMillis();
        if (now < nextAllowedPollMs) {
            return;
        }

        if (accessToken.isBlank() && refreshToken.isBlank()) {
            return;
        }

        if (!ensureValidAccessToken()) {
            return;
        }

        int localInterval;
        synchronized (lock) {
            localInterval = refreshIntervalMs;
        }
        if (now - snapshot.lastUpdatedMs() < localInterval) {
            return;
        }

        pollNowPlaying();

        devicesRefreshCounter--;
        if (devicesRefreshCounter <= 0) {
            pollDevices();
            devicesRefreshCounter = 6;
        }
    }

    private void pollNowPlaying() {
        if (!ensureValidAccessToken()) {
            return;
        }

        Request request = authedRequestBuilder(API_BASE + "/me/player/currently-playing").get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 204) {
                snapshot = new SpotifySnapshot(true, false, "Connected (no active playback)", null, snapshot.devices(), snapshot.activeDeviceId(), getRefreshIntervalMs(), System.currentTimeMillis());
                return;
            }

            if (response.code() == 401) {
                if (refreshAccessToken()) {
                    pollNowPlaying();
                }
                return;
            }

            if (response.code() == 429) {
                handleRateLimit(response);
                return;
            }

            if (!response.isSuccessful() || response.body() == null) {
                setStatus("Spotify API error " + response.code(), false, true);
                return;
            }

            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            SpotifyTrack track = parseTrack(json);
            String activeDeviceId = parseActiveDeviceId(json);

            if (track != null && track.albumArtUrl() != null && !track.albumArtUrl().isBlank()) {
                cacheAlbumArtIfNeeded(track.albumArtUrl());
            }

            snapshot = new SpotifySnapshot(
                    true,
                    false,
                    "Connected",
                    track,
                    snapshot.devices(),
                    activeDeviceId,
                    getRefreshIntervalMs(),
                    System.currentTimeMillis()
            );
        } catch (Exception e) {
            LOGGER.debug("pollNowPlaying error", e);
        }
    }

    private void pollDevices() {
        if (!ensureValidAccessToken()) {
            return;
        }

        Request request = authedRequestBuilder(API_BASE + "/me/player/devices").get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 401) {
                refreshAccessToken();
                return;
            }
            if (response.code() == 429) {
                handleRateLimit(response);
                return;
            }
            if (!response.isSuccessful() || response.body() == null) {
                return;
            }

            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            List<SpotifyDevice> devices = parseDevices(json);
            String activeId = snapshot.activeDeviceId();
            for (SpotifyDevice device : devices) {
                if (device.active()) {
                    activeId = device.id();
                    break;
                }
            }

            snapshot = new SpotifySnapshot(
                    snapshot.authenticated(),
                    snapshot.connecting(),
                    snapshot.status(),
                    snapshot.track(),
                    List.copyOf(devices),
                    activeId,
                    getRefreshIntervalMs(),
                    System.currentTimeMillis()
            );
        } catch (Exception e) {
            LOGGER.debug("pollDevices error", e);
        }
    }

    private Request.Builder authedRequestBuilder(String url) {
        String token;
        synchronized (lock) {
            token = accessToken;
        }
        return new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token);
    }

    private void postNoBody(String path) {
        requestWithBody("POST", path, RequestBody.create(new byte[0], null));
    }

    private void putNoBody(String path) {
        requestWithBody("PUT", path, RequestBody.create(new byte[0], null));
    }

    private void requestWithBody(String method, String path, RequestBody body) {
        if (!ensureValidAccessToken()) {
            return;
        }

        String url = API_BASE + path;
        Request.Builder builder = authedRequestBuilder(url);
        if (body == null) {
            body = RequestBody.create(new byte[0], null);
        }

        switch (method) {
            case "POST" -> builder.post(body);
            case "PUT" -> builder.put(body);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (response.code() == 401) {
                refreshAccessToken();
            } else if (response.code() == 429) {
                handleRateLimit(response);
            } else if (!response.isSuccessful()) {
                setStatus("Spotify action failed " + response.code(), false, true);
            }
        } catch (Exception e) {
            LOGGER.debug("Spotify action failed", e);
        }
    }

    private boolean ensureValidAccessToken() {
        synchronized (lock) {
            if (accessToken.isBlank() && refreshToken.isBlank()) {
                return false;
            }
            if (!accessToken.isBlank() && System.currentTimeMillis() + 15_000L < expiresAtMs) {
                return true;
            }
        }
        return refreshAccessToken();
    }

    private boolean refreshAccessToken() {
        String localRefreshToken;
        String localClientId;
        synchronized (lock) {
            localRefreshToken = refreshToken;
            localClientId = clientId;
        }

        if (localRefreshToken.isBlank() || localClientId.isBlank()) {
            setStatus("Missing refresh token or client id", false, false);
            return false;
        }

        FormBody form = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", localRefreshToken)
                .add("client_id", localClientId)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(form)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                setStatus("Token refresh failed (" + response.code() + ")", false, false);
                return false;
            }

            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            String nextAccessToken = getString(root, "access_token");
            String nextRefreshToken = getString(root, "refresh_token");
            int expiresIn = getInt(root, "expires_in", 3600);

            synchronized (lock) {
                accessToken = safe(nextAccessToken);
                if (!nextRefreshToken.isBlank()) {
                    refreshToken = nextRefreshToken;
                }
                expiresAtMs = System.currentTimeMillis() + (expiresIn * 1000L);
                persistAuth();
            }

            setStatus("Connected", false, true);
            return true;
        } catch (Exception e) {
            LOGGER.debug("refreshAccessToken error", e);
            setStatus("Token refresh failed", false, false);
            return false;
        }
    }

    private void startCallbackServer() throws IOException {
        stopCallbackServer();
        callbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 8888), 0);
        callbackServer.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            String code = queryParam(query, "code");
            String stateToken = queryParam(query, "state");

            String localPendingState;
            String localVerifier;
            synchronized (lock) {
                localPendingState = pendingState;
                localVerifier = pendingVerifier;
            }

            String html;
            if (code.isBlank() || stateToken.isBlank() || !stateToken.equals(localPendingState)) {
                html = "<html><body><h2>Spotify login failed.</h2><p>You can close this page.</p></body></html>";
                setStatus("Spotify login rejected", false, false);
            } else {
                html = "<html><body><h2>Spotify login successful.</h2><p>You can return to Minecraft.</p></body></html>";
                scheduler.execute(() -> exchangeAuthCode(code, localVerifier));
            }

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

            scheduler.schedule(this::stopCallbackServer, 1L, TimeUnit.SECONDS);
        });
        callbackServer.start();
    }

    private void stopCallbackServer() {
        if (callbackServer != null) {
            callbackServer.stop(0);
            callbackServer = null;
        }
    }

    private void exchangeAuthCode(String code, String verifier) {
        String localClientId;
        synchronized (lock) {
            localClientId = clientId;
        }

        FormBody form = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("client_id", localClientId)
                .add("code_verifier", verifier)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(form)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                setStatus("Spotify token exchange failed (" + response.code() + ")", false, false);
                return;
            }

            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            String nextAccessToken = getString(root, "access_token");
            String nextRefreshToken = getString(root, "refresh_token");
            int expiresIn = getInt(root, "expires_in", 3600);

            synchronized (lock) {
                accessToken = safe(nextAccessToken);
                refreshToken = safe(nextRefreshToken);
                expiresAtMs = System.currentTimeMillis() + (expiresIn * 1000L);
                pendingState = "";
                pendingVerifier = "";
                persistAuth();
            }

            setStatus("Connected", false, true);
            pollNowPlaying();
            pollDevices();
            pollPlaylists();
        } catch (Exception e) {
            LOGGER.error("Spotify token exchange failed", e);
            setStatus("Spotify login failed", false, false);
        }
    }

    private void runTrackSearch(String query) {
        lastSearchQuery = query;
        if (query.isBlank()) {
            searchResults = List.of();
            return;
        }
        if (!ensureValidAccessToken()) {
            return;
        }

        HttpUrl url = HttpUrl.parse(API_BASE + "/search").newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("type", "track")
                .addQueryParameter("limit", "30")
                .build();

        Request request = authedRequestBuilder(url.toString()).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 401) {
                if (refreshAccessToken()) {
                    runTrackSearch(query);
                }
                return;
            }
            if (response.code() == 429) {
                handleRateLimit(response);
                return;
            }
            if (!response.isSuccessful() || response.body() == null) {
                setStatus("Search failed " + response.code(), false, true);
                return;
            }

            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            searchResults = parseSearchTracks(root);
        } catch (Exception e) {
            LOGGER.debug("Spotify search failed", e);
        }
    }

    private List<SpotifySearchTrack> parseSearchTracks(JsonObject root) {
        if (!root.has("tracks") || !root.get("tracks").isJsonObject()) {
            return List.of();
        }
        JsonObject tracks = root.getAsJsonObject("tracks");
        if (!tracks.has("items") || !tracks.get("items").isJsonArray()) {
            return List.of();
        }

        List<SpotifySearchTrack> out = new ArrayList<>();
        for (JsonElement el : tracks.getAsJsonArray("items")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject item = el.getAsJsonObject();
            String title = getString(item, "name");
            String uri = getString(item, "uri");
            int durationMs = getInt(item, "duration_ms", 0);

            String artists = "";
            if (item.has("artists") && item.get("artists").isJsonArray()) {
                List<String> names = new ArrayList<>();
                for (JsonElement artistElement : item.getAsJsonArray("artists")) {
                    if (artistElement.isJsonObject()) {
                        names.add(getString(artistElement.getAsJsonObject(), "name"));
                    }
                }
                artists = String.join(", ", names);
            }

            String album = "";
            String albumArtUrl = "";
            if (item.has("album") && item.get("album").isJsonObject()) {
                JsonObject albumObj = item.getAsJsonObject("album");
                album = getString(albumObj, "name");
                if (albumObj.has("images") && albumObj.get("images").isJsonArray()) {
                    JsonArray images = albumObj.getAsJsonArray("images");
                    if (!images.isEmpty() && images.get(0).isJsonObject()) {
                        albumArtUrl = getString(images.get(0).getAsJsonObject(), "url");
                    }
                }
            }

            if (!albumArtUrl.isBlank()) {
                cacheAlbumArtIfNeeded(albumArtUrl);
            }
            out.add(new SpotifySearchTrack(title, artists, album, uri, albumArtUrl, durationMs));
        }
        return List.copyOf(out);
    }

    private void pollPlaylists() {
        if (!ensureValidAccessToken()) {
            playlistsStatus = "Not connected";
            return;
        }

        HttpUrl url = HttpUrl.parse(API_BASE + "/me/playlists").newBuilder()
                .addQueryParameter("limit", "30")
                .build();
        Request request = authedRequestBuilder(url.toString()).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 401) {
                if (refreshAccessToken()) {
                    pollPlaylists();
                }
                return;
            }
            if (response.code() == 429) {
                handleRateLimit(response);
                return;
            }
            if (!response.isSuccessful() || response.body() == null) {
                playlistsStatus = "Library failed " + response.code();
                return;
            }

            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            playlists = parsePlaylists(root);
            playlistsStatus = playlists.isEmpty() ? "No playlists found" : "Library loaded";
        } catch (Exception e) {
            LOGGER.debug("pollPlaylists error", e);
            playlistsStatus = "Library error";
        }
    }

    private List<SpotifyPlaylist> parsePlaylists(JsonObject root) {
        if (!root.has("items") || !root.get("items").isJsonArray()) {
            return List.of();
        }

        List<SpotifyPlaylist> out = new ArrayList<>();
        for (JsonElement el : root.getAsJsonArray("items")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject item = el.getAsJsonObject();
            String name = getString(item, "name");
            String uri = getString(item, "uri");
            int tracksCount = 0;
            if (item.has("tracks") && item.get("tracks").isJsonObject()) {
                tracksCount = getInt(item.getAsJsonObject("tracks"), "total", 0);
            }
            String owner = "";
            if (item.has("owner") && item.get("owner").isJsonObject()) {
                owner = getString(item.getAsJsonObject("owner"), "display_name");
            }
            out.add(new SpotifyPlaylist(name, owner, uri, tracksCount));
        }
        return List.copyOf(out);
    }

    private void handleRateLimit(Response response) {
        String retryAfter = response.header("Retry-After", "2");
        int waitSeconds;
        try {
            waitSeconds = Integer.parseInt(retryAfter);
        } catch (NumberFormatException ignored) {
            waitSeconds = 2;
        }
        nextAllowedPollMs = System.currentTimeMillis() + (waitSeconds * 1000L);
        setStatus("Rate limited. Waiting " + waitSeconds + "s", false, true);
    }

    private void persistAuth() {
        SpotifyAuthStore.AuthData data = new SpotifyAuthStore.AuthData();
        data.clientId = safe(clientId);
        data.accessToken = safe(accessToken);
        data.refreshToken = safe(refreshToken);
        data.expiresAtMs = expiresAtMs;
        authStore.save(data);
    }

    private SpotifyTrack parseTrack(JsonObject root) {
        JsonObject item = root.has("item") && root.get("item").isJsonObject() ? root.getAsJsonObject("item") : null;
        if (item == null) {
            return null;
        }

        String title = getString(item, "name");
        String album = "";
        String albumArtUrl = "";
        if (item.has("album") && item.get("album").isJsonObject()) {
            JsonObject albumJson = item.getAsJsonObject("album");
            album = getString(albumJson, "name");
            if (albumJson.has("images") && albumJson.get("images").isJsonArray()) {
                JsonArray images = albumJson.getAsJsonArray("images");
                if (!images.isEmpty() && images.get(0).isJsonObject()) {
                    albumArtUrl = getString(images.get(0).getAsJsonObject(), "url");
                }
            }
        }

        String artists = "";
        if (item.has("artists") && item.get("artists").isJsonArray()) {
            List<String> names = new ArrayList<>();
            for (JsonElement artistElement : item.getAsJsonArray("artists")) {
                if (artistElement.isJsonObject()) {
                    names.add(getString(artistElement.getAsJsonObject(), "name"));
                }
            }
            artists = String.join(", ", names);
        }

        int durationMs = getInt(item, "duration_ms", 0);
        int progressMs = getInt(root, "progress_ms", 0);
        boolean playing = getBoolean(root, "is_playing", false);

        return new SpotifyTrack(title, artists, album, albumArtUrl, durationMs, progressMs, playing);
    }

    private void cacheAlbumArtIfNeeded(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || albumArtCache.containsKey(imageUrl)) {
            return;
        }

        Request request = new Request.Builder().url(imageUrl).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return;
            }
            byte[] data = response.body().bytes();
            if (data.length > 0) {
                albumArtCache.put(imageUrl, data);
            }
        } catch (Exception ignored) {
        }
    }

    private String parseActiveDeviceId(JsonObject root) {
        if (!root.has("device") || !root.get("device").isJsonObject()) {
            return "";
        }
        return getString(root.getAsJsonObject("device"), "id");
    }

    private List<SpotifyDevice> parseDevices(JsonObject root) {
        if (!root.has("devices") || !root.get("devices").isJsonArray()) {
            return List.of();
        }

        List<SpotifyDevice> out = new ArrayList<>();
        for (JsonElement el : root.getAsJsonArray("devices")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject d = el.getAsJsonObject();
            out.add(new SpotifyDevice(
                    getString(d, "id"),
                    getString(d, "name"),
                    getString(d, "type"),
                    getBoolean(d, "is_active", false),
                    getInt(d, "volume_percent", 0),
                    getBoolean(d, "is_restricted", false)
            ));
        }
        return out;
    }

    private void setStatus(String status, boolean connecting, boolean authenticated) {
        snapshot = new SpotifySnapshot(
                authenticated,
                connecting,
                status,
                snapshot.track(),
                snapshot.devices(),
                snapshot.activeDeviceId(),
                getRefreshIntervalMs(),
                System.currentTimeMillis()
        );
    }

    private String queryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return "";
        }

        for (String part : query.split("&")) {
            int idx = part.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String partKey = urlDecode(part.substring(0, idx));
            if (!partKey.equals(key)) {
                continue;
            }
            return urlDecode(part.substring(idx + 1));
        }
        return "";
    }

    private String urlDecode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String randomUrlSafe(int lengthBytes) {
        byte[] bytes = new byte[lengthBytes];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Base64Url(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private boolean openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return true;
            }
        } catch (Exception ignored) {
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                return true;
            }
            if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
                return true;
            }
            new ProcessBuilder("xdg-open", url).start();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeClientId(String raw) {
        String value = safe(raw).replace("\r", "").replace("\n", "");
        if (value.isEmpty()) {
            return "";
        }

        // Allow pasting a full authorize URL and extract client_id automatically.
        int idx = value.indexOf("client_id=");
        if (idx >= 0) {
            String part = value.substring(idx + "client_id=".length());
            int amp = part.indexOf('&');
            if (amp >= 0) {
                part = part.substring(0, amp);
            }
            value = urlDecode(part);
        }

        value = value.replace("\"", "").replace("'", "").trim();
        if (value.contains(" ")) {
            value = value.replace(" ", "");
        }
        return value;
    }

    private boolean isLikelyClientId(String value) {
        if (value == null) {
            return false;
        }
        if (value.length() < 20 || value.length() > 80) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private int clampRefreshInterval(int value) {
        return Math.max(750, Math.min(5000, value));
    }

    private String getString(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return "";
        }
        JsonElement e = obj.get(key);
        if (e == null || e.isJsonNull()) {
            return "";
        }
        if (e.isJsonPrimitive()) {
            JsonPrimitive primitive = e.getAsJsonPrimitive();
            if (primitive.isString()) {
                return primitive.getAsString();
            }
            if (primitive.isNumber() || primitive.isBoolean()) {
                return primitive.getAsString();
            }
        }
        return "";
    }

    private int getInt(JsonObject obj, String key, int fallback) {
        if (!obj.has(key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (!obj.has(key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
