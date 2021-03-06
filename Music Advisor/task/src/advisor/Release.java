package advisor;

import java.net.URI;
import java.util.List;

public class Release {
    String name;
    List<String> performers;
    URI href;

    public Release(String name, List<String> performers, URI href) {
        this.name = name;
        this.performers = performers;
        this.href = href;
    }

    @Override
    public String toString() {
        return name + System.lineSeparator() +
                performers + System.lineSeparator() +
                href + System.lineSeparator();
    }
}
