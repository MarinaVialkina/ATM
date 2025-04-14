package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

import controller.AuthController;
import controller.AuthControllerImpl;
import controller.TransactionController;
import controller.TransactionControllerImpl;
import dto.TransactionDTO;
import dto.TransactionLogDTO;
import exception.TransactionError;
import model.TransactionType;
import repository.*;


public class Server {

    private final AuthController authController;
    private final TransactionController transactionController;
    private final AccountsDB accountsDB;
    private final TransactionsDB transactionsDB;

    public Server(AuthController authController, TransactionController transactionController, AccountsDB accountsDB, TransactionsDB transactionsDB) {
        this.authController = authController;
        this.transactionController = transactionController;
        this.accountsDB = accountsDB;
        this.transactionsDB = transactionsDB;
    }

    public static void main(String[] args) throws IOException {
        AuthController authController = new AuthControllerImpl(new ClientsDBImpl());
        TransactionController transactionController = new TransactionControllerImpl(new ClientsDBImpl());
        AccountsDB accountsDB = new AccountsDBImpl();
        TransactionsDB transactionsDB = new TransactionsLogDB();

        Server server = new Server(authController, transactionController, accountsDB, transactionsDB);

        int port = 8080;
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/login", new AuthorizationHandler(server.authController));
        httpServer.createContext("/account", new AccountHandler(server.accountsDB));
        httpServer.createContext("/transactions/replenishment", new ReplenishmentHandler(server.transactionController));
        httpServer.createContext("/transactions/withdrawal", new WithdrawalHandler(server.transactionController));
        httpServer.createContext("/transactions/transfer", new TransferHandler(server.transactionController));
        httpServer.createContext("/transactionsLog", new TransactionsLogHandler(server.transactionsDB));
        httpServer.setExecutor(null);
        httpServer.start();
        System.out.println("Сервер запущен на порту " + port);
    }


    static class AuthorizationHandler implements HttpHandler {
        private final AuthController authController;

        public AuthorizationHandler(AuthController authController) {
            this.authController = authController;
        }

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

                    if(authController.login(accountNumber, pinCode)){
                        Server.sendJsonResponse(exchange, 200, "{\"success\": true}");
                    }else {
                        Server.sendJsonResponse(exchange, 200, "{\"success\": false}");
                    }

                } catch (Exception e) {
                    Server.sendErrorResponse(exchange, 500, "{\"success\": false}");
                }

            }
        }
    }

    static class AccountHandler implements HttpHandler {
        private final AccountsDB accountsDB;

        public AccountHandler(AccountsDB accountsDB) {
            this.accountsDB = accountsDB;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "accountNumber, Content-Type");
                exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String cardNumber = exchange.getRequestHeaders().getFirst("accountNumber");
                    BigDecimal balance = accountsDB.getBalance(cardNumber);

                    String response = "{\"balance\": \"" + balance.toString() + "\"}";
                    sendJsonResponse(exchange, 200, response);

                } catch (Exception e) {
                    sendErrorResponse(exchange, 500, "Ошибка при получении баланса");
                }
            }
        }
    }

    static class ReplenishmentHandler implements HttpHandler {
        private final TransactionController transactionController;

        public ReplenishmentHandler(TransactionController transactionController) {
            this.transactionController = transactionController;
        }

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

                    BigDecimal amount = null;
                    try {
                        amount = new BigDecimal(amountString);
                    } catch (NumberFormatException e) {
                        sendErrorResponse(exchange, 400, "Некорректный формат суммы.");
                        return;
                    }
                    try{
                        transactionController.createTransaction(new TransactionDTO(TransactionType.REPLENISHMENT, accountNumber, amount, null));
                        sendJsonResponse(exchange, 200, "{\"success\": true}");
                    }catch (TransactionError e){
                        sendJsonResponse(exchange, 200, "{\"success\": false, \"message\": \""+e.getMessage()+"\"}");
                    }
                } catch (Exception e) {
                    sendErrorResponse(exchange, 500, "Ошибка при пополнении баланса.");
                }
            }
        }
    }

    static class WithdrawalHandler implements HttpHandler {
        private final TransactionController transactionController;

        public WithdrawalHandler(TransactionController transactionController) {
            this.transactionController = transactionController;
        }

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

                    BigDecimal amount = null;
                    try {
                        amount = new BigDecimal(amountString);
                    } catch (NumberFormatException e) {
                        sendErrorResponse(exchange, 400, "Некорректный формат суммы.");
                        return;
                    }

                    try {
                        transactionController.createTransaction(new TransactionDTO(TransactionType.WITHDRAW, accountNumber, amount, null));
                        sendJsonResponse(exchange, 200, "{\"success\": true}");
                    }catch (TransactionError e){
                        sendJsonResponse(exchange, 200, "{\"success\": false, \"message\": \""+e.getMessage()+"\"}");
                    }

                } catch (Exception e) {
                    sendErrorResponse(exchange, 500, "Ошибка при снятии средств.");
                }
            }

        }
    }

    static class TransferHandler implements HttpHandler {
        private final TransactionController transactionController;

        public TransferHandler(TransactionController transactionController) {
            this.transactionController = transactionController;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try (InputStream requestBody = exchange.getRequestBody();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8))) {

                    String requestBodyString = reader.lines().collect(Collectors.joining("\n"));
                    String senderAccountNumber = null;
                    String recipientAccountNumber = null;
                    String amountString = null;
                    String[] params = requestBodyString.split("&");
                    for (String param : params) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2) {
                            switch (keyValue[0]) {
                                case "senderAccountNumber" -> senderAccountNumber = keyValue[1];
                                case "recipientCardNumber" -> recipientAccountNumber = keyValue[1];
                                case "amount" -> amountString = keyValue[1];
                            }
                        }
                    }
                    BigDecimal amount = null;
                    try {
                        amount = new BigDecimal(amountString);
                    } catch (NumberFormatException e) {
                        sendErrorResponse(exchange, 400, "Некорректный формат суммы.");
                        return;
                    }

                    try {
                        transactionController.createTransaction(new TransactionDTO(TransactionType.TRANSFER, senderAccountNumber, amount, recipientAccountNumber));
                        sendJsonResponse(exchange, 200, "{\"success\": true}");
                    }catch (TransactionError e){
                        sendJsonResponse(exchange, 200, "{\"success\": false, \"message\": \""+e.getMessage()+"\"}");
                    }

                } catch (Exception e) {
                    sendErrorResponse(exchange, 500, "Ошибка при переводе.");
                }
            }
        }
    }

    static class TransactionsLogHandler implements HttpHandler {
        private final TransactionsDB transactionsDB;

        public TransactionsLogHandler(TransactionsDB transactionsDB) {
            this.transactionsDB = transactionsDB;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "accountNumber, Content-Type");
                exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String accountNumber = exchange.getRequestHeaders().getFirst("accountNumber");
                    Map<String, TransactionLogDTO> transactions = transactionsDB.getTransactionHistory(accountNumber);

                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonResponse = objectMapper.writeValueAsString(transactions);
                    sendJsonResponse(exchange, 200, jsonResponse);

                } catch (Exception e) {
                    sendErrorResponse(exchange, 500, "Ошибка при получении истории транзакций.");
                }
            }
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private static void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
        String response = "{\"error\": \"" + errorMessage + "\"}";
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        sendJsonResponse(exchange, statusCode, response);
    }
}
