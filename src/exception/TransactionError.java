package exception;

public class TransactionError extends RuntimeException {
    public TransactionError(String message) {
        super(message);
    }
}
