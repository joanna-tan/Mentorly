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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;


public class CalendarFragment extends Fragment {

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

    final int callbackId = 412;

    public static final String TAG = "CalendarFragment";
    public static final int RC_SIGN_IN = 42;
    GoogleSignInClient mGoogleSignInClient;
    Button btnSignIn;

    //views
    Button mMakeApiCall;
    Button mSignOut;
    TextView mGivenName;
    TextView mFamilyName;
    TextView mFullName;
    ImageView mProfileView;

    // configure the recycler view
    List<String> allEvents;
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
        return inflater.inflate(R.layout.fragment_calendar_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnSignIn = view.findViewById(R.id.authorize);

        mMakeApiCall = view.findViewById(R.id.makeApiCall);
        mSignOut = view.findViewById(R.id.signOut);
        mGivenName = view.findViewById(R.id.givenName);
        mFamilyName = view.findViewById(R.id.familyName);
        mFullName = view.findViewById(R.id.fullName);
        mProfileView = view.findViewById(R.id.profileImage);
        rvEvents = view.findViewById(R.id.rvEvents);

        allEvents = new ArrayList<>();
        adapter = new EventsAdapter(getContext(), allEvents);
        rvEvents.setAdapter(adapter);
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        Log.i(TAG, "set up adapter");


        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id_web))
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/calendar"))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
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

            // Do something with the values...
            Log.i(TAG, "calID" + calID);
            Log.i(TAG, "display name " +displayName);
            Log.i(TAG, "account name " + accountName);
            Log.i(TAG, "owner name " + ownerName);

            // check events here
            checkEvents(calID);
        }
    }

    private void checkEvents(long calID) {
        Uri.Builder builder = Uri.parse("content://com.android.calendar/instances/when").buildUpon();
        //Uri.Builder builder = Uri.parse("content://com.android.calendar/calendars").buildUpon();
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
                do {
                    Object mbeg_date, beg_date, beg_time, end_date, end_time;

                    final String title = eventCursor.getString(0);
                    final Date begin = new Date(eventCursor.getLong(1));
                    final Date end = new Date(eventCursor.getLong(2));
                    final Boolean allDay = !eventCursor.getString(3).equals("0");

                    allEvents.add("Title: " + title + "\nBegin: " + begin + "\nEnd: " + end + "\nAll Day: " + allDay);
                    adapter.notifyDataSetChanged();

                }
                while (eventCursor.moveToNext());
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addEvent(long calID) {
        long startMillis = 0;
        long endMillis = 0;
        Calendar beginTime = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            beginTime = Calendar.getInstance();
        }
        beginTime.set(2020, 7, 24, 7, 30);
        startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        endTime.set(2020, 7, 24, 8, 45);
        endMillis = endTime.getTimeInMillis();


        ContentResolver crEvent = getContext().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.TITLE, "Jazzercise1");
        values.put(CalendarContract.Events.DESCRIPTION, "Group workout");
        values.put(CalendarContract.Events.CALENDAR_ID, calID);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, "America/Los_Angeles");
        Uri uriEvent = crEvent.insert(CalendarContract.Events.CONTENT_URI, values);

        // get the event ID that is the last element in the Uri
        long eventID = Long.parseLong(uriEvent.getLastPathSegment());
        Log.i(TAG, "eventID" + eventID + " event created");
        // ... do something with event ID
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
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    // after sign in, update UI with account (if sign-in unsuccessful, account == null)
    private void updateUI(GoogleSignInAccount account) {
        //load relevant data
        if (account != null) {
            btnSignIn.setVisibility(View.GONE);

            emailUsername = account.getEmail();
            if (mMakeApiCall.getVisibility() == View.GONE) {
                mMakeApiCall.setVisibility(View.VISIBLE);
                mMakeApiCall.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onClick(View view) {
                        // first run a check to see if permissions have already been granted
                        checkPermission(callbackId, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR);
                        // try to access calendars only if permission granted
                        if (permissionsGranted) {
                            checkCalendars();
                        }

                        // Check that calendar read/write has been granted before making call
                        else {
                            checkPermission(callbackId, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR);
                        }

                    }
                });
            }
            // configure sign out button
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
        // hide API call and sign out button if not logged in
        else {
            mMakeApiCall.setVisibility(View.GONE);
            mSignOut.setVisibility(View.GONE);
        }
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