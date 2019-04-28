package com.example.workitchat;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * the opening activity for clients who are not logged in.
 */
public class StartActivity extends AppCompatActivity {

    private Button mRegbtn;
    private Button mLoginBtn;
    private TextView mNoInternet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mLoginBtn = (Button) findViewById(R.id.start_login_btn);
        mRegbtn = (Button) findViewById(R.id.start_reg_btn);
        mNoInternet = (TextView) findViewById(R.id.start_no_internet);

        mRegbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent regIntent = new Intent(StartActivity.this, RegisterActivity.class);
                startActivity(regIntent);
            }
        });

        //clicking on login button sends to another activity.
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent logIntent = new Intent(StartActivity.this, LoginActivity.class);
                startActivity(logIntent);
            }
        });

        //checks internet connectivity and shows a message accordingly.
        ConnectivityManager connectivityManager =
                (ConnectivityManager)this.getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
            mLoginBtn.setVisibility(View.GONE);
            mRegbtn.setVisibility(View.GONE);
            mNoInternet.setVisibility(View.VISIBLE);
        }
    }
}
