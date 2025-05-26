package com.example.pbdv_project;

import static com.example.pbdv_project.Constants.DEFAULT_ALERT_CATEGORY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PanicAlert {
    private String documentId;
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private double latitude;
    private double longitude;
    private String address;
    private long timestamp;
    private long resolvedTimestamp;
    private String status;
    private List<Map<String, String>> emergencyContactsList;
    private String category;
    private String resolvedBy;

    public PanicAlert() {}

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getResolvedTimestamp() {
        return resolvedTimestamp;
    }

    public void setResolvedTimestamp(long resolvedTimestamp) {
        this.resolvedTimestamp = resolvedTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Map<String, String>> getEmergencyContactsList() {
        return emergencyContactsList;
    }

    public void setEmergencyContactsList(List<Map<String, String>> emergencyContactsList) {
        this.emergencyContactsList = emergencyContactsList;
    }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }


    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("userEmail", userEmail);
        map.put("userPhone", userPhone != null ? userPhone : "");
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("address", address != null ? address : "");
        map.put("timestamp", timestamp);
        map.put("resolvedTimestamp", resolvedTimestamp);
        map.put("status", status);
        map.put("emergencyContactsList", emergencyContactsList);
        map.put("category", category != null ? category : DEFAULT_ALERT_CATEGORY);
        map.put("resolvedBy", resolvedBy != null ? resolvedBy : "");
        return map;
    }
}