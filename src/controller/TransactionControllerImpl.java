package controller;

import dto.TransactionDTO;
import exception.TransactionError;
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
    public void createTransaction(TransactionDTO transactionData) throws TransactionError {
        if (transactionData.transactionType() == TransactionType.TRANSFER && (transactionData.accountNumberRecipient() == null || !clientsDB.checkTheExistenceOfTheAccount(transactionData.accountNumberRecipient()))) {
            throw new TransactionError("Не введён номер счёта для перевода");
        }
        if (transactionData.amount().compareTo(BigDecimal.ZERO) == 0) {
            throw new TransactionError("Сумма транзакции 0");

        }
        Transaction transaction = TransactionFactory.createTransaction(transactionData.transactionType());
        transaction.conductTransaction(transactionData);

    }
}
