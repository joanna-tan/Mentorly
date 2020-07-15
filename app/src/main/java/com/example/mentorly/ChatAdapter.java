package com.example.mentorly;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mentorly.models.Message;
import com.parse.ParseFile;
import com.parse.ParseUser;

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
        //if the message fromId == userId, message is sent by the current user
        final boolean isMe = message.getFromId() != null && message.getFromId().getObjectId().equals(userId);

        if (isMe) {
            holder.imageMe.setVisibility(View.VISIBLE);
            holder.imageOther.setVisibility(View.GONE);
            holder.body.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        } else {
            holder.imageOther.setVisibility(View.VISIBLE);
            holder.imageMe.setVisibility(View.GONE);
            holder.body.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        }

        final ImageView profileView = isMe ? holder.imageMe : holder.imageOther;

        //retrieve the user from the message (aka from ID)
        ParseUser user = message.getFromId();

        //set the profileView image to user.getImage()
        ParseFile profileImage = user.getParseFile(PROFILE_IMAGE_KEY);

        //Load into Glide
        if (profileImage != null) {
            Glide.with(context).load(profileImage.getUrl()).into(profileView);
        }
        else {
            profileView.setImageResource(R.drawable.ic_baseline_person_24);
        }
        holder.body.setText(message.getBody());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageOther;
        ImageView imageMe;
        TextView body;

        public ViewHolder(View itemView) {
            super(itemView);
            imageOther = itemView.findViewById(R.id.ivProfileOther);
            imageMe = itemView.findViewById(R.id.ivProfileMe);
            body = itemView.findViewById(R.id.tvBody);
        }
    }
}
