package com.example.mentorly;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.example.mentorly.models.MyEvent;

import java.util.Date;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {

    private List<MyEvent> events;
    private Context context;

    // global variable to track expanded card
    public static int mExpandedPosition = -1;
    boolean isExpandable;

    public EventsAdapter(Context context, List<MyEvent> events) {
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
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        MyEvent event = events.get(position);
        holder.bind(event);

        isExpandable = (event.getEventDescription() != null && !event.getEventDescription().equals("")) ||
                (event.getAttendees() != null && !event.getAttendees().isEmpty());

        final boolean isExpanded = position == mExpandedPosition;
        holder.eventDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        // if isExpanded set up arrow, else set down arrow
        holder.ivEventDropDown.setImageResource(isExpanded ?
                R.drawable.ic_baseline_keyboard_arrow_up_24 : R.drawable.ic_baseline_keyboard_arrow_down_24);
        holder.itemView.setActivated(isExpanded);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpandedPosition = isExpanded ? -1 : position;
                TransitionManager.beginDelayedTransition(holder.eventDetails);
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout clEvent;
        RelativeLayout eventDetails;
        TextView eventDescription;
        TextView eventAttendees;
        ImageView icon;
        ImageView ivEventDropDown;
        TextView tvEventStart;
        TextView tvTitleEvent;

        public ViewHolder(View itemView) {
            super(itemView);
            tvEventStart = itemView.findViewById(R.id.tvEventStart);
            tvTitleEvent = itemView.findViewById(R.id.titleEvent);
            icon = itemView.findViewById(R.id.icon);
            clEvent = itemView.findViewById(R.id.clEventLayout);
            eventDetails = itemView.findViewById(R.id.rlEventDetails);
            ivEventDropDown = itemView.findViewById(R.id.ivEventDropDown);
            eventDescription = itemView.findViewById(R.id.eventDetails);
            eventAttendees = itemView.findViewById(R.id.eventAttendees);
        }

        public void bind(MyEvent event) {
            tvTitleEvent.setText(event.getEventTitle());
            // if the description has text & attendees, show the drop down arrow
            if (event.getEventDescription() != null && !event.getEventDescription().equals("") && event.getAttendees() != null) {
                eventDescription.setVisibility(View.VISIBLE);
                ivEventDropDown.setVisibility(View.VISIBLE);
                eventAttendees.setVisibility(View.VISIBLE);

                eventDescription.setText("Description: " + event.getEventDescription());
                StringBuilder allAttendees = new StringBuilder();
                allAttendees.append("Attendees: ");
                for (String attendee : event.getAttendees()) {
                    allAttendees.append(attendee);
                }
                eventAttendees.setText(allAttendees);
            }
            // else hide the drop down arrow & the description view
            else if (event.getAttendees() != null && !event.getAttendees().isEmpty()) {
                ivEventDropDown.setVisibility(View.VISIBLE);
                eventAttendees.setVisibility(View.VISIBLE);
                eventDescription.setVisibility(View.GONE);

                StringBuilder allAttendees = new StringBuilder();
                allAttendees.append("Attendees: ");
                for (String attendee : event.getAttendees()) {
                    allAttendees.append(attendee);
                }
                eventAttendees.setText(allAttendees);
            } else if (event.getEventDescription() != null && !event.getEventDescription().equals("")) {
                eventDescription.setVisibility(View.VISIBLE);
                ivEventDropDown.setVisibility(View.VISIBLE);
                eventAttendees.setVisibility(View.GONE);
                eventDescription.setText("Description: " + event.getEventDescription());
            } else {
                eventAttendees.setVisibility(View.GONE);
                eventDescription.setVisibility(View.GONE);
                ivEventDropDown.setVisibility(View.GONE);
            }


            //set the startDate view
            Date date = event.getStartDate();
            String day = (String) DateFormat.format("dd", date); // 20
            String monthNumber = (String) DateFormat.format("MM", date); // 06
            String year = (String) DateFormat.format("yyyy", date); // 2013
            String dueString = monthNumber + "/" + day + "/" + year;
            tvEventStart.setText("Due: " + dueString);

            icon.setImageResource(R.drawable.ic_outline_calendar_today_24);
        }
    }
}

