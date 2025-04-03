package model;

public enum TransactionType {
    REPLENISHMENT("Пополнение счёта "),
    WITHDRAW("Снятия со счёта "),
    TRANSFER("Перевод на счёт ");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }


    public String getDescription() {
        return description;
    }


}
