package service;

import dto.TransactionDTO;
import exception.TransactionError;
import repository.AccountsDB;
import repository.TransactionsDB;


import java.math.BigDecimal;


public class TransferTransaction implements Transaction {
    private final AccountsDB accountsDB;
    private final TransactionsDB transactionsLogDB;

    public TransferTransaction(AccountsDB accountsDB, TransactionsDB transactionsLogDB) {
        this.accountsDB = accountsDB;
        this.transactionsLogDB = transactionsLogDB;
    }

    @Override
    public void conductTransaction(TransactionDTO transactionData) {
        BigDecimal senderBalance = accountsDB.getBalance(transactionData.accountNumber());
        BigDecimal recipientBalance = accountsDB.getBalance(transactionData.accountNumberRecipient());
        if (senderBalance.compareTo(transactionData.amount()) < 0) {
            throw new TransactionError("Недостаточно средств на счёте");
        }

        accountsDB.changeBalance(transactionData.accountNumber(), senderBalance.subtract(transactionData.amount()));
        accountsDB.changeBalance(transactionData.accountNumberRecipient(), recipientBalance.add(transactionData.amount()));

        transactionsLogDB.addRecord(transactionData);
    }
}
