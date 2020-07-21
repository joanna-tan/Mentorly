package com.example.mentorly.fragments;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mentorly.R;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.snackbar.Snackbar;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class CalendarFragment extends Fragment {

    private static final String SHARED_PREFERENCES_NAME = "AuthStatePreference";
    private static final String AUTH_STATE = "AUTH_STATE";
    private static final String USED_INTENT = "USED_INTENT";
    private final static String LOGIN_HINT = "login_hint";

    public static final String TAG = "CalendarFragment";
    public static final int RC_SIGN_IN = 42;
    GoogleSignInClient mGoogleSignInClient;
    Button btnSignIn;

    // state
    AuthState mAuthState;

    //views
    Button mMakeApiCall;
    Button mSignOut;
    TextView mGivenName;
    TextView mFamilyName;
    TextView mFullName;
    ImageView mProfileView;

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

        enablePostAuthorizationFlows();

        // wire click listeners
        btnSignIn.setOnClickListener(new AuthorizeListener(this));


    }


    private void enablePostAuthorizationFlows() {
        mAuthState = restoreAuthState();
        if (mAuthState != null && mAuthState.isAuthorized()) {
            if (mMakeApiCall.getVisibility() == View.GONE) {
                mMakeApiCall.setVisibility(View.VISIBLE);
                mMakeApiCall.setOnClickListener(new MakeApiCallListener(this, mAuthState, new AuthorizationService(getContext())));
            }
            if (mSignOut.getVisibility() == View.GONE) {
                mSignOut.setVisibility(View.VISIBLE);
                mSignOut.setOnClickListener(new SignOutListener(this));
            }
        } else {
            mMakeApiCall.setVisibility(View.GONE);
            mSignOut.setVisibility(View.GONE);
        }
    }

    @Nullable
    private AuthState restoreAuthState() {
        String jsonString = getContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(AUTH_STATE, null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.fromJson(jsonString);
            } catch (JSONException jsonException) {
                // should never happen
            }
        }
        return null;
    }

    private void clearAuthState() {
        getContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(AUTH_STATE)
                .apply();
    }


    /**
     * Kicks off the authorization flow.
     */
    public static class AuthorizeListener implements Button.OnClickListener {
        private final CalendarFragment mMainActivity;

        public AuthorizeListener(@NonNull CalendarFragment mainActivity) {
            mMainActivity = mainActivity;
        }

        @Override
        public void onClick(View view) {
            // to create the authorization request
            AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                    Uri.parse("https://accounts.google.com/o/oauth2/v2/auth") /* auth endpoint */,
                    Uri.parse("https://www.googleapis.com/oauth2/v4/token") /* token endpoint */
            );

      /* Register your own client ID &
       update the clientId and redirectUri values with your own (and the custom scheme registered in the AndroidManifest.xml)
       */
            String clientId = "511828570984-fuprh0cm7665emlne3rnf9pk34kkn86s.apps.googleusercontent.com";
            Uri redirectUri = Uri.parse("com.google.codelabs.appauth:/oauth2callback");
            AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                    serviceConfiguration,
                    clientId,
                    AuthorizationRequest.RESPONSE_TYPE_CODE,
                    redirectUri
            );
            builder.setScope("https://www.googleapis.com/auth/calendar");

            // Build the authorization request
            AuthorizationRequest request = builder.build();

            //Perform the Authorization Request
            AuthorizationService authorizationService = new AuthorizationService(view.getContext());
            String action = "com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE";
            Intent postAuthorizationIntent = new Intent(action);
            PendingIntent pendingIntent = PendingIntent.getActivity(view.getContext(), request.hashCode(), postAuthorizationIntent, 0);
            authorizationService.performAuthorizationRequest(request, pendingIntent);

        }
    }

    // listener class for allowing user to sign out
    public static class SignOutListener implements Button.OnClickListener {

        private final CalendarFragment mMainActivity;

        public SignOutListener(@NonNull CalendarFragment mainActivity) {
            mMainActivity = mainActivity;
        }

        @Override
        public void onClick(View view) {
            mMainActivity.mAuthState = null;
            mMainActivity.clearAuthState();
            mMainActivity.enablePostAuthorizationFlows();
        }
    }

    // listener class for making API calls on click
    public class MakeApiCallListener implements Button.OnClickListener {

        private final CalendarFragment mMainActivity;
        private AuthState mAuthState;
        private AuthorizationService mAuthorizationService;

        public MakeApiCallListener(@NonNull CalendarFragment mainActivity, @NonNull AuthState authState, @NonNull AuthorizationService authorizationService) {
            mMainActivity = mainActivity;
            mAuthState = authState;
            mAuthorizationService = authorizationService;
        }

        // Will want to serialize the AuthState object to disk to preserve authorization state between app runs.
        @Override
        public void onClick(View view) {
            mAuthState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {
                @SuppressLint("StaticFieldLeak")
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException exception) {
                    new AsyncTask<String, Void, JSONArray>() {
                        @Override
                        protected JSONArray doInBackground(String... tokens) {
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url("https://www.googleapis.com/auth/calendar/users/me/calendarList")
                                    .addHeader("Authorization", String.format("Bearer %s", tokens[0]))
                                    .build();

                            try {
                                Response response = client.newCall(request).execute();
                                String jsonBody = response.body().string();
                                Log.i(TAG, String.format("User Info Response %s", jsonBody));
                                return new JSONArray(jsonBody);
                            } catch (Exception exception) {
                                Log.w(TAG, exception);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(JSONArray jsonArray) {
                            if (jsonArray != null) {
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = new JSONObject();
                                    try {
                                        jsonObject = jsonArray.getJSONObject(i);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    if (jsonObject != null) {
                                        String fullName = jsonObject.optString("name", null);
                                    }

                                }

                                String message = "Logged in.";
                                Snackbar.make(mMainActivity.mProfileView, message, Snackbar.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }.execute(accessToken);
                }
            });
        }
    }
}