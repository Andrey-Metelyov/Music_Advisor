package advisor;

import java.net.URI;

public class Playlist {
    String id;
    String description;
    URI href;

    public Playlist(String id, String description, URI href) {
        this.id = id;
        this.description = description;
        this.href = href;
    }

    @Override
    public String toString() {
        return description + System.lineSeparator() +
                href + System.lineSeparator();
    }
}
