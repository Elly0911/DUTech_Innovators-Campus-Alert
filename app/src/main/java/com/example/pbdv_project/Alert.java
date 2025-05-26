package com.example.pbdv_project;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Alert {
    private String title;
    private String message;
    private Timestamp timestamp;
    private boolean read;
    private String type;
    private String senderId;

    private String documentId;

    public Alert() {}

    public Alert(String title, String message, Timestamp timestamp, boolean read, String type, String senderId) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
        this.type = type;
        this.senderId = senderId;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    @Exclude
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    @Exclude
    public long getTimestampLong() {
        return timestamp != null ? timestamp.toDate().getTime() : 0;
    }
}