package com.axolotl.receiptmanager.model;

public class ReceiptData {
    public String type;
    public Double amount;
    public String date;
    public String uid;

    public ReceiptData(String type, Double amount, String date, String uid) {
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.uid = uid;
    }
}
