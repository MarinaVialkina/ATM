package repository;

import dto.TransactionDTO;
import dto.TransactionLogDTO;
import model.TransactionType;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;


import static model.ConnectionData.URL;
import static model.ConnectionData.USER;
import static model.ConnectionData.PASSWORD;

public class TransactionsLogDB implements TransactionsDB {


    @Override
    public void addRecord(TransactionDTO transactionData) {

        String sql = "INSERT INTO transactions(account_number, transaction_type, transaction_amount) VALUES ('%s', '%s', %s);";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute(String.format(sql, transactionData.accountNumber(),
                    (transactionData.transactionType().getDescription()), transactionData.amount()));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        if (transactionData.transactionType() == TransactionType.TRANSFER) {
            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                 Statement statement = connection.createStatement()) {
                statement.execute(String.format(sql, transactionData.accountNumberRecipient(),
                        "Перевод со счёта " + transactionData.accountNumber(), transactionData.amount()));

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Map<String, TransactionLogDTO> getTransactionHistory(String accountNumber) {
        Map<String, TransactionLogDTO> transactionLog = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM transactions WHERE account_number = '" + accountNumber + "';");
            while (resultSet.next()) {
                TransactionLogDTO transactionLogDTO = new TransactionLogDTO(resultSet.getString("transaction_type"),
                        resultSet.getString("transaction_amount").replace('?', 'P'), resultSet.getTimestamp("date_and_time"));
                transactionLog.put(String.valueOf(resultSet.getBigDecimal("id")), transactionLogDTO);
            }
            return transactionLog;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
