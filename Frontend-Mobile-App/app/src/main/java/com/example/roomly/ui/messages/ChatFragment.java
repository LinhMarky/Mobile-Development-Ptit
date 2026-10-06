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

import com.example.roomly.R;
import com.example.roomly.data.model.ChatMessage;
import com.example.roomly.data.model.Conversation;
import com.example.roomly.data.repository.DemoChatRepository;
import com.example.roomly.data.repository.SessionAccess;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentChatBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị hội thoại và gửi tin nhắn bằng dữ liệu mẫu.
 * Kiểm tra đăng nhập, xác minh email và giữ nội dung đang soạn.
 */
public class ChatFragment extends Fragment {

    // Khóa truyền mã hội thoại qua Bundle.
    private static final String ARG_CONVERSATION_ID =
            "conversation_id";

    // Khóa lưu bản nháp khi Android tạo lại Fragment.
    private static final String STATE_DRAFT = "chat_draft";

    private FragmentChatBinding binding;
    private ChatMessageAdapter messageAdapter;

    private String conversationId;

    // Giữ bản nháp khi chuyển sang đăng nhập hoặc xác minh email.
    private String messageDraft = "";

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
     * Đọc mã hội thoại và khôi phục bản nháp khi Fragment được tạo lại.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            conversationId = args.getString(ARG_CONVERSATION_ID);
        }

        if (savedInstanceState != null) {
            messageDraft = savedInstanceState.getString(
                    STATE_DRAFT,
                    ""
            );
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
     * Thiết lập danh sách tin nhắn, bàn phím và các nút thao tác.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupMessageList();
        setupKeyboardInsets();
        setupActionButtons();

        displayConversation();
    }

    /**
     * Khôi phục bản nháp sau khi Android khôi phục trạng thái giao diện.
     * Người dùng cần tự bấm Gửi sau khi đăng nhập hoặc xác minh email.
     */
    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        if (binding == null) {
            return;
        }

        binding.edtChatMessage.setText(messageDraft);
        binding.edtChatMessage.setSelection(
                binding.edtChatMessage.length()
        );
    }

    /**
     * Lưu nội dung đang soạn khi Android lưu trạng thái Fragment.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding != null) {
            messageDraft = binding.edtChatMessage
                    .getText()
                    .toString();
        }

        outState.putString(STATE_DRAFT, messageDraft);

        super.onSaveInstanceState(outState);
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
     * Đăng ký thao tác quay lại và gửi tin nhắn.
     * Nút Gửi trên bàn phím dùng chung xử lý với nút trên giao diện.
     */
    private void setupActionButtons() {
        binding.btnChatBack.setOnClickListener(view -> {
            hideKeyboard();

            getParentFragmentManager().popBackStack();
        });

        binding.btnSendMessage.setOnClickListener(
                view -> sendMessage()
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
    }

    /**
     * Thêm khoảng trống khi bàn phím mở để ô nhập không bị che.
     * Giữ padding ban đầu và trừ phần hệ thống MainActivity đã xử lý.
     */
    private void setupKeyboardInsets() {
        View root = binding.getRoot();

        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {
                    int keyboardBottom = insets.getInsets(
                            WindowInsetsCompat.Type.ime()
                    ).bottom;

                    int systemBottom = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    ).bottom;

                    int extraBottom = Math.max(
                            0,
                            keyboardBottom - systemBottom
                    );

                    view.setPadding(
                            initialLeft,
                            initialTop,
                            initialRight,
                            initialBottom + extraBottom
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(root);
    }

    /**
     * Hiển thị tên người liên hệ, tên phòng và danh sách tin nhắn.
     * Vô hiệu hóa thao tác gửi nếu hội thoại mẫu không còn dữ liệu.
     */
    private void displayConversation() {
        if (binding == null || messageAdapter == null) {
            return;
        }

        Conversation conversation = conversationId == null
                ? null
                : DemoChatRepository.getInstance()
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

        binding.edtChatMessage.setEnabled(true);
        binding.btnSendMessage.setEnabled(true);

        List<ChatMessage> messages = conversation.getMessages();

        messageAdapter.updateMessages(messages);

        binding.tvChatEmpty.setText(
                "Chưa có tin nhắn. Hãy bắt đầu cuộc trò chuyện."
        );

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
     * Kiểm tra quyền rồi lưu tin nhắn vào repository mẫu.
     * Chỉ xóa bản nháp khi lưu thành công và không gửi nội dung trống.
     */
    private void sendMessage() {
        if (binding == null
                || !binding.btnSendMessage.isEnabled()) {
            return;
        }

        String content = binding.edtChatMessage
                .getText()
                .toString()
                .trim();

        if (content.isEmpty()) {
            return;
        }

        if (!checkSendMessagePermission()) {
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

        messageDraft = "";
        binding.edtChatMessage.setText("");

        displayConversation();
    }

    /**
     * Yêu cầu đăng nhập và xác minh email trước khi gửi tin.
     * Giữ bản nháp và mở màn hình phù hợp nếu chưa đủ điều kiện.
     */
    private boolean checkSendMessagePermission() {
        SessionAccess.Result result =
                SessionAccess.requireVerifiedEmail();

        if (result == SessionAccess.Result.ALLOWED) {
            return true;
        }

        // Giữ cả khoảng trắng và xuống dòng của nội dung đang soạn.
        messageDraft = binding.edtChatMessage
                .getText()
                .toString();

        hideKeyboard();

        Fragment nextFragment;

        if (result == SessionAccess.Result.LOGIN_REQUIRED) {
            Toast.makeText(
                    requireContext(),
                    "Bạn cần đăng nhập để gửi tin nhắn.",
                    Toast.LENGTH_SHORT
            ).show();

            nextFragment = new LoginFragment();

        } else if (
                result == SessionAccess.Result.EMAIL_VERIFICATION_REQUIRED
        ) {
            Toast.makeText(
                    requireContext(),
                    "Bạn cần xác minh email để gửi tin nhắn.",
                    Toast.LENGTH_SHORT
            ).show();

            String email = SessionRepository.getInstance()
                    .getCurrentSession()
                    .getEmail();

            nextFragment = VerifyEmailFragment.newInstance(email);

        } else {
            Toast.makeText(
                    requireContext(),
                    "Tài khoản chưa có quyền gửi tin nhắn.",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        nextFragment
                )
                .addToBackStack(null)
                .commit();

        return false;
    }

    /**
     * Đóng bàn phím và bỏ focus trước khi chuyển màn hình.
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
     * Giữ bản nháp và gỡ các sự kiện trước khi giao diện bị hủy.
     * Giải phóng adapter và binding để không giữ View cũ.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            messageDraft = binding.edtChatMessage
                    .getText()
                    .toString();

            ViewCompat.setOnApplyWindowInsetsListener(
                    binding.getRoot(),
                    null
            );

            binding.btnChatBack.setOnClickListener(null);
            binding.btnSendMessage.setOnClickListener(null);
            binding.edtChatMessage.setOnEditorActionListener(null);
            binding.rvChatMessages.setAdapter(null);
        }

        messageAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}