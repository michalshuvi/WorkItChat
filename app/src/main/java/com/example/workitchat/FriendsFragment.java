package com.example.workitchat;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 * Part of Main Activity.
 * display all friends of the current user.
 *
 */
public class FriendsFragment extends Fragment {

    private RecyclerView mFriendsList;

    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mUsersDatabase;

    private FirebaseAuth mAuth;

    private String mCurUserId;

    private View mMainView;


    public FriendsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mMainView = inflater.inflate(R.layout.fragment_friends, container, false);

        mFriendsList = (RecyclerView) mMainView.findViewById(R.id.friends_list);

        mAuth = FirebaseAuth.getInstance();
        mCurUserId = mAuth.getCurrentUser().getUid();

        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child(WorkitContract.FRIENDS_SCHEMA).child(mCurUserId);
        mFriendsDatabase.keepSynced(true);
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child(WorkitContract.USERS_SCHEMA);
        mUsersDatabase.keepSynced(true);

        mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));

        return mMainView;

    }

    @Override
    public void onStart() {
        super.onStart();

        Query query = mFriendsDatabase;

        FirebaseRecyclerOptions<Friends> options = new FirebaseRecyclerOptions.Builder<Friends>()
                .setQuery(query, Friends.class)
                .build();

        FirebaseRecyclerAdapter<Friends, FriendsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FriendsViewHolder holder, int position, @NonNull Friends user) {

                holder.setDate(user.getDate());
                String curUserId = getRef(position).getKey();

                mUsersDatabase.child(curUserId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String userName = dataSnapshot.child(WorkitContract.USERS_COLUMN_NAME).getValue().toString();
                        String userThumb = dataSnapshot.child(WorkitContract.USERS_COLUMN_THUMB_IMAGE).getValue().toString();
                        boolean userOnline = (boolean) dataSnapshot.child(WorkitContract.USERS_COLUMN_ONLINE).getValue();

                        holder.setName(userName);
                        holder.setThumb(userThumb);
                        holder.setOnlineSign(userOnline);

                        //clicking on a list items opens alert dialog with two options: send a message or view profile.
                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CharSequence options[] = new CharSequence[]{"Open Profile", "Send message"};
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                builder.setTitle("Select");
                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0){
                                            Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                            profileIntent.putExtra("userId", curUserId);
                                            startActivity(profileIntent);
                                        }
                                        else {
                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            chatIntent.putExtra("userId", curUserId);
                                            chatIntent.putExtra("userName", userName);
                                            chatIntent.putExtra("userThumb", userThumb);
                                            startActivity(chatIntent);
                                        }

                                    }
                                });
                                builder.show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.all_users_list_item, parent, false);

                return new FriendsViewHolder(view);
            }
        };
        mFriendsList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    private class FriendsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            this.mView = itemView;
        }

        public void setDate(String date){
            TextView userStatus = (TextView) mView.findViewById(R.id.all_users_status);
            userStatus.setText("Friends since " + date);
        }

        public void setName(String name){
            TextView userName = (TextView) mView.findViewById(R.id.all_users_name);
            userName.setText(name);
        }

        public void setThumb(String thumb){
            ImageView userImg = (CircleImageView) mView.findViewById(R.id.all_users_image);
            Picasso.get().load(thumb).placeholder(R.drawable.avatar).into(userImg);

        }

        public void setOnlineSign(boolean onlineStatus){
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
