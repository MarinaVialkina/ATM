package model;

public class Client {
    private final String surname;
    private final String name;
    private final String patronymic;
    private final String accountNumber;

    public Client(String surname, String name, String patronymic, String accountNumber) {
        this.surname = surname;
        this.name = name;
        this.patronymic = patronymic;
        this.accountNumber = accountNumber;
    }

    public String getSurname() {
        return surname;
    }

    public String getName() {
        return name;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
