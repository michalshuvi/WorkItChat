package com.example.workitchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.zelory.compressor.Compressor;

/**
 * Chat Activity contains one-on-one chat window.
 * abilities: sending messages, images, loading old messages.
 *
 *
 */

public class ChatActivity extends AppCompatActivity {

    //Appbar fields.
    private Toolbar mChatToolbar;
    private String mChatUserName;
    private String mChatUserThumb;

    //footer fields.
    private ImageButton mAddBtn;
    private ImageButton mSendBtn;
    private EditText mMessageEditText;

    private TextView mTitleView;
    private TextView mLastSeenView;
    private ImageView mProfileImageView;

    //for loading more messages.
    private SwipeRefreshLayout mSwipeLayout;

    //Firebase fields.
    private DatabaseReference mRootRef;
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private String mChatUserId;
    private String mCurUserId;


    private RecyclerView mMessagesRecView;
    private MessageAdapter mAdapter;

    private ProgressDialog mProgress;

    //fields to maintain the load more messages feature.
    private static final int NUMBER_OF_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;
    private String mLastKey = "";
    private String mPrevKey = "";
    private int itemPos = 0;

    private final List<Message> mMessagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;

    private final String LOG_TAG = ChatActivity.class.getSimpleName();

    @Override
    protected void onStart() {
        super.onStart();
        mRootRef.child(WorkitContract.USERS_SCHEMA).child(mCurUserId).child(WorkitContract.USERS_COLUMN_ONLINE).setValue(true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mRootRef = FirebaseDatabase.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();
        mCurUserId = mAuth.getCurrentUser().getUid();

        mChatUserId = getIntent().getExtras().getString("userId");
        mChatUserName =  getIntent().getExtras().getString("userName");
        mChatUserThumb =  getIntent().getExtras().getString("userThumb");

        //keep messages synced for offline
        mRootRef.child(WorkitContract.MESSAGES_SCHEMA).child(mCurUserId).child(mChatUserId).keepSynced(true);

        mChatToolbar = (Toolbar) findViewById(R.id.chat_app_bar);

        mProgress = new ProgressDialog(this);

        //setting appbar
        setSupportActionBar(mChatToolbar);
        ActionBar chatActionBar = getSupportActionBar();
        chatActionBar.setDisplayHomeAsUpEnabled(true);
        chatActionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBar = inflater.inflate(R.layout.chat_costum_bar, null);
        chatActionBar.setCustomView(actionBar);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        //Recycler View and Adapter
        mAdapter = new MessageAdapter(mMessagesList, this);
        mMessagesRecView = (RecyclerView) findViewById(R.id.chat_messages_recyclerView);
        mMessagesRecView.setHasFixedSize(true);
        mLinearLayout = new LinearLayoutManager(this);
        mMessagesRecView.setLayoutManager(mLinearLayout);
        mMessagesRecView.setAdapter(mAdapter);
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);

        loadMessages();

        // APP BAR FEATURES
        mTitleView = (TextView) findViewById(R.id.chat_appbar_name);
        mLastSeenView = (TextView) findViewById(R.id.chat_appbar_lastseen);
        mProfileImageView = (ImageView) findViewById(R.id.chat_appbar_image);

        mTitleView.setText(mChatUserName);
        if (!mChatUserThumb.equals("default")) {
            Picasso.get().load(mChatUserThumb).networkPolicy(NetworkPolicy.OFFLINE).fit().into(mProfileImageView, new Callback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load(mChatUserThumb).centerCrop().placeholder(R.drawable.avatar).into(mProfileImageView);
                }
            });
        }

        //EDIT TEXT AREA (footer)
        mAddBtn = (ImageButton) findViewById(R.id.chat_add_btn);
        mSendBtn = (ImageButton) findViewById(R.id.chat_send_btn);
        mMessageEditText = (EditText) findViewById(R.id.chat_message_text);

        //set other user last seen.
        setChattedUserLastSeen();

        // Creates a new chat instance in chats schema in Firebase realtime database.
        addChatToDatabase();

       // EDIT TEXT AREA FUNCTIONS
       mSendBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               sendMessage();
           }
       });


       mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
           @Override
           public void onRefresh() {
               mCurrentPage++;
               itemPos = 0;
               loadMoreMessages();
           }
       });

       //add photo - opens a picker
       mAddBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               CropImage.activity()
                       .setGuidelines(CropImageView.Guidelines.ON)
                       .start(ChatActivity.this);
           }
       });

    }

    /**
     * add a new chat if it's not already exist. in Root->chats
     */
    private void addChatToDatabase() {
        mRootRef.child(WorkitContract.CHAT_SCHEMA).child(mCurUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(mChatUserId)){

                }
                else {
                    Map chatAddMap = new HashMap<>();
                    chatAddMap.put(WorkitContract.CHAT_COLUMN_SEEN, false);
                    chatAddMap.put(WorkitContract.CHAT_COLUMN_TIMESTAMP, ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put(WorkitContract.CHAT_SCHEMA + "/" + mCurUserId + "/" + mChatUserId, chatAddMap);
                    chatUserMap.put(WorkitContract.CHAT_SCHEMA + "/" + mChatUserId + "/" + mCurUserId, chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError == null){

                            }
                            else {
                                Log.v(LOG_TAG, databaseError.getMessage());
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    /**
     * checks if the other user is online and sets last seen according to the last time he went offline.
     */
    private void setChattedUserLastSeen() {
        mRootRef.child(WorkitContract.USERS_SCHEMA).child(mChatUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean online = (boolean) dataSnapshot.child(WorkitContract.USERS_COLUMN_ONLINE).getValue();
                long lastSeen = (long) dataSnapshot.child(WorkitContract.USERS_COLUMN_LAST_SEEN).getValue();

                if (online){
                    mLastSeenView.setText("Online");
                } else {
                    mLastSeenView.setText(TimeAgo.getTimeAgo(lastSeen));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            //get cropped image result
            CropImage.ActivityResult imgResult = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                mProgress.setTitle("Uploading Image");
                mProgress.setMessage("Wait a minute...");
                mProgress.setCanceledOnTouchOutside(false);
                mProgress.show();

                Uri resultUri = imgResult.getUri();

                File thumbFilePath = new File(resultUri.getPath());
                byte[] thumbByte = null;

                //compress image into a bitmap file, fixed size of 200x200 pxl^2
                try {
                    Bitmap compressedImageBitmap = new Compressor(this)
                            .setMaxHeight(300)
                            .setMaxWidth(300)
                            .setQuality(80)
                            .compressToBitmap(thumbFilePath);

                    //from bitmap file to byte array stream
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    thumbByte = baos.toByteArray();

                } catch (IOException e) {
                    Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                final byte[] thumbBytes = thumbByte;

                String curUserRef = WorkitContract.MESSAGES_SCHEMA + "/" + mCurUserId + "/" + mChatUserId;
                String chatUserRef = WorkitContract.MESSAGES_SCHEMA + "/" + mChatUserId + "/" + mCurUserId;

                DatabaseReference curUserMessagePush = mRootRef.child(WorkitContract.MESSAGES_SCHEMA).child(mCurUserId).child(mChatUserId).push();
                String curUserMessageId = curUserMessagePush.getKey();

                StorageReference filePath = mStorageRef.child(WorkitContract.STORAGE_MESSAGE_IMAGES).child(curUserMessageId + ".jpg");
                StorageReference thumbPath = mStorageRef.child(WorkitContract.STORAGE_MESSAGE_IMAGES).child(WorkitContract.STORAGE_MESSAGE_THUMB_IMAGES).child(curUserMessageId + ".jpg");

                filePath.putFile(resultUri).addOnCompleteListener(task -> {
                    if (task.isSuccessful()){

                        filePath.getDownloadUrl().addOnSuccessListener(uri -> {

                            String downloadUri = uri.toString();

                            if (thumbBytes != null){
                                thumbPath.putBytes(thumbBytes).addOnSuccessListener(taskSnapshot -> thumbPath.getDownloadUrl().addOnSuccessListener(uri1 -> {

                                    String downloadThumbUri = uri1.toString();

                                    writeMessageToDB(downloadUri, WorkitContract.MESSAGES_COLUMN_TYPE_IMAGE, downloadThumbUri);

                                    mProgress.dismiss();

                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        mProgress.dismiss();
                                        Toast.makeText(ChatActivity.this,"Error Uploading Thumbnail image " +
                                                e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                })).addOnFailureListener(e -> {
                                    mProgress.dismiss();
                                    Toast.makeText(ChatActivity.this,"Error Uploading Thumbnail image " +
                                            e.getMessage(), Toast.LENGTH_SHORT).show();

                                });
                            }

                        }).addOnFailureListener(e -> Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                    else {
                        Toast.makeText(ChatActivity.this, "Error Occurred: " + task.getResult().getError().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });



            }
        }
    }

    /**
     * loads more NUMBER_OF_ITEMS_TO_LOAD messages and move to the beginning of them.
     */
    private void loadMoreMessages() {

        DatabaseReference messageRef = mRootRef.child(WorkitContract.MESSAGES_SCHEMA)
                .child(mCurUserId).child(mChatUserId);

        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(ChatActivity.NUMBER_OF_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Message message = dataSnapshot.getValue(Message.class);
                String messageKey = dataSnapshot.getKey();

                if(!mPrevKey.equals(messageKey)){
                    mMessagesList.add(itemPos++, message);
                } else {
                    mPrevKey = mLastKey;
                }

                if(itemPos == 1) {
                    mLastKey = messageKey;
                }

                mAdapter.notifyDataSetChanged();

                mSwipeLayout.setRefreshing(false);

                mLinearLayout.scrollToPositionWithOffset(Math.min(itemPos, itemPos - ChatActivity.NUMBER_OF_ITEMS_TO_LOAD), 0);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                mSwipeLayout.setRefreshing(false);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                mSwipeLayout.setRefreshing(false);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                mSwipeLayout.setRefreshing(false);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mSwipeLayout.setRefreshing(false);

            }

        });

    }

    /**
     * Loads messages for the first time
     */
    private void loadMessages() {

        DatabaseReference messageRef = mRootRef.child(WorkitContract.MESSAGES_SCHEMA).child(mCurUserId).child(mChatUserId);
        Query messageQuery = messageRef.limitToLast(NUMBER_OF_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Message message = dataSnapshot.getValue(Message.class);

                itemPos++;

                if(itemPos == 1){
                    String messageKey = dataSnapshot.getKey();
                    mLastKey = messageKey;
                    mPrevKey = messageKey;
                }

                mMessagesList.add(message);
                mAdapter.notifyDataSetChanged();
                mMessagesRecView.scrollToPosition(mMessagesList.size() - 1);
                mSwipeLayout.setRefreshing(false);

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

    }

    private void sendMessage() {

        String message = mMessageEditText.getText().toString();
        if (!TextUtils.isEmpty(message)){

            writeMessageToDB(message, WorkitContract.MESSAGES_COLUMN_TYPE_TEXT, null);

        }
    }

    /**
     * writes a message to DB: to notification schema and messages schema
     * @param content
     * @param type
     */
    private void writeMessageToDB(String content, int type, String thumbImage){

        DatabaseReference curUserMessagePush = mRootRef.child(WorkitContract.MESSAGES_SCHEMA).child(mCurUserId).child(mChatUserId).push();
        String curUserMessageId = curUserMessagePush.getKey();

        DatabaseReference notificationRef = mRootRef.child(WorkitContract.NOTIFICATIONS_SCHEMA).push();
        String newNotificationId = notificationRef.getKey();

        String curUserRef = WorkitContract.MESSAGES_SCHEMA + "/" + mCurUserId + "/" + mChatUserId;
        String chatUserRef = WorkitContract.MESSAGES_SCHEMA + "/" + mChatUserId + "/" + mCurUserId;

        String notificationDataRef = WorkitContract.NOTIFICATIONS_SCHEMA + "/" + mChatUserId + "/" + newNotificationId;

        Map notificationMap = new HashMap();
        notificationMap.put(WorkitContract.NOTIFICATIONS_COLUMN_FROM, mCurUserId);
        notificationMap.put(WorkitContract.NOTIFICATIONS_COLUMN_TYPE, WorkitContract.NOTIFICATIONS_MESSAGE_TYPE);
        notificationMap.put(WorkitContract.NOTIFICATIONS_COLUMN_CONTENT_TYPE, type);
        notificationMap.put(WorkitContract.NOTIFICATIONS_COLUMN_MESSAGE_ID, curUserMessageId);


        Map messageMap = new HashMap();
        messageMap.put(WorkitContract.MESSAGES_COLUMN_MESSAGE, content);
        messageMap.put(WorkitContract.MESSAGES_COLUMN_SEEN, false);
        messageMap.put(WorkitContract.MESSAGES_COLUMN_TYPE, type);
        messageMap.put(WorkitContract.MESSAGES_COLUMN_TIME, ServerValue.TIMESTAMP);
        messageMap.put(WorkitContract.MESSAGES_COLUMN_FROM, mCurUserId);
        if ((type == WorkitContract.MESSAGES_COLUMN_TYPE_IMAGE) && thumbImage != null){
            messageMap.put(WorkitContract.MESSAGES_COLUMN_thumb, thumbImage);
        }

        Map messageUserMap = new HashMap();
        messageUserMap.put(curUserRef + "/" + curUserMessageId, messageMap);
        messageUserMap.put(chatUserRef + "/" + curUserMessageId, messageMap);
        messageUserMap.put(notificationDataRef, notificationMap);

        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                if (databaseError != null){
                    Log.v(LOG_TAG, databaseError.getMessage());
                }
                else {
                    mMessageEditText.setText("");
                }
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();

        mRootRef.child(WorkitContract.USERS_SCHEMA).child(mCurUserId).child(WorkitContract.USERS_COLUMN_ONLINE).setValue(false);
        mRootRef.child(WorkitContract.USERS_SCHEMA).child(mCurUserId).child(WorkitContract.USERS_COLUMN_LAST_SEEN).setValue(ServerValue.TIMESTAMP);

    }
}
