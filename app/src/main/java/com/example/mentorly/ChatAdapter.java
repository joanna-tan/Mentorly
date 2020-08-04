package com.example.mentorly;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mentorly.models.Message;
import com.google.android.material.snackbar.Snackbar;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    public static final String PROFILE_IMAGE_KEY = "profileImage";
    private List<Message> messages;
    private Context context;
    private String userId;

    public ChatAdapter(Context context, String userId, List<Message> messages) {
        this.messages = messages;
        this.userId = userId;
        this.context = context;
    }

    @NonNull
    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.item_chat, parent, false);

        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);

        // Call the bind method in ViewHolder class
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageOther;
        ImageView imageMe;
        TextView bodyOther;
        TextView bodyMe;
        boolean isSaved;

        public ViewHolder(View itemView) {
            super(itemView);
            imageOther = itemView.findViewById(R.id.ivProfileOther);
            imageMe = itemView.findViewById(R.id.ivProfileMe);
            bodyOther = itemView.findViewById(R.id.tvBodyOther);
            bodyMe = itemView.findViewById(R.id.tvBodyMe);
        }

        public void bind(final Message message) {
            //if the message fromId == userId, message is sent by the current user
            final boolean isMe = message.getFromId() != null && message.getFromId().getObjectId().equals(userId);

            if (isMe) {
                imageMe.setVisibility(View.VISIBLE);
                imageOther.setVisibility(View.GONE);
                bodyOther.setVisibility(View.GONE);
                bodyMe.setVisibility(View.VISIBLE);

                bodyMe.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
                bodyMe.setText(message.getBody());

            } else {
                imageOther.setVisibility(View.VISIBLE);
                imageMe.setVisibility(View.GONE);
                bodyMe.setVisibility(View.GONE);
                bodyOther.setVisibility(View.VISIBLE);


                bodyOther.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                bodyOther.setText(message.getBody());
            }

            final ImageView profileView = isMe ? imageMe : imageOther;

            //retrieve the user from the message (aka from ID)
            ParseUser user = message.getFromId();

            try {
                user.fetchIfNeeded();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //set the profileView image to user.getImage()
            ParseFile profileImage = user.getParseFile(PROFILE_IMAGE_KEY);

            //Load into Glide
            if (profileImage != null) {
                Glide.with(context).load(profileImage.getUrl()).into(profileView);
            }
            else {
                profileView.setImageResource(R.drawable.ic_baseline_person_24);
            }

            // Check if the current user has liked the message
            final ParseUser currentUser = ParseUser.getCurrentUser();
            try {
                currentUser.fetchIfNeeded();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            try {
                message.fetchIfNeeded();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            ArrayList<ParseUser> savedBy = (ArrayList<ParseUser>) message.get(Message.SAVED_BY_KEY);
            if (savedBy != null && !savedBy.isEmpty()) {
                for (ParseUser savedByUser : savedBy) {
                    try {
                        savedByUser.fetch();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    if (savedByUser.getUsername().equals(currentUser.getUsername())) {
                        isSaved = true;
                    }
                }
            }


            // set onLongClickListeners to the text views
            final TextView messageView = isMe ? bodyMe : bodyOther;
            messageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (isSaved) {
                        Snackbar.make(view, "Already saved", Snackbar.LENGTH_SHORT).
                                setAnchorView(((AppCompatActivity)context).findViewById(R.id.bottomNavigation)).show();
                    }
                    else {
                        message.addUnique(Message.SAVED_BY_KEY, currentUser);
                        message.saveInBackground();

                        Snackbar.make(view, "Chat saved", Snackbar.LENGTH_SHORT).
                                setAnchorView(((AppCompatActivity)context).findViewById(R.id.bottomNavigation)).show();
                    }
                    return true;
                }
            });
        }
    }
}
