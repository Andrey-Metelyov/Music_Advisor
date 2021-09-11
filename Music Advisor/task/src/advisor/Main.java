package advisor;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    static boolean isAuth = false;
    static String code = "";
    static String spotifyServer = "https://accounts.spotify.com";

    public static void main(String[] args) {
        System.err.print("Arguments: ");
        System.err.println(Arrays.toString(args));

        if (args.length > 1) {
            if (args[0].equals("-access")) {
                spotifyServer = args[1];
            }
        }

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
            HttpClient client = HttpClient.newBuilder().build();
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
                    .uri(URI.create(spotifyServer + "/api/token"))
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
        List<String> categories = List.of(
                "Top Lists",
                "Pop",
                "Mood",
                "Latin"
        );
        System.out.println("---CATEGORIES---");
        categories.forEach(System.out::println);
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
