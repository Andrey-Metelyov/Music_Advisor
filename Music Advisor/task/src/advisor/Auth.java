package advisor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Auth {
    private final String authServer;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri = "http://localhost:8080";
    private HttpServer server;
    private String accessToken = "";

    Auth(String authServer, String clientId, String clientSecret) {
        this.authServer = authServer;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    String getAuthUri() {
        String responseType = "code";
        String uri = String.format(
                "https://accounts.spotify.com/authorize?client_id=%s&redirect_uri=%s&response_type=%s",
                clientId,
                redirectUri,
                responseType);
        return uri;
    }

    boolean start() {
        try {
            server = HttpServer.create();
            server.bind(new InetSocketAddress(8080), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        server.createContext("/",
                exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    System.err.println("HttpServerThread: " + query);
                    String answer;
                    if (query != null && query.startsWith("code=")) {
                        answer = "Got the code. Return back to your program.";
                        String code = query.substring("code=".length());
                        requestToken(code);
                    } else {
                        answer = "Authorization code not found. Try again.";
                    }
//                        if (httpServerResponse[0].isEmpty()) {
//                            if (query == null || query.isEmpty() || query.startsWith("error=")) {
//                                answer = "Authorization code not found. Try again.";
//                            } else {
//                                answer = "Got the code. Return back to your program.";
//                                httpServerResponse[0] = query;
//                            }
//                        } else {
//                            answer = httpServerResponse[0];
//                        }
                    exchange.sendResponseHeaders(200, answer.length());
                    exchange.getResponseBody().write(answer.getBytes());
                    exchange.getResponseBody().close();
                });
        server.start();
        return true;
    }

    private void requestToken(String code) {
        System.out.println("making http request for access_token...");
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(authServer + "/api/token"))
                .POST(HttpRequest.BodyPublishers.ofString(
                        String.format("client_id=%s&client_secret=%s&grant_type=authorization_code&code=%s&redirect_uri=%s",
                                clientId,
                                clientSecret,
                                code,
                                redirectUri)))
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return;
        }
        System.err.println("2. response.body() = '" + response.body() + "'");
        System.out.println("response:");
        System.out.println(response.body());
        JsonObject jo = JsonParser.parseString(response.body()).getAsJsonObject();
        accessToken = jo.get("access_token").getAsString();
        server.stop(1);
    }

    boolean isAuth() {
        return !accessToken.isEmpty();
    }

    String getAccessToken() {
        return accessToken;
    }
}
