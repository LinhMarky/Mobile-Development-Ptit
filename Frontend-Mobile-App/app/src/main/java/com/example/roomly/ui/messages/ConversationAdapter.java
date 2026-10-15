package com.example.roomly.ui.messages;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.Conversation;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.ItemConversationBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter
        extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private final List<Conversation> conversations = new ArrayList<>();

    private OnConversationClickListener clickListener;
    private String currentUserId = "";

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationAdapter(List<Conversation> initialItems) {
        refreshCurrentUser();

        if (initialItems != null) {
            conversations.addAll(initialItems);
        }
    }

    private void refreshCurrentUser() {
        SessionState session = SessionRepository.getInstance().getCurrentSession();

        String userId = session.getUserId();

        currentUserId = session.isLoggedIn() && userId != null ? userId.trim() : "";
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        clickListener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConversationBinding binding =
                ItemConversationBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false);

        return new ConversationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);

        holder.bind(conversation, currentUserId);

        holder.itemView.setOnClickListener(
                view -> {
                    int currentPosition = holder.getBindingAdapterPosition();

                    if (currentPosition != RecyclerView.NO_POSITION && clickListener != null) {
                        clickListener.onConversationClick(conversations.get(currentPosition));
                    }
                });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void updateConversations(List<Conversation> newItems) {
        List<Conversation> copy = newItems == null ? new ArrayList<>() : new ArrayList<>(newItems);

        refreshCurrentUser();

        conversations.clear();
        conversations.addAll(copy);

        notifyDataSetChanged();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {

        private final ItemConversationBinding binding;

        private final SimpleDateFormat timeFormat =
                new SimpleDateFormat("dd/MM HH:mm", new Locale("vi", "VN"));

        ConversationViewHolder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Conversation conversation, String currentUserId) {
            binding.imgConversationRoom.setImageResource(conversation.getRoomImageResId());

            binding.tvConversationName.setText(conversation.getContactName(currentUserId));

            binding.tvConversationRoomTitle.setText(conversation.getRoomTitle());

            binding.tvConversationLastMessage.setText(
                    conversation.getLastMessagePreview(currentUserId));

            binding.tvConversationTime.setText(
                    timeFormat.format(new Date(conversation.getLastActivityMillis())));
        }
    }
}
