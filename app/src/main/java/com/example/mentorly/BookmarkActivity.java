package com.example.mentorly;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentorly.fragments.ChatFragment;
import com.example.mentorly.models.Message;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class BookmarkActivity extends AppCompatActivity {

    public static final String TAG = "BookmarkActivity";
    List<Message> allBookmarks;
    BookmarkAdapter adapter;
    RecyclerView rvBookmarks;
    TextView tvNoBookmarks;

    ParseUser pairPartner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = findViewById(R.id.toolbar_bookmark);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Get access to the custom title view
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView mTitle = toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("Bookmarks");

        // Initialize the fields and buttons
        allBookmarks = new ArrayList<>();
        rvBookmarks = findViewById(R.id.rvBookmarks);
        adapter = new BookmarkAdapter(this, allBookmarks);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvBookmarks.setLayoutManager(linearLayoutManager);
        rvBookmarks.setAdapter(adapter);

        //retrieve mentoring pair as a ParseUser
        findPartnerId();

        // If current user has no mentor, show text view and hide chat layout
        tvNoBookmarks = findViewById(R.id.tvNoBookmarks);
        if (pairPartner != null) {
            refreshMessages();
            // if empty set default message visible, else set it gone
            tvNoBookmarks.setVisibility(allBookmarks.isEmpty() ? View.GONE : View.VISIBLE );

        } else {
            rvBookmarks.setVisibility(View.GONE);
            tvNoBookmarks.setVisibility(View.VISIBLE);
        }
    }

    // Show saved messages
    private void refreshMessages() {
        // Construct query to execute
        ParseQuery<Message> query = ParseQuery.getQuery(Message.class);
        // Configure limit and sort order
        query.include(Message.FROM_ID_KEY);
        query.include(Message.TO_ID_KEY);

        // Query messages which have been saved by the current user & set query params
        ArrayList<ParseUser> users = new ArrayList<>();
        users.add(ParseUser.getCurrentUser());
        users.add(pairPartner);
        query.whereContainedIn(Message.FROM_ID_KEY, users);
        query.whereContainedIn(Message.TO_ID_KEY, users);
        query.whereEqualTo(Message.SAVED_BY_KEY, ParseUser.getCurrentUser());
        query.orderByAscending("createdAt");

        query.findInBackground(new FindCallback<Message>() {
            @Override
            public void done(List<Message> messages, ParseException e) {
                if (e == null) {
                    allBookmarks.clear();
                    allBookmarks.addAll(messages);
                    adapter.notifyDataSetChanged(); //update adapter
                } else {
                    Log.e(TAG, "Error retrieving bookmarks: " + e);
                }
            }
        });
    }

    private void findPartnerId() {
        ParseUser user = ParseUser.getCurrentUser();
        try {
            // fetch all fields of user data in sync task
            user.fetch();
            ParseUser mentor = user.getParseUser(ChatFragment.CHAT_PAIR_KEY);
            pairPartner = mentor;

            // retrieve pairPartner data if it exists
            if (mentor != null) {
                mentor.fetch();
            } else {
                Log.i(TAG, "No mentor found for " + user.getUsername());
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}