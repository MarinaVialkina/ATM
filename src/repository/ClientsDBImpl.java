package repository;

import model.Client;

import java.sql.*;

import static model.ConnectionData.URL;
import static model.ConnectionData.USER;
import static model.ConnectionData.PASSWORD;

public class ClientsDBImpl implements ClientsDB {

    @Override
    public Client checkAuthentication(String accountNumber, String pinCode) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM clientele WHERE account_number='" + accountNumber + "';");
            if (!resultSet.next() || !(resultSet.getString("pin_code").equals(pinCode))) {
                return null;
            }
            return new Client(resultSet.getString("surname"), resultSet.getString("name"),
                    resultSet.getString("patronymic"), resultSet.getString("account_number"));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean checkTheExistenceOfTheAccount(String accountNumber) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM clientele WHERE account_number='" + accountNumber + "';");
            return resultSet.next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
