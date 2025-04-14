package dto;

import model.TransactionType;

import java.math.BigDecimal;

public record TransactionDTO(TransactionType transactionType, String accountNumber, BigDecimal amount,
                             String accountNumberRecipient) {
}
