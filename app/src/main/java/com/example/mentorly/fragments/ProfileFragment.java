package com.example.mentorly.fragments;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.mentorly.ChatAdapter;
import com.example.mentorly.GlideApp;
import com.example.mentorly.R;
import com.example.mentorly.models.PairRequest;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class ProfileFragment extends Fragment implements EditMentorDialogFragment.EditMentorDialogListener {

    public static final String TAG = "ProfileFragment";
    public static final String IS_PAIRED_KEY = "isPaired";

    ImageView ivProfileImage;
    TextView tvUsername;
    ImageView ivPairProfileImage;
    TextView tvPairUsername;
    Button btnAddPairPartner;
    Button btnDeletePairPartner;

    ParseUser user;
    ParseUser pairPartner;
    boolean hasPendingRequests;

    public ProfileFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvUsername = view.findViewById(R.id.tvUsername);
        ivPairProfileImage = view.findViewById(R.id.ivPairProfileImage);
        tvPairUsername = view.findViewById(R.id.tvPairUsername);
        btnAddPairPartner = view.findViewById(R.id.btnAddPairPartner);
        btnDeletePairPartner = view.findViewById(R.id.btnDeletePairPartner);

        user = ParseUser.getCurrentUser();

        //Load current user info into the view
        tvUsername.setText(user.getUsername());
        ParseFile profileImage = user.getParseFile(ChatAdapter.PROFILE_IMAGE_KEY);
        if (profileImage != null) {
            GlideApp.with(getContext())
                    .load(profileImage.getUrl())
                    .transform(new RoundedCornersTransformation(50, 20))
                    .into(ivProfileImage);
        } else {
            ivProfileImage.setImageResource(R.drawable.ic_baseline_person_24);
        }

        // fetch all the fields of the current user, including pairPartner
        findPartnerId();

        // If the user has a pending request, set their pairPartner to null
        checkIfPairRequestPending(pairPartner);

        // TODO: send a Parse query to check if the current user has any pending requests to accept

        // If current user is paired with no pending request, load their partner info
        if (pairPartner != null && !hasPendingRequests) {
            btnAddPairPartner.setVisibility(View.GONE);


            tvPairUsername.setText(pairPartner.getUsername());
            ParseFile pairProfileImage = pairPartner.getParseFile(ChatAdapter.PROFILE_IMAGE_KEY);
            if (pairProfileImage != null) {
                GlideApp.with(getContext())
                        .load(pairProfileImage.getUrl())
                        .transform(new RoundedCornersTransformation(50, 20))
                        .into(ivPairProfileImage);
            } else {
                ivPairProfileImage.setImageResource(R.drawable.ic_baseline_person_24);
            }

            btnDeletePairPartner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deletePairing();
                }
            });
        }

        // If current user has a pending request, load "request pending" into text view and hide add/delete buttons
        else if (hasPendingRequests) {
            ivPairProfileImage.setVisibility(View.GONE);
            btnAddPairPartner.setVisibility(View.GONE);
            btnDeletePairPartner.setVisibility(View.GONE);

            tvPairUsername.setText("Your request is currently pending...");

        }

        // Show only the addPair button if the current user has no mentor assigned & no pending requests
        else {
            ivPairProfileImage.setVisibility(View.GONE);
            tvPairUsername.setVisibility(View.GONE);
            btnDeletePairPartner.setVisibility(View.GONE);

            btnAddPairPartner.setVisibility(View.VISIBLE);


            // On button click, go to dialog for entering another user's username
            btnAddPairPartner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showAlertDialog();
                }
            });
        }

    }

    // Query requests where userSending is currentUser && userReceiving is the pairingPartner
    private void checkIfPairRequestPending(final ParseUser pairingPartner) {
        if (pairingPartner != null) {
            ParseQuery<PairRequest> pairRequestQuery = ParseQuery.getQuery(PairRequest.class);
            pairRequestQuery.include(PairRequest.USER_SENDING_KEY);
            pairRequestQuery.include(PairRequest.USER_RECEIVING_KEY);
            pairRequestQuery.whereEqualTo(PairRequest.USER_SENDING_KEY, user);
            pairRequestQuery.whereEqualTo(PairRequest.USER_RECEIVING_KEY, pairingPartner);

            try {
                ArrayList pairRequests = (ArrayList) pairRequestQuery.find();
                if (!pairRequests.isEmpty()) {
                    pairPartner = null;
                    hasPendingRequests = true;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlertDialog() {
        FragmentManager fm = getParentFragmentManager();
        EditMentorDialogFragment editNameDialogFragment = EditMentorDialogFragment.newInstance("Enter mentor name");
        // SETS the target fragment for use later when sending results
        editNameDialogFragment.setTargetFragment(ProfileFragment.this, 300);
        editNameDialogFragment.show(fm, "fragment_edit_name");
    }

    private void findPartnerId() {
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

    // Retrieve input username and send request to Parse to attempt adding user
    @Override
    public void onFinishEditDialog(final String pairUsername) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.include(IS_PAIRED_KEY);
        query.whereEqualTo(ChatFragment.USERNAME_KEY, pairUsername);

        // Find user with unique username
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> users, ParseException e) {
                if (!users.isEmpty()) {
                    // If a user with the username is found, check if they're already paired
                    boolean usernamePaired = (boolean) users.get(0).get(IS_PAIRED_KEY);

                    // If user is already paired, show Toast saying that pairing failed
                    if (usernamePaired) {
                        Toast.makeText(getContext(), "Pairing failed, " + pairUsername + " is already paired", Toast.LENGTH_SHORT).show();
                    }

                    // Else send a pair request and refresh the fragment to show request pending
                    else {
                        sendPairRequest(users.get(0));

                        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                        if (Build.VERSION.SDK_INT >= 26) {
                            ft.setReorderingAllowed(false);
                        }
                        ft.detach(ProfileFragment.this).attach(ProfileFragment.this).commit();
                    }
                }

                // Show toast error message if the user can't be found
                else {
                    Toast.makeText(getContext(), "Sorry, couldn't find user " + pairUsername, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Create a pairing relationship between current user and pairingUpPartner
    private void sendPairRequest(ParseUser pairingUpPartner) {
        PairRequest newRequest = new PairRequest();
        newRequest.put(PairRequest.USER_SENDING_KEY, ParseUser.getCurrentUser());
        newRequest.put(PairRequest.USER_RECEIVING_KEY, pairingUpPartner);
        newRequest.saveInBackground();
        Toast.makeText(getContext(), "Request sent!", Toast.LENGTH_SHORT).show();

        /* Set the current user's pair partner to their pairRequest;
          but unless the pairRequest is accepted, this info won't show up in profileFragment */
        user.put(ChatFragment.CHAT_PAIR_KEY, pairingUpPartner);
        user.put(IS_PAIRED_KEY, true);
        user.saveInBackground();
    }

    // Method for deleting a pairing relationship and refresh the fragment
    // TODO: add confirmation dialog popup before deleting
    private void deletePairing() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        currentUser.remove(ChatFragment.CHAT_PAIR_KEY);
        currentUser.remove(IS_PAIRED_KEY);
        currentUser.saveInBackground();

        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        if (Build.VERSION.SDK_INT >= 26) {
            ft.setReorderingAllowed(false);
        }
        ft.detach(ProfileFragment.this).attach(ProfileFragment.this).commit();
    }
}