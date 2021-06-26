package com.axolotl.receiptmanager.model;

import java.util.ArrayList;

public class ReceiptData {
    public ArrayList<String> type;
    public Double amount;
    public String date;
    public String uid;

    public ReceiptData() {
    }

    public ReceiptData(ArrayList<String> type, Double amount, String date, String uid) {
        type.add("any");
        ArrayList<String> _type = new ArrayList<>();
        for (String t : type)
            if (!_type.contains(t))
                _type.add(t);
        this.type = _type;
        this.amount = amount;
        this.date = date;
        this.uid = uid;
    }
}
