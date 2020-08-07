package com.example.mentorly.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mentorly.EventsAdapter;
import com.example.mentorly.R;
import com.example.mentorly.models.DateInterval;
import com.example.mentorly.models.MyEvent;
import com.example.mentorly.zoom.InitAuthSDKCallback;
import com.example.mentorly.zoom.ZoomEmailLoginActivity;
import com.example.mentorly.zoom.ZoomInitAuthSDKHelper;
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

import us.zoom.sdk.AccountService;
import us.zoom.sdk.MeetingItem;
import us.zoom.sdk.PreMeetingService;
import us.zoom.sdk.PreMeetingServiceListener;
import us.zoom.sdk.ZoomSDK;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;


public class CalendarFragment extends Fragment implements AddEventDialogFragment.AddEventDialogFragmentListener, InitAuthSDKCallback {

    // Projection array. Creating indices for this array instead of doing
    // dynamic lookups improves performance.
    public static final String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };

    // Check for calendar permissions
    boolean permissionsGranted;

    // Save user logged in info
    private String emailUsername;
    public static final String GOOGLE_MAIL = "com.google";
    private long calendarId;

    public static final String TAG = "CalendarFragment";
    public static final String PUBLIC_EMAIL_KEY = "publicEmail";
    public static final int RC_SIGN_IN = 429;
    public static final int RC_CALENDAR_PERMISSIONS = 412;
    GoogleSignInClient mGoogleSignInClient;

    // Save app user info for retrieving partner email
    ParseUser user;
    ParseUser pairPartner;
    String pairEmail;

    //views
    Button btnSignIn;
    Button btnZoomSignIn;
    Button btnZoomSignOut;
    Button mSignOut;
    TextView mFullName;
    ImageView mProfileView;
    FloatingActionButton fabAddEvent;
    ConstraintLayout clProfileView;

    // configure the recycler view
    List<MyEvent> allEvents;
    RecyclerView rvEvents;
    EventsAdapter adapter;

    // collect list of event dates to pas to addEvent prediction
    List<DateInterval> eventDates;

    // Zoom info
    ZoomSDK mZoomSDK;
    public static final int ZOOM_DURATION_MINUTES = 60;

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

        Toolbar toolbar = getActivity().findViewById(R.id.toolbar_main);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Get access to the custom title view
        TextView mTitle = toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("Calendar");

        // Fetch current user's partner email if needed, when creating event
        try {
            fetchPartnerInfo();
        } catch (com.parse.ParseException e) {
            e.printStackTrace();
        }

        // Configure the layout views
        btnSignIn = view.findViewById(R.id.btnGoogleSignIn);
        btnZoomSignIn = view.findViewById(R.id.btnZoomSignIn);
        btnZoomSignOut = view.findViewById(R.id.btnZoomSignOut);
        mSignOut = view.findViewById(R.id.signOut);
        mFullName = view.findViewById(R.id.fullName);
        mProfileView = view.findViewById(R.id.profileImage);
        rvEvents = view.findViewById(R.id.rvEvents);
        fabAddEvent = view.findViewById(R.id.fabAddEvent);
        clProfileView = view.findViewById(R.id.clProfileView);

        // Set up zoom SDK
        mZoomSDK = ZoomSDK.getInstance();
        ZoomInitAuthSDKHelper.getInstance().initSDK(getContext(), new ZoomInitAuthSDKHelper());

        // Set up the event recycler view
        eventDates = new ArrayList<>();
        allEvents = new ArrayList<>();
        adapter = new EventsAdapter(getContext(), allEvents);
        rvEvents.setAdapter(adapter);
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        float offsetPx = 30;
        BottomOffsetDecoration bottomOffsetDecoration = new BottomOffsetDecoration((int) offsetPx);
        rvEvents.addItemDecoration(bottomOffsetDecoration);

        // Configure sign-in to request the user's ID, email address, and basic
        //  profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
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

    @Override
    public void onZoomSDKInitializeResult(int i, int i1) {
    }

    @Override
    public void onZoomAuthIdentityExpired() {
    }

    // Class to define the offset height in recycler view
    static class BottomOffsetDecoration extends RecyclerView.ItemDecoration {
        private int mBottomOffset;

        public BottomOffsetDecoration(int bottomOffset) {
            mBottomOffset = bottomOffset;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int dataSize = state.getItemCount();
            int position = parent.getChildAdapterPosition(view);
            if (dataSize > 0 && position == dataSize - 1) {
                outRect.set(0, 0, 0, mBottomOffset);
            } else {
                outRect.set(0, 0, 0, 0);
            }

        }
    }

    // Fetch partner info and partner email (if it exists)
    private void fetchPartnerInfo() throws com.parse.ParseException {
        user = ParseUser.getCurrentUser();
        user.fetchIfNeeded();
        pairPartner = (ParseUser) user.get(ChatFragment.CHAT_PAIR_KEY);
        if (pairPartner != null) {
            pairPartner.fetchIfNeeded();
            pairEmail = (String) pairPartner.get(PUBLIC_EMAIL_KEY);
            Log.i(TAG, "name: " + pairPartner.getUsername() + "\nemail: " + pairEmail);
        }
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
            // Get the field values
            calID = cur.getLong(0);

            checkEvents(calID);
            calendarId = calID;
        }
    }

    private void checkEvents(long calID) {
        Cursor cur = null;
        ContentResolver cr = getContext().getContentResolver();
        Uri uri = CalendarContract.Events.CONTENT_URI;

        // array for values to store from the event
        String[] EVENT_PROJECTION = new String[]{
                CalendarContract.Events._ID,                    // 0
                CalendarContract.Events.DTSTART,                // 1
                CalendarContract.Events.DTEND,                  // 2
                CalendarContract.Events.TITLE,                  // 3
                CalendarContract.Events.DESCRIPTION             // 4
        };
        cur = cr.query(uri, EVENT_PROJECTION, "CALENDAR_ID=" + calID, null, "dtstart ASC, dtend ASC");

        System.out.println("eventCursor count=" + cur.getCount());
        if (cur.getCount() > 0) {
            if (cur.moveToFirst()) {
                allEvents.clear();
                do {
                    final long eventId = cur.getLong(0);
                    final Date begin = new Date(cur.getLong(1));
                    final Date end = new Date(cur.getLong(2));
                    final String title = cur.getString(3);
                    final String description = cur.getString(4);

                    Date currentTime = new Date(System.currentTimeMillis());

                    // if the event hasn't ended yet, show it on the calendar
                    if (end.after(currentTime)) {
                        // Add the event info to the adapter
                        MyEvent event = new MyEvent();
                        event.setEventTitle(title);
                        event.setEventDescription(description);
                        event.setStartDate(begin);
                        event.setEndDate(end);

                        event.setAttendees(checkGuests(eventId));

                        eventDates.add(new DateInterval(begin, end));
                        Log.i(TAG, eventDates.toString());

                        allEvents.add(event);
                        adapter.notifyDataSetChanged();
                    }
                }
                while (cur.moveToNext());
            }
        }
    }

    private List<String> checkGuests(Long eventId) {
        Cursor cur = null;
        ContentResolver cr = getContext().getContentResolver();
        // array for values to store from the event
        String[] ATTENDEE_PROJECTION = new String[]{
                CalendarContract.Attendees._ID,                    // 0
                CalendarContract.Attendees.ATTENDEE_EMAIL,         // 1
                CalendarContract.Attendees.ATTENDEE_STATUS,         // 1
        };

        cur = CalendarContract.Attendees.query(cr, eventId, ATTENDEE_PROJECTION);

        System.out.println("attendeeCursor count=" + cur.getCount());
        List<String> attendees = new ArrayList<>();

        if (cur.getCount() > 0) {
            if (cur.moveToFirst()) {
                do {
                    final String email = cur.getString(1);
                    final int status = cur.getInt(2);

                    attendees.add(email);

                    // retrieve the guest info for the event
                    Log.i(TAG, "Attendee email: " + email);
                    Log.i(TAG, "Status: " + status);
                }
                while (cur.moveToNext());
            }
        }
        return attendees;
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
    private void addEvent(final long calID, final String title, String description, int[] startSelected,
                          int[] endSelected, final boolean sendInvite, boolean isZoomMeeting) {
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

        if (isZoomMeeting) {
            PreMeetingService mPreMeetingService = null;
            AccountService mAccountService;

            // Check if the zoom account has ability to add new meeting
            if (ZoomSDK.getInstance().isInitialized()) {
                mAccountService = ZoomSDK.getInstance().getAccountService();
                mPreMeetingService = ZoomSDK.getInstance().getPreMeetingService();
                if (mAccountService == null || mPreMeetingService == null) {
                    Toast.makeText(getContext(), "Couldn't create meeting", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Create the meeting item
            MeetingItem meetingItem = mPreMeetingService.createScheduleMeetingItem();
            meetingItem.setMeetingTopic(title);
            meetingItem.setTimeZoneId(TimeZone.getDefault().toString());
            meetingItem.setStartTime(beginTime.getTimeInMillis());
            // Set the meeting to one hour
            meetingItem.setDurationInMinutes(ZOOM_DURATION_MINUTES);


            if (mPreMeetingService != null) {
                final PreMeetingService finalMPreMeetingService = mPreMeetingService;
                final long finalStartMillis = startMillis;
                final long finalEndMillis = endMillis;
                final String finalDescription = description;
                mPreMeetingService.addListener(new PreMeetingServiceListener() {
                    @Override
                    public void onListMeeting(int i, List<Long> list) {
                    }

                    @Override
                    public void onScheduleMeeting(int i, long meetingUniqueId) {
                        MeetingItem item = finalMPreMeetingService.getMeetingItemByUniqueId(meetingUniqueId);
                        // Save the invitation content and cut if description is null
                        String newDescription = finalDescription == null ? item.getInvitationEmailContentWithTime() :
                                finalDescription + "\n" + item.getInvitationEmailContentWithTime();
                        // Call method to continue creating event in GCal
                        continueSavingNewEvent(calID, title, newDescription, finalStartMillis, finalEndMillis, sendInvite);
                    }

                    @Override
                    public void onUpdateMeeting(int i, long l) {
                    }

                    @Override
                    public void onDeleteMeeting(int i) {
                    }
                });
                PreMeetingService.ScheduleOrEditMeetingError error = mPreMeetingService.scheduleMeeting(meetingItem);

                if (error == PreMeetingService.ScheduleOrEditMeetingError.SUCCESS) {
                    Toast.makeText(getContext(), "Success", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), error.toString(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_LONG).show();
            }
        }
        // If meeting is not Zoom meeting then save it normally in GCal
        else {
            continueSavingNewEvent(calID, title, description, startMillis, endMillis, sendInvite);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void continueSavingNewEvent(long calID, String title, String description, long startMillis, long endMillis, boolean sendInvite) {
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

        if (sendInvite) {
            addAttendee(eventID);
        }

        // Update the adapter with the new event
        checkCalendars();
        adapter.notifyDataSetChanged();
        Snackbar.make(getView().findViewById(R.id.fabAddEvent), "Event created", Snackbar.LENGTH_SHORT).
                setAnchorView(getView().findViewById(R.id.bottomNavigation))
                .show();
    }

    private void addAttendee(long eventID) {
        ContentResolver crAttendee = getContext().getContentResolver();
        ContentValues values = new ContentValues();

        // If partner has valid email, add their email as an attendee
        if (pairPartner != null && pairEmail != null) {
            values.put(CalendarContract.Attendees.ATTENDEE_EMAIL, pairEmail);
            values.put(CalendarContract.Attendees.EVENT_ID, eventID);
            crAttendee.insert(CalendarContract.Attendees.CONTENT_URI, values);
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        getActivity().startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        updateUI(account);
    }

    public void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
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
            String displayName = account.getDisplayName();
            final Uri imageUrl = account.getPhotoUrl();

            if (displayName != null) mFullName.setText(displayName);
            Glide.with(getContext())
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
                        googleSignOut();
                    }
                });
            }

            // Show Zoom sign-in option only if user is also Google authenticated
            handleZoomSignIn();
        }
    }

    private void handleZoomSignIn() {
        if (!mZoomSDK.isLoggedIn()) {
            // Hide the sign out UI until user is authenticated by Zoom
            btnZoomSignIn.setVisibility(View.VISIBLE);
            btnZoomSignOut.setVisibility(View.GONE);

            // Reset the constraint params for Google sign in button
            ConstraintLayout constraintLayout = getView().findViewById(R.id.clEvents);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.signOut, ConstraintSet.END, R.id.btnZoomSignIn, ConstraintSet.START, 0);
            constraintSet.applyTo(constraintLayout);

            btnZoomSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mZoomSDK.isInitialized()) {
                        Toast.makeText(getContext(), "Init SDK First", Toast.LENGTH_SHORT).show();
                        ZoomInitAuthSDKHelper.getInstance().initSDK(getContext(), new ZoomInitAuthSDKHelper());
                    }

                    // Start intent for email login
                    Intent intent = new Intent(getContext(), ZoomEmailLoginActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Reveal and configure zoom sign out if zoom authenticated
        else if (btnZoomSignOut.getVisibility() == View.GONE && mZoomSDK.isLoggedIn()) {
            btnZoomSignOut.setVisibility(View.VISIBLE);
            btnZoomSignIn.setVisibility(View.GONE);

            // Reset the constraint params for Google sign out button if the user is zoom logged in
            ConstraintLayout constraintLayout = getView().findViewById(R.id.clEvents);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.signOut, ConstraintSet.END, R.id.btnZoomSignOut, ConstraintSet.START, 0);
            constraintSet.applyTo(constraintLayout);
            btnZoomSignOut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mZoomSDK.logoutZoom()) {
                        Toast.makeText(getContext(), "ZoomSDK has not been initialized successfully", Toast.LENGTH_LONG).show();
                    } else {
                        // show a Snackbar for the user to un-delete the To Do item
                        Snackbar.make(getView().findViewById(R.id.fabAddEvent), "Signed out of Zoom", Snackbar.LENGTH_SHORT).
                                setAnchorView(view.findViewById(R.id.bottomNavigation))
                                .show();
                        handleZoomSignIn();
                    }
                }
            });
        }

    }

    public void googleSignOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Snackbar.make(getView().findViewById(R.id.fabAddEvent), "Signed out of Google", Snackbar.LENGTH_SHORT).
                                setAnchorView(getView().findViewById(R.id.bottomNavigation))
                                .show();
                        refreshFragment();
                    }
                });
        mZoomSDK.logoutZoom();
        handleZoomSignIn();
    }

    // Show a dialog to request user input on new event
    private void showAddEventDialog() {
        FragmentManager fm = getParentFragmentManager();
        AddEventDialogFragment addEventDialogFragment = AddEventDialogFragment.newInstance(eventDates, mZoomSDK.isLoggedIn());
        // Set the calendar fragment for use later when sending event back
        addEventDialogFragment.setTargetFragment(CalendarFragment.this, 300);
        addEventDialogFragment.show(fm, "fragment_add_event");
    }

    @Override
    public void onFinishAddEventDialog(String title, String description, int[] startSelected,
                                       int[] endSelected, boolean sendInvite, boolean isZoomMeeting) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            addEvent(calendarId, title, description, startSelected, endSelected, sendInvite, isZoomMeeting);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        updateUI(account);
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