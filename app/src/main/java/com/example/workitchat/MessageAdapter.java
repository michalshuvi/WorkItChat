package com.example.workitchat;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> mMessageList;
    Context mContext;
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;

    public MessageAdapter(List<Message> messageList, Context context) {
        this.mMessageList = messageList;
        this.mContext = context;
    }


    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.message_single_layout, viewGroup, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder messageViewHolder, int i) {

        mAuth = FirebaseAuth.getInstance();
        Message message = mMessageList.get(i);

        String fromUserId = message.getFrom();
        int messageType = message.getType();
        long messageTime = message.getTime();

        mRootRef = FirebaseDatabase.getInstance().getReference();

        //get from_user data
        mRootRef.child(WorkitContract.USERS_SCHEMA).child(fromUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child(WorkitContract.USERS_COLUMN_NAME).getValue().toString();
                String thumbImg = dataSnapshot.child(WorkitContract.USERS_COLUMN_THUMB_IMAGE).getValue().toString();

                messageViewHolder.nameView.setText(name);
                Picasso.get().load(thumbImg).placeholder(R.drawable.avatar).into(messageViewHolder.profileImage);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        //handle images
        switch (messageType){
            case WorkitContract.MESSAGES_COLUMN_TYPE_TEXT:
                messageViewHolder.messageText.setVisibility(View.VISIBLE);
                messageViewHolder.messageImage.setVisibility(View.GONE);
                messageViewHolder.messageText.setText(message.getMessage().trim());
                break;
            case WorkitContract.MESSAGES_COLUMN_TYPE_IMAGE:
                Picasso.get().load(message.getThumb()).placeholder(R.drawable.avatar).into(messageViewHolder.messageImage);
                messageViewHolder.messageImage.setVisibility(View.VISIBLE);
                messageViewHolder.messageText.setVisibility(View.GONE);
                break;
        }

        messageViewHolder.messageTime.setText(TimeAgo.getTimeAgo(messageTime));

        messageViewHolder.messageImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, FullScreenImage.class);
                intent.putExtra("imageUri", message.getMessage());
                mContext.startActivity(intent);
            }
        });


    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        private TextView messageText;
        private ImageView profileImage;
        private TextView nameView;
        private ImageView messageImage;
        private TextView messageTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.chat_single_massage_text);
            profileImage = (CircleImageView) itemView.findViewById(R.id.chat_message_img);
            nameView = (TextView) itemView.findViewById(R.id.chat_message_name);
            messageImage = (ImageView) itemView.findViewById(R.id.chat_message_sent_image);
            messageTime = (TextView) itemView.findViewById(R.id.chat_message_time);

        }
    }
}
