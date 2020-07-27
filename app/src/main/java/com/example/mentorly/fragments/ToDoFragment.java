package com.example.mentorly.fragments;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentorly.R;
import com.example.mentorly.ToDoAdapter;
import com.example.mentorly.models.ToDoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class ToDoFragment extends Fragment implements AddToDoDialogFragment.AddToDoDialogListener {

    public static final String TAG = "ToDoFragment";
    List<ToDoItem> items;
    RecyclerView rvItems;
    FloatingActionButton btnAdd;
    ToDoAdapter adapter;
    ParseUser currentUser;
    ParseUser pairPartner;

    public ToDoFragment() {
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
        return inflater.inflate(R.layout.fragment_to_do, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        currentUser = ParseUser.getCurrentUser();
        findPartnerId();

        items = new ArrayList<>();
        rvItems = view.findViewById(R.id.rvItems);
        btnAdd = view.findViewById(R.id.btnAddToDo);


        // Set up the To Do item adapter
        adapter = new ToDoAdapter(getContext(), items);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        rvItems.setLayoutManager(linearLayoutManager);
        rvItems.setAdapter(adapter);

        refreshToDoItems();

        // set onClick listener on add button, bring up dialog to check for adding a to do item
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialog();
            }
        });

        animateToDoActionButton();
    }

    // Spinning animation when To Do fragment is created
    private void animateToDoActionButton() {
        btnAdd.animate()
                .rotationBy(180)
                .setDuration(100)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        btnAdd.setImageResource(R.drawable.ic_baseline_post_add_24);   // setting other icon
                        //Shrink Animation
                        btnAdd.animate()
                                .rotationBy(180)   //Complete the rest of the rotation
                                .setDuration(100)
                                .scaleX(1)              //Scaling back to what it was
                                .scaleY(1)
                                .start();

                    }
                })
                .start();
    }

    // Query To Do items from Parse where current user is included
    private void refreshToDoItems() {
        ParseQuery<ToDoItem> query = ParseQuery.getQuery(ToDoItem.class);

        query.whereEqualTo(ToDoItem.USERS_KEY, ParseUser.getCurrentUser());

        query.findInBackground(new FindCallback<ToDoItem>() {
            @Override
            public void done(List<ToDoItem> objects, ParseException e) {
                if (e == null) {
                    items.clear();
                    items.addAll(objects);
                    adapter.notifyDataSetChanged();
                }
                else {
                    Log.i(TAG, "Error retrieving ToDo items" + e);
                }
            }
        });
    }

    private void showAlertDialog() {
        FragmentManager fm = getParentFragmentManager();
        AddToDoDialogFragment editNameDialogFragment = AddToDoDialogFragment.newInstance("Enter mentor name");
        // SETS the target fragment for use later when sending results
        editNameDialogFragment.setTargetFragment(ToDoFragment.this, 300);
        editNameDialogFragment.show(fm, "fragment_edit_name");
    }

    @Override
    public void onFinishEditDialog(String title, String body) {
        // Create a new To Do item and saveInBackground to ParseServer
        ToDoItem newItem = new ToDoItem();
        newItem.setTitle(title);
        if (body != null && !body.equals("")) {
            newItem.setBody(body);
        }

        List<ParseUser> users = new ArrayList<>();
        users.add(currentUser);
        if (pairPartner != null) {
            users.add(pairPartner);
        }

        newItem.setUsers(users);

        newItem.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                // Refresh the fragment to display the new item
                FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                if (Build.VERSION.SDK_INT >= 26) {
                    ft.setReorderingAllowed(false);
                }
                ft.detach(ToDoFragment.this).attach(ToDoFragment.this).commit();
            }
        });

    }

    // Fetch all of the fields of current user data before loading the profile fragment
    private void findPartnerId() {
        try {
            currentUser.fetch();
            ParseUser mentor = currentUser.getParseUser(ChatFragment.CHAT_PAIR_KEY);
            pairPartner = mentor;

            // retrieve pairPartner data if it exists
            if (mentor != null) {
                mentor.fetch();
            } else {
                Log.i(TAG, "No mentor found for " + currentUser.getUsername());
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}