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
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.example.mentorly.models.ToDoItem;

import java.util.Date;
import java.util.List;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.ViewHolder> {

    public static int mExpandedPosition = -1;
    private boolean isExpandable;
    private List<ToDoItem> items;
    private Context context;

    public ToDoAdapter(Context context, List<ToDoItem> items) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.item_to_do, parent, false);

        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        ToDoItem item = items.get(position);
        holder.bind(item);

        if (item.getBody() == null || item.getBody().equals("")) {
            isExpandable = false;
        }
        else {
            isExpandable = true;
        }

        if (isExpandable) {
            final boolean isExpanded = position == mExpandedPosition;
            holder.details.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.itemView.setActivated(isExpanded);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mExpandedPosition = isExpanded ? -1 : position;
                    TransitionManager.beginDelayedTransition(holder.details);
                    notifyItemChanged(position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        CardView content;
        RelativeLayout details;
        TextView tvTitle;
        TextView tvBody;
        TextView tvDueDate;
        ImageView ivDropDown;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvBody = itemView.findViewById(R.id.tvDescription);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            content = itemView.findViewById(R.id.cardViewToDo);
            details = itemView.findViewById(R.id.rlToDoDetails);
            ivDropDown = itemView.findViewById(R.id.ivDropDown);
        }

        public void bind(final ToDoItem item) {
            String title = item.getTitle();
            String body = item.getBody();
            Date dueDate = item.getDueDate();

            tvTitle.setText(title);

            // retrieve due date format from Date
            if (dueDate != null && dueDate.toString() != null) {
                String day = (String) DateFormat.format("dd", dueDate); // 20
                String monthNumber = (String) DateFormat.format("MM", dueDate); // 06
                String year = (String) DateFormat.format("yyyy", dueDate); // 2013
                String dueString = monthNumber + "/" + day + "/" + year;
                tvDueDate.setVisibility(View.VISIBLE);
                tvDueDate.setText("Due: " + dueString);
            } else {
                tvDueDate.setVisibility(View.GONE);
            }
            // Check if the optional description has an input, else remove the view
            if (body != null && !body.equals("")) {
                tvBody.setText(body);
                ivDropDown.setVisibility(View.VISIBLE);
            } else {
                details.setVisibility(View.GONE);
                ivDropDown.setVisibility(View.GONE);
                tvBody.setVisibility(View.GONE);
            }
        }

    }
}
