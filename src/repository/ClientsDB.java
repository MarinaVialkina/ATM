package repository;

import model.Client;

public interface ClientsDB {
    Client checkAuthentication(String accountNumber, String pinCode);

    boolean checkTheExistenceOfTheAccount(String accountNumber);
}
