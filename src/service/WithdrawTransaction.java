package service;

import dto.TransactionDTO;
import exception.TransactionError;
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
        BigDecimal obsoleteBalance = accountsDB.getBalance(transactionData.accountNumber());
        if (obsoleteBalance.compareTo(transactionData.amount()) < 0) {
            throw new TransactionError("Недостаточно средств на счёте");
        }

        BigDecimal newBalance = obsoleteBalance.subtract(transactionData.amount());

        accountsDB.changeBalance(transactionData.accountNumber(), newBalance);
        transactionsLogDB.addRecord(transactionData);
    }
}
