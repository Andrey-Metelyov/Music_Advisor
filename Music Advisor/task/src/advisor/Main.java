package advisor;

import com.google.gson.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class Main {
    static boolean isAuth = false;
    static String code = "";
    static String accessToken = "";
    static String spotifyAuthServer;
    static String spotifyResourceServer;
    static Map<String, String> config = new HashMap<>();
    static HttpClient client = HttpClient.newBuilder().build();

    public static void main(String[] args) {
        System.err.print("Arguments: ");
        System.err.println(Arrays.toString(args));

        for (int i = 0; i < args.length; i += 2) {
            config.put(args[i], args[i + 1]);
        }

        spotifyAuthServer = config.getOrDefault("-access", "https://accounts.spotify.com");
        spotifyResourceServer = config.getOrDefault("-resource", "https://api.spotify.com");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String[] command = scanner.nextLine().split(" ", 2);
            switch (command[0]) {
                case "auth":
                    auth("5c9c1eb78ad14532a4d238f4d579a8d3", "a4235f104ffb4e9ab9ce8ccb72cb80aa");
                    break;
                case "featured": // a list of Spotify-featured playlists with their links fetched from API;
                    featured();
                    break;
                case "new": // a list of new albums with artists and links on Spotify;
                    newReleases();
                    break;
                case "categories": // a list of all available categories on Spotify (just their names);
                    categories();
                    break;
                case "playlists": // C_NAME, where C_NAME is the name of category. The list contains playlists of this category and their links on Spotify;
                    if (command.length > 1) {
                        playlists(command[1]);
                    }
                    break;
                case "exit":
                    System.out.println("---GOODBYE!---");
                    return;
            }
        }
    }

    private static void auth(String clientId, String clientSecret) {
        Auth auth = new Auth(spotifyAuthServer, clientId, clientSecret);
        System.out.println("use this link to request the access code:");
        System.out.println(auth.getAuthUri());
        if (!auth.start()) {
            return;
        }
        System.err.println("waiting auth");
        while (!auth.isAuth()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        accessToken = auth.getAccessToken();
/*        String redirectUri = "http://localhost:8080";
        String responseType = "code";
        String uri = String.format(
                "https://accounts.spotify.com/authorize?client_id=%s&redirect_uri=%s&response_type=%s",
                clientId,
                redirectUri,
                responseType);

        HttpServer server;
        final String[] httpServerResponse = {""};
        try {
            server = HttpServer.create();
            server.bind(new InetSocketAddress(8080), 0);
            server.createContext("/",
                    new HttpHandler() {
                        @Override
                        public void handle(HttpExchange exchange) throws IOException {
                            String query = exchange.getRequestURI().getQuery();
                            System.err.println("HttpServerThread: " + query);
                            String answer;
                            if (httpServerResponse[0].isEmpty()) {
                                if (query == null || query.isEmpty() || query.startsWith("error=")) {
                                    answer = "Authorization code not found. Try again.";
                                } else {
                                    answer = "Got the code. Return back to your program.";
                                    httpServerResponse[0] = query;
                                }
                            } else {
                                answer = httpServerResponse[0];
                            }
                            exchange.sendResponseHeaders(200, answer.length());
                            exchange.getResponseBody().write(answer.getBytes());
                            exchange.getResponseBody().close();
                        }

                        void requestToken(String code) {

                        }
                    }
            );
            server.start();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return;
        }

        System.out.println("use this link to request the access code:");
        System.out.println(uri);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080"))
                    .GET()
                    .build();
            HttpResponse<String> response = null;
            System.out.println("waiting for code...");
            while (response == null || response.body().isEmpty() ||
                    response.body().equals("Authorization code not found. Try again.")) {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.err.println("1. response.body() = '" + response.body() + "'");
                Thread.sleep(1000);
            }
            code = httpServerResponse[0].split("=")[1];
            System.err.printf("'%s'\n", code);
            System.out.println("code received");
            server.stop(1);

            System.out.println("making http request for access_token...");
            request = HttpRequest.newBuilder()
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .uri(URI.create(spotifyAuthServer + "/api/token"))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            String.format("client_id=%s&client_secret=%s&grant_type=authorization_code&code=%s&redirect_uri=%s",
                                    clientId,
                                    clientSecret,
                                    code,
                                    redirectUri)))
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.err.println("2. response.body() = '" + response.body() + "'");
            System.out.println("response:");
            System.out.println(response.body());
            JsonObject jo = JsonParser.parseString(response.body()).getAsJsonObject();
            accessToken = jo.get("access_token").getAsString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
*/
        isAuth = true;
        System.out.println("---SUCCESS---");
    }

    private static void playlists(String category) {
        if (!isAuth) {
            System.out.println("Please, provide access for application.");
            return;
        }

        URI uri = URI.create(spotifyResourceServer + "/v1/browse/categories?limit=50");
        System.err.println("get all categories: " + uri);
        List<Category> categories = new ArrayList<>();
        do {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .uri(uri)
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.err.println(response.body());
                JsonObject jo = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray jsonCategories = jo.getAsJsonObject("categories").getAsJsonArray("items");
                for (JsonElement jsonCategory : jsonCategories) {
                    categories.add(new Category(
                            jsonCategory.getAsJsonObject().get("id").getAsString(),
                            jsonCategory.getAsJsonObject().get("name").getAsString()
                    ));
                }
                JsonElement next = jo.getAsJsonObject("categories").get("next");
                if (next.isJsonPrimitive()) {
                    uri = URI.create(next.getAsString());
                } else {
                    break;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);

        Optional<Category> categoryId = categories.stream()
                .filter(it -> it.name.equals(category))
                .findAny();
        if (categoryId.isPresent()) {
            try {
                List<Playlist> playlists = getPlaylistById(categoryId.get().id);
                playlists.forEach(System.out::println);
            } catch (IllegalArgumentException e)  {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("Unknown category name.");
        }

//        List<String> list = List.of(
//                "Walk Like A Badass",
//                "Rage Beats",
//                "Arab Mood Booster",
//                "Sunday Stroll"
//        );
//        System.out.printf("---%s PLAYLISTS---\n", playlist.toUpperCase());
//        list.forEach(System.out::println);
    }

    private static List<Playlist> getPlaylistById(String id) {
        System.err.println("getPlaylistById(" + id + ")");
        URI uri = URI.create(spotifyResourceServer + "/v1/browse/categories/" + id + "/playlists");
        List<Playlist> playlists = new ArrayList<>();
        do {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .uri(uri)
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.err.println(response.body());
                JsonObject jo = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject joPlaylists = jo.getAsJsonObject("playlists");
                System.err.println("joPlaylists=" + joPlaylists);
                if (joPlaylists == null) {
                    JsonObject joError = jo.getAsJsonObject("error");
                    throw new IllegalArgumentException(joError.getAsJsonPrimitive("message").toString());
                }
                JsonArray jsonPlaylists = joPlaylists.getAsJsonArray("items");
                for (JsonElement jsonPlaylist : jsonPlaylists) {
                    System.err.println(jsonPlaylist);
                    playlists.add(new Playlist(
                            jsonPlaylist.getAsJsonObject().get("id").getAsString(),
                            jsonPlaylist.getAsJsonObject().get("name").getAsString(),
                            URI.create(jsonPlaylist.getAsJsonObject()
                                    .getAsJsonObject("external_urls")
                                    .getAsJsonPrimitive("spotify").getAsString())
                    ));
                }
                JsonElement next = jo.getAsJsonObject("playlists").get("next");
                if (next.isJsonPrimitive()) {
                    uri = URI.create(next.getAsString());
                } else {
                    break;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);
        return playlists;
    }

    private static void featured() {
        if (!isAuth) {
            System.out.println("Please, provide access for application.");
            return;
        }

        URI uri = URI.create(spotifyResourceServer + "/v1/browse/featured-playlists?limit=50");
        List<Playlist> playlists = new ArrayList<>();
        do {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .uri(uri)
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.err.println(response.body());
                JsonObject jo = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray jsonCategories = jo.getAsJsonObject("playlists").getAsJsonArray("items");
                for (JsonElement playlist : jsonCategories) {
                    playlists.add(new Playlist(
                            playlist.getAsJsonObject().get("id").getAsString(),
                            playlist.getAsJsonObject().get("name").getAsString(),
                            URI.create(playlist.getAsJsonObject()
                                    .getAsJsonObject("external_urls")
                                    .getAsJsonPrimitive("spotify").getAsString())
                    ));
                }
                JsonElement next = jo.getAsJsonObject("playlists").get("next");
                if (next.isJsonPrimitive()) {
                    uri = URI.create(next.getAsString());
                } else {
                    break;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);
        playlists.forEach(System.out::println);

//        List<String> songs = List.of(
//                "Mellow Morning",
//                "Wake Up and Smell the Coffee",
//                "Monday Motivation",
//                "Songs to Sing in the Shower"
//        );
//        System.out.println("---FEATURED---");
//        songs.forEach(System.out::println);
    }

    private static void categories() {
        if (!isAuth) {
            System.out.println("Please, provide access for application.");
            return;
        }
        URI uri = URI.create(spotifyResourceServer + "/v1/browse/categories?limit=50");
        List<String> categories = new ArrayList<>();
        do {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .uri(uri)
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.err.println(response.body());
                JsonObject jo = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray jsonCategories = jo.getAsJsonObject("categories").getAsJsonArray("items");
                for (JsonElement category : jsonCategories) {
                    categories.add(category.getAsJsonObject().get("name").getAsString());
                }
                JsonElement next = jo.getAsJsonObject("categories").get("next");
                if (next.isJsonPrimitive()) {
                    uri = URI.create(next.getAsString());
                } else {
                    break;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);
        categories.forEach(System.out::println);
//        System.out.println("---CATEGORIES---");
    }

    private static void newReleases() {
        if (!isAuth) {
            System.out.println("Please, provide access for application.");
            return;
        }

        URI uri = URI.create(spotifyResourceServer + "/v1/browse/new-releases?limit=50");
        List<Release> releases = new ArrayList<>();
        do {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .uri(uri)
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.err.println(response.body());
                JsonObject jo = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray jsonAlbums = jo.getAsJsonObject("albums").getAsJsonArray("items");
                for (JsonElement album : jsonAlbums) {
                    Release release = new Release(
                            album.getAsJsonObject().get("name").getAsString(),
                            new ArrayList<>(),
                            URI.create(album.getAsJsonObject()
                                    .getAsJsonObject("external_urls")
                                    .getAsJsonPrimitive("spotify").getAsString())
                    );
                    JsonArray jsonArtists = album.getAsJsonObject().getAsJsonArray("artists");
                    for (JsonElement artist : jsonArtists) {
                        release.performers.add(artist.getAsJsonObject().get("name").getAsString());
                    }
                    releases.add(release);
                }
                JsonElement next = jo.getAsJsonObject("albums").get("next");
                if (next.isJsonPrimitive()) {
                    uri = URI.create(next.getAsString());
                } else {
                    break;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);
        releases.forEach(System.out::println);


//        List<Release> releases = List.of(
//                new Release("Mountains", List.of("Sia", "Diplo", "Labrinth")),
//                new Release("Runaway", List.of("Lil Peep")),
//                new Release("The Greatest Show", List.of("Panic! At The Disco")),
//                new Release("All Out Life", List.of("Slipknot"))
//        );
//        System.out.println("---NEW RELEASES---");
        System.err.println("releases.size()=" + releases.size());
        releases.forEach(System.out::println);
    }
}
