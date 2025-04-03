package repository;


import java.math.BigDecimal;
import java.sql.*;

import static model.ConnectionData.URL;
import static model.ConnectionData.USER;
import static model.ConnectionData.PASSWORD;


public class AccountsDBImpl implements AccountsDB {
    @Override
    public BigDecimal getBalance(String accountNumber) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM accounts WHERE account_number = '" + accountNumber + "';");

            String balance = "";
            while (resultSet.next()) {
                balance = resultSet.getString("balance").replaceAll(",", ".").replaceAll("[^\\d.-]", "");
                System.out.println(balance);
            }
            return new BigDecimal(balance);

        } catch (SQLException e) {
            throw new RuntimeException(e);

        }

    }

    @Override
    public void changeBalance(String accountNumber, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance=%s  WHERE account_number='%s';";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute(String.format(sql, newBalance, accountNumber));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
