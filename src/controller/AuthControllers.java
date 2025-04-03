package controller;

import model.Client;
import repository.ClientsDB;


public class AuthControllers {
    private final ClientsDB clientsDB;


    public AuthControllers(ClientsDB clientsDB, TransactionController transactionController) {
        this.clientsDB = clientsDB;
    }

    public void login(String accountNumber, String pinCode) {
        String validAccountNumber = accountNumber.trim();
        String validPinCode = pinCode.trim();

        if (validAccountNumber.length() != 20 || !isDigit(validAccountNumber) || validPinCode.length() != 4 || !isDigit(validPinCode)) {
            System.out.println("Неверно введён логин или пароль");
            return;
        }

        Client client = clientsDB.checkAuthentication(validAccountNumber, pinCode);
        if (client != null) {
            //transactionController.createTransaction(validAccountNumber);
            //запуск 2ого интефейса с командами
            System.out.println("Вход");
        }
    }


    private static boolean isDigit(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
