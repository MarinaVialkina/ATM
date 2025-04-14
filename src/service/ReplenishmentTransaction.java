package service;

import dto.TransactionDTO;
import repository.AccountsDB;
import repository.TransactionsDB;


import java.math.BigDecimal;


public class ReplenishmentTransaction implements Transaction {
    private final AccountsDB accountsDB;
    private final TransactionsDB transactionsLogDB;

    public ReplenishmentTransaction(AccountsDB accountsDB, TransactionsDB transactionsLogDB) {
        this.accountsDB = accountsDB;
        this.transactionsLogDB = transactionsLogDB;
    }

    @Override
    public void conductTransaction(TransactionDTO transactionData) {
        BigDecimal obsoleteBalance = accountsDB.getBalance(transactionData.accountNumber());
        BigDecimal newBalance = obsoleteBalance.add(transactionData.amount());

        accountsDB.changeBalance(transactionData.accountNumber(), newBalance);
        transactionsLogDB.addRecord(transactionData);
    }
}
