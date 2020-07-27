package com.example.mentorly.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mentorly.EventsAdapter;
import com.example.mentorly.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.parse.ParseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;


public class CalendarFragment extends Fragment implements AddEventDialogFragment.AddEventDialogFragmentListener {

    // Projection array. Creating indices for this array instead of doing
    // dynamic lookups improves performance.
    public static final String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };

    // The indices for the projection array above.
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;
    private static final int PROJECTION_OWNER_ACCOUNT_INDEX = 3;

    // Check for calendar permissions
    boolean permissionsGranted;

    // Save user logged in info
    private String emailUsername;
    public static final String GOOGLE_MAIL = "com.google";
    private long calendarId;

    public static final String TAG = "CalendarFragment";
    public static final String PUBLIC_EMAIL_KEY = "publicEmail";
    public static final int RC_SIGN_IN = 42;
    public static final int RC_CALENDAR_PERMISSIONS = 412;
    GoogleSignInClient mGoogleSignInClient;

    // Save app user info for retrieving partner email
    ParseUser user;
    ParseUser pairPartner;
    String pairEmail;

    //views
    Button btnSignIn;
    Button mSignOut;
    TextView mFullName;
    ImageView mProfileView;
    FloatingActionButton fabAddEvent;
    ConstraintLayout clProfileView;

    // configure the recycler view
    List<String[]> allEvents;
    RecyclerView rvEvents;
    EventsAdapter adapter;


    public CalendarFragment() {
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
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fetch current user's partner email if needed, when creating event
        try {
            fetchPartnerInfo();
        } catch (com.parse.ParseException e) {
            e.printStackTrace();
        }

        // Configure the layout views
        btnSignIn = view.findViewById(R.id.btnGoogleSignIn);
        mSignOut = view.findViewById(R.id.signOut);
        mFullName = view.findViewById(R.id.fullName);
        mProfileView = view.findViewById(R.id.profileImage);
        rvEvents = view.findViewById(R.id.rvEvents);
        fabAddEvent = view.findViewById(R.id.fabAddEvent);
        clProfileView = view.findViewById(R.id.clProfileView);

        // Set up the event recycler view
        allEvents = new ArrayList<>();
        adapter = new EventsAdapter(getContext(), allEvents);
        rvEvents.setAdapter(adapter);
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id_web))
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/calendar"))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

        // Handle the sign in flow; UI updated if the user is correctly signed in
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        // Add spin animation on floating button when fragment is created
        animateFloatingActionButton();
    }

    // Fetch partner info and partner email (if it exists)
    private void fetchPartnerInfo() throws com.parse.ParseException {
        user = ParseUser.getCurrentUser();
        user.fetchIfNeeded();
        pairPartner = (ParseUser) user.get(ChatFragment.CHAT_PAIR_KEY);
        pairPartner.fetch();
        if (pairPartner != null) {
            pairEmail = (String) pairPartner.get(PUBLIC_EMAIL_KEY);
        }
        Log.i(TAG, "name: " + pairPartner.getUsername() + "\nemail: " + pairEmail);
    }

    private void animateFloatingActionButton() {
        // animate floating button, no check for first load
        fabAddEvent.animate()
                .rotationBy(-180)
                .setDuration(100)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        fabAddEvent.setImageResource(R.drawable.ic_baseline_note_add_24);   // setting other icon
                        //Shrink Animation
                        fabAddEvent.animate()
                                .rotationBy(-180)   //Complete the rest of the rotation
                                .setDuration(100)
                                .scaleX(1)              //Scaling back to what it was
                                .scaleY(1)
                                .start();
                    }
                })
                .start();
    }

    private void checkPermission(int callbackId, String... permissionsId) {
        boolean permissions = true;
        for (String p : permissionsId) {
            permissions = permissions && ContextCompat.checkSelfPermission(getContext(), p) == PERMISSION_GRANTED;
        }
        if (!permissions)
            requestPermissions(permissionsId, callbackId);
        else {
            // set granted to true if yes permissions
            permissionsGranted = permissions;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (String p : permissions) {
            // if one permission is rejected, ask for them all again
            permissionsGranted = ContextCompat.checkSelfPermission(getContext(), p) == PERMISSION_GRANTED;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkCalendars() {
        // Run query
        Cursor cur = null;
        ContentResolver cr = getContext().getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
                + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?) AND ("
                + CalendarContract.Calendars.OWNER_ACCOUNT + " = ?))";
        String[] selectionArgs = new String[]{emailUsername, GOOGLE_MAIL,
                emailUsername};

        // Submit the query and get a Cursor object back.
        cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null);

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            long calID = 0;
            String displayName = null;
            String accountName = null;
            String ownerName = null;

            // Get the field values
            calID = cur.getLong(PROJECTION_ID_INDEX);
            displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
            ownerName = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX);

            checkEvents(calID);
            calendarId = calID;
        }
    }

    private void checkEvents(long calID) {
        Uri.Builder builder = Uri.parse("content://com.android.calendar/instances/when").buildUpon();

        long now = new Date().getTime();

        ContentUris.appendId(builder, now - DateUtils.DAY_IN_MILLIS * 10000);
        ContentUris.appendId(builder, now + DateUtils.DAY_IN_MILLIS * 10000);

        ContentResolver contentResolver = getContext().getContentResolver();

        Cursor eventCursor = contentResolver.query(builder.build(),
                new String[]{"title", "begin", "end", "allDay"}, "CALENDAR_ID=" + calID,
                null, "startDay ASC, startMinute ASC");

        System.out.println("eventCursor count=" + eventCursor.getCount());
        if (eventCursor.getCount() > 0) {
            if (eventCursor.moveToFirst()) {
                allEvents.clear();
                do {
                    final String title = eventCursor.getString(0);
                    final Date begin = new Date(eventCursor.getLong(1));
                    final Date end = new Date(eventCursor.getLong(2));

                    Date currentTime = new Date(System.currentTimeMillis());

                    // if the event hasn't ended yet, show it on the calendar
                    if (end.after(currentTime)) {
                        String beginRelativeTime = getRelativeTimeAgo(begin.toString());
                        String endRelativeTime = getRelativeTimeAgo(end.toString());
                        // Add the event info to the adapter
                        allEvents.add(new String[]{title, beginRelativeTime});
                        adapter.notifyDataSetChanged();
                        Log.i(TAG, "Event title: " + title);
                    }
                }
                while (eventCursor.moveToNext());
            }
        }
    }

    // Format: getRelativeTimeAgo("Mon Apr 01 21:16:23 +0000 2014");
    public static String getRelativeTimeAgo(String rawJsonDate) {
        String dateFormat = "EEE MMM dd HH:mm:ss ZZZZZ yyyy";
        SimpleDateFormat sf = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
        sf.setLenient(true);

        String relativeDate = "";
        try {
            long dateMillis = Objects.requireNonNull(sf.parse(rawJsonDate)).getTime();
            relativeDate = DateUtils.getRelativeTimeSpanString(dateMillis,
                    System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return relativeDate;
    }

    // Method for adding and saving an event to calendar
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addEvent(long calID, String title, String description, int[] startSelected, int[] endSelected) {
        long startMillis = 0;
        long endMillis = 0;
        Calendar beginTime = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            beginTime = Calendar.getInstance();
        }
        beginTime.set(startSelected[0], startSelected[1], startSelected[2], startSelected[3], startSelected[4]);
        startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        endTime.set(endSelected[0], endSelected[1], endSelected[2], endSelected[3], endSelected[4]);
        endMillis = endTime.getTimeInMillis();


        ContentResolver crEvent = getContext().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.TITLE, title);
        if (description != null) {
            values.put(CalendarContract.Events.DESCRIPTION, description);
        }
        values.put(CalendarContract.Events.CALENDAR_ID, calID);
        // Set event time zone to system time zone
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().toString());
        Uri uriEvent = crEvent.insert(CalendarContract.Events.CONTENT_URI, values);

        // get the event ID that is the last element in the Uri
        long eventID = Long.parseLong(uriEvent.getLastPathSegment());
        Log.i(TAG, "eventID" + eventID + " event created");

        addAttendee(eventID);
    }

    private void addAttendee(long eventID) {
        ContentResolver crAttendee = getContext().getContentResolver();
        ContentValues values = new ContentValues();
        if (pairPartner == null) {
            Snackbar.make(getView(), "Event created, no invites sent", Snackbar.LENGTH_SHORT).
                    setAnchorView(getActivity().findViewById(R.id.bottomNavigation)).show();
        }
        else if (pairEmail == null){
            Snackbar.make(getView(), "Event created, partner does not have email invite", Snackbar.LENGTH_SHORT).
                    setAnchorView(getActivity().findViewById(R.id.bottomNavigation)).show();
        }
        // If partner has valid email, add their email as an attendee
        else {
            values.put(CalendarContract.Attendees.ATTENDEE_EMAIL, pairEmail);
            values.put(CalendarContract.Attendees.EVENT_ID, eventID);
            crAttendee.insert(CalendarContract.Attendees.CONTENT_URI, values);
            Snackbar.make(getView(), "Event created", Snackbar.LENGTH_SHORT).
                    setAnchorView(getActivity().findViewById(R.id.bottomNavigation)).show();

        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        updateUI(account);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    // After sign in, update UI with account (if sign-in unsuccessful, account == null)
    private void updateUI(GoogleSignInAccount account) {
        // Load calendar data if account is signed in
        if (account != null) {
            btnSignIn.setVisibility(View.GONE);

            fabAddEvent.setVisibility(View.VISIBLE);
            clProfileView.setVisibility(View.VISIBLE);
            String displayName = account.getGivenName();
            Uri imageUrl = account.getPhotoUrl();

            if (displayName != null) mFullName.setText(displayName + "'s Calendar");
            if (imageUrl != null) Glide.with(getContext())
                    .load(imageUrl).placeholder(R.drawable.ic_baseline_person_24).into(mProfileView);

            emailUsername = account.getEmail();
            // First run a check to see if calendar permissions have already been granted
            checkPermission(RC_CALENDAR_PERMISSIONS, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR);
            // Try to access calendars only if permission granted
            if (permissionsGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    checkCalendars();
                }
            }
            // If no calendar access, ask for user input again
            else {
                checkPermission(RC_CALENDAR_PERMISSIONS, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR);
            }

            // Configure add event button
            fabAddEvent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        showAddEventDialog();
                    }
                }
            });

            // Reveal and configure sign out button
            if (mSignOut.getVisibility() == View.GONE) {
                mSignOut.setVisibility(View.VISIBLE);
                mSignOut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mGoogleSignInClient.signOut()
                                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Snackbar snackbar = Snackbar.make(getView(), "Signed out", Snackbar.LENGTH_SHORT);
                                        snackbar.setAnchorView(getActivity().findViewById(R.id.bottomNavigation)).show();
                                        refreshFragment();
                                    }
                                });
                    }
                });
            }
        }
    }

    // Show a dialog to request user input on new event
    private void showAddEventDialog() {
        FragmentManager fm = getParentFragmentManager();
        AddEventDialogFragment addEventDialogFragment = AddEventDialogFragment.newInstance();
        // Set the calendar fragment for use later when sending event back
        addEventDialogFragment.setTargetFragment(CalendarFragment.this, 300);
        addEventDialogFragment.show(fm, "fragment_add_event");
    }

    @Override
    public void onFinishAddEventDialog(String title, String description, int[] startSelected, int[] endSelected) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            addEvent(calendarId, title, description, startSelected, endSelected);
        }
        refreshFragment();
    }

    // Method to refresh the fragment view when an action has been performed
    private void refreshFragment() {
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        if (Build.VERSION.SDK_INT >= 26) {
            ft.setReorderingAllowed(false);
        }
        ft.detach(CalendarFragment.this).attach(CalendarFragment.this).commit();
    }

}