package dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class TransactionLogDTO {
    private final String transactionType;
    private final String amount;
    private final Timestamp dateAndTime;


    public TransactionLogDTO(String transactionType, String amount, Timestamp dateAndTime) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.dateAndTime = dateAndTime;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getAmount() {
        return amount;
    }

    public Timestamp getDateAndTime() {
        return dateAndTime;
    }
}
