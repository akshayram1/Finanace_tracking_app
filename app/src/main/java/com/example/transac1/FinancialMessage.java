package com.example.transac1;

public class FinancialMessage {
    private String accountNumber;
    private String transactionType;
    private String amount;
    private String transactionDate;
    private String referenceNo;
    private String personName;

    // Default constructor required for calls to DataSnapshot.getValue(FinancialMessage.class)
    public FinancialMessage() {
    }

    public FinancialMessage(String accountNumber, String transactionType, String amount, String transactionDate, String referenceNo, String personName) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.referenceNo = referenceNo;
        this.personName = personName;
    }

    // Getters and setters
    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }
}