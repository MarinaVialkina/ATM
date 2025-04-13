package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import controller.AuthController;
import controller.AuthControllerImpl;
import dto.TransactionDTO;
import dto.TransactionLogDTO;
import model.TransactionType;
import repository.*;
import service.ReplenishmentTransaction;
import service.Transaction;
import service.TransferTransaction;
import service.WithdrawTransaction;

public class Server {

    private static AuthController authController = null;
    private static AccountsDB accountsDB = null;
    private static TransactionsDB transactionsDB = null;



    public Server(AuthController authController, AccountsDB accountsDB, TransactionsDB transactionsDB) {
        this.authController = authController;
        this.accountsDB = accountsDB;
        this.transactionsDB = transactionsDB;
    }

    public static void main(String[] args) throws IOException {
        AuthController authController = new AuthControllerImpl(new ClientsDBImpl());
        AccountsDB accountsDB = new AccountsDBImpl();
        TransactionsDB transactionsDB = new TransactionsLogDB();

        int port = 8080;
        Server server = new Server(authController, accountsDB, transactionsDB);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/login", new Authorization());
        httpServer.createContext("/account", new ATMAccount());
        httpServer.createContext("/transactions/replenishment", new Replenishment());
        httpServer.createContext("/transactions/withdrawal", new Withdrawal());
        httpServer.createContext("/transactions/transfer", new Transfer());
        httpServer.createContext("/transactionsLog", new TransactionsHandler(transactionsDB));
        httpServer.setExecutor(null);
        httpServer.start();
        System.out.println("Сервер запущен на порту " + port);
    }

    static class Authorization implements HttpHandler{

        @Override
        public void handle(HttpExchange exchange) throws IOException {
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

    static class ATMAccount implements HttpHandler{

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                // Обработка OPTIONS запроса (CORS preflight)
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS"); // Укажите разрешенные методы
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "accountNumber, Content-Type"); // Укажите разрешенные заголовки
                exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600"); // Укажите время кэширования (в секундах)
                exchange.sendResponseHeaders(204, -1); // 204 No Content, без тела ответа
                exchange.close();
                return;
            }
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String cardNumber = exchange.getRequestHeaders().getFirst("accountNumber");
                    System.out.println(cardNumber);

                    // 2. Проверяем, что номер карты указан
                    if (cardNumber == null || cardNumber.isEmpty()) {
                        sendErrorResponse(exchange, 400, "Номер карты не указан");
                        return;
                    }

                    // 3. Получаем баланс из базы данных
                    BigDecimal balance = accountsDB.getBalance(cardNumber);

                    // 4. Проверяем, что счет найден
                    if (balance == null) {
                        sendErrorResponse(exchange, 404, "Счет не найден");
                        return;
                    }

