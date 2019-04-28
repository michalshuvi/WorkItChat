package com.example.workitchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

/**
 * One of the first two activities to reach the App.
 * Authentication using Firebase Auth.
 */

public class LoginActivity extends AppCompatActivity {

    private final String LOG = getClass().getSimpleName();

    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private Button mLoginBtn;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;

    private Toolbar mToolbar;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //initiate Firebase and declare Firebase auth instance
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        //set toolbar
        mToolbar = (Toolbar) findViewById(R.id.login_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProgressDialog = new ProgressDialog(this);

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child(WorkitContract.USERS_SCHEMA);

        //login Fields
        mEmail = (TextInputLayout) findViewById(R.id.login_email);
        mPassword = (TextInputLayout) findViewById(R.id.login_password);
        mLoginBtn = (Button) findViewById(R.id.login_btn);

        //clicking on the login button checks if both email and password are filled and run loginUser method.
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = mEmail.getEditText().getText().toString().trim();
                String password = mPassword.getEditText().getText().toString().trim();

                if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)){

                    mProgressDialog.setTitle("Logging In");
                    mProgressDialog.setMessage("Wait a sec...");
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.show();

                    loginUser(email, password);

                } else {
                    Toast.makeText(LoginActivity.this, "Please enter an Email an Password", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * @param email
     * @param password
     * Login user using Firebase Authentication, and send to Main Activity.
     */
    private void loginUser(String email, String password) {
        //try to sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        mProgressDialog.dismiss();

                        FirebaseUser currentUser = task.getResult().getUser();
                        String curUserId = currentUser.getUid();
                        //get device token for future notification sending.
                        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( LoginActivity.this, instanceIdResult -> {
                            String newToken = instanceIdResult.getToken();
                            //put device token in users schema.
                            mUserDatabase.child(curUserId)
                                    .child(WorkitContract.USERS_COLUMN_TOKEN).setValue(newToken)
                                    .addOnSuccessListener(aVoid -> {

                                        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(mainIntent);
                                        finish();

                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    makeToast(e.getMessage());
                                }
                            });
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                makeToast(e.getMessage());
                            }
                        });

                    } else {
                        mProgressDialog.hide();
                        makeToast(task.getException().getMessage());

                    }


                });

    }

    private void makeToast(String exceptionMessage){
        Toast.makeText(LoginActivity.this, "Login failed, " + exceptionMessage,
                Toast.LENGTH_LONG).show();
    }
}
