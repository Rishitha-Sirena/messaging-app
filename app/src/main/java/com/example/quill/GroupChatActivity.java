package com.example.quill;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupChatActivity extends AppCompatActivity {
    private static final String TAG = "GroupChatActivity";

    private String groupId;
    private String groupName;
    private String currentUserId;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private DatabaseReference groupRef;
    private DatabaseReference messagesRef;
    private List<ChatMessage> messageList;
    private ChatAdapter chatAdapter;
    private AlertDialog addMembersDialog;
    private ValueEventListener messagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_group_chat);

            // Get group details from intent
        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");

        if (groupId == null || groupName == null) {
                Log.e(TAG, "Group details missing. GroupId: " + groupId + ", GroupName: " + groupName);
                Toast.makeText(this, "Error: Group details missing", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "Opening group: " + groupName + " (ID: " + groupId + ")");

            // Initialize Firebase
            try {
                currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                groupRef = FirebaseDatabase.getInstance().getReference().child("groups").child(groupId);
                messagesRef = groupRef.child("messages");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase", e);
                Toast.makeText(this, "Error connecting to database", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize views
            try {
                Toolbar toolbar = findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(groupName);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
                    toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_black));
                }

                recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
                editTextMessage = findViewById(R.id.editTextMessage);
                buttonSend = findViewById(R.id.buttonSend);

                if (recyclerViewMessages == null || editTextMessage == null || buttonSend == null) {
                    Log.e(TAG, "Error finding views");
                    Toast.makeText(this, "Error initializing chat", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing views", e);
                Toast.makeText(this, "Error initializing chat", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Verify group exists and user is a member
            verifyGroupAndMembership();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error opening chat", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void verifyGroupAndMembership() {
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "Group does not exist: " + groupId);
                    Toast.makeText(GroupChatActivity.this, 
                        "Error: Group not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                GroupModel group = snapshot.getValue(GroupModel.class);
                if (group == null || group.getMembers() == null) {
                    Log.e(TAG, "Invalid group data");
                    Toast.makeText(GroupChatActivity.this, 
                        "Error: Invalid group data", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Check if user is a member
                if (!group.getMembers().contains(currentUserId)) {
                    Log.e(TAG, "User is not a member of the group: " + groupId);
                    Toast.makeText(GroupChatActivity.this, 
                        "Error: You are not a member of this group", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // If verification successful, setup chat
                setupChat();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error verifying group: " + error.getMessage());
                Toast.makeText(GroupChatActivity.this, 
                    "Error verifying group access", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupChat() {
        try {
            // Setup RecyclerView
            messageList = new ArrayList<>();
            chatAdapter = new ChatAdapter(this, messageList, currentUserId);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setStackFromEnd(true);
            recyclerViewMessages.setLayoutManager(layoutManager);
            recyclerViewMessages.setAdapter(chatAdapter);

            // Load messages
            loadMessages();

            // Setup send button
            buttonSend.setOnClickListener(v -> sendMessage());

            // Enable views
            editTextMessage.setEnabled(true);
            buttonSend.setEnabled(true);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up chat", e);
            Toast.makeText(this, "Error setting up chat", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadMessages() {
        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }

        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    messageList.clear();
                    for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                        ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                        if (message != null) {
                            messageList.add(message);
                            Log.d(TAG, "Loaded message: " + message.getMessage());
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                    }
                    Log.d(TAG, "Loaded " + messageList.size() + " messages");
                } catch (Exception e) {
                    Log.e(TAG, "Error processing messages", e);
                    Toast.makeText(GroupChatActivity.this, 
                        "Error loading messages", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading messages: " + error.getMessage());
                Toast.makeText(GroupChatActivity.this, 
                    "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        };

        messagesRef.addValueEventListener(messagesListener);
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            try {
                String messageId = messagesRef.push().getKey();
                if (messageId != null) {
                    ChatMessage message = new ChatMessage(
                        messageId,
                        currentUserId,
                        messageText,
                        System.currentTimeMillis()
                    );

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("/messages/" + messageId, message);
                    updates.put("/lastMessage", messageText);
                    updates.put("/lastMessageTime", message.getTimestamp());

                    groupRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                            editTextMessage.setText("");
                            Log.d(TAG, "Message sent successfully");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error sending message", e);
                            Toast.makeText(GroupChatActivity.this, 
                                "Error sending message", Toast.LENGTH_SHORT).show();
                        });
                }
        } catch (Exception e) {
                Log.e(TAG, "Error creating message", e);
            Toast.makeText(this, "Error sending message", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        if (addMembersDialog != null && addMembersDialog.isShowing()) {
            addMembersDialog.dismiss();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.group_chat_menu, menu);
            return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.action_add_member) {
                showAddMembersDialog();
                return true;
            }
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddMembersDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_members, null);
            RecyclerView membersRecyclerView = dialogView.findViewById(R.id.membersRecyclerView);
            
            AddMembersAdapter adapter = new AddMembersAdapter();
            membersRecyclerView.setAdapter(adapter);
            membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // Get existing members
            groupRef.child("members").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Set<String> existingMembers = new HashSet<>();
                        for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    existingMembers.add(memberSnapshot.getKey());
                        }
                        adapter.setExistingMembers(existingMembers);

                        // Get all users
                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    List<UserModel> users = new ArrayList<>();
                                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                        UserModel user = userSnapshot.getValue(UserModel.class);
                            if (user != null && !user.getUid().equals(currentUserId)) {
                                                users.add(user);
                                            }
                                        }
                                    adapter.setUsers(users);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(GroupChatActivity.this, 
                            "Error loading users: " + error.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(GroupChatActivity.this, 
                    "Error loading members: " + error.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                }
            });

            builder.setView(dialogView)
                .setTitle("Add Members")
                .setPositiveButton("Add", (dialog, which) -> {
                        Set<String> selectedUsers = adapter.getSelectedUsers();
                        if (!selectedUsers.isEmpty()) {
                            for (String userId : selectedUsers) {
                                groupRef.child("members").child(userId).setValue(true);
                            }
                    Toast.makeText(this, selectedUsers.size() + " members added", 
                        Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null);

            addMembersDialog = builder.create();
            addMembersDialog.show();
    }
} 