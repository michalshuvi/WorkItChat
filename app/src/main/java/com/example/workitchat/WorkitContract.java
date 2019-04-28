package com.example.workitchat;

/**
 * Contract for Firebase realtime database schemes, fields and storage.
 */
public class WorkitContract {

    //------ Realtime database ------
    //users database
    public static final String USERS_SCHEMA = "users";
    public static final String USERS_COLUMN_NAME = "name";
    public static final String USERS_COLUMN_IMAGE = "image";
    public static final String USERS_COLUMN_STATUS = "status";
    public static final String USERS_COLUMN_THUMB_IMAGE = "thumb_image";
    public static final String USERS_COLUMN_TOKEN = "device_token";
    public static final String USERS_COLUMN_ONLINE = "online";
    public static final String USERS_COLUMN_LAST_SEEN = "last_seen";

    //friends database
    public static final String FRIENDS_SCHEMA = "friends";
    public static final String FRIEND_FRIENDSHIP_DATE = "date";

    //friend request database
    public static final String FRIEND_REQUEST_SCHEMA = "friend_request";
    public static final String FRIENDS_REQ_TYPE = "request_type";
    public static final int FRIENDS_STATE_NOT_FRIENDS = 0;
    public static final int FRIENDS_STATE_SENT = 1;
    public static final int FRIENDS_STATE_RECEIVED = 2;
    public static final int FRIENDS_STATE_FRIENDS = 3;

    //Notifications database
    public static final String NOTIFICATIONS_SCHEMA = "notifications";
    public static final String NOTIFICATIONS_COLUMN_FROM = "from";
    public static final String NOTIFICATIONS_COLUMN_TYPE = "type";
    public static final String NOTIFICATIONS_COLUMN_CONTENT_TYPE = "content_type";
    public static final String NOTIFICATIONS_COLUMN_MESSAGE_ID = "message_id";
    public static final int NOTIFICATIONS_REQ_TYPE = 0;
    public static final int NOTIFICATIONS_MESSAGE_TYPE = 1;

    //Chat database
    public static final String CHAT_SCHEMA = "chat";
    public static final String CHAT_COLUMN_SEEN = "seen";
    public static final String CHAT_COLUMN_TIMESTAMP = "timestamp";

    //Messages database
    public static final String MESSAGES_SCHEMA = "messages";
    public static final String MESSAGES_COLUMN_MESSAGE = "message";
    public static final String MESSAGES_COLUMN_SEEN = "seen";
    public static final String MESSAGES_COLUMN_TYPE = "type";
    public static final String MESSAGES_COLUMN_TIME = "time";
    public static final String MESSAGES_COLUMN_FROM = "from";
    public static final String MESSAGES_COLUMN_thumb = "thumb";

    public static final int MESSAGES_COLUMN_TYPE_TEXT = 0;
    public static final int MESSAGES_COLUMN_TYPE_IMAGE = 1;

    //------ Storage ------
    public static final String STORAGE_PROFILE_IMAGES = "profile_images";
    public static final String STORAGE_THUMB_IN_PROFILE_IMAGE = "thumbs";
    public static final String STORAGE_MESSAGE_IMAGES = "message_images";
    public static final String STORAGE_MESSAGE_THUMB_IMAGES = "message_thumb_images";

}
