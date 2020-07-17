package com.example.mentorly.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentorly.ChatAdapter;
import com.example.mentorly.R;
import com.example.mentorly.models.Message;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    static final String TAG = "ChatFragment";
    static final int MAX_CHAT_MESSAGES_TO_SHOW = 50;
    public static final String CHAT_PAIR_KEY = "pairID";
    public static final String USERNAME_KEY = "username";

    List<Message> allMessages;
    ChatAdapter adapter;
    RecyclerView rvChat;
    EditText etMessage;
    Button btnSend;
    TextView tvNoPair;

    String pairUsername;
    String pairId;
    ParseUser pairPartner;

    // Keep track of initial load to scroll to the bottom of the ListView
    boolean mFirstLoad;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //retrieve mentoring pair as a ParseUser
        findPartnerId();

        // If current user has no mentor, show text view and hide chat layout
        tvNoPair = view.findViewById(R.id.tvNoPair);
        if (pairPartner != null) {
            setupMessagePosting(view);
            tvNoPair.setVisibility(View.GONE);
        } else {
            RelativeLayout chatLayout = view.findViewById(R.id.rlChatLayout);
            chatLayout.setVisibility(View.GONE);
        }
    }

    //set up event handler which posts user message to Parse
    private void setupMessagePosting(View view) {
        // Initialize the fields and buttons
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btSend);
        allMessages = new ArrayList<>();
        mFirstLoad = true;
        rvChat = view.findViewById(R.id.rvChat);

        adapter = new ChatAdapter(getContext(), ParseUser.getCurrentUser().getObjectId(), allMessages);

        // associate the LayoutManger with the Recycler View
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);

        rvChat.setLayoutManager(linearLayoutManager);
        rvChat.setAdapter(adapter);

        refreshMessages();

        // set on click listener on send button to create message on Parse
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = etMessage.getText().toString();
                if (data.isEmpty()) {
                    Toast.makeText(getContext(), "Message cannot be empty!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Message message = new Message();
                message.setBody(data);
                message.setFromId(ParseUser.getCurrentUser());
                message.setToId(pairPartner);

                message.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Toast.makeText(getContext(), "Successfully created message on Parse", Toast.LENGTH_SHORT).show();
                        refreshMessages();
                    }
                });
                etMessage.setText(null);
            }
        });
    }

    // Query messages from Parse so we can load them into the chat adapter
    // Retrieve messages where fromId == currentUser, toId == mentor
    //      AND messages where fromId == mentor, toId == currentUser
    private void refreshMessages() {
        // Construct query to execute
        ParseQuery<Message> query = ParseQuery.getQuery(Message.class);
        // Configure limit and sort order
        query.setLimit(MAX_CHAT_MESSAGES_TO_SHOW);
        query.include(Message.FROM_ID_KEY);
        query.include(Message.TO_ID_KEY);

        // Check if the messages are sent to/from the current user and their pair partner
        ArrayList<ParseUser> users = new ArrayList<>();
        users.add(ParseUser.getCurrentUser());
        users.add(pairPartner);
        query.whereContainedIn(Message.FROM_ID_KEY, users);
        query.whereContainedIn(Message.TO_ID_KEY, users);

        // get the latest 50 messages, order will show up newest to oldest of this group
        query.orderByDescending("createdAt");

        query.findInBackground(new FindCallback<Message>() {
            @Override
            public void done(List<Message> messages, ParseException e) {
                if (e == null) {
                    allMessages.clear();
                    allMessages.addAll(messages);
                    adapter.notifyDataSetChanged(); //update adapter
                    // Scroll to the bottom of the list on initial load
                    if (mFirstLoad) {
                        rvChat.scrollToPosition(0);
                        mFirstLoad = false;
                    }
                } else {
                    Log.e(TAG, "Error retrieving messages: " + e);
                }
            }
        });
    }

    private void findPartnerId() {
        ParseUser user = ParseUser.getCurrentUser();
        try {
            // fetch all fields of user data in sync task
            user.fetch();
            ParseUser mentor = user.getParseUser(CHAT_PAIR_KEY);
            pairPartner = mentor;

            // retrieve pairPartner data if it exists
            if (mentor != null) {
                mentor.fetch();
                pairId = pairPartner.getObjectId();
                pairUsername = pairPartner.getUsername();
            }
            else {
                Log.i(TAG, "No mentor found for " + user.getUsername());
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}