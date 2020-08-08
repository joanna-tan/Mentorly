package com.example.mentorly.fragments;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.request.RequestOptions;
import com.example.mentorly.ChatAdapter;
import com.example.mentorly.GlideApp;
import com.example.mentorly.R;
import com.example.mentorly.models.PairRequest;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.livequery.ParseLiveQueryClient;
import com.parse.livequery.SubscriptionHandling;

import java.io.File;
import java.util.ArrayList;

public class ProfileFragment extends Fragment implements AddPictureDialog.AddPictureDialogListener, EditMentorDialogFragment.EditMentorDialogListener {

    public static final String TAG = "ProfileFragment";
    public static final String IS_PAIRED_KEY = "isPaired";
    public static final String PROFILE_IMAGE_KEY = "profileImage";

    // views
    ImageView ivProfileImage;
    TextView tvUsername;
    ImageView ivPairProfileImage;
    TextView tvPairUsername;
    Button btnAddPairPartner;
    Button btnDeletePairPartner;
    Button btnAcceptRequest;
    Button btnRejectRequest;
    TextView tvPairProfile;
    ImageButton btnChangeProfilePic;

    // handle user info and pairing info
    ParseUser user;
    ParseUser pairPartner;
    boolean hasPendingRequests;
    PairRequest pendingAcceptRequests;
    PairRequest pendingSentRequests;

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

        Toolbar toolbar = getActivity().findViewById(R.id.toolbar_main);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().show();
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Get access to the custom title view
        TextView mTitle = toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("Profile");

        ivProfileImage = view.findViewById(R.id.ivBookmarkProfileImage);
        tvUsername = view.findViewById(R.id.tvUsername);
        ivPairProfileImage = view.findViewById(R.id.ivPairProfileImage);
        tvPairUsername = view.findViewById(R.id.tvPairUsername);
        btnAddPairPartner = view.findViewById(R.id.btnAddPairPartner);
        btnDeletePairPartner = view.findViewById(R.id.btnDeletePairPartner);
        btnAcceptRequest = view.findViewById(R.id.btnAddRequest);
        btnRejectRequest = view.findViewById(R.id.btnRejectRequest);
        tvPairProfile = view.findViewById(R.id.tvPairProfile);
        btnChangeProfilePic = view.findViewById(R.id.btnChangeProfilePic);

        user = ParseUser.getCurrentUser();

        //Load current user info into the view
        tvUsername.setText(user.getUsername());
        ParseFile profileImage = user.getParseFile(ChatAdapter.PROFILE_IMAGE_KEY);
        if (profileImage != null) {
            GlideApp.with(getContext())
                    .load(profileImage.getUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivProfileImage);
        } else {
            ivProfileImage.setImageResource(R.drawable.ic_baseline_person_24);
        }

