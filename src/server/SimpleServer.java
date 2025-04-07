package server;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import controller.AuthController;
import controller.AuthControllerImpl;
import repository.ClientsDBImpl;

public class SimpleServer {

    private final AuthController authController;

    public SimpleServer(AuthController authController) {
        this.authController = authController;
    }

    public static void main(String[] args) throws IOException {
        AuthController authController = new AuthControllerImpl(new ClientsDBImpl());

        int port = 8080;
        SimpleServer server = new SimpleServer(authController);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/login", server::handleLogin);
        httpServer.setExecutor(null);
        httpServer.start();
        System.out.println("Сервер запущен на порту " + port);
    }

    void handleLogin(HttpExchange exchange) throws IOException {

        if ("POST".equals(exchange.getRequestMethod())) {
            try (InputStream requestBody = exchange.getRequestBody();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8))) {

                String requestBodyString = reader.lines().collect(Collectors.joining("\n"));

                String accountNumber = null;
                String pinCode = null;
                String[] params = requestBodyString.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        if (keyValue[0].equals("cardNumber")) {
                            accountNumber = keyValue[1];
                        } else if (keyValue[0].equals("pinCode")) {
                            pinCode = keyValue[1];
                        }
                    }
                }
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

                String response = "{\"success\": false}";

                if (accountNumber != null && pinCode != null) {
                    if(authController.login(accountNumber, pinCode)){
                        response = "{\"success\": true}";

                    }
                }
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                System.out.println(response);


            } catch (Exception e) {

                String errorMessage = "{\"success\": false}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorMessage.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorMessage.getBytes(StandardCharsets.UTF_8));
                }
                e.printStackTrace();
            }

        } else {
            String response = "Только POST запросы разрешены для /login";
            exchange.sendResponseHeaders(405, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}