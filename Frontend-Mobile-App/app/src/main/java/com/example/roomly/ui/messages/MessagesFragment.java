package com.example.roomly.ui.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.data.model.Conversation;
import com.example.roomly.data.repository.DemoChatRepository;
import com.example.roomly.databinding.FragmentMessagesBinding;

import java.util.ArrayList;
import java.util.List;
import com.example.roomly.R;

/**
 * Hiển thị danh sách các cuộc trò chuyện mẫu.
 */
public class MessagesFragment extends Fragment {

    private FragmentMessagesBinding binding;
    private ConversationAdapter conversationAdapter;

    /**
     * Tạo giao diện Tin nhắn từ file XML.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentMessagesBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách hội thoại theo chiều dọc.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvConversations.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        conversationAdapter = new ConversationAdapter(
                new ArrayList<>()
        );

// Mở chat khi người dùng chọn một hội thoại.
        conversationAdapter.setOnConversationClickListener(
                this::openConversation
        );

        binding.rvConversations.setAdapter(conversationAdapter);
    }

    /**
     * Cập nhật danh sách mỗi khi trở lại màn hình Tin nhắn.
     */
    @Override
    public void onResume() {
        super.onResume();

        displayConversations();
    }

    /**
     * Lấy hội thoại từ repository và hiển thị trạng thái phù hợp.
     */
    private void displayConversations() {
        if (binding == null || conversationAdapter == null) {
            return;
        }

        List<Conversation> conversations =
                DemoChatRepository.getInstance().getConversations();

        conversationAdapter.updateConversations(conversations);

        boolean isEmpty = conversations.isEmpty();

        binding.layoutMessagesEmpty.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        binding.rvConversations.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );
    }

    /**
     * Mở hội thoại được chọn và giữ màn hình danh sách trong back stack.
     * Người dùng có thể bấm quay lại để trở về danh sách Tin nhắn.
     */
    private void openConversation(Conversation conversation) {
        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        ChatFragment.newInstance(
                                conversation.getId()
                        )
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Gỡ adapter và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        binding.rvConversations.setAdapter(null);

        conversationAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}