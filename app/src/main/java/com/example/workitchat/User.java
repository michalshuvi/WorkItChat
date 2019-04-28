package com.example.workitchat;

/**
 * For Firebase adapter in User Activity.
 * Refer to Database root -> users -> user
 */
public class User {

    private String mName;
    private String mStatus;
    private String mImage;
    private String mThumb_image;
    private boolean mOnline;

    public boolean isOnline() {
        return mOnline;
    }

    public void setOnline(boolean mOnline) {
        this.mOnline = mOnline;
    }

    public User(String name, String status, String image, String thumbImage){
        this.mName = name;
        this.mStatus = status;
        this.mImage = image;
        this.mThumb_image = thumbImage;
    }

    public User(){}

    public String getName() {
        return mName;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public void setStatus(String mStatus) {
        this.mStatus = mStatus;
    }

    public void setImage(String mImage) {
        this.mImage = mImage;
    }

    public String getImage() {
        return mImage;
    }

    public void setThumb_image(String mThumb_image) {
        this.mThumb_image = mThumb_image;
    }

    public String getThumb_image() {
        return this.mThumb_image;
    }

  }
