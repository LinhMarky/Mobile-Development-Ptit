package com.example.roomly.ui.messages;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.ChatMessage;
import com.example.roomly.databinding.ItemChatMessageReceivedBinding;
import com.example.roomly.databinding.ItemChatMessageSentBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị tin gửi bên phải và tin nhận bên trái.
 */
public class ChatMessageAdapter
        extends RecyclerView.Adapter<
        ChatMessageAdapter.MessageViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages = new ArrayList<>();

    /**
     * Nhận danh sách tin nhắn ban đầu.
     */
    public ChatMessageAdapter(List<ChatMessage> initialMessages) {
        messages.addAll(initialMessages);
    }

    /**
     * Chọn loại giao diện dựa vào người gửi tin nhắn.
     */
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isSentByMe()
                ? TYPE_SENT
                : TYPE_RECEIVED;
    }

    /**
     * Tạo bong bóng tin gửi hoặc tin nhận bằng ViewBinding.
     */
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

    /**
     * Hiển thị nội dung và thời gian của tin nhắn.
     */
    @Override
    public void onBindViewHolder(
            @NonNull MessageViewHolder holder,
            int position
    ) {
        holder.bind(messages.get(position));
    }

    /**
     * Trả về tổng số tin nhắn.
     */
    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * Thay danh sách tin nhắn và cập nhật giao diện.
     */
    public void updateMessages(List<ChatMessage> newMessages) {
        List<ChatMessage> copy = new ArrayList<>(newMessages);

        messages.clear();
        messages.addAll(copy);

        notifyDataSetChanged();
    }

    /**
     * Giữ các thành phần chung của bong bóng tin gửi và tin nhận.
     */
    static class MessageViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView contentView;
        private final TextView timeView;

        private final SimpleDateFormat timeFormat =
                new SimpleDateFormat(
                        "HH:mm",
                        new Locale("vi", "VN")
                );

        /**
         * Nhận giao diện bong bóng, ô nội dung và ô thời gian.
         */
        MessageViewHolder(
                android.view.View itemView,
                TextView contentView,
                TextView timeView
        ) {
            super(itemView);

            this.contentView = contentView;
            this.timeView = timeView;
        }

        /**
         * Gán nội dung và định dạng thời gian gửi cho bong bóng.
         */
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