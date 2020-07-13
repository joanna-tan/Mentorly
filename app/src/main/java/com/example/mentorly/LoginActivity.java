package com.example.mentorly;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

public class LoginActivity extends AppCompatActivity {

    public static final String TAG = "LoginActivity";
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnSignUp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (ParseUser.getCurrentUser() != null) {
            goMainActivity();
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick Login button");
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();
                logInUser(username, password);
            }
        });

        // clicking on Sign Up brings users to a new screen for signup
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), SignUpActivity.class);
                startActivity(i);
            }
        });
    }

    public void logInUser(String username, String password) {
        Log.i(TAG,"Attempting to log in user " + username);
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username/password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e == null) {
                    goMainActivity();
                    Toast.makeText(LoginActivity.this, "Login success!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Log.i(TAG, "Issue with login: " + e);
                    Toast.makeText(LoginActivity.this, "Please enter a valid username and password ", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void goMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish(); //clears the activity from the stack
    }
}