package advisor;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String[] command = scanner.nextLine().split(" ");
            switch (command[0]) {
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

    private static void playlist(String playlist) {
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
        List<Release> releases = List.of(
                new Release("Mountains", List.of("Sia", "Diplo", "Labrinth")),
                new Release("Runaway", List.of("Lil Peep")),
                new Release("The Greatest Show", List.of("Panic! At The Disco")),
                new Release("All Out Life", List.of("Slipknot"))
        );
        System.out.println("---NEW RELEASES---");
        releases.forEach(release -> {
            System.out.printf("%s %s\n", release.name, release.performers);
        });
    }
}
