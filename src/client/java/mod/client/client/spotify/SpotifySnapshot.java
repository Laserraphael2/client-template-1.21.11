package mod.client.client.spotify;

import java.util.List;

public record SpotifySnapshot(
        boolean authenticated,
        boolean connecting,
        String status,
        SpotifyTrack track,
        List<SpotifyDevice> devices,
        String activeDeviceId,
        int refreshIntervalMs,
        long lastUpdatedMs
) {
    public static SpotifySnapshot disconnected(String status, int refreshIntervalMs) {
        return new SpotifySnapshot(false, false, status, null, List.of(), "", refreshIntervalMs, System.currentTimeMillis());
    }

    public static SpotifySnapshot connecting(String status, int refreshIntervalMs) {
        return new SpotifySnapshot(false, true, status, null, List.of(), "", refreshIntervalMs, System.currentTimeMillis());
    }

    public SpotifySnapshot withStatus(String nextStatus) {
        return new SpotifySnapshot(authenticated, connecting, nextStatus, track, devices, activeDeviceId, refreshIntervalMs, System.currentTimeMillis());
    }
}
