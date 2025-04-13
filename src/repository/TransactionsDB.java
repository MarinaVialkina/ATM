package repository;

import dto.TransactionDTO;
import dto.TransactionLogDTO;

import java.util.Map;

public interface TransactionsDB {
    void addRecord(TransactionDTO transactionData);

    Map<String, TransactionLogDTO> getTransactionHistory(String accountNumber);
}
