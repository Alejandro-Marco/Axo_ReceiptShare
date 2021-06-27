package com.axolotl.receiptmanager.model;

import java.util.ArrayList;
import java.util.Arrays;

public class ReceiptData {
    public ArrayList<String> type;
    public Double amount;
    public String date;
    public String uid;

    public ReceiptData() {
    }

    public ReceiptData(ArrayList<String> type, Double amount, String date, String uid) {
        type.add("any");
        // Remove redundant types
        ArrayList<String> _type = new ArrayList<>();
        for (String t : type)
            if (!_type.contains(t))
                _type.add(t);

        this.type = _type;
        this.amount = amount;
        this.date = date;
        this.uid = uid;
    }

    public double numericalDateValue() {
        ArrayList<String> arrDate = new ArrayList(Arrays.asList(date.split("/")));
        return
                Double.parseDouble(arrDate.get(0))
                        + (Double.parseDouble(arrDate.get(1)) / 12)
                        + (Double.parseDouble(arrDate.get(2)) / 365);
    }

}
