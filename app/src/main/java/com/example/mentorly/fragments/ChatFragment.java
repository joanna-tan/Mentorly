package com.example.mentorly.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    static final String TAG = "ChatFragment";
    static final int MAX_CHAT_MESSAGES_TO_SHOW = 50;

    List<Message> allMessages;
    ChatAdapter adapter;
    RecyclerView rvChat;
    EditText etMessage;
    Button btnSend;

    ArrayList<String> ids;

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
        setupMessagePosting(view);
    }

    //set up event handler which posts user message to Parse
    private void setupMessagePosting(View view) {
        final String toId;
        // Initialize the fields and buttons
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btSend);
        allMessages = new ArrayList<>();
        mFirstLoad = true;
        rvChat = view.findViewById(R.id.rvChat);

        findPartnerId();

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
                Message message = new Message();
                message.setBody(data);
                message.setFromId(ParseUser.getCurrentUser());
                //message.setToId(allUsers.get(0).getObjectId());

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
    private void refreshMessages() {
        // Construct query to execute
        ParseQuery<Message> query = ParseQuery.getQuery(Message.class);
        // Configure limit and sort order
        query.setLimit(MAX_CHAT_MESSAGES_TO_SHOW);
        query.include(Message.FROM_ID_KEY);
        query.include(Message.TO_ID_KEY);

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
                }
                else {
                    Log.e(TAG, "Error retrieving messages: " + e);
                }
            }
        });
    }

    //TODO: Retrieve pointer to current user's pairId
    private void findPartnerId() {
        // fetch Messages where toId == ParseUser with object id XXX
        ParseQuery query = new ParseQuery<>("Message")
                .whereEqualTo("toId", ParseObject.createWithoutData(ParseUser.class, ParseUser.getCurrentUser().getObjectId()));
        query.findInBackground(new FindCallback() {
            @Override
            public void done(List objects, ParseException e) {

            }

            @Override
            public void done(Object o, Throwable throwable) {

            }
        });

        ParseQuery<ParseUser> parseUserParseQuery = ParseUser.getQuery();
        parseUserParseQuery.include("pairID");
        ids = new ArrayList<>();
        parseUserParseQuery.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> pairs, ParseException e) {
                if (e == null) {
                    if (pairs.size() > 0) {
                        for (ParseUser user : pairs) {
                            ids.add(user.getObjectId());
                        }
                    }
                }
                else {
                    Log.e(TAG, "Error retrieving pair" + e);
                }
            }
        });
    }

}