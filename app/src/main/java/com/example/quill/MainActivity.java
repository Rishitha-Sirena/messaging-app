package com.example.quill;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import java.util.List;

public class MainActivity extends AppCompatActivity implements UsersAdapter.OnUserClickListener {

    private RecyclerView recyclerView;
    private UsersAdapter usersAdapter;
    private DatabaseReference databaseReference;
    private List<UserModel> userList;
    private FirebaseAuth auth;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, SigninActivity.class));
            finish();
            return;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Load and display username
        loadUserName();

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        usersAdapter = new UsersAdapter(this, userList);
        usersAdapter.setOnUserClickListener(this);
        recyclerView.setAdapter(usersAdapter);

        // Setup FAB
        findViewById(R.id.fabNewChat).setOnClickListener(v -> 
            startActivity(new Intent(MainActivity.this, UsersActivity.class))
        );

        // Load users
        loadUsers();
    }

    private void loadUserName() {
        String currentUserId = auth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
            .child("users").child(currentUserId);
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UserModel user = snapshot.getValue(UserModel.class);
                    if (user != null) {
                        TextView username = findViewById(R.id.username);
                        username.setText(user.getName());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading user name: " + error.getMessage());
            }
        });
    }

    private void loadUsers() {
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    UserModel userModel = dataSnapshot.getValue(UserModel.class);
                    if (userModel != null &&
                            userModel.getUid() != null &&
                            !userModel.getUid().equals(FirebaseAuth.getInstance().getUid())) {
                        userList.add(userModel);
                    }
                }
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle Firebase errors
                Log.e(TAG, "Error loading users: " + error.getMessage());
            }
        });
    }

    @Override
    public void onUserClick(UserModel user) {
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra("receiverId", user.getUid());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_groups) {
            startActivity(new Intent(this, GroupsActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, SigninActivity.class));
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update user status to online
        if (auth.getCurrentUser() != null) {
            updateUserStatus(true);
        }
        // Force close any open menus
        invalidateOptionsMenu();
        if (getCurrentFocus() != null) {
            getCurrentFocus().clearFocus();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Update user status to offline
        if (auth.getCurrentUser() != null) {
            updateUserStatus(false);
        }
        // Ensure menus are closed when leaving activity
        closeOptionsMenu();
        invalidateOptionsMenu();
    }

    private void updateUserStatus(boolean online) {
        String currentUserId = auth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
            .child("users").child(currentUserId);
        
        userRef.child("online").setValue(online);
    }

    private void showClearChatsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chats")
                .setMessage("Are you sure you want to clear all chats? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> clearAllChats())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(MainActivity.this, SigninActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllChats() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        chatsRef.orderByKey()
                .startAt(currentUserId)
                .endAt(currentUserId + "\uf8ff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                            chatSnapshot.getRef().removeValue();
                        }
                        Toast.makeText(MainActivity.this, "All chats cleared", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Failed to clear chats: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
