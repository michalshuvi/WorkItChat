package com.example.workitchat;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 * Placed in Main Activity.
 * Display all Friends request: sent and received.
 */
public class RequestFragment extends Fragment {

    private DatabaseReference mRootRef;
    private DatabaseReference mUserRequestRef;
    private FirebaseAuth mAuth;
    private String mCurUserId;
    private View mMainView;
    private RecyclerView mReqListView;
    private String mOtherUserId;


    public RequestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMainView = inflater.inflate(R.layout.fragment_request, container, false);
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mCurUserId = mAuth.getCurrentUser().getUid();
        //for offline use
        mRootRef.child(WorkitContract.FRIEND_REQUEST_SCHEMA).child(mCurUserId).keepSynced(true);

        mUserRequestRef = mRootRef.child(WorkitContract.FRIEND_REQUEST_SCHEMA).child(mCurUserId);
        mUserRequestRef.keepSynced(true);

        mReqListView = (RecyclerView) mMainView.findViewById(R.id.requests_list);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mReqListView.setHasFixedSize(true);
        mReqListView.setLayoutManager(linearLayoutManager);

        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Query query = mUserRequestRef;

        FirebaseRecyclerOptions<Request> options = new FirebaseRecyclerOptions.Builder<Request>()
                .setQuery(query, Request.class)
                .build();

        FirebaseRecyclerAdapter<Request, RequestViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Request, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull Request request) {

                mOtherUserId = getRef(position).getKey();

                Log.v("HERE", mOtherUserId);

                mRootRef.child(WorkitContract.USERS_SCHEMA).child(mOtherUserId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.hasChild(WorkitContract.USERS_COLUMN_NAME)) {
                            String name = dataSnapshot.child(WorkitContract.USERS_COLUMN_NAME).getValue().toString();
                            String thumb = dataSnapshot.child(WorkitContract.USERS_COLUMN_THUMB_IMAGE).getValue().toString();
                            String status = dataSnapshot.child(WorkitContract.USERS_COLUMN_STATUS).getValue().toString();
                            boolean userOnline = (boolean) dataSnapshot.child(WorkitContract.USERS_COLUMN_ONLINE).getValue();

                            holder.setName(name);
                            holder.setThumb(thumb);
                            holder.setOnlineSign(userOnline);
                            holder.setStatus(status);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                //clicking on list item sends to corresponding profile.
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent profileIntent = new Intent(mMainView.getContext(), ProfileActivity.class);
                        profileIntent.putExtra("userId", mOtherUserId);
                        startActivity(profileIntent);
                    }
                });

                //handle buttons according to friends state.
                switch (request.getRequest_type()) {

                    //received request
                    case WorkitContract.FRIENDS_STATE_RECEIVED:
                        holder.setRecievedBtns();
                        //become friends - update friends, notification databases and delete request from requests database.
                        holder.mAccept.setOnClickListener(v -> {

                            final String currDate = DateFormat.getDateTimeInstance().format(new Date());

                            Map friendsMap = new HashMap();
                            friendsMap.put(WorkitContract.FRIENDS_SCHEMA + "/" + mCurUserId + "/" + mOtherUserId + "/" + WorkitContract.FRIEND_FRIENDSHIP_DATE, currDate);
                            friendsMap.put(WorkitContract.FRIENDS_SCHEMA + "/" + mOtherUserId + "/" + mCurUserId + "/" + WorkitContract.FRIEND_FRIENDSHIP_DATE, currDate);

                            friendsMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + mCurUserId + "/" + mOtherUserId, null);
                            friendsMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + mOtherUserId + "/" + mCurUserId, null);

                            mRootRef.updateChildren(friendsMap, (databaseError, databaseReference) -> {
                                if(databaseError != null) {
                                    Toast.makeText(mMainView.getContext(), databaseError.getMessage(), Toast.LENGTH_LONG).show();
                                }
                                else {
                                    Toast.makeText(mMainView.getContext(), "You are now friends!", Toast.LENGTH_SHORT).show();
                                }
                            });

                        });

                        //decline friend request - delete request from requests database.
                        holder.mDecline.setOnClickListener(v -> {
                            Map deleteReqMap = new HashMap();
                            deleteReqMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" +mCurUserId + "/" + mOtherUserId, null);
                            deleteReqMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + mOtherUserId + "/" + mCurUserId, null);

                            mRootRef.updateChildren(deleteReqMap, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                    if (databaseError != null){
                                        Toast.makeText(mMainView.getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        Toast.makeText(mMainView.getContext(), "Request Declined", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        });
                        break;

                    //sent a request
                    case WorkitContract.FRIENDS_STATE_SENT:

                        holder.setSentBtns();
                        //cancel sent request, delete request from requests database.
                        holder.mCancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Map declineMap = new HashMap();
                                declineMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + mOtherUserId + "/" + mCurUserId, null);
                                declineMap.put(WorkitContract.FRIEND_REQUEST_SCHEMA + "/" + mCurUserId + "/" + mOtherUserId, null);

                                mRootRef.updateChildren(declineMap, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            Toast.makeText(mMainView.getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            Toast.makeText(mMainView.getContext(), "Request Canceled", Toast.LENGTH_SHORT).show();
                                        }

                                    }
                                });
                            }
                        });
                        break;
                }
            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.request_list_item, parent, false);

                return new RequestViewHolder(view);
            }
        };

        mReqListView.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
        
    }

    public class RequestViewHolder extends RecyclerView.ViewHolder {

        View mView;
        Button mCancel;
        Button mAccept;
        Button mDecline;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            this.mView = itemView;
        }

        public void setName(String userName){
            TextView nameView = (TextView) itemView.findViewById(R.id.req_name);
            nameView.setText(userName);

        }

        public void setThumb(String thumb){
            ImageView userImg = (CircleImageView) mView.findViewById(R.id.req_image);
            Picasso.get().load(thumb).placeholder(R.drawable.avatar).into(userImg);

        }

        public void setOnlineSign(boolean onlineStatus){
            ImageView userOnlineStatus = (ImageView) mView.findViewById(R.id.req_online_signal);
            GradientDrawable circle = (GradientDrawable) userOnlineStatus.getDrawable();
            if (!onlineStatus){

                circle.setColor(Color.RED);

            } else {
                circle.setColor(Color.GREEN);
            }
            userOnlineStatus.setVisibility(View.VISIBLE);

        }

        public void setStatus(String status){
            TextView userStatusView = (TextView) mView.findViewById(R.id.req_status);
            userStatusView.setText(status);
        }

        public void setRecievedBtns(){

            mCancel = mView.findViewById(R.id.req_cancel_btn);
            mAccept = mView.findViewById(R.id.req_accept_btn);
            mDecline = mView.findViewById(R.id.req_decline_btn);

            mCancel.setVisibility(View.GONE);
            mAccept.setVisibility(View.VISIBLE);
            mDecline.setVisibility(View.VISIBLE);
        }

        public void setSentBtns() {

            mCancel = mView.findViewById(R.id.req_cancel_btn);
            mAccept = mView.findViewById(R.id.req_accept_btn);
            mDecline = mView.findViewById(R.id.req_decline_btn);

            mAccept.setVisibility(View.GONE);
            mDecline.setVisibility(View.GONE);
            mCancel.setVisibility(View.VISIBLE);

        }
    }
}
