package com.example.mentorly;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mentorly.fragments.CalendarFragment;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class SignUpActivity extends AppCompatActivity {

    public static final String TAG = "SignUpActivity";
    public static final String FIRST_NAME_KEY = "firstName";
    public static final String LAST_NAME_KEY = "lastName";
    private EditText etFirstName;
    private EditText etLastName;
    private EditText etEmail;
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
        etEmail = findViewById(R.id.etEmail);
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
                String inputEmail = etEmail.getText().toString();
                String firstName = etFirstName.getText().toString();
                String lastName = etLastName.getText().toString();

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
                else {
                    // Check if email is valid or not by searching chars
                    if (!isEmailValid(inputEmail)){
                        Toast.makeText(SignUpActivity.this, "Invalid email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                // if all fields are correct, then sign up user
                signUpNewUser(username, password, inputEmail, firstName, lastName);
            }
        });
    }

    private boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void signUpNewUser(final String username, final String password, String email, String firstName, String lastName) {
        // Create the ParseUser
        ParseUser user = new ParseUser();
        // Set core properties
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);

        // Save the email in accessible field
        user.put(CalendarFragment.PUBLIC_EMAIL_KEY, email);
        user.put(FIRST_NAME_KEY, firstName);
        user.put(LAST_NAME_KEY, lastName);

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