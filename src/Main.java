import controller.AuthController;
import controller.AuthControllerImpl;
import controller.TransactionController;
import controller.TransactionControllerImpl;
import dto.TransactionDTO;
import model.TransactionType;
import repository.ClientsDBImpl;

import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        //AuthController authControllers = new AuthControllerImpl(new ClientsDBImpl());
        //authControllers.login("12345678901234567890", "1234");

        //TransactionController transactionController = new TransactionControllerImpl(new ClientsDBImpl());
        //transactionController.createTransaction(new TransactionDTO(TransactionType.REPLENISHMENT, "12345678901234567890", new BigDecimal(25), null));

        //TransactionsDB transactionsDB = new TransactionsLogDB();
        //System.out.println(transactionsDB.getTransactionHistory("12345678901234567890"));

    }
}