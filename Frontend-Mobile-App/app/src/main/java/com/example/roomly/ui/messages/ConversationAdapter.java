package com.example.roomly.ui.messages;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.Conversation;
import com.example.roomly.databinding.ItemConversationBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị danh sách hội thoại bằng item_conversation.xml.
 */
public class ConversationAdapter
        extends RecyclerView.Adapter<
        ConversationAdapter.ConversationViewHolder> {

    private final List<Conversation> conversations =
            new ArrayList<>();

    private OnConversationClickListener clickListener;

    /**
     * Nhận sự kiện người dùng chọn một hội thoại.
     */
    public interface OnConversationClickListener {

        /**
         * Báo hội thoại được chọn cho màn hình danh sách.
         */
        void onConversationClick(Conversation conversation);
    }

    /**
     * Nhận danh sách hội thoại ban đầu.
     */
    public ConversationAdapter(List<Conversation> initialItems) {
        conversations.addAll(initialItems);
    }

    /**
     * Đăng ký xử lý thao tác bấm vào hội thoại.
     */
    public void setOnConversationClickListener(
            OnConversationClickListener listener
    ) {
        clickListener = listener;
    }

    /**
     * Tạo giao diện cho một dòng hội thoại bằng ViewBinding.
     */
    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemConversationBinding binding =
                ItemConversationBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new ConversationViewHolder(binding);
    }

    /**
     * Hiển thị hội thoại và gắn thao tác mở cuộc trò chuyện.
     */
    @Override
    public void onBindViewHolder(
            @NonNull ConversationViewHolder holder,
            int position
    ) {
        Conversation conversation = conversations.get(position);

        holder.bind(conversation);

        holder.itemView.setOnClickListener(view -> {
            if (clickListener != null) {
                clickListener.onConversationClick(conversation);
            }
        });
    }

    /**
     * Trả về số hội thoại trong danh sách.
     */
    @Override
    public int getItemCount() {
        return conversations.size();
    }

    /**
     * Thay danh sách và cập nhật giao diện.
     */
    public void updateConversations(List<Conversation> newItems) {
        List<Conversation> copy = new ArrayList<>(newItems);

        conversations.clear();
        conversations.addAll(copy);

        notifyDataSetChanged();
    }

    /**
     * Giữ các thành phần giao diện của một dòng hội thoại.
     */
    static class ConversationViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemConversationBinding binding;

        private final SimpleDateFormat timeFormat =
                new SimpleDateFormat(
                        "dd/MM HH:mm",
                        new Locale("vi", "VN")
                );

        /**
         * Khởi tạo ViewHolder với binding của dòng hội thoại.
         */
        ConversationViewHolder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Hiển thị ảnh phòng, người liên hệ và tin nhắn gần nhất.
         */
        void bind(Conversation conversation) {
            binding.imgConversationRoom.setImageResource(
                    conversation.getRoomImageResId()
            );

            binding.tvConversationName.setText(
                    conversation.getContactName()
            );

            binding.tvConversationRoomTitle.setText(
                    conversation.getRoomTitle()
            );

            binding.tvConversationLastMessage.setText(
                    conversation.getLastMessagePreview()
            );

            binding.tvConversationTime.setText(
                    timeFormat.format(
                            new Date(
                                    conversation.getLastActivityMillis()
                            )
                    )
            );
        }
    }
}