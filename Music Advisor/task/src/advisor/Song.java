package advisor;

import java.net.URI;

public class Song {
    String name;
    URI href;

    public Song(String name, URI href) {
        this.name = name;
        this.href = href;
    }

    @Override
    public String toString() {
        return name + System.lineSeparator() +
                href + System.lineSeparator();
    }
}
