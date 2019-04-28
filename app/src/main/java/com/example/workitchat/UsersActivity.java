package com.example.workitchat;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView mUsersList;
    private RecyclerView.LayoutManager mLayoutManager;
    private DatabaseReference mDatabaseRef;

    private FirebaseAuth mAuth;
    private String mCurUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        mAuth = FirebaseAuth.getInstance();
        mCurUserId = mAuth.getCurrentUser().getUid();

        //Set toolbar
        mToolbar = (Toolbar) findViewById(R.id.users_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.all_users_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child(WorkitContract.USERS_SCHEMA);
        mDatabaseRef.child(mCurUserId).child(WorkitContract.USERS_COLUMN_ONLINE).setValue(true);

        //set recycler view with linear layoutManager
        mLayoutManager = new LinearLayoutManager(this);
        mUsersList = (RecyclerView) findViewById(R.id.users_list);
        mUsersList.setHasFixedSize(true);
        mUsersList.setLayoutManager(mLayoutManager);

    }


    @Override
    protected void onPause() {
        super.onPause();
        //update online to false and change last seen time.
        mDatabaseRef.child(mCurUserId).child(WorkitContract.USERS_COLUMN_ONLINE).setValue(false);
        mDatabaseRef.child(mCurUserId).child(WorkitContract.USERS_COLUMN_LAST_SEEN).setValue(ServerValue.TIMESTAMP);

    }

    @Override
    protected void onStart() {
        super.onStart();
        //make sure the user online field in dataase is true.
        mDatabaseRef.child(mCurUserId).child(WorkitContract.USERS_COLUMN_ONLINE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean online = (boolean) dataSnapshot.getValue();
                if (!online){
                    mDatabaseRef.child(mCurUserId).child(WorkitContract.USERS_COLUMN_ONLINE).setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Query query = mDatabaseRef;

        FirebaseRecyclerOptions<User> options = new FirebaseRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .build();


        FirebaseRecyclerAdapter<User, UsersViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<User, UsersViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UsersViewHolder holder, int position, @NonNull User user) {
                holder.setName(user.getName());
                holder.setStatus(user.getStatus());
                holder.setImage(user.getThumb_image());
                holder.setOnline(user.isOnline());

                final String userId = getRef(position).getKey();

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent profileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                        profileIntent.putExtra("userId", userId);
                        startActivity(profileIntent);
                    }
                });
            }

            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.all_users_list_item, parent, false);

                return new UsersViewHolder(view);
            }
        };

        mUsersList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();

    }

    private class UsersViewHolder extends RecyclerView.ViewHolder{

        View mView;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setName(String name){
            TextView nameView = (TextView) mView.findViewById(R.id.all_users_name);
            nameView.setText(name);
        }

        public void setStatus(String status) {
            TextView statusView = (TextView) mView.findViewById(R.id.all_users_status);
            statusView.setText(status);
        }

        public void setImage(String thumb_image) {
            ImageView imageView = (CircleImageView) mView.findViewById(R.id.all_users_image);
            if (thumb_image == null || thumb_image.equals("default")){
                imageView.setImageResource(R.drawable.avatar);
            }
            else {
                Picasso.get().load(thumb_image).into(imageView);
            }
        }

        public void setOnline(boolean onlineStatus){
            ImageView userOnlineStatus = (ImageView) mView.findViewById(R.id.online_signal);
            GradientDrawable circle = (GradientDrawable) userOnlineStatus.getDrawable();
            if (!onlineStatus){

                circle.setColor(Color.RED);

            } else {
                circle.setColor(Color.GREEN);
            }
            userOnlineStatus.setVisibility(View.VISIBLE);
        }
    }
}
