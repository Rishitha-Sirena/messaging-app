package com.example.quill;

public class UserModel {
    private String uid;
    private String name;
    private String email;
    private boolean online;
    private String profileImage;
    private String bio;
    private long lastSeen;

    // Required empty constructor for Firebase
    public UserModel() {
        this.online = false;
        this.lastSeen = System.currentTimeMillis();
    }

    public UserModel(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.online = false;
        this.lastSeen = System.currentTimeMillis();
    }

    public String getUid() {
        return uid;
    }

    public String getUserId() {
        return uid;  // Alias for getUid() for compatibility
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getProfilePicUrl() {
        return profileImage;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profileImage = profilePicUrl;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}
