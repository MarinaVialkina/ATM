package dto;


import java.sql.Timestamp;

public record TransactionLogDTO(String transactionType, String amount, Timestamp dateAndTime) {
}
