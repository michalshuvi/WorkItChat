package com.example.workitchat;

/**
 * important for Firebase recycler view in request fragment.
 */
public class Request {

    private int request_type;

    public Request(int request_type) {
        this.request_type = request_type;
    }

    public Request() {
    }

    public int getRequest_type() {
        return request_type;
    }

    public void setRequest_type(int request_type) {
        this.request_type = request_type;
    }
}
