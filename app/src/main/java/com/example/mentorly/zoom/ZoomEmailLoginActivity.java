package com.example.mentorly.zoom;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.mentorly.R;

import us.zoom.sdk.ZoomApiError;
import us.zoom.sdk.ZoomAuthenticationError;

public class ZoomEmailLoginActivity extends AppCompatActivity implements UserLoginCallback.ZoomDemoAuthenticationListener, View.OnClickListener, InitAuthSDKCallback {

    private EditText mEdtUserName;
    private EditText mEdtPassord;
    private Button mBtnLogin;
    private View mProgressPanel;
    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.zoom_login_activity);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = findViewById(R.id.toolbar_login);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        setSupportActionBar(toolbar);
        // Get access to the custom title view
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView mTitle = toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText("Log in to Zoom");

        mEdtUserName = (EditText)findViewById(R.id.userName);
        mEdtPassord = (EditText)findViewById(R.id.password);
        mBtnLogin = (Button)findViewById(R.id.btnLogin);
        mBtnLogin.setOnClickListener(this);
        mProgressPanel = (View)findViewById(R.id.progressPanel);

        // Try to log in with previous data
        SharedPreferences pref =
                PreferenceManager.getDefaultSharedPreferences(this);
        String username = pref.getString("username", "");
        String password = pref.getString("password", "");
        mEdtUserName.setText(username);
        mEdtPassord.setText(password);
        tryZoomLogin(username, password);

    }

    @Override
    protected void onResume() {
        super.onResume();

        UserLoginCallback.getInstance().addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        UserLoginCallback.getInstance().removeListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btnLogin) {
            onClickBtnLogin();
        }
    }

    private void onClickBtnLogin() {
        String userName = mEdtUserName.getText().toString().trim();
        String password = mEdtPassord.getText().toString().trim();
        if(userName.length() == 0 || password.length() == 0) {
            Toast.makeText(this, "You need to enter user name and password.", Toast.LENGTH_LONG).show();
            return;
        }

        tryZoomLogin(userName, password);
    }

    private void tryZoomLogin(String userName, String password) {
        int ret=ZoomLoginHelper.getInstance().login(userName, password);
        if(!(ret== ZoomApiError.ZOOM_API_ERROR_SUCCESS)) {
            if (ret == ZoomApiError.ZOOM_API_ERROR_EMAIL_LOGIN_IS_DISABLED) {
                Toast.makeText(this, "Account has disabled email login ", Toast.LENGTH_LONG).show();
            }
        } else {
            mBtnLogin.setVisibility(View.GONE);
            mProgressPanel.setVisibility(View.VISIBLE);
            this.username = userName;
            this.password = password;
        }
    }

    @Override
    public void onZoomSDKLoginResult(long result) {
        if(result == ZoomAuthenticationError.ZOOM_AUTH_ERROR_SUCCESS) {
            UserLoginCallback.getInstance().removeListener(this);

            SharedPreferences pref =
                    PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString("username", username);
            edit.putString("password", password);
            edit.commit();

            finish();
        } else {
            Toast.makeText(this, "Login failed result code = " + result, Toast.LENGTH_SHORT).show();
        }
        mBtnLogin.setVisibility(View.VISIBLE);
        mProgressPanel.setVisibility(View.GONE);
    }

    @Override
    public void onZoomSDKLogoutResult(long result) {
        if(result == ZoomAuthenticationError.ZOOM_AUTH_ERROR_SUCCESS) {
            Toast.makeText(this, "Logout successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Logout failed result code = " + result, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onZoomIdentityExpired() {
        //Zoom identity expired, please re-login
    }

    @Override
    public void onZoomSDKInitializeResult(int i, int i1) {

    }

    @Override
    public void onZoomAuthIdentityExpired() {

    }
}

