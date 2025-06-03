package com.example.quill;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GroupsActivity extends AppCompatActivity {
    private static final String TAG = "GroupsActivity";
    
    private RecyclerView recyclerViewGroups;
    private FloatingActionButton fabCreateGroup;
    private DatabaseReference groupsRef;
    private DatabaseReference usersRef;
    private String currentUserId;
    private List<GroupModel> groupList;
    private GroupsAdapter groupsAdapter;
    private List<UserModel> userList;
    private AddMembersAdapter membersAdapter;
    private Dialog createGroupDialog;
    private ValueEventListener groupsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_groups);

            // Setup toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Groups");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            // Check if user is logged in
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "User not logged in");
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, SigninActivity.class));
                finish();
                return;
            }

            currentUserId = currentUser.getUid();
            Log.d(TAG, "Current user ID: " + currentUserId);

            // Initialize Firebase
            try {
                groupsRef = FirebaseDatabase.getInstance().getReference().child("groups");
                usersRef = FirebaseDatabase.getInstance().getReference().child("users");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase", e);
                Toast.makeText(this, "Error connecting to database", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize views
            try {
                recyclerViewGroups = findViewById(R.id.recyclerViewGroups);
                fabCreateGroup = findViewById(R.id.fabCreateGroup);

                if (recyclerViewGroups == null || fabCreateGroup == null) {
                    Log.e(TAG, "Error finding views");
                    Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing views", e);
                Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Setup RecyclerView
            try {
                groupList = new ArrayList<>();
                groupsAdapter = new GroupsAdapter(this, groupList);
                recyclerViewGroups.setLayoutManager(new LinearLayoutManager(this));
                recyclerViewGroups.setAdapter(groupsAdapter);

                userList = new ArrayList<>();
                membersAdapter = new AddMembersAdapter();
            } catch (Exception e) {
                Log.e(TAG, "Error setting up RecyclerView", e);
                Toast.makeText(this, "Error setting up groups list", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Setup click listener for groups
            groupsAdapter.setOnItemClickListener(group -> {
                if (group != null && group.getGroupId() != null) {
                    // Verify membership before opening chat
                    verifyMembershipAndOpenChat(group);
                } else {
                    Log.e(TAG, "Invalid group data");
                    Toast.makeText(this, "Error opening group chat", Toast.LENGTH_SHORT).show();
                }
            });

            // Load groups
            loadGroups();

            // Load users for member selection
            loadUsers();

            // Setup FAB click listener
            fabCreateGroup.setOnClickListener(v -> showCreateGroupDialog());

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing groups", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void verifyMembershipAndOpenChat(GroupModel group) {
        groupsRef.child(group.getGroupId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    GroupModel currentGroup = snapshot.getValue(GroupModel.class);
                    if (currentGroup != null && currentGroup.getMembers() != null && 
                        currentGroup.getMembers().contains(currentUserId)) {
                        // User is a member, open chat
                        Intent intent = new Intent(GroupsActivity.this, GroupChatActivity.class);
                        intent.putExtra("groupId", group.getGroupId());
                        intent.putExtra("groupName", group.getName());
                        startActivity(intent);
                    } else {
                        Toast.makeText(GroupsActivity.this, 
                            "You are not a member of this group", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error verifying membership", e);
                    Toast.makeText(GroupsActivity.this, 
                        "Error opening group chat", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(GroupsActivity.this, 
                    "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGroups() {
        if (groupsListener != null) {
            groupsRef.removeEventListener(groupsListener);
        }

        groupsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    groupList.clear();
                    for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                        try {
                            GroupModel group = groupSnapshot.getValue(GroupModel.class);
                            if (group != null && group.getMembers() != null && 
                                group.getMembers().contains(currentUserId)) {
                                group.setGroupId(groupSnapshot.getKey()); // Ensure groupId is set
                                groupList.add(group);
                                Log.d(TAG, "Added group: " + group.getName());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing group: " + groupSnapshot.getKey(), e);
                        }
                    }
                    groupsAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Loaded " + groupList.size() + " groups");

                    // Show/hide empty state
                    if (groupList.isEmpty()) {
                        // You might want to show an empty state view here
                        Toast.makeText(GroupsActivity.this, 
                            "No groups found", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing groups data", e);
                    Toast.makeText(GroupsActivity.this, 
                        "Error loading groups", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(GroupsActivity.this, 
                    "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        groupsRef.addValueEventListener(groupsListener);
    }

    private void loadUsers() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    userList.clear();
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        UserModel user = userSnapshot.getValue(UserModel.class);
                        if (user != null && !user.getUid().equals(currentUserId)) {
                            userList.add(user);
                            Log.d(TAG, "Added user: " + user.getName());
                        }
                    }
                    membersAdapter.setUsers(userList);
                    Log.d(TAG, "Loaded " + userList.size() + " users");
                } catch (Exception e) {
                    Log.e(TAG, "Error processing users", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading users: " + error.getMessage());
            }
        });
    }

    private void showCreateGroupDialog() {
        try {
            createGroupDialog = new Dialog(this);
            createGroupDialog.setContentView(R.layout.dialog_create_group);
            createGroupDialog.setCancelable(true);

            EditText editTextGroupName = createGroupDialog.findViewById(R.id.editTextGroupName);
            RecyclerView recyclerViewUsers = createGroupDialog.findViewById(R.id.recyclerViewUsers);
            View btnCreate = createGroupDialog.findViewById(R.id.btnCreate);

            if (editTextGroupName == null || btnCreate == null || recyclerViewUsers == null) {
                Log.e(TAG, "Dialog views not found");
                Toast.makeText(this, "Error showing dialog", Toast.LENGTH_SHORT).show();
                return;
            }

            // Setup users RecyclerView
            recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewUsers.setAdapter(membersAdapter);

            // Update adapter with current users
            membersAdapter.setUsers(userList);
            Log.d(TAG, "Dialog showing " + userList.size() + " users");

            btnCreate.setOnClickListener(v -> {
                String groupName = editTextGroupName.getText().toString().trim();
                if (!groupName.isEmpty()) {
                    Set<String> selectedUsers = membersAdapter.getSelectedUsers();
                    createNewGroup(groupName, new ArrayList<>(selectedUsers));
                    createGroupDialog.dismiss();
                } else {
                    editTextGroupName.setError("Group name is required");
                }
            });

            createGroupDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing create group dialog", e);
            Toast.makeText(this, "Error showing dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void createNewGroup(String groupName, List<String> selectedMembers) {
        try {
            String groupId = groupsRef.push().getKey();
            if (groupId == null) {
                Log.e(TAG, "Failed to generate group ID");
                Toast.makeText(this, "Error creating group", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add current user to members if not already included
            if (!selectedMembers.contains(currentUserId)) {
                selectedMembers.add(currentUserId);
            }

            GroupModel newGroup = new GroupModel(groupId, groupName, currentUserId);
            newGroup.setMembers(selectedMembers);
            
            Log.d(TAG, "Creating group: " + groupName + " with " + selectedMembers.size() + " members");
            
            groupsRef.child(groupId).setValue(newGroup)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Group created successfully");
                    Toast.makeText(GroupsActivity.this, 
                        "Group created successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create group", e);
                    Toast.makeText(GroupsActivity.this, 
                        "Failed to create group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e(TAG, "Error creating new group", e);
            Toast.makeText(this, "Error creating group", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (groupsListener != null) {
            groupsRef.removeEventListener(groupsListener);
        }
        if (createGroupDialog != null && createGroupDialog.isShowing()) {
            createGroupDialog.dismiss();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 