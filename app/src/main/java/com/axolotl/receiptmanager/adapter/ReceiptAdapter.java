package com.axolotl.receiptmanager.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.axolotl.receiptmanager.R;
import com.axolotl.receiptmanager.model.ReceiptData;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.ViewHolder> {
    public Context context;
    public ArrayList<ReceiptData> receipts;
    public ClickReceipt clickReceipt;

    public ReceiptAdapter(Context context, ArrayList<ReceiptData> receipts, ClickReceipt clickReceipt) {
        this.context = context;
        this.receipts = receipts;
        this.clickReceipt = clickReceipt;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.rv_receipt_price_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        holder.price.setText(String.valueOf(receipts.get(position).amount));
//        holder.uid.setText(receipts.get(position).uid);
        holder.date.setText(getStringDate(receipts.get(position).date));
        holder.type.setText(TextUtils.join(", ", receipts.get(position).type));
        holder.layout.setOnClickListener(v -> {
            clickReceipt.onClickReceipt(receipts.get(position).uid);
        });
    }

    @Override
    public int getItemCount() {
        return receipts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        //        TextView uid;
        TextView price;
        TextView date;
        TextView type;
        LinearLayout layout;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
//            uid = itemView.findViewById(R.id.rvReceiptUID);
            price = itemView.findViewById(R.id.rvReceiptPrice);
            date = itemView.findViewById(R.id.rvReceiptDate);
            type = itemView.findViewById(R.id.rvReceiptType);
            layout = itemView.findViewById(R.id.rvReceiptLayout);
        }
    }

    private String getStringDate(String date) {
        String month = "";
        ArrayList<String> arrDate = new ArrayList(Arrays.asList(date.split("/")));
        switch (arrDate.get(1)) {
            case "1":
                month = "January";
                break;
            case "2":
                month = "February";
                break;
            case "3":
                month = "March";
                break;
            case "4":
                month = "April";
                break;
            case "5":
                month = "May";
                break;
            case "6":
                month = "June";
                break;
            case "7":
                month = "July";
                break;
            case "8":
                month = "August";
                break;
            case "9":
                month = "September";
                break;
            case "10":
                month = "October";
                break;
            case "11":
                month = "November";
                break;
            default:
                month = "December";
                break;
        }
        return month + "-" + arrDate.get(2) + "-" + arrDate.get(0);
    }

    public interface ClickReceipt {
        void onClickReceipt(String uid);
    }
}
