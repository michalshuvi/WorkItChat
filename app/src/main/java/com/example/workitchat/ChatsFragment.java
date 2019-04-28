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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A {@link Fragment} subclass.
 * part of the Main Activity.
 * display all chats of current user, ordered by time of the last message (descending).
 */
public class ChatsFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private DatabaseReference mUserChatsRef;
    private String mCurUserId;
    private View mMainView;
    private String mOtherUserId;
    private RecyclerView mConvList;


    public ChatsFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        mMainView = inflater.inflate(R.layout.fragment_chats, container, false);

        mAuth = FirebaseAuth.getInstance();
        mCurUserId = mAuth.getCurrentUser().getUid();

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUserChatsRef = mRootRef.child(WorkitContract.CHAT_SCHEMA).child(mCurUserId);
        mUserChatsRef.keepSynced(true);

        mConvList = (RecyclerView) mMainView.findViewById(R.id.chats_list);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mConvList.setHasFixedSize(true);
        mConvList.setLayoutManager(linearLayoutManager);

        return mMainView;

    }

    @Override
    public void onStart() {
        super.onStart();

        Query query = mUserChatsRef.orderByChild(WorkitContract.CHAT_COLUMN_TIMESTAMP);

        FirebaseRecyclerOptions<Conversations> options = new FirebaseRecyclerOptions.Builder<Conversations>()
                .setQuery(query, Conversations.class)
                .build();

        FirebaseRecyclerAdapter<Conversations, ConversationsViewHolder> firebaseRecyclerAdapter  = new FirebaseRecyclerAdapter<Conversations, ConversationsViewHolder>(options) {

            @NonNull
            @Override
            public ConversationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.all_users_list_item, parent, false);

                return new ConversationsViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ConversationsViewHolder holder, int position, @NonNull Conversations conversations) {

                mOtherUserId = getRef(position).getKey();


                mRootRef.child(WorkitContract.USERS_SCHEMA).child(mOtherUserId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String userName = dataSnapshot.child(WorkitContract.USERS_COLUMN_NAME).getValue().toString();
                        String userThumb = dataSnapshot.child(WorkitContract.USERS_COLUMN_THUMB_IMAGE).getValue().toString();
                        boolean userOnline = (boolean) dataSnapshot.child(WorkitContract.USERS_COLUMN_ONLINE).getValue();

                        holder.setName(userName);
                        holder.setThumb(userThumb);
                        holder.setOnlineSign(userOnline);

                        mOtherUserId = getRef(position).getKey();

                        Query lastMessage = mRootRef.child(WorkitContract.MESSAGES_SCHEMA).child(mOtherUserId).child(mCurUserId).limitToLast(1);
                        lastMessage.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                                if (dataSnapshot.hasChild(WorkitContract.MESSAGES_COLUMN_MESSAGE)) {
                                    String data = dataSnapshot.child(WorkitContract.MESSAGES_COLUMN_MESSAGE).getValue().toString();
                                    String from = dataSnapshot.child(WorkitContract.MESSAGES_COLUMN_FROM).getValue().toString();
                                    int type = Integer.parseInt(dataSnapshot.child(WorkitContract.MESSAGES_COLUMN_TYPE).getValue().toString());

                                    //handle images
                                    switch (type) {
                                        case WorkitContract.MESSAGES_COLUMN_TYPE_TEXT:
                                            holder.setLastMessage(data);
                                            break;
                                        case WorkitContract.MESSAGES_COLUMN_TYPE_IMAGE:
                                            holder.setLastMessage("Image");
                                            break;
                                    }
                                }
                                else {
                                    holder.setLastMessage("");
                                }
                            }


                            @Override
                            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                            }

                            @Override
                            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                            }

                            @Override
                            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                String userId = getRef(position).getKey();

                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                chatIntent.putExtra("userId", userId);
                                chatIntent.putExtra("userName", userName);
                                chatIntent.putExtra("userThumb",userThumb );

                                startActivity(chatIntent);
                            }
                        });



                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });



            }
        };

        mConvList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();

    }

    public class ConversationsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public ConversationsViewHolder(@NonNull View itemView) {
            super(itemView);
            this.mView = itemView;
        }

        public void setName(String userName){
            TextView nameView = (TextView) itemView.findViewById(R.id.all_users_name);
            nameView.setText(userName);

        }

        public void setThumb(String thumb){
            ImageView userImg = (CircleImageView) mView.findViewById(R.id.all_users_image);
            Picasso.get().load(thumb).placeholder(R.drawable.avatar).into(userImg);

        }

        public void setLastMessage(String message){
            TextView messageView = (TextView) itemView.findViewById(R.id.all_users_status);
            messageView.setText(message);
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
