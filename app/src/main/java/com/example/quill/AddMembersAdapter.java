package com.example.quill;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddMembersAdapter extends RecyclerView.Adapter<AddMembersAdapter.ViewHolder> {
    private List<UserModel> users;
    private Set<String> selectedUserIds;
    private Set<String> existingMembers;

    public AddMembersAdapter() {
        this.users = new ArrayList<>();
        this.selectedUserIds = new HashSet<>();
        this.existingMembers = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserModel user = users.get(position);
        holder.textName.setText(user.getName());
        holder.textEmail.setText(user.getEmail());
        
        // Disable checkbox if user is already a member
        boolean isMember = existingMembers.contains(user.getUid());
        holder.checkBox.setEnabled(!isMember);
        holder.checkBox.setChecked(selectedUserIds.contains(user.getUid()) || isMember);
        
        holder.itemView.setOnClickListener(v -> {
            if (!isMember) {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
                if (holder.checkBox.isChecked()) {
                    selectedUserIds.add(user.getUid());
                } else {
                    selectedUserIds.remove(user.getUid());
                }
            }
        });
        
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isMember) {
                if (isChecked) {
                    selectedUserIds.add(user.getUid());
                } else {
                    selectedUserIds.remove(user.getUid());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void setUsers(List<UserModel> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    public void setExistingMembers(Set<String> members) {
        this.existingMembers = members;
        notifyDataSetChanged();
    }

    public Set<String> getSelectedUsers() {
        return selectedUserIds;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textEmail;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
} 