package mod.client.client.spotify;

public record SpotifySearchTrack(
        String title,
        String artists,
        String album,
        String uri,
        String albumArtUrl,
        int durationMs
) {
}
