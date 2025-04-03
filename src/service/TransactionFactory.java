package service;

import model.TransactionType;
import repository.AccountsDBImpl;
import repository.TransactionsLogDB;


public class TransactionFactory {

    public static Transaction createTransaction(TransactionType transactionType) {
        switch (transactionType) {
            case REPLENISHMENT -> {
                return new ReplenishmentTransaction(new AccountsDBImpl(), new TransactionsLogDB());
            }
            case WITHDRAW -> {
                return new WithdrawTransaction(new AccountsDBImpl(), new TransactionsLogDB());
            }
            case TRANSFER -> {
                return new TransferTransaction(new AccountsDBImpl(), new TransactionsLogDB());
            }
            default -> {
                throw new IllegalArgumentException("Неверный тип транзакции");
            }

        }
    }
}
