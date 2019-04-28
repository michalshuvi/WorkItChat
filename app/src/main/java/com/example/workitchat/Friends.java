package com.example.workitchat;

/**
 * Friends in friends schema.
 * for Firebase Recycler Adapter in Friends Fragment.
 */
class Friends {

    private String date;

    public Friends() {
    }

    public Friends(String date) {
        this.date = date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }
}
