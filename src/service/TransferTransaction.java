package service;

import dto.TransactionDTO;
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
        BigDecimal senderBalance = accountsDB.getBalance(transactionData.getAccountNumber());
        BigDecimal recipientBalance = accountsDB.getBalance(transactionData.getAccountNumberRecipient());
        if (senderBalance.compareTo(transactionData.getAmount()) < 0) {
            return;//сообщение об ошибке снятия
        }

        accountsDB.changeBalance(transactionData.getAccountNumber(), senderBalance.subtract(transactionData.getAmount()));
        accountsDB.changeBalance(transactionData.getAccountNumberRecipient(), recipientBalance.add(transactionData.getAmount()));


        transactionsLogDB.addRecord(transactionData);
    }
}
