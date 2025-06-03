package com.example.quill;

import java.util.List;
import java.util.ArrayList;

public class GroupModel {
    private String groupId;
    private String name;
    private String createdBy;
    private long timestamp;
    private List<String> members;
    private String lastMessage;
    private long lastMessageTime;

    public GroupModel() {
        // Required empty constructor for Firebase
        members = new ArrayList<>();
    }

    public GroupModel(String groupId, String name, String createdBy) {
        this.groupId = groupId;
        this.name = name;
        this.createdBy = createdBy;
        this.timestamp = System.currentTimeMillis();
        this.members = new ArrayList<>();
        this.members.add(createdBy); // Add creator as first member
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public void addMember(String userId) {
        if (!members.contains(userId)) {
            members.add(userId);
        }
    }

    public void removeMember(String userId) {
            members.remove(userId);
        }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
} 