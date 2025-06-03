package com.example.quill;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private Context context;
    private List<MessageModel> messageModelList;
    private boolean isGroupChat;
    private String groupId;
    private SimpleDateFormat timeFormat;

    public MessageAdapter(Context context) {
        this(context, null, false);
    }

    public MessageAdapter(Context context, String groupId, boolean isGroupChat) {
        this.context = context;
        this.messageModelList = new ArrayList<>();
        this.isGroupChat = isGroupChat;
        this.groupId = groupId;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public void add(MessageModel messageModel) {
        if (messageModel != null) {
            messageModelList.add(messageModel);
            notifyItemInserted(messageModelList.size() - 1);
        }
    }

    public void clear() {
        messageModelList.clear();
        notifyDataSetChanged();
    }

    public void setMessages(List<MessageModel> messages) {
        if (messages != null) {
            this.messageModelList.clear();
            this.messageModelList.addAll(messages);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.message_row_sent, parent, false);
            return new MyViewHolder(view, true);
        } else {
            View view = inflater.inflate(R.layout.message_row_received, parent, false);
            return new MyViewHolder(view, false);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        try {
            MessageModel messageModel = messageModelList.get(position);
            if (messageModel == null) return;

            String time = timeFormat.format(new Date(messageModel.getTimestamp()));

            if (getItemViewType(position) == VIEW_TYPE_SENT) {
                if (holder.textViewSentMessage != null) {
                    holder.textViewSentMessage.setText(messageModel.getMessage());
                }
                if (holder.messageTime != null) {
                    holder.messageTime.setText(time);
                }
            } else {
                if (holder.textViewReceivedMessage != null) {
                    holder.textViewReceivedMessage.setText(messageModel.getMessage());
                }
                if (holder.messageTime != null) {
                    holder.messageTime.setText(time);
                }
                
                // Show sender name in group chat
                if (isGroupChat && holder.senderName != null && messageModel.getSenderId() != null) {
                    holder.senderName.setVisibility(View.VISIBLE);
                    // Set a default name while loading
                    holder.senderName.setText("Loading...");
                    
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(messageModel.getSenderId())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    UserModel user = snapshot.getValue(UserModel.class);
                                    if (user != null && user.getName() != null && holder.senderName != null) {
                                        holder.senderName.setVisibility(View.VISIBLE);
                                        holder.senderName.setText(user.getName());
                                    } else {
                                        if (holder.senderName != null) {
                                            holder.senderName.setVisibility(View.GONE);
                                        }
                                    }
                                } else {
                                    if (holder.senderName != null) {
                                        holder.senderName.setVisibility(View.GONE);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                if (holder.senderName != null) {
                                    holder.senderName.setVisibility(View.GONE);
                                }
                            }
                        });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messageModelList.get(position);
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (message != null && message.getSenderId() != null && 
            currentUserId != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewSentMessage, textViewReceivedMessage;
        private TextView messageTime;
        private TextView senderName;

        public MyViewHolder(@NonNull View itemView, boolean isSent) {
            super(itemView);
            if (isSent) {
                textViewSentMessage = itemView.findViewById(R.id.textViewSentMessage);
                messageTime = itemView.findViewById(R.id.messageTime);
            } else {
                textViewReceivedMessage = itemView.findViewById(R.id.textViewReceivedMessage);
                messageTime = itemView.findViewById(R.id.messageTime);
                senderName = itemView.findViewById(R.id.senderName);
            }
        }
    }
}
