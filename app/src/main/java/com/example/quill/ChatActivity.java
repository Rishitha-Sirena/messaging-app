package com.example.quill;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private String receiverId;
    private String senderRoom, receiverRoom;
    private DatabaseReference dbreferenceSender, dbreferenceReceiver;
    private ImageView sendBtn;
    private EditText messageText;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private DatabaseReference userRef;
    private EditText searchEditText;
    private List<MessageModel> allMessages;
    private List<MessageModel> filteredMessages;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private Toolbar toolbar;
    private List<MessageModel> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, SigninActivity.class));
            finish();
            return;
        }

        // Get receiver info from intent
        receiverId = getIntent().getStringExtra("receiverId");
        if (receiverId == null) {
            Toast.makeText(this, "Error: No receiver specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize views
        TextView chatUserName = findViewById(R.id.chatUserName);
        sendBtn = findViewById(R.id.buttonSend);
        messageText = findViewById(R.id.editTextMessage);
        recyclerView = findViewById(R.id.recyclerViewMessages);

        // Load receiver info and set username
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(receiverId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel user = snapshot.getValue(UserModel.class);
                if (user != null) {
                    chatUserName.setText(user.getName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load user info", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        
        messageList = new ArrayList<>();
        allMessages = new ArrayList<>();
        filteredMessages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this);
        messageAdapter.setMessages(messageList);
        recyclerView.setAdapter(messageAdapter);

        // Load messages
        loadMessages();

        // Setup click listener for send button
        sendBtn.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        String senderId = auth.getUid();
        if (senderId == null) {
            Toast.makeText(this, "User not logged in. Please login again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SigninActivity.class));
            finish();
            return;
        }

        senderRoom = senderId + receiverId;
        receiverRoom = receiverId + senderId;

        dbreferenceSender = FirebaseDatabase.getInstance().getReference("chats").child(senderRoom);
        dbreferenceReceiver = FirebaseDatabase.getInstance().getReference("chats").child(receiverRoom);

        // Load messages from Firebase
        dbreferenceSender.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allMessages.clear();
                messageList.clear();
                for (DataSnapshot messageSnap : snapshot.getChildren()) {
                    MessageModel message = messageSnap.getValue(MessageModel.class);
                    if (message != null) {
                        allMessages.add(message);
                        messageList.add(message);
                    }
                }
                
                // Sort messages by timestamp
                Collections.sort(messageList, (m1, m2) -> 
                    Long.compare(m1.getTimestamp(), m2.getTimestamp()));
                
                messageAdapter.setMessages(messageList);
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Firebase load error: ", error.toException());
            }
        });
    }

    private void sendMessage() {
        String message = messageText.getText().toString().trim();
        if (!message.isEmpty()) {
            sendMessage(message);
        } else {
            Toast.makeText(ChatActivity.this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage(String message) {
        String messageId = UUID.randomUUID().toString();
        long timestamp = new Date().getTime();
        String senderId = auth.getUid();

        if (senderId == null) {
            Toast.makeText(this, "User not logged in. Please login again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SigninActivity.class));
            finish();
            return;
        }

        MessageModel messageModel = new MessageModel(messageId, senderId, message, timestamp);

        dbreferenceSender.child(messageId).setValue(messageModel)
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show());

        dbreferenceReceiver.child(messageId).setValue(messageModel);

        messageList.add(messageModel);
        messageAdapter.setMessages(messageList);
        recyclerView.scrollToPosition(messageList.size() - 1);
        messageText.setText("");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
