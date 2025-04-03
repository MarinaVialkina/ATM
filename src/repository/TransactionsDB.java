package repository;

import dto.TransactionDTO;

import java.util.Map;

public interface TransactionsDB {
    void addRecord(TransactionDTO transactionData);

    String getTransactionHistory(String accountNumber);
}
