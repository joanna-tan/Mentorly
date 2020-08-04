package com.example.mentorly;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.mentorly.fragments.CalendarFragment;
import com.example.mentorly.models.Message;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {
    public static final String PROFILE_IMAGE_KEY = "profileImage";
    private List<Message> messages;
    private Context context;

    public BookmarkAdapter(Context context, List<Message> messages) {
        this.messages = messages;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.item_bookmark, parent, false);

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
        RelativeLayout content;
        ImageView ivProfileView;
        TextView tvBookmarkBody;
        TextView tvScreenName;
        TextView tvTimestamp;

        public ViewHolder(View itemView) {
            super(itemView);
            ivProfileView = itemView.findViewById(R.id.ivBookmarkProfileImage);
            tvBookmarkBody = itemView.findViewById(R.id.tvBookmarkBody);
            tvScreenName = itemView.findViewById(R.id.tvScreenName);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            content = itemView.findViewById(R.id.content);
        }

        public void bind(final Message message) {
            //retrieve the user from the message (aka from ID)
            ParseUser sendingUser = message.getFromId();

            try {
                sendingUser.fetchIfNeeded();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //set the profileView image to user.getImage() and load into Glide
            ParseFile profileImage = sendingUser.getParseFile(PROFILE_IMAGE_KEY);
            if (profileImage != null) {
                Glide.with(context).load(profileImage.getUrl())
                        .apply(RequestOptions.centerCropTransform())
                        .apply(RequestOptions.circleCropTransform())
                        .into(ivProfileView);
            } else {
                ivProfileView.setImageResource(R.drawable.ic_baseline_person_24);
            }

            // Set the message body, timestamp and username
            tvScreenName.setText(sendingUser.getUsername());
            tvTimestamp.setText(CalendarFragment.getRelativeTimeAgo(message.getCreatedAt().toString()));
            tvBookmarkBody.setText(message.getBody());

            // Check if the current user has saved the message
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

            content.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    // delete the current user from the saved array
                                    ArrayList<ParseUser> savedByUsers = (ArrayList<ParseUser>) message.get(Message.SAVED_BY_KEY);
                                    for (ParseUser user : savedByUsers) {
                                        if (user.getUsername().equals(currentUser.getUsername())) {
                                            savedByUsers.remove(user);
                                        }
                                    }
                                    message.put(Message.SAVED_BY_KEY, savedByUsers);
                                    message.saveInBackground();

                                    messages.remove(message);
                                    notifyDataSetChanged();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    dialog.cancel();
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Are you sure you want to delete this bookmark?").setPositiveButton("Delete", dialogClickListener)
                            .setNegativeButton("Cancel", dialogClickListener).show();

                    message.addUnique(Message.SAVED_BY_KEY, currentUser);
                    message.saveInBackground();

                    return true;
                }
            });
        }
    }

}
