package com.example.pbdv_project;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class Incident {
    private String documentId;
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String type;
    private String description;
    private String locationText;
    private long timestamp;
    private long resolvedTimestamp;
    private String status;
    private boolean anonymous;
    private boolean hasMedia;
    private String mediaUrl;
    private String mediaType; // "image" or "video"
    private String resolvedBy;

    public Incident() {}

    // Getters and setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocationText() { return locationText; }
    public void setLocationText(String locationText) { this.locationText = locationText; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public long getResolvedTimestamp() { return resolvedTimestamp; }
    public void setResolvedTimestamp(long resolvedTimestamp) { this.resolvedTimestamp = resolvedTimestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isAnonymous() { return anonymous; }
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
    public boolean isHasMedia() { return hasMedia; }
    public void setHasMedia(boolean hasMedia) { this.hasMedia = hasMedia; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public void setDocumentIdFromSnapshot(DocumentSnapshot document) {
        this.documentId = document.getId();
        this.resolvedBy = document.getString("resolvedBy");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName != null ? userName : "");
        map.put("userEmail", userEmail != null ? userEmail : "");
        map.put("userPhone", userPhone != null ? userPhone : "");
        map.put("type", type);
        map.put("description", description);
        map.put("locationText", locationText);
        map.put("timestamp", timestamp);
        map.put("resolvedTimestamp", resolvedTimestamp);
        map.put("status", status);
        map.put("anonymous", anonymous);
        map.put("hasMedia", hasMedia);
        map.put("mediaUrl", mediaUrl);
        map.put("mediaType", mediaType);
        map.put("resolvedBy", resolvedBy != null ? resolvedBy : "");
        return map;
    }
}