package com.example.workitchat;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

/**
 * Main Activity, reached after sign-in or login.
 * Contains 3 Fragments: chats, friends and requests.
 * Appbar contains 3 options: account settings, all users, log out.
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Toolbar mToolbar;

    private ViewPager mViewPager;
    private TabsPagerAdapter mTabsPagerAdapter;

    private DatabaseReference mUserRef;

    private TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initiate Firebase and get an instance of it.
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null){
            mUserRef = FirebaseDatabase.getInstance().getReference().child(WorkitContract.USERS_SCHEMA).child(mAuth.getCurrentUser().getUid());
        }


        //setup toolbar
        mToolbar = (Toolbar) findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("WorkIt");


        //setup tabs
        mViewPager = (ViewPager) findViewById(R.id.tab_pager);
        mTabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        mViewPager.setAdapter(mTabsPagerAdapter);

        mTabLayout = (TabLayout) findViewById(R.id.main_tabs);
        mTabLayout.setupWithViewPager(mViewPager);


    }

    @Override
    protected void onPause() {
        super.onPause();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        //updates online/offline in db.
        if (currentUser != null) {
            mUserRef.child(WorkitContract.USERS_COLUMN_ONLINE).setValue(false);
            mUserRef.child(WorkitContract.USERS_COLUMN_LAST_SEEN).setValue(ServerValue.TIMESTAMP);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and move to start activity accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null){
            sendToStart();
        }
        else {
            mUserRef.child(WorkitContract.USERS_COLUMN_ONLINE).setValue(true);
        }

    }

    /**
     * send to login/register activity
     */
    private void sendToStart() {
        Intent startIntent = new Intent(MainActivity.this, StartActivity.class);
        startActivity(startIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            case R.id.main_logout_btn:
                FirebaseAuth.getInstance().signOut();
                mUserRef.child(WorkitContract.USERS_COLUMN_ONLINE).setValue(false);
                mUserRef.child(WorkitContract.USERS_COLUMN_LAST_SEEN).setValue(ServerValue.TIMESTAMP);
                sendToStart();
                break;
            case R.id.settings_btn:
                Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings);
                break;
            case R.id.all_users_btn:
                Intent allUsers = new Intent(MainActivity.this, UsersActivity.class);
                startActivity(allUsers);
                break;
        }

        return true;
    }
}
