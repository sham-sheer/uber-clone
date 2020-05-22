package com.example.etows.historyRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.etows.HistorySingleActivity;
import com.example.etows.R;

import org.w3c.dom.Text;

public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView rideId;
    public TextView timestamp;
    public HistoryViewHolders(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        rideId = (TextView) itemView.findViewById(R.id.rideId);
        timestamp = (TextView) itemView.findViewById(R.id.timestamp);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("rideId", rideId.getText().toString());
        intent.putExtras(b);
        v.getContext().startActivity(intent);

    }
}
