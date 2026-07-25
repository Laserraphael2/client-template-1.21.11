package mod.client.client.spotify;

public record SpotifyTrack(
        String title,
        String artists,
        String album,
        String albumArtUrl,
        int durationMs,
        int progressMs,
        boolean playing
) {
}
