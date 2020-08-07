package com.example.mentorly.fragments;

import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentorly.R;
import com.example.mentorly.ToDoAdapter;
import com.example.mentorly.models.ToDoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class ToDoFragment extends Fragment implements AddToDoDialogFragment.AddToDoDialogListener {

    public static final String TAG = "ToDoFragment";
    TextView tvNoToDos;
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
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the toolbar text & remove default text
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar_main);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().show();
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Get access to the custom title view
        TextView mTitle = toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("Objectives");


        // Retrieve user info
        currentUser = ParseUser.getCurrentUser();
        findPartnerId();

        // Set up the views for to-do list
        items = new ArrayList<>();
        rvItems = view.findViewById(R.id.rvItems);
        btnAdd = view.findViewById(R.id.btnAddToDo);
        tvNoToDos = view.findViewById(R.id.tvNoToDoItems);

        // Set up the To Do item adapter
        adapter = new ToDoAdapter(getContext(), items);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        rvItems.setLayoutManager(linearLayoutManager);
        rvItems.setAdapter(adapter);

        // Query To Do items for the current user
        refreshToDoItems();

        // set onClick listener on add button, bring up dialog to check for adding a to do item
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialog();
            }
        });

        animateToDoActionButton();

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final int itemPosition = viewHolder.getAdapterPosition();
                ToDoItem item = items.get(itemPosition);

                // Save the item in a new temp variable in case user restores item
                final ToDoItem newItem = new ToDoItem();
                newItem.setTitle(item.getTitle());
                if (item.getBody() != null) {
                    newItem.setBody(item.getBody());
                }
                newItem.setDueDate(item.getDueDate());
                newItem.setUsers(item.getUsers());

                // Delete the swiped item
                item.deleteInBackground();
                viewHolder.getAdapterPosition();
                items.remove(viewHolder.getAdapterPosition());
                adapter.notifyDataSetChanged();

                // show a Snackbar for the user to un-delete the To Do item
                Snackbar.make(view.findViewById(R.id.btnAddToDo), "Deleted ToDo item", Snackbar.LENGTH_SHORT).
                        setAnchorView(view.findViewById(R.id.bottomNavigation))
                        .setAction("Undo", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                    newItem.saveInBackground();
                                    items.add(itemPosition, newItem);
                                    adapter.notifyDataSetChanged();
                            }
                        })
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_green_done))
                        .addActionIcon(R.drawable.ic_baseline_check_24)
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rvItems);
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
        query.orderByAscending(ToDoItem.DUE_DATE_KEY);

        query.findInBackground(new FindCallback<ToDoItem>() {
            @Override
            public void done(List<ToDoItem> objects, ParseException e) {
                if (e == null) {
                    items.clear();
                    items.addAll(objects);

                    // If items list is empty, show text view to add some items
                    if (items.isEmpty()) {
                        tvNoToDos.setVisibility(View.VISIBLE);
                    } else {
                        tvNoToDos.setVisibility(View.GONE);
                    }

                    adapter.notifyDataSetChanged();
                } else {
                    Log.i(TAG, "Error retrieving ToDo items" + e);
                }
            }
        });
    }

    private void showAlertDialog() {
        FragmentManager fm = getParentFragmentManager();
        AddToDoDialogFragment addToDoDialog = AddToDoDialogFragment.newInstance("Enter mentor name");
        // Sets the to do fragment for use when dialog is finished
        addToDoDialog.setTargetFragment(ToDoFragment.this, 300);
        addToDoDialog.show(fm, "fragment_edit_name");
    }

    @Override
    public void onFinishEditDialog(String title, String body, Date dueDate) {
        // Create a new To Do item and saveInBackground to ParseServer
        ToDoItem newItem = new ToDoItem();
        newItem.setTitle(title);
        if (body != null && !body.equals("")) {
            newItem.setBody(body);
        }

        newItem.setDueDate(dueDate);

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