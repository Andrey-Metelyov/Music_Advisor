package advisor;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
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
            String[] command = scanner.nextLine().split(" ");
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
                    playlist(command[1]);
                    break;
                case "exit":
                    System.out.println("---GOODBYE!---");
                    return;
            }
        }
    }

    private static void auth(String clientId, String clientSecret) {
        String redirectUri = "http://localhost:8080";
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
                    exchange -> {
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

        isAuth = true;
        System.out.println("---SUCCESS---");
    }

    private static void playlist(String playlist) {
        if (!isAuth) {
            System.out.println("Please, provide access for application.");
            return;
        }
        List<String> list = List.of(
                "Walk Like A Badass",
                "Rage Beats",
                "Arab Mood Booster",
                "Sunday Stroll"
        );
        System.out.printf("---%s PLAYLISTS---", playlist.toUpperCase());
        list.forEach(System.out::println);
    }

    private static void featured() {
        if (!isAuth) {
            System.out.println("Please, provide access for application.");
            return;
        }
        List<String> songs = List.of(
                "Mellow Morning",
                "Wake Up and Smell the Coffee",
                "Monday Motivation",
                "Songs to Sing in the Shower"
        );
        System.out.println("---FEATURED---");
        songs.forEach(System.out::println);
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
        List<Release> releases = List.of(
                new Release("Mountains", List.of("Sia", "Diplo", "Labrinth")),
                new Release("Runaway", List.of("Lil Peep")),
                new Release("The Greatest Show", List.of("Panic! At The Disco")),
                new Release("All Out Life", List.of("Slipknot"))
        );
        System.out.println("---NEW RELEASES---");
        releases.forEach(release -> System.out.printf("%s %s\n", release.name, release.performers));
    }
}
