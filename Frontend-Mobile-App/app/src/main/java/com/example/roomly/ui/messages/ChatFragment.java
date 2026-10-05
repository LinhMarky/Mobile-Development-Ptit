package com.example.roomly.ui.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.data.model.ChatMessage;
import com.example.roomly.data.model.Conversation;
import com.example.roomly.data.repository.DemoChatRepository;
import com.example.roomly.databinding.FragmentChatBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị và gửi tin nhắn trong một hội thoại mẫu.
 */
public class ChatFragment extends Fragment {

    private static final String ARG_CONVERSATION_ID =
            "conversation_id";

    private FragmentChatBinding binding;
    private ChatMessageAdapter messageAdapter;

    private String conversationId;

    /**
     * Tạo màn hình chat với mã của hội thoại cần mở.
     */
    public static ChatFragment newInstance(String conversationId) {
        ChatFragment fragment = new ChatFragment();

        Bundle args = new Bundle();
        args.putString(ARG_CONVERSATION_ID, conversationId);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Đọc mã hội thoại từ Bundle khi Fragment được tạo.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            conversationId = args.getString(ARG_CONVERSATION_ID);
        }
    }

    /**
     * Tạo giao diện trò chuyện bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentChatBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách, bàn phím và các nút thao tác.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupMessageList();
        setupKeyboardInsets();

        binding.btnChatBack.setOnClickListener(clickedView -> {
            hideKeyboard();
            getParentFragmentManager().popBackStack();
        });

        binding.btnSendMessage.setOnClickListener(
                clickedView -> sendMessage()
        );

        binding.edtChatMessage.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        sendMessage();
                        return true;
                    }

                    return false;
                }
        );

        displayConversation();
    }

    /**
     * Thiết lập danh sách tin nhắn, ưu tiên hiển thị phía cuối.
     */
    private void setupMessageList() {
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext());

        layoutManager.setStackFromEnd(true);

        binding.rvChatMessages.setLayoutManager(layoutManager);

        messageAdapter = new ChatMessageAdapter(
                new ArrayList<>()
        );

        binding.rvChatMessages.setAdapter(messageAdapter);
    }

    /**
     * Thêm khoảng trống khi bàn phím mở để ô nhập không bị che.
     * Trừ phần thanh hệ thống đã được MainActivity xử lý.
     */
    private void setupKeyboardInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                binding.getRoot(),
                (view, insets) -> {
                    int keyboardBottom = insets.getInsets(
                            WindowInsetsCompat.Type.ime()
                    ).bottom;

                    int systemBottom = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    ).bottom;

                    view.setPadding(
                            0,
                            0,
                            0,
                            Math.max(0, keyboardBottom - systemBottom)
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    /**
     * Hiển thị tên người liên hệ, phòng và danh sách tin nhắn.
     * Nếu dữ liệu mẫu đã mất, vô hiệu hóa thao tác gửi.
     */
    private void displayConversation() {
        if (binding == null || messageAdapter == null) {
            return;
        }

        Conversation conversation =
                DemoChatRepository.getInstance()
                        .getConversation(conversationId);

        if (conversation == null) {
            binding.tvChatContactName.setText("Hội thoại");
            binding.tvChatRoomTitle.setText("");

            binding.tvChatEmpty.setText(
                    "Hội thoại mẫu không còn dữ liệu. "
                            + "Bạn quay lại và mở Nhắn tin từ phòng nhé."
            );
            binding.tvChatEmpty.setVisibility(View.VISIBLE);

            binding.edtChatMessage.setEnabled(false);
            binding.btnSendMessage.setEnabled(false);

            messageAdapter.updateMessages(new ArrayList<>());
            return;
        }

        binding.tvChatContactName.setText(
                conversation.getContactName()
        );

        binding.tvChatRoomTitle.setText(
                conversation.getRoomTitle()
        );

        List<ChatMessage> messages = conversation.getMessages();

        messageAdapter.updateMessages(messages);

        binding.tvChatEmpty.setVisibility(
                messages.isEmpty() ? View.VISIBLE : View.GONE
        );

        if (!messages.isEmpty()) {
            binding.rvChatMessages.scrollToPosition(
                    messages.size() - 1
            );
        }
    }

    /**
     * Lưu tin nhắn mẫu, xóa ô nhập và cập nhật danh sách.
     * Không gửi nội dung trống và không tạo phản hồi tự động.
     */
    private void sendMessage() {
        if (binding == null) {
            return;
        }

        String content = "";

        if (binding.edtChatMessage.getText() != null) {
            content = binding.edtChatMessage
                    .getText()
                    .toString()
                    .trim();
        }

        if (content.isEmpty()) {
            return;
        }

        boolean sent = DemoChatRepository.getInstance()
                .sendMessage(conversationId, content);

        if (!sent) {
            Toast.makeText(
                    requireContext(),
                    "Không thể lưu tin nhắn mẫu.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        binding.edtChatMessage.setText("");
        displayConversation();
    }

    /**
     * Đóng bàn phím trước khi quay lại màn hình trước.
     */
    private void hideKeyboard() {
        if (binding == null) {
            return;
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        requireActivity().getWindow(),
                        binding.getRoot()
                );

        controller.hide(WindowInsetsCompat.Type.ime());
        binding.edtChatMessage.clearFocus();
    }

    /**
     * Gỡ listener, adapter và binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        ViewCompat.setOnApplyWindowInsetsListener(
                binding.getRoot(),
                null
        );

        binding.edtChatMessage.setOnEditorActionListener(null);
        binding.rvChatMessages.setAdapter(null);

        messageAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}