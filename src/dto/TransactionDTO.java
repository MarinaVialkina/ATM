package dto;

import model.TransactionType;

import java.math.BigDecimal;

public class TransactionDTO {
    private final TransactionType transactionType;
    private final String accountNumber;
    private final BigDecimal amount;
    private final String accountNumberRecipient;


    public TransactionDTO(TransactionType transactionType, String accountNumber, BigDecimal amount, String accountNumberRecipient) {
        this.transactionType = transactionType;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.accountNumberRecipient = accountNumberRecipient;

    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getAccountNumberRecipient() {
        return accountNumberRecipient;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }
}
