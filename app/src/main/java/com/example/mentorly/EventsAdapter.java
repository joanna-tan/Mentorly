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
    private List<String[]> events;
    private Context context;

    public EventsAdapter(Context context, List<String[]> events) {
        this.events = events;
        this.context = context;
    }

    @NonNull
    @Override
    public EventsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_event, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView tvEventStart;
        TextView tvTitleEvent;

        public ViewHolder(View itemView) {
            super(itemView);
            tvEventStart = itemView.findViewById(R.id.tvEventStart);
            tvTitleEvent = itemView.findViewById(R.id.titleEvent);
            icon = itemView.findViewById(R.id.icon);
        }

        public void bind(String[] event) {
            if (event.length >= 2) {
                tvTitleEvent.setText(event[0]);
                tvEventStart.setText(event[1]);
            }
            icon.setImageResource(R.drawable.ic_outline_calendar_today_24);
        }
    }
}

