package repository;

import java.math.BigDecimal;

public interface AccountsDB {
    BigDecimal getBalance(String accountNumber);

    void changeBalance(String accountNumber, BigDecimal newBalance);
}
