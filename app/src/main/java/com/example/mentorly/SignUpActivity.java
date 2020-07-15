package com.example.mentorly;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class SignUpActivity extends AppCompatActivity {

    public static final String TAG = "SignUpActivity";
    private EditText etFirstName;
    private EditText etLastName;
    private EditText etUsername;
    private EditText etPassword;
    private EditText etPasswordConfirm;
    private Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        btnSignUp = findViewById(R.id.btnSignUp);


        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick sign up button");
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();
                String passwordConfirm = etPasswordConfirm.getText().toString();

                //check if the two password fields are the same
                if (!password.equals(passwordConfirm)) {
                    Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }
                //warn user of empty sign up
                else if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Username/password cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                signUpNewUser(username, password);
            }
        });
    }

    private void signUpNewUser(final String username, final String password) {
        // Create the ParseUser
        ParseUser user = new ParseUser();
        // Set core properties
        user.setUsername(username);
        user.setPassword(password);

        // TODO: save user first/last names in Parse

        // Invoke signUpInBackground
        user.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Toast.makeText(SignUpActivity.this, "Sign up success!", Toast.LENGTH_SHORT).show();
                    goLogInActivity();
                }
                else {
                    Log.i(TAG, "Issue with login: " + e);
                    Toast.makeText(SignUpActivity.this, "Issue with signing up, please try again", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void goLogInActivity() {
        // go to log in activity with the signed in user to remove both login/sign up screens from the stack
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish(); //clears the sign up activity from the stack
    }


}