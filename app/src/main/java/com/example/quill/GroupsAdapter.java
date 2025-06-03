package com.example.quill;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.ViewHolder> {
    private Context context;
    private List<GroupModel> groups;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(GroupModel group);
    }

    public GroupsAdapter(Context context, List<GroupModel> groups) {
        this.context = context;
        this.groups = groups;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupModel group = groups.get(position);
        holder.groupName.setText(group.getName());
        holder.memberCount.setText(group.getMembers().size() + " members");
        
        if (group.getLastMessage() != null) {
            holder.lastMessage.setVisibility(View.VISIBLE);
            holder.lastMessage.setText(group.getLastMessage());
        } else {
            holder.lastMessage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView groupName;
        TextView memberCount;
        TextView lastMessage;

        ViewHolder(View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.textGroupName);
            memberCount = itemView.findViewById(R.id.textMemberCount);
            lastMessage = itemView.findViewById(R.id.textLastMessage);
        }
    }
} 