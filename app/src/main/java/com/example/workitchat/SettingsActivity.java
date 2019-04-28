package com.example.workitchat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import id.zelory.compressor.Compressor;

/**
 * Setting Activity - change image, status.
 * insert profile image: create thumbnail image and store it aswell.
 */
public class SettingsActivity extends AppCompatActivity {

    private ImageView mImageView;
    private TextView mNameTextView;
    private TextView mStatusTextView;
    private Button mChangeImage;
    private Button mChangeStatus;
    private TextInputEditText mChangeStatusEditText;
    private RelativeLayout mYesNo;
    private ProgressDialog mProgress;


    //realtime database
    private DatabaseReference mUserDatabase;
    private FirebaseUser mCurrentUser;

    //storage
    private StorageReference mStorageRef;

    @Override
    protected void onPause() {
        super.onPause();
        //update online to false and update last seen time.
        mUserDatabase.child(WorkitContract.USERS_COLUMN_ONLINE).setValue(false);
        mUserDatabase.child(WorkitContract.USERS_COLUMN_LAST_SEEN).setValue(ServerValue.TIMESTAMP);
    }


    @Override
    protected void onStart() {
        super.onStart();

        //make sure the user is online on database.
        mUserDatabase.child(WorkitContract.USERS_COLUMN_ONLINE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean online = (boolean) dataSnapshot.getValue();
                if (!online) {
                    mUserDatabase.child(WorkitContract.USERS_COLUMN_ONLINE).setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mImageView = (ImageView) findViewById(R.id.settings_img);
        mNameTextView = (TextView) findViewById(R.id.settings_name);
        mStatusTextView = (TextView) findViewById(R.id.settings_status);
        mChangeImage = (Button) findViewById(R.id.settings_change_image);
        mChangeStatus = (Button) findViewById(R.id.settings_change_status);
        mChangeStatusEditText = (TextInputEditText) findViewById(R.id.settings_status_editText);
        mYesNo = (RelativeLayout) findViewById(R.id.settings_change_status_yes_no);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child(WorkitContract.USERS_SCHEMA).child(mCurrentUser.getUid());
        mUserDatabase.keepSynced(true);

        //retrieve current data about the user from Database.
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String name = dataSnapshot.child(WorkitContract.USERS_COLUMN_NAME).getValue().toString();
                final String image = dataSnapshot.child(WorkitContract.USERS_COLUMN_IMAGE).getValue().toString();
                String status = dataSnapshot.child(WorkitContract.USERS_COLUMN_STATUS).getValue().toString();
                String thumb_image = dataSnapshot.child(WorkitContract.USERS_COLUMN_THUMB_IMAGE).getValue().toString();

                mNameTextView.setText(name);
                mStatusTextView.setText(status);
                uploadImage(image, mImageView);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        //clicking on change status opens an Edit text view.
        mChangeStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChangeStatus.setVisibility(View.GONE);

                mChangeStatusEditText.setText(mStatusTextView.getText());
                mChangeStatusEditText.setVisibility(View.VISIBLE);
                mYesNo.setVisibility(View.VISIBLE);

                ImageView dontChangeStatusBtn = (ImageView) findViewById(R.id.settings_dont_save_status);
                ImageView changeStatusBtn = (ImageView) findViewById(R.id.settings_save_status);

                dontChangeStatusBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                       fromChangeModeToSettings();
                    }
                });

                changeStatusBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showProgressBar();

                        mUserDatabase.child(WorkitContract.USERS_COLUMN_STATUS).setValue(mChangeStatusEditText.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()){
                                    mProgress.dismiss();
                                    fromChangeModeToSettings();
                                } else {
                                    Toast.makeText(SettingsActivity.this, "There is some problem, please try again later.", Toast.LENGTH_LONG).show();
                                }
                            }
                        });

                    }
                });

            }
        });
        //clicking on change image button opens a picker.
        mChangeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start picker to get image for cropping and then use the image in cropping activity
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .start(SettingsActivity.this);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            //get cropping image result
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                mProgress = new ProgressDialog(SettingsActivity.this);
                mProgress.setTitle("Uploading Image");
                mProgress.setMessage("Wait a minute...");
                mProgress.setCanceledOnTouchOutside(false);
                mProgress.show();

                Uri resultUri = result.getUri();
                String currUserId = mCurrentUser.getUid();

                File thumbFilePath = new File(resultUri.getPath());
                byte[] thumbByte = null;

                //compress image into a bitmap file, fixed size of 200x200 pxl^2
                try {
                    Bitmap compressedImageBitmap = new Compressor(this)
                            .setMaxHeight(200)
                            .setMaxWidth(200)
                            .setQuality(75)
                            .compressToBitmap(thumbFilePath);

                    //from bitmap file to byte array stream
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    thumbByte = baos.toByteArray();

                } catch (IOException e) {
                    Toast.makeText(SettingsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                final byte[] thumbBytes = thumbByte;

                //Images references in storage
                final StorageReference imagePath = mStorageRef.child(WorkitContract.STORAGE_PROFILE_IMAGES).child(currUserId + ".jpg");
                final StorageReference thumbPath = mStorageRef.child(WorkitContract.STORAGE_PROFILE_IMAGES)
                        .child(WorkitContract.STORAGE_THUMB_IN_PROFILE_IMAGE)
                        .child(currUserId + ".jpg");

                //upload both images: normal and thumbnail
                //1.Upload image to storage
                imagePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){

                            //get image uri to put in "image" key on database
                          imagePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                              @Override
                              public void onSuccess(Uri uri) {
                                  final String downloadUri = uri.toString();

                                  if (thumbBytes != null) {

                                      //2.Storing thumbnail image in Storage - happens only if original image stored successfully
                                      thumbPath.putBytes(thumbBytes).addOnFailureListener(new OnFailureListener() {
                                          @Override
                                          public void onFailure(@NonNull Exception exception) {
                                              mProgress.dismiss();
                                              Toast.makeText(SettingsActivity.this,"Error Uploading Thumbnail image " +
                                                      exception.getMessage(), Toast.LENGTH_SHORT).show();
                                          }
                                      }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                          @Override
                                          public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                              //get thumb image path for storing in database
                                                thumbPath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri uri) {
                                                        final String downloadThumbUri = uri.toString();

                                                        Map images = new HashMap<>();
                                                        images.put(WorkitContract.USERS_COLUMN_IMAGE, downloadUri);
                                                        images.put(WorkitContract.USERS_COLUMN_THUMB_IMAGE,downloadThumbUri);

                                                        //Set image and thumb image URIs in user's directory in database
                                                        mUserDatabase.updateChildren(images).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (!task.isSuccessful()){
                                                                    Toast.makeText(SettingsActivity.this,
                                                                            "Error writing data " + task.getException().getMessage(),
                                                                            Toast.LENGTH_SHORT).show();
                                                                }
                                                                else {
                                                                    uploadImage(downloadUri, mImageView);
                                                                }

                                                                mProgress.dismiss();
                                                            }
                                                        });
                                                    }
                                                });
                                          }
                                      });
                                  }
                              }
                          }).addOnFailureListener(new OnFailureListener() {
                              @Override
                              public void onFailure(@NonNull Exception e) {
                                  mProgress.dismiss();
                                  Toast.makeText(SettingsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                              }
                          });

                        }
                        else {
                            Toast.makeText(SettingsActivity.this,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }


    }

    /**
     * move from change status mode (with edit text on activity) to display setting mode (the default mode).
     */
    private void fromChangeModeToSettings() {
        mChangeStatusEditText.setVisibility(View.GONE);
        mYesNo.setVisibility(View.GONE);
        mChangeStatus.setVisibility(View.VISIBLE);
    }

    /**
     * showing a default progress dialog
     */
    private void showProgressBar(){
        mProgress = new ProgressDialog(SettingsActivity.this);
        mProgress.setTitle("Saving changes");
        mProgress.setMessage("working on it...");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

    }

    /**
     * load image onto image view in settings activity.
     * @param imageUri
     * @param imageView
     */
    private void uploadImage(String imageUri, ImageView imageView){
        if (!imageUri.equals("default")) {
            Picasso.get().load(imageUri).networkPolicy(NetworkPolicy.OFFLINE).into(imageView, new Callback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load(imageUri).placeholder(R.drawable.avatar).into(imageView);
                }
            });
        }
    }


}