                    // 5. Отправляем JSON-ответ с балансом
                    String response = "{\"balance\": \"" + balance.toString() + "\"}";
                    sendJsonResponse(exchange, 200, response);

                } catch (Exception e) {
                    // Обрабатываем ошибки
                    System.err.println("Ошибка при получении баланса: " + e.getMessage());
                    sendErrorResponse(exchange, 500, "Ошибка при получении баланса");
                }
            } /*else {
            System.out.println("тут");
            // Если метод не GET, возвращаем ошибку
            String response = "Только GET запросы разрешены для /api/balance";
            exchange.sendResponseHeaders(405, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }*/
        }
    }

    static class Replenishment implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            if ("POST".equals(exchange.getRequestMethod())) {
                try (InputStream requestBody = exchange.getRequestBody();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8))) {
                    String requestBodyString = reader.lines().collect(Collectors.joining("\n"));


                    String accountNumber = null;
                    String amountString = null;
                    String[] params = requestBodyString.split("&");
                    for (String param : params) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2) {
                            if (keyValue[0].equals("accountNumber")) {
                                accountNumber = keyValue[1];
                            } else if (keyValue[0].equals("amount")) {
                                amountString = keyValue[1];
                            }
                        }
                    }
                    System.out.println("Account"+accountNumber);
                    System.out.println("Amount"+amountString);


                    BigDecimal amount = null;
                    try {
                        amount = new BigDecimal(amountString);
                        System.out.println(amount);
                    } catch (NumberFormatException e) {
                        sendErrorResponse(exchange, 400, "Некорректный формат суммы.");
                        return;
                    }
                    System.out.println("Account"+accountNumber);
                    System.out.println("Amount"+amountString);
                    System.out.println(amount);

                    Transaction transaction = new ReplenishmentTransaction(accountsDB, new TransactionsLogDB());
                    transaction.conductTransaction(new TransactionDTO(TransactionType.REPLENISHMENT, accountNumber, amount, null));
                    //sendJsonResponse(exchange, 200, "{\"success\": true}");
                    String response = "{\"success\": true}";
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Content-Type", "application/json"); // ТОЛЬКО JSON
                    exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }


                } catch (Exception e) {
                    System.err.println("Ошибка при пополнении баланса: " + e.getMessage());
                    sendErrorResponse(exchange, 500, "Ошибка при пополнении баланса.");
                }
            } else {
                String response = "Только POST запросы разрешены для /deposit";
                exchange.sendResponseHeaders(405, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    static class Withdrawal implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            if ("POST".equals(exchange.getRequestMethod())) {
                try (InputStream requestBody = exchange.getRequestBody();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8))) {

                    String requestBodyString = reader.lines().collect(Collectors.joining("\n"));

                    String accountNumber = null;
                    String amountString = null;
                    String[] params = requestBodyString.split("&");
                    for (String param : params) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2) {
                            if (keyValue[0].equals("accountNumber")) {
                                accountNumber = keyValue[1];
                            } else if (keyValue[0].equals("amount")) {
                                amountString = keyValue[1];
                            }
                        }
                    }
                    System.out.println("Account"+accountNumber);
                    System.out.println("Amount"+amountString);


                    BigDecimal amount = null;
                    try {
                        amount = new BigDecimal(amountString);
                        System.out.println(amount);
                    } catch (NumberFormatException e) {
                        sendErrorResponse(exchange, 400, "Некорректный формат суммы.");
                        return;
                    }
                    System.out.println("Account"+accountNumber);
                    System.out.println("Amount"+amountString);
                    System.out.println(amount);

                    Transaction transaction = new WithdrawTransaction(accountsDB, new TransactionsLogDB());
                    transaction.conductTransaction(new TransactionDTO(TransactionType.WITHDRAW, accountNumber, amount, null));
                    //sendJsonResponse(exchange, 200, "{\"success\": true}");
                    String response = "{\"success\": true}";
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Content-Type", "application/json"); // ТОЛЬКО JSON
                    exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }


                } catch (Exception e) {
                    System.err.println("Ошибка при снятии средств: " + e.getMessage());
                    sendErrorResponse(exchange, 500, "Ошибка при снятии средств.");
                }
            } else {
                String response = "Только POST запросы разрешены для /withdrawal";
                exchange.sendResponseHeaders(405, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    static class Transfer implements  HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            if ("POST".equals(exchange.getRequestMethod())) {

                try (InputStream requestBody = exchange.getRequestBody();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8))) {
                    System.out.println(" перевод");
                    String requestBodyString = reader.lines().collect(Collectors.joining("\n"));

                    String senderAccountNumber = null;
                    String recipientAccountNumber = null;
                    String amountString = null;
                    String[] params = requestBodyString.split("&");
                    for (String param : params) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2) {
                            if (keyValue[0].equals("senderAccountNumber")) {
                                senderAccountNumber = keyValue[1];
                            } else if (keyValue[0].equals("recipientCardNumber")) {
                                recipientAccountNumber = keyValue[1];
                            } else if (keyValue[0].equals("amount")) {
                                amountString = keyValue[1];
                            }
                        }
                    }
                    //System.out.println("Account"+accountNumber);
                    //System.out.println("Amount"+amountString);


                    BigDecimal amount = null;
                    try {
                        amount = new BigDecimal(amountString);
                        System.out.println(amount);
                    } catch (NumberFormatException e) {
                        sendErrorResponse(exchange, 400, "Некорректный формат суммы.");
                        return;
                    }
                    //System.out.println("Account"+accountNumber);
                    //System.out.println("Amount"+amountString);
                    //System.out.println(amount);

                    Transaction transaction = new TransferTransaction(accountsDB, new TransactionsLogDB());
                    transaction.conductTransaction(new TransactionDTO(TransactionType.TRANSFER, senderAccountNumber, amount, recipientAccountNumber));
                    //sendJsonResponse(exchange, 200, "{\"success\": true}");
                    String response = "{\"success\": true}";
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Content-Type", "application/json"); // ТОЛЬКО JSON
                    exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }


                } catch (Exception e) {
                    System.err.println("Ошибка при переводе: " + e.getMessage());
                    sendErrorResponse(exchange, 500, "Ошибка при переводе.");
                }
            } else {
                String response = "Только POST запросы разрешены для /withdrawal";
                exchange.sendResponseHeaders(405, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }



    static class TransactionsHandler implements HttpHandler {

        private TransactionsDB transactionsDB;

        public TransactionsHandler(TransactionsDB transactionsDB) {
            this.transactionsDB = transactionsDB;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                // Обработка OPTIONS запроса (CORS preflight)
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS"); // Укажите разрешенные методы
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "accountNumber, Content-Type"); // Укажите разрешенные заголовки
                exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600"); // Укажите время кэширования (в секундах)
                exchange.sendResponseHeaders(204, -1); // 204 No Content, без тела ответа
                exchange.close();
                return;
            }
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String accountNumber = exchange.getRequestHeaders().getFirst("accountNumber");
                    System.out.println("АККАУНТ"+accountNumber);

                    // 2. Проверяем, что номер счета указан
                    if (accountNumber == null || accountNumber.isEmpty()) {
                        sendErrorResponse(exchange, 400, "Не указан номер счета.");
                        return;
                    }

                    // 3. Получаем историю транзакций из базы данных
                    Map<String, TransactionLogDTO> transactions = transactionsDB.getTransactionHistory(accountNumber);
                    //List<TransactionDTO> transactions = transactionsDB.getTransactionHistory(accountNumber);

                    // 4. Преобразуем список транзакций в JSON
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonResponse = objectMapper.writeValueAsString(transactions);
                    System.out.println("\n\n\n "+ jsonResponse);

                    // 5. Отправляем JSON-ответ
                    sendJsonResponse(exchange, 200, jsonResponse);
                    System.out.println("Всё прошло хорошо");

                } catch (Exception e) {
                    System.err.println("Ошибка при получении истории транзакций: " + e.getMessage());
                    sendErrorResponse(exchange, 500, "Ошибка при получении истории транзакций.");
                    e.printStackTrace();
                }
            } else {
                String response = "Только GET запросы разрешены для /transactions";
                exchange.sendResponseHeaders(405, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
        }

    }
    // Отправляет JSON-ответ с кодом состояния и сообщением
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    // Отправляет JSON-ответ об ошибке
    private static void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
        String response = "{\"error\": \"" + errorMessage + "\"}";
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        sendJsonResponse(exchange, statusCode, response);
    }
}