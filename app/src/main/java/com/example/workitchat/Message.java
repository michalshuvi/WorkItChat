package com.example.workitchat;

/**
 * all message fields from database root -> messages -> current user -> other user.
 * used for message adapter.
 */
public class Message {

    private String message;
    private String from;
    private String thumb;
    private long time;
    private int type;
    private boolean seen;

    public Message() {
    }

    public Message(String message, String from, long time, int type, boolean seen, String thumb) {
        this.message = message;
        this.time = time;
        this.type = type;
        this.seen = seen;
        this.from = from;
        this.thumb = thumb;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}
