package com.example.roomly.ui.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.model.Conversation;
import com.example.roomly.data.repository.DemoChatRepository;
import com.example.roomly.data.repository.SessionAccess;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentMessagesBinding;
import com.example.roomly.ui.auth.LoginFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị danh sách hội thoại mẫu khi đã đăng nhập.
 * Khách được hiển thị lời mời đăng nhập.
 */
public class MessagesFragment extends Fragment {

    private FragmentMessagesBinding binding;
    private ConversationAdapter conversationAdapter;

    /**
     * Tạo giao diện Tin nhắn bằng ViewBinding.
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
     * Thiết lập danh sách, nút đăng nhập và theo dõi phiên.
     * Observer chỉ hoạt động theo vòng đời giao diện Fragment.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupConversationList();

        binding.btnMessagesLogin.setOnClickListener(
                clickedView -> openLoginScreen()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayConversations()
                );
    }

    /**
     * Thiết lập danh sách hội thoại theo chiều dọc.
     * Đăng ký mở màn hình chat khi chọn một hội thoại.
     */
    private void setupConversationList() {
        binding.rvConversations.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        conversationAdapter = new ConversationAdapter(
                new ArrayList<>()
        );

        conversationAdapter.setOnConversationClickListener(
                this::openConversation
        );

        binding.rvConversations.setAdapter(conversationAdapter);
    }

    /**
     * Làm mới danh sách khi trở lại từ màn hình chat hoặc đăng nhập.
     */
    @Override
    public void onResume() {
        super.onResume();

        displayConversations();
    }

    /**
     * Kiểm tra đăng nhập trước khi đọc hội thoại mẫu.
     * Hiển thị một trong ba trạng thái: khách, trống hoặc danh sách.
     */
    private void displayConversations() {
        if (binding == null || conversationAdapter == null) {
            return;
        }

        SessionAccess.Result result =
                SessionAccess.requireLogin();

        if (result != SessionAccess.Result.ALLOWED) {
            // Xóa dữ liệu đang hiển thị khi phiên trở thành khách.
            conversationAdapter.updateConversations(
                    new ArrayList<>()
            );

            binding.rvConversations.setVisibility(View.GONE);
            binding.layoutMessagesEmpty.setVisibility(View.GONE);
            binding.layoutMessagesGuest.setVisibility(View.VISIBLE);

            return;
        }

        binding.layoutMessagesGuest.setVisibility(View.GONE);

        List<Conversation> conversations =
                DemoChatRepository.getInstance()
                        .getConversations();

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
     * Mở Đăng nhập khi người dùng chủ động bấm nút.
     * Giữ màn hình Tin nhắn trong back stack để quay lại.
     */
    private void openLoginScreen() {
        if (binding == null) {
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new LoginFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Kiểm tra lại đăng nhập trước khi mở hội thoại được chọn.
     * Giữ màn hình danh sách trong back stack.
     */
    private void openConversation(Conversation conversation) {
        if (binding == null) {
            return;
        }

        SessionAccess.Result result =
                SessionAccess.requireLogin();

        if (result != SessionAccess.Result.ALLOWED) {
            displayConversations();
            openLoginScreen();

            return;
        }

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
     * Gỡ sự kiện và Adapter trước khi giải phóng Binding.
     * Observer phiên tự được gỡ theo vòng đời giao diện.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnMessagesLogin.setOnClickListener(null);
            binding.rvConversations.setAdapter(null);
        }

        if (conversationAdapter != null) {
            conversationAdapter.setOnConversationClickListener(null);
        }

        conversationAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}