package controller;

import dto.TransactionDTO;
import model.TransactionType;

import repository.ClientsDB;
import service.*;

import java.math.BigDecimal;


public class TransactionControllerImpl implements TransactionController {
    private final ClientsDB clientsDB;

    public TransactionControllerImpl(ClientsDB clientsDB) {
        this.clientsDB = clientsDB;
    }


    @Override
    public void createTransaction(TransactionDTO transactionData) {
        if (transactionData.getTransactionType() == TransactionType.TRANSFER && (transactionData.getAccountNumberRecipient() == null || !clientsDB.checkTheExistenceOfTheAccount(transactionData.getAccountNumberRecipient()))) {
            System.out.println("Ошибка в вводе номера счёта для перевода");
            return;
        }
        if (transactionData.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("Ошибка в вводе суммы транзакции");
            return;

        }

        Transaction transaction = TransactionFactory.createTransaction(transactionData.getTransactionType());
        transaction.conductTransaction(transactionData);


    }
}
