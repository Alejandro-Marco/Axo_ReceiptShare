package com.axolotl.receiptshare.model;

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

    public int numericalDateValue() {
        ArrayList<String> arrDate = new ArrayList(Arrays.asList(date.split("/")));
        return
                Integer.parseInt(arrDate.get(0)) * 365
                        + monthsToDays(Integer.parseInt(arrDate.get(1)))
                        + (Integer.parseInt(arrDate.get(2)));
    }

    private int monthsToDays(int monthNum){
        switch (monthNum){
            case 1:
                return 31;
            case 2:
                return 28 + monthsToDays(monthNum - 1);
            case 3:
            case 7:
            case 5:
            case 8:
            case 10:
            case 12:
                return 31 + monthsToDays(monthNum - 1);
            case 4:
            case 6:
            case 9:
            case 11:
                return 30 + monthsToDays(monthNum - 1);
        }
        return 0;
    }

}
