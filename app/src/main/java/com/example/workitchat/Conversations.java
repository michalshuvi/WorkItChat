package com.example.workitchat;

/**
 * conversations in chats schema.
 * for Firebase Recycler Adapter in chats Fragments.
 */
public class Conversations {

    private long timestamp;
    private boolean seen;

    public Conversations() {
    }

    public Conversations(long timestamp, boolean seen) {
        this.timestamp = timestamp;
        this.seen = seen;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}
