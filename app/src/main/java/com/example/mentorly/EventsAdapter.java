package com.example.mentorly;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {
    private List<String> events;
    private Context context;

    public EventsAdapter(Context context, List<String> events) {
        this.events = events;
        this.context = context;
    }

    @NonNull
    @Override
    public EventsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.item_event, parent, false);

        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String event = events.get(position);
        //if the message fromId == userId, message is sent by the current user
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView tvDetails;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDetails = itemView.findViewById(R.id.text1);
            icon = itemView.findViewById(R.id.icon);
        }

        public void bind(String event) {
            tvDetails.append(event);
            icon.setImageResource(R.drawable.ic_baseline_person_24);
        }
    }
}

