package com.example.workitchat;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private String profileUserId;

    private ImageView mProfileImg;
    private TextView mProfileName;
    private TextView mProfileStatus;
    private Button mSendReqBtn;
    private Button mDeclineReqBtn;

    private DatabaseReference mProfileUserDatabase;
    private DatabaseReference mFriendRequestDatabase;
    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mNotificationDatabase;
    private DatabaseReference mRootRef;

    private FirebaseUser mCurUser;

    private ProgressDialog mProgress;

    private int mCurrentFriendState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileUserId = getIntent().getStringExtra("userId");

        mProfileImg = (ImageView) findViewById(R.id.profile_image);
        mProfileName = (TextView) findViewById(R.id.profile_user_name);
        mProfileStatus = (TextView) findViewById(R.id.profile_user_status);
        mSendReqBtn = (Button) findViewById(R.id.profile_send_req);
        mDeclineReqBtn = (Button) findViewById(R.id.profile_decline_req);
        mDeclineReqBtn.setEnabled(false);

        setProgressBar("Loading Profile", "Please wait while we load user data");
        mProgress.show();

        mProfileUserDatabase = FirebaseDatabase.getInstance().getReference().child(WorkitContract.USERS_SCHEMA).child(profileUserId);
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child(WorkitContract.FRIEND_REQUEST_SCHEMA);
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child(WorkitContract.FRIENDS_SCHEMA);
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child(WorkitContract.NOTIFICATIONS_SCHEMA);
        mRootRef = FirebaseDatabase.getInstance().getReference();

        mCurUser = FirebaseAuth.getInstance().getCurrentUser();

         //not friends
        mCurrentFriendState = WorkitContract.FRIENDS_STATE_NOT_FRIENDS;

        mProfileUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String name = dataSnapshot.child(WorkitContract.USERS_COLUMN_NAME).getValue().toString();
                String image = dataSnapshot.child(WorkitContract.USERS_COLUMN_IMAGE).getValue().toString();
                String status = dataSnapshot.child(WorkitContract.USERS_COLUMN_STATUS).getValue().toString();

                mProfileName.setText(name);
                mProfileStatus.setText(status);
                if (!image.equals("default")) {
                    Picasso.get().load(image).placeholder(R.drawable.avatar).into(mProfileImg);
                }

                //choose text on friend request button
                setBtnText();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        mDeclineReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mSendReqBtn.setEnabled(false);
                mDeclineReqBtn.setEnabled(false);
                setProgressBar("Processing", "Please wait a moment");
                mProgress.show();

                Map declineMap = new HashMap();
                declineMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + profileUserId + "/" + mCurUser.getUid(), null);
                declineMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + mCurUser.getUid() + "/" + profileUserId, null);

                mRootRef.updateChildren(declineMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if (databaseError == null){
                            mCurrentFriendState = WorkitContract.FRIENDS_STATE_NOT_FRIENDS;
                            mDeclineReqBtn.setVisibility(View.INVISIBLE);
                            mSendReqBtn.setText(R.string.profile_send_friend_request);
                        }
                        else {
                            mDeclineReqBtn.setEnabled(true);
                        }
                        mSendReqBtn.setEnabled(true);
                        mProgress.dismiss();
                    }
                });
            }
        });

        mSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mSendReqBtn.setEnabled(false);
                setProgressBar("Sending", "Please wait a moment");
                mProgress.show();

                // friends state - functionality of the mSendRequestBtn:
                // 1. Non friends - send request
                // 2. request sent (from current user) - cancel request
                // 3. request received - accept friendship
                // 4. friends - unfriend
                switch (mCurrentFriendState){

                // -------- NON-FRIENDS STATE --------
                case WorkitContract.FRIENDS_STATE_NOT_FRIENDS:

                    DatabaseReference notificationRef = mRootRef.child(WorkitContract.NOTIFICATIONS_SCHEMA).push();
                    String newNotificationId = notificationRef.getKey();

                    HashMap<String, String> notifications = new HashMap<>();
                    notifications.put(WorkitContract.NOTIFICATIONS_COLUMN_FROM, mCurUser.getUid());
                    notifications.put(WorkitContract.NOTIFICATIONS_COLUMN_TYPE, String.valueOf(WorkitContract.NOTIFICATIONS_REQ_TYPE));

                    Map requestMap = new HashMap();
                    requestMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + mCurUser.getUid() + "/" + profileUserId + "/" + WorkitContract.FRIENDS_REQ_TYPE, WorkitContract.FRIENDS_STATE_SENT);
                    requestMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + profileUserId + "/" + mCurUser.getUid() + "/" + WorkitContract.FRIENDS_REQ_TYPE, WorkitContract.FRIENDS_STATE_RECEIVED);
                    requestMap.put(WorkitContract.NOTIFICATIONS_SCHEMA + "/" + profileUserId + "/" + newNotificationId + "/" + WorkitContract.NOTIFICATIONS_COLUMN_FROM, mCurUser.getUid());
                    requestMap.put(WorkitContract.NOTIFICATIONS_SCHEMA + "/" + profileUserId + "/" + newNotificationId + "/" + WorkitContract.NOTIFICATIONS_COLUMN_TYPE,String.valueOf(WorkitContract.NOTIFICATIONS_REQ_TYPE));

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if (databaseError != null){
                                Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_LONG).show();
                            }
                            else {
                                mCurrentFriendState = WorkitContract.FRIENDS_STATE_SENT;
                                mSendReqBtn.setText(R.string.profile_cancel_friend_request);

                                mDeclineReqBtn.setVisibility(View.INVISIBLE);
                                mDeclineReqBtn.setEnabled(false);
                            }
                            mSendReqBtn.setEnabled(true);
                        }
                    });
                    mProgress.dismiss();
                    break;

                //-------- REQUEST SENT STATE - Cancel Friend request -------------
                case WorkitContract.FRIENDS_STATE_SENT:
                    Map deleteReqMap = new HashMap();
                    deleteReqMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + mCurUser.getUid() + "/" + profileUserId, null);
                    deleteReqMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + profileUserId + "/" + mCurUser.getUid(), null);

                    mRootRef.updateChildren(deleteReqMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError != null){
                                Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_LONG).show();
                            }
                            else {
                                mCurrentFriendState = WorkitContract.FRIENDS_STATE_NOT_FRIENDS;
                                mSendReqBtn.setText(R.string.profile_send_friend_request);
                            }
                            mSendReqBtn.setEnabled(true);
                        }
                    });
                    mDeclineReqBtn.setVisibility(View.INVISIBLE);
                    mDeclineReqBtn.setEnabled(false);
                    mProgress.dismiss();
                    break;

                //-------------- Request received -------------
                case WorkitContract.FRIENDS_STATE_RECEIVED:

                    final String currDate = DateFormat.getDateTimeInstance().format(new Date());

                    Map friendsMap = new HashMap();
                    friendsMap.put(WorkitContract.FRIENDS_SCHEMA + "/" + mCurUser.getUid() + "/" + profileUserId + "/" + WorkitContract.FRIEND_FRIENDSHIP_DATE, currDate);
                    friendsMap.put(WorkitContract.FRIENDS_SCHEMA + "/" + profileUserId + "/" + mCurUser.getUid() + "/" + WorkitContract.FRIEND_FRIENDSHIP_DATE, currDate);

                    friendsMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + mCurUser.getUid() + "/" + profileUserId, null);
                    friendsMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + profileUserId + "/" + mCurUser.getUid(), null);

                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                           if(databaseError != null) {
                               Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_LONG).show();
                           }
                           else {
                               mCurrentFriendState = WorkitContract.FRIENDS_STATE_FRIENDS;
                               mSendReqBtn.setText(R.string.profile_unfriend);
                               mDeclineReqBtn.setVisibility(View.INVISIBLE);
                               mDeclineReqBtn.setEnabled(false);

                           }
                            mSendReqBtn.setEnabled(true);
                            mProgress.dismiss();
                        }
                    });
                    break;

                    //----- FRIENDS STATE - unfriend ------
                    case WorkitContract.FRIENDS_STATE_FRIENDS:
                        mSendReqBtn.setEnabled(false);

                        Map unfriend = new HashMap();
                        unfriend.put(WorkitContract.FRIENDS_SCHEMA + "/" + profileUserId + "/" + mCurUser.getUid(), null);
                        unfriend.put(WorkitContract.FRIENDS_SCHEMA + "/" + mCurUser.getUid() + "/" + profileUserId, null);

                        mRootRef.updateChildren(unfriend, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                if (databaseError == null){
                                    mCurrentFriendState = WorkitContract.FRIENDS_STATE_NOT_FRIENDS;
                                    mSendReqBtn.setText(R.string.profile_send_friend_request);
                                    mSendReqBtn.setEnabled(true);
                                    mDeclineReqBtn.setVisibility(View.INVISIBLE);
                                    mDeclineReqBtn.setEnabled(false);

                                }
                                else {
                                    Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_LONG).show();
                                }
                                mProgress.dismiss();
                            }
                        });
                }
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        //change online field in users database to true.
        mRootRef.child(WorkitContract.USERS_SCHEMA).child(mCurUser.getUid()).child(WorkitContract.USERS_COLUMN_ONLINE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean online = (boolean) dataSnapshot.getValue();
                if (!online){
                    mRootRef.child(WorkitContract.USERS_SCHEMA).child(mCurUser.getUid()).child(WorkitContract.USERS_COLUMN_ONLINE).setValue(true);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        //update online field in users -> user, also update last seen field.
        mRootRef.child(WorkitContract.USERS_SCHEMA).child(mCurUser.getUid()).child(WorkitContract.USERS_COLUMN_ONLINE).setValue(false);
        mRootRef.child(WorkitContract.USERS_SCHEMA).child(mCurUser.getUid()).child(WorkitContract.USERS_COLUMN_LAST_SEEN).setValue(ServerValue.TIMESTAMP);
    }

    /**
     * set profile button text according to friends state.
     *
     */
    private void setBtnText() {
        if (mCurUser.getUid().equals(profileUserId)){
            mSendReqBtn.setVisibility(View.GONE);
        }
        mFriendRequestDatabase.child(mCurUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild(profileUserId)){
                    int reqType = Integer.parseInt(dataSnapshot.child(profileUserId).child(WorkitContract.FRIENDS_REQ_TYPE).getValue().toString());
                    switch (reqType){
                        case WorkitContract.FRIENDS_STATE_RECEIVED:
                            mCurrentFriendState = WorkitContract.FRIENDS_STATE_RECEIVED;
                            mSendReqBtn.setText(R.string.profile_accept_req);
                            mDeclineReqBtn.setVisibility(View.VISIBLE);
                            mDeclineReqBtn.setEnabled(true);
                            break;
                        case WorkitContract.FRIENDS_STATE_SENT:
                            mCurrentFriendState = WorkitContract.FRIENDS_STATE_SENT;
                            mSendReqBtn.setText(R.string.profile_cancel_friend_request);
                            mDeclineReqBtn.setVisibility(View.INVISIBLE);
                            mDeclineReqBtn.setEnabled(false);
                            break;
                    }
                    mProgress.dismiss();
                }
                else {
                    mFriendsDatabase.child(mCurUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(profileUserId)){
                                mCurrentFriendState = WorkitContract.FRIENDS_STATE_FRIENDS;
                                mSendReqBtn.setText(R.string.profile_unfriend);
                                mDeclineReqBtn.setVisibility(View.INVISIBLE);
                                mDeclineReqBtn.setEnabled(false);
                            }
                            mProgress.dismiss();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            mProgress.dismiss();
                        }
                    });
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mProgress.dismiss();
            }
        });
    }

    /**
     *
     * @param title
     * @param message
     * set progress dialog
     */
    private void setProgressBar(String title, String message) {
        mProgress = new ProgressDialog(this);
        mProgress.setTitle(title);
        mProgress.setMessage(message);
        mProgress.setCanceledOnTouchOutside(false);

    }

}
