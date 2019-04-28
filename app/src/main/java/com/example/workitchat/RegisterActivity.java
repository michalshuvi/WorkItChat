package com.example.workitchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;

/**
 * Register to the App, using Firebase Authentication.
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout mName;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private Button mCreateBtn;
    private FirebaseAuth mAuth;
    private final String LOG = getClass().getSimpleName();

    private Toolbar mToolbar;

    //Progress dialog
    private ProgressDialog mRegDialog;

    //Firebase database
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        //initiate Firebase and declare Firebase auth instance
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        mToolbar = (Toolbar) findViewById(R.id.register_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRegDialog = new ProgressDialog(this);

        //Registration Fields
        mName = (TextInputLayout) findViewById(R.id.reg_name);
        mEmail = (TextInputLayout) findViewById(R.id.reg_email);
        mPassword = (TextInputLayout) findViewById(R.id.reg_password);
        mCreateBtn = (Button) findViewById(R.id.reg_create_acc);

        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //extract registration details from edit text views
                String name = mName.getEditText().getText().toString().trim();
                String email = mEmail.getEditText().getText().toString().trim();
                String password = mPassword.getEditText().getText().toString().trim();

                if (!TextUtils.isEmpty(name) || !TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)){
                    mRegDialog.setTitle("Register User");
                    mRegDialog.setMessage("Please wait...");
                    mRegDialog.setCanceledOnTouchOutside(false);
                    mRegDialog.show();

                    registerUser(name, email, password);
                }
            }
        });

        //Declare database - reference to the root
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * register user using Firebase Authentication and move to Main Activity if succeed.
     * @param name
     * @param email
     * @param password
     */
    private void registerUser(final String name, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            // Sign in is successful, update UI with the signed-in user's information
                            mRegDialog.dismiss();

                            FirebaseUser currUser = task.getResult().getUser();
                            String currUserId = currUser.getUid();

                            // get device token id for future notifications.
                            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( RegisterActivity.this,  new OnSuccessListener<InstanceIdResult>() {
                                @Override
                                public void onSuccess(InstanceIdResult instanceIdResult) {
                                    String tokenId = instanceIdResult.getToken();

                                    mDatabase = mDatabase.child(WorkitContract.USERS_SCHEMA).child(currUserId);

                                    HashMap<String, String> userMap = new HashMap<>();
                                    userMap.put(WorkitContract.USERS_COLUMN_NAME, name);
                                    userMap.put(WorkitContract.USERS_COLUMN_STATUS, "Hey, I'm using WorkIt App!");
                                    userMap.put(WorkitContract.USERS_COLUMN_IMAGE, "default");
                                    userMap.put(WorkitContract.USERS_COLUMN_THUMB_IMAGE, "default");
                                    userMap.put(WorkitContract.USERS_COLUMN_TOKEN, tokenId);

                                    //insert a new user to firebase database -> users
                                    mDatabase.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                //after registration has successfully placed move to Main Activity.
                                                Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                                                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(mainIntent);
                                                finish();
                                            }
                                        }
                                    });
                                }
                            });
                        } else {
                            mRegDialog.hide();
                            Toast.makeText(RegisterActivity.this, "Registration failed, "+ task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
