package service;

import dto.TransactionDTO;
import repository.AccountsDB;
import repository.TransactionsDB;

import java.math.BigDecimal;


public class WithdrawTransaction implements Transaction {
    private final AccountsDB accountsDB;
    private final TransactionsDB transactionsLogDB;

    public WithdrawTransaction(AccountsDB accountsDB, TransactionsDB transactionsLogDB) {
        this.accountsDB = accountsDB;
        this.transactionsLogDB = transactionsLogDB;
    }

    @Override
    public void conductTransaction(TransactionDTO transactionData) {
        BigDecimal obsoleteBalance = accountsDB.getBalance(transactionData.getAccountNumber());
        if (obsoleteBalance.compareTo(transactionData.getAmount()) < 0) {
            return;
            //сообщение об ошибке снятия
        }

        BigDecimal newBalance = obsoleteBalance.subtract(transactionData.getAmount());

        accountsDB.changeBalance(transactionData.getAccountNumber(), newBalance);
        transactionsLogDB.addRecord(transactionData);
    }
}
