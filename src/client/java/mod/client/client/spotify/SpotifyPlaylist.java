package mod.client.client.spotify;

public record SpotifyPlaylist(
        String name,
        String owner,
        String uri,
        int tracksCount
) {
}