        // bring up UI for user to change their picture
        btnChangeProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch picture dialog to display image preview after camera is clicked
                showAddPictureDialog();
            }
        });

        // fetch all the fields of the current user, including pairPartner
        findPartnerId();

        // If the user has a pending request, set their pairPartner to null
        checkIfPairRequestPending(pairPartner);

        // If current user is paired with no pending request, load their partner info
        if (pairPartner != null && !hasPendingRequests) {
            btnAddPairPartner.setVisibility(View.GONE);
            btnAcceptRequest.setVisibility(View.GONE);
            btnRejectRequest.setVisibility(View.GONE);

            tvPairUsername.setText(pairPartner.getUsername());
            ParseFile pairProfileImage = pairPartner.getParseFile(ChatAdapter.PROFILE_IMAGE_KEY);
            if (pairProfileImage != null) {
                GlideApp.with(getContext())
                        .load(pairProfileImage.getUrl())
                        .apply(RequestOptions.circleCropTransform())
                        .into(ivPairProfileImage);
            } else {
                ivPairProfileImage.setImageResource(R.drawable.ic_baseline_person_24);
            }

            btnDeletePairPartner.setVisibility(View.GONE);
            btnDeletePairPartner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deletePairing();
                }
            });
        }

        // If current user has a pending request, load "request pending" into text view and hide add/delete buttons
        else if (hasPendingRequests && pendingAcceptRequests == null) {
            ivPairProfileImage.setVisibility(View.GONE);
            btnAddPairPartner.setVisibility(View.GONE);
            btnAcceptRequest.setVisibility(View.GONE);
            btnRejectRequest.setVisibility(View.GONE);

            tvPairUsername.setText("Your request is currently pending...");
            btnDeletePairPartner.setVisibility(View.VISIBLE);
            btnDeletePairPartner.setText("Delete pair request");

            btnDeletePairPartner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pendingSentRequests.deleteInBackground();
                    hasPendingRequests = false;
                    pendingSentRequests.saveInBackground();

                    user.put(IS_PAIRED_KEY, false);
                    user.remove(ChatFragment.CHAT_PAIR_KEY);
                    user.saveInBackground();

                    refreshFragment();
                }
            });

        }

        // If current user has a valid pending request, display all requests for action
        else if (hasPendingRequests) {
            tvPairProfile.setText("Pending requests:");
            btnAddPairPartner.setVisibility(View.GONE);
            btnDeletePairPartner.setVisibility(View.GONE);
            btnAcceptRequest.setVisibility(View.VISIBLE);
            btnRejectRequest.setVisibility(View.VISIBLE);

            ParseUser sendingUser = pendingAcceptRequests.getUserSending();
            tvPairUsername.setText(sendingUser.getUsername());

            ParseFile sendingUserProfileImage = sendingUser.getParseFile(ChatAdapter.PROFILE_IMAGE_KEY);

            if (sendingUserProfileImage != null) {
                GlideApp.with(getContext())
                        .load(sendingUserProfileImage.getUrl())
                        .apply(RequestOptions.circleCropTransform())
                        .into(ivPairProfileImage);
            } else {
                ivPairProfileImage.setImageResource(R.drawable.ic_baseline_person_24);
            }


            // when yes is clicked, refresh the fragment and validate the request
            btnAcceptRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ParseUser userSending = pendingAcceptRequests.getUserSending();
                    user.put(ChatFragment.CHAT_PAIR_KEY, userSending);
                    user.put(IS_PAIRED_KEY, true);
                    user.saveInBackground();
                    hasPendingRequests = false;

                    pendingAcceptRequests.setIsAccepted(true);
                    pendingAcceptRequests.saveInBackground();

                    refreshFragment();
                }
            });

            // when no is clicked, set requestRejected to true and refresh the fragment to display add button
            btnRejectRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pendingAcceptRequests.setIsRejected(true);
                    pendingAcceptRequests.saveInBackground();

                    refreshFragment();
                }
            });
        }

        // Show only the addPair button if the current user has no mentor assigned & no pending requests
        else {
            ivPairProfileImage.setVisibility(View.GONE);
            tvPairUsername.setVisibility(View.GONE);
            btnDeletePairPartner.setVisibility(View.GONE);
            btnAcceptRequest.setVisibility(View.GONE);
            btnRejectRequest.setVisibility(View.GONE);

            btnAddPairPartner.setVisibility(View.VISIBLE);


            // On button click, go to dialog for entering another user's username
            btnAddPairPartner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showAddMentorDialog();
                }
            });
        }

        ParseLiveQueryClient parseLiveQueryClient = ParseLiveQueryClient.Factory.getClient();

        ParseQuery<PairRequest> parseQuery = ParseQuery.getQuery(PairRequest.class);

        // Connect to Parse server
        SubscriptionHandling<PairRequest> subscriptionHandling = parseLiveQueryClient.subscribe(parseQuery);
        // Listen for CREATE events
        subscriptionHandling.handleEvent(SubscriptionHandling.Event.CREATE, new
                SubscriptionHandling.HandleEventCallback<PairRequest>() {
                    @Override
                    public void onEvent(ParseQuery<PairRequest> query, PairRequest object) {
                        refreshFragment();
                    }
                });

        SubscriptionHandling<PairRequest> subscriptionHandlingUpdate = parseLiveQueryClient.subscribe(parseQuery);
        // Listen for UPDATE events
        subscriptionHandlingUpdate.handleEvent(SubscriptionHandling.Event.UPDATE, new
                SubscriptionHandling.HandleEventCallback<PairRequest>() {
                    @Override
                    public void onEvent(ParseQuery<PairRequest> query, PairRequest object) {
                        refreshFragment();
                    }
                });

        SubscriptionHandling<PairRequest> subscriptionHandlingDelete = parseLiveQueryClient.subscribe(parseQuery);
        // Listen for UPDATE events
        subscriptionHandlingDelete.handleEvent(SubscriptionHandling.Event.DELETE, new
                SubscriptionHandling.HandleEventCallback<PairRequest>() {
                    @Override
                    public void onEvent(ParseQuery<PairRequest> query, PairRequest object) {
                        refreshFragment();
                    }
                });
    }

    // Bring up the add picture fragment
    private void showAddPictureDialog() {
        FragmentManager fm = getParentFragmentManager();
        AddPictureDialog editNameDialogFragment = AddPictureDialog.newInstance("Add picture");
        // Sets the profile fragment for use later when sending photo
        editNameDialogFragment.setTargetFragment(ProfileFragment.this, 300);
        editNameDialogFragment.show(fm, "fragment_edit_picture");
    }


    // Retrieve the photo file from the addPictureDialog and save it as as the current user's image
    @Override
    public void onFinishAddPictureDialog(File photo) {
        user.put(PROFILE_IMAGE_KEY, new ParseFile(photo));
        try {
            user.save();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        refreshFragment();
    }

    private void checkIfPairRequestPending(final ParseUser pairingPartner) {
        // First, query requests where userSending is currentUser && userReceiving is the pairingPartner
        if (pairingPartner != null) {
            ParseQuery<PairRequest> pairRequestQuery = ParseQuery.getQuery(PairRequest.class);
            pairRequestQuery.include(PairRequest.USER_SENDING_KEY);
            pairRequestQuery.include(PairRequest.USER_RECEIVING_KEY);
            pairRequestQuery.whereEqualTo(PairRequest.USER_SENDING_KEY, user);
            pairRequestQuery.whereEqualTo(PairRequest.USER_RECEIVING_KEY, pairingPartner);

            try {
                ArrayList pairRequests = (ArrayList) pairRequestQuery.find();
                if (!pairRequests.isEmpty()) {
                    pendingSentRequests = (PairRequest) pairRequests.get(0);

                    // if the request has been rejected, delete the request & reset the user's info
                    if (pendingSentRequests.getIsRejected()){
                        user.put(IS_PAIRED_KEY, false);
                        user.remove(ChatFragment.CHAT_PAIR_KEY);
                        user.saveInBackground();

                        pairPartner = null;
                        pendingSentRequests.deleteInBackground();
                    }

                    // if the request has been accepted, delete the request and set the user fields as appropriate
                    else if (pendingSentRequests.getIsAccepted()) {
                        pendingSentRequests.deleteInBackground();
                    }

                    // if the request is neither accepted or rejected, show it as pending
                    else {
                        pairPartner = null;
                        hasPendingRequests = true;
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // if the pairingPartner is null, do a query to search for any pending requests to accept
        else {
            ParseQuery<PairRequest> acceptRequestQuery = ParseQuery.getQuery(PairRequest.class);
            acceptRequestQuery.include(PairRequest.USER_SENDING_KEY);
            acceptRequestQuery.include(PairRequest.USER_RECEIVING_KEY);
            acceptRequestQuery.whereEqualTo(PairRequest.USER_RECEIVING_KEY, user);

            try {
                // there might a potential bug where multiple requests are sent to the same user
                // for now, only retrieve the first request for action
                PairRequest pairRequest = acceptRequestQuery.getFirst();
                if (pairRequest != null) {
                    pendingAcceptRequests = pairRequest;
                    hasPendingRequests = !pairRequest.getIsRejected();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    // Set a pair request between current user and pairingUpPartner
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


    // Fetch all of the fields of current user data before loading the profile fragment
    private void findPartnerId() {
        try {
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

    // Show a dialog for inputting a pairing username
    private void showAddMentorDialog() {
        FragmentManager fm = getParentFragmentManager();
        EditMentorDialogFragment editNameDialogFragment = EditMentorDialogFragment.newInstance("Enter mentor name");
        // Sets the profile fragment as target for sending results
        editNameDialogFragment.setTargetFragment(ProfileFragment.this, 300);
        editNameDialogFragment.show(fm, "fragment_edit_name");
    }

    // Retrieve input username from the dialog and send request to Parse to attempt adding user
    @Override
    public void onFinishEditMentorDialog(final String pairUsername) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(ChatFragment.USERNAME_KEY, pairUsername);
        query.include(IS_PAIRED_KEY);

        // Find the user with the input unique username
        query.getFirstInBackground(new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser object, ParseException e) {
                if (object != null) {
                    // If a user with the username is found, check if they're already paired
                    boolean usernamePaired = (boolean) object.get(IS_PAIRED_KEY);
                    // If user is already paired, show Toast saying that pairing failed
                    if (usernamePaired) {
                        Toast.makeText(getContext(), "Pairing failed, " + pairUsername + " is already paired", Toast.LENGTH_SHORT).show();
                    }
                    // Else send a pair request and refresh the fragment to show request pending
                    else {
                        sendPairRequest(object);
                        refreshFragment();
                    }
                }
                // Show toast error message if the user can't be found
                else {
                    Toast.makeText(getContext(), "Sorry, couldn't find user " + pairUsername, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Method for deleting a pairing relationship
    private void deletePairing() {
        user.remove(ChatFragment.CHAT_PAIR_KEY);
        user.put(IS_PAIRED_KEY, false);
        user.saveInBackground();
        refreshFragment();
    }

    // Method to refresh the fragment view when an action has been performed
    private void refreshFragment() {
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        if (Build.VERSION.SDK_INT >= 26) {
            ft.setReorderingAllowed(false);
        }
        ft.detach(ProfileFragment.this).attach(ProfileFragment.this).commit();
    }
}