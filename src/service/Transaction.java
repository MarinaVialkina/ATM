package service;

import dto.TransactionDTO;


public interface Transaction {
    void conductTransaction(TransactionDTO transactionData);
}
