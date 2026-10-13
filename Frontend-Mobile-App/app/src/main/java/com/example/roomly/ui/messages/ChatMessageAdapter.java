package com.example.roomly.ui.messages;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.ChatMessage;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.ItemChatMessageReceivedBinding;
import com.example.roomly.databinding.ItemChatMessageSentBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị tin của tài khoản đang xem bên phải,
 * tin của người còn lại bên trái.
 */
public class ChatMessageAdapter
        extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    private String currentUserId = "";

    public ChatMessageAdapter(List<ChatMessage> initialMessages) {
        refreshCurrentUser();

        if (initialMessages != null) {
            messages.addAll(initialMessages);
        }
    }

    private void refreshCurrentUser() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        String userId = session.getUserId();

        currentUserId = session.isLoggedIn() && userId != null
                ? userId.trim()
                : "";
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isSentByMe(currentUserId)
                ? TYPE_SENT
                : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater =
                LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_SENT) {
            ItemChatMessageSentBinding binding =
                    ItemChatMessageSentBinding.inflate(
                            inflater,
                            parent,
                            false
                    );

            return new MessageViewHolder(
                    binding.getRoot(),
                    binding.tvMessageContent,
                    binding.tvMessageTime
            );
        }

        ItemChatMessageReceivedBinding binding =
                ItemChatMessageReceivedBinding.inflate(
                        inflater,
                        parent,
                        false
                );

        return new MessageViewHolder(
                binding.getRoot(),
                binding.tvMessageContent,
                binding.tvMessageTime
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull MessageViewHolder holder,
            int position
    ) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<ChatMessage> newMessages) {
        List<ChatMessage> copy = newMessages == null
                ? new ArrayList<>()
                : new ArrayList<>(newMessages);

        refreshCurrentUser();

        messages.clear();
        messages.addAll(copy);

        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView contentView;
        private final TextView timeView;

        private final SimpleDateFormat timeFormat =
                new SimpleDateFormat(
                        "HH:mm",
                        new Locale("vi", "VN")
                );

        MessageViewHolder(
                View itemView,
                TextView contentView,
                TextView timeView
        ) {
            super(itemView);

            this.contentView = contentView;
            this.timeView = timeView;
        }

        void bind(ChatMessage message) {
            contentView.setText(message.getContent());

            timeView.setText(
                    timeFormat.format(
                            new Date(message.getSentAtMillis())
                    )
            );
        }
    }
}