package com.example.quill;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        // Setup click listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Clear Chats option
        View clearChatsOption = findViewById(R.id.clearChatsLayout);
        if (clearChatsOption != null) {
            clearChatsOption.setOnClickListener(v -> showClearChatsDialog());
        }

        // My Profile option
        View profileOption = findViewById(R.id.profileLayout);
        if (profileOption != null) {
            profileOption.setOnClickListener(v -> {
                startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
            });
        }
    }

    private void showClearChatsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Chats")
                .setMessage("Are you sure you want to clear all chats? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> clearAllChats())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllChats() {
        if (currentUser == null) return;

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        chatsRef.orderByChild("participants/" + currentUser.getUid())
                .equalTo(true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (var chatSnapshot : snapshot.getChildren()) {
                        chatSnapshot.getRef().removeValue();
                    }
                    Toast.makeText(this, "All chats cleared", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to clear chats: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
} 