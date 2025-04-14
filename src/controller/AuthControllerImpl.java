package controller;

import model.Client;
import repository.ClientsDB;


public class AuthControllerImpl implements AuthController{
    private final ClientsDB clientsDB;

    public AuthControllerImpl(ClientsDB clientsDB) {
        this.clientsDB = clientsDB;
    }

    public boolean login(String accountNumber, String pinCode) {
        String validAccountNumber = accountNumber.trim();
        String validPinCode = pinCode.trim();

        if (validAccountNumber.length() != 20 || !isDigit(validAccountNumber) || validPinCode.length() != 4 || !isDigit(validPinCode)) {
            return false;
        }

        Client client = clientsDB.checkAuthentication(validAccountNumber, pinCode);

        return client != null;

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
