import controller.AuthControllers;
import controller.TransactionController;
import controller.TransactionControllerImpl;
import dto.TransactionDTO;
import model.TransactionType;
import repository.ClientsDBImpl;
import repository.TransactionsDB;
import repository.TransactionsLogDB;
import service.TransactionFactory;

import java.math.BigDecimal;


public class Main {
    public static void main(String[] args) {
        //AuthControllers authControllers = new AuthControllers(new ClientsDBImpl(),new TransactionControllerImpl(new TransactionFactory()));
        //authControllers.login("12345678901234567890", "1234");

        //TransactionController transactionController = new TransactionControllerImpl(new ClientsDBImpl());
        //transactionController.createTransaction(new TransactionDTO(TransactionType.TRANSFER, "12345678901234567890", new BigDecimal(25), null));

        //TransactionsDB transactionsDB = new TransactionsLogDB();
        //System.out.println(transactionsDB.getTransactionHistory("12345678901234567890"));

    }
}