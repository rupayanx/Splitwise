package com.rupayan.splitwise.models;
public class Transaction {
    private User debtor;
    private User creditor;
    private double amount;

    public Transaction(User debtor, User creditor, double amount) {
        this.debtor = debtor;
        this.creditor = creditor;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return debtor.getName() + " will pay $" + amount + " to " + creditor.getName();
    }
}
