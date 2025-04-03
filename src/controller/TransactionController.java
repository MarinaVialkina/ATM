package controller;

import dto.TransactionDTO;

public interface TransactionController {
    void createTransaction(TransactionDTO transactionData);
}
