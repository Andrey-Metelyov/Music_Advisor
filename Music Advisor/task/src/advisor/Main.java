package advisor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Scanner;

public class Main {
    static boolean isAuth = false;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String[] command = scanner.nextLine().split(" ");
            switch (command[0]) {
                case "auth":
                    auth("5c9c1eb78ad14532a4d238f4d579a8d3");
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

    private static void auth(String clientId) {
        String redirectUri = "http://localhost:8080&response_type=code";
        String uri = String.format("https://accounts.spotify.com/authorize?client_id=%s&redirect_uri=%s",
                clientId,
                redirectUri);

        class HttpServerThread extends Thread {
            HttpServer server;
            String code = "";

            @Override
            public void run() {
                super.run();
                System.err.println("Starting http server");
                try {
                    server = HttpServer.create();
                    server.bind(new InetSocketAddress(8080), 0);
                    server.createContext("/",
                            exchange -> {
                                String query = exchange.getRequestURI().getQuery();
                                System.err.println("HttpServerThread: " + query);
                                if (query == null) {
                                    exchange.sendResponseHeaders(200, code.length());
                                    exchange.getResponseBody().write(code.getBytes());
                                    exchange.getResponseBody().close();
                                } else if (query.startsWith("code=")) {
                                    code = query.split("=")[1];
                                    exchange.sendResponseHeaders(200, code.length());
                                    exchange.getResponseBody().write(code.getBytes());
                                    exchange.getResponseBody().close();
                                }
                            }
                    );
                    server.start();
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }

            @Override
            public void interrupt() {
                super.interrupt();
                System.err.println("HttpServerThread: interrupt");
                server.stop(1);
            }
        }
        HttpServerThread httpServer = new HttpServerThread();
        httpServer.start();

        System.out.println("use this link to request the access code:");
        System.out.println(uri);

        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080"))
                .GET()
                .build();
        HttpResponse<String> response = null;
        try {
            System.out.println("waiting for code...");
            while (response == null || response.body().isEmpty()) {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.err.println("response.body() = '" + response.body() + "'");
                Thread.sleep(1000);
            }
            String code = response.body();
            System.err.printf("'%s'\n", code);
            System.out.println("code received");
            httpServer.interrupt();
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
