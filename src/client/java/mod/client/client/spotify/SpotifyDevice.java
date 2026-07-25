package mod.client.client.spotify;

public record SpotifyDevice(
        String id,
        String name,
        String type,
        boolean active,
        int volumePercent,
        boolean restricted
) {
}
