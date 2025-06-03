package com.example.quill;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UsersActivity extends AppCompatActivity implements UsersAdapter.OnUserClickListener {

    private static final String TAG = "UsersActivity";
    private RecyclerView recyclerView;
    private UsersAdapter usersAdapter;
    private List<UserModel> userList;
    private List<UserModel> filteredList;
    private TextView emptyView;
    private SearchView searchView;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;
    private Set<String> addedUserIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        // Initialize Firebase
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, SigninActivity.class));
            finish();
            return;
        }

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference().child("users");

        // Initialize set for tracking added users
        addedUserIds = new HashSet<>();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Select User");
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        searchView = findViewById(R.id.searchView);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        filteredList = new ArrayList<>();
        usersAdapter = new UsersAdapter(this, filteredList);
        usersAdapter.setOnUserClickListener(this);
        recyclerView.setAdapter(usersAdapter);

        // Setup search functionality
        setupSearch();

        // Load users
        loadUsers();
    }

    private void setupSearch() {
        if (searchView != null) {
            // Set search view properties
            searchView.setQueryHint("Search by name or email...");
            searchView.setIconified(false);
            searchView.clearFocus();

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterUsers(query);
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterUsers(newText);
                    return true;
                }
            });
        }
    }

    private void filterUsers(String query) {
        filteredList.clear();
        
        if (query == null || query.trim().isEmpty()) {
            // If query is empty, show all users
            filteredList.addAll(userList);
        } else {
            // Convert query to lowercase for case-insensitive search
            String lowercaseQuery = query.toLowerCase().trim();
            
            // Search through users
            for (UserModel user : userList) {
                if (user != null) {
                    boolean matchesName = user.getName() != null && 
                                        user.getName().toLowerCase().contains(lowercaseQuery);
                    boolean matchesEmail = user.getEmail() != null && 
                                         user.getEmail().toLowerCase().contains(lowercaseQuery);
                    
                    if (matchesName || matchesEmail) {
                        filteredList.add(user);
                    }
                }
            }
        }

        // Sort filtered results
        filteredList.sort((u1, u2) -> {
            if (u1.isOnline() != u2.isOnline()) {
                return u2.isOnline() ? 1 : -1;  // Online users first
            }
            return u1.getName().compareToIgnoreCase(u2.getName());  // Then alphabetically
        });

        // Update UI
        updateUI();
    }

    private void loadUsers() {
        // Show loading state
        emptyView.setText("Loading users...");
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                addedUserIds.clear();
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    try {
                        String uid = dataSnapshot.getKey();
                        
                        // Skip if uid is null or it's the current user
                        if (uid == null || uid.equals(currentUser.getUid())) {
                            continue;
                        }

                        // Skip if we've already added this user
                        if (addedUserIds.contains(uid)) {
                            continue;
                        }

                        String name = dataSnapshot.child("name").getValue(String.class);
                        String email = dataSnapshot.child("email").getValue(String.class);
                        
                        // Skip users with no name or email
                        if (name == null || name.trim().isEmpty() || 
                            email == null || email.trim().isEmpty()) {
                            continue;
                        }

                        boolean online = dataSnapshot.child("online").getValue(Boolean.class) != null ? 
                                      dataSnapshot.child("online").getValue(Boolean.class) : false;

                        UserModel user = new UserModel(uid, name.trim(), email.trim());
                        user.setOnline(online);
                        
                        // Add user to tracking set and list
                        addedUserIds.add(uid);
                        userList.add(user);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing user data: " + e.getMessage());
                    }
                }

                // Sort users by online status and name
                userList.sort((u1, u2) -> {
                    if (u1.isOnline() != u2.isOnline()) {
                        return u2.isOnline() ? 1 : -1;  // Online users first
                    }
                    return u1.getName().compareToIgnoreCase(u2.getName());  // Then alphabetically
                });

                // Update UI
                filteredList.clear();
                filteredList.addAll(userList);
                
                if (userList.isEmpty()) {
                    emptyView.setText("No users found");
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                emptyView.setText("Error loading users");
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void updateUI() {
        if (usersAdapter != null) {
            usersAdapter.notifyDataSetChanged();
            
            if (emptyView != null && recyclerView != null) {
                if (filteredList.isEmpty()) {
                    if (searchView != null && !searchView.getQuery().toString().isEmpty()) {
                        emptyView.setText("No matching users found");
                    } else {
                        emptyView.setText("No users found");
                    }
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onUserClick(UserModel user) {
        if (user != null && user.getUid() != null) {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiverId", user.getUid());
            startActivity(intent);
        }
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