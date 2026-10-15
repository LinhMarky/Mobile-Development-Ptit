package com.example.roomly.ui.messages;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.ChatRepository;
import com.example.roomly.data.repository.SessionAccess;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentChatBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Hiển thị hội thoại từ API, giữ bản nháp và cập nhật tin nhắn khi màn hình đang mở. */
public class ChatFragment extends Fragment {

    private static final String ARG_CONVERSATION_ID = "conversation_id";
    private static final String STATE_DRAFT = "chat_draft";
    private static final String STATE_DRAFT_OWNER = "chat_draft_owner";

    private final ChatRepository chatRepository = new ChatRepository();
    private final Handler pollHandler = new Handler(Looper.getMainLooper());

    private FragmentChatBinding binding;
    private ChatMessageAdapter messageAdapter;
    private String conversationId;
    private String messageDraft = "";
    private String draftOwnerId = "";
    private String pendingClientId = "";
    private String pendingContent = "";
    private boolean sending;
    private boolean loadingConversation;

    private final Runnable poll =
            new Runnable() {
                @Override
                public void run() {
                    if (binding == null || !isResumed()) {
                        return;
                    }

                    displayConversation();
                    pollHandler.postDelayed(this, 4000);
                }
            };

    public static ChatFragment newInstance(String conversationId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONVERSATION_ID, conversationId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            conversationId = getArguments().getString(ARG_CONVERSATION_ID);
        }

        if (savedInstanceState != null) {
            messageDraft = savedInstanceState.getString(STATE_DRAFT, "");
            draftOwnerId = savedInstanceState.getString(STATE_DRAFT_OWNER, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupMessageList();
        setupKeyboardInsets();
        setupActionButtons();

        SessionRepository.getInstance()
                .getSessionState()
                .observe(getViewLifecycleOwner(), session -> displayConversation());
    }

    @Override
    public void onResume() {
        super.onResume();
        pollHandler.post(poll);
    }

    @Override
    public void onPause() {
        pollHandler.removeCallbacks(poll);
        super.onPause();
    }

    // Không giữ bản nháp hoặc trạng thái gửi của tài khoản trước.
    private void syncDraftOwner() {
        String userId = currentUserId();
        if (userId.equals(draftOwnerId)) {
            return;
        }

        draftOwnerId = userId;
        messageDraft = "";
        pendingClientId = "";
        pendingContent = "";
        sending = false;
        loadingConversation = false;
        binding.edtChatMessage.setText("");
        messageAdapter.updateMessages(new ArrayList<>());
    }

    private String currentUserId() {
        SessionState session = SessionRepository.getInstance().getCurrentSession();
        String userId = session.getUserId();
        return session.isLoggedIn() && userId != null ? userId : "";
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (binding == null) {
            return;
        }

        syncDraftOwner();
        binding.edtChatMessage.setText(messageDraft);
        binding.edtChatMessage.setSelection(binding.edtChatMessage.length());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding != null) {
            syncDraftOwner();
            messageDraft = binding.edtChatMessage.getText().toString();
        }

        outState.putString(STATE_DRAFT, messageDraft);
        outState.putString(STATE_DRAFT_OWNER, draftOwnerId);
        super.onSaveInstanceState(outState);
    }

    private void setupMessageList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvChatMessages.setLayoutManager(layoutManager);

        messageAdapter = new ChatMessageAdapter(new ArrayList<>());
        binding.rvChatMessages.setAdapter(messageAdapter);
    }

    private void setupActionButtons() {
        binding.btnChatBack.setOnClickListener(
                view -> {
                    hideKeyboard();
                    getParentFragmentManager().popBackStack();
                });
        binding.btnSendMessage.setOnClickListener(view -> sendMessage());
        binding.edtChatMessage.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        sendMessage();
                        return true;
                    }

                    return false;
                });
    }

    private void setupKeyboardInsets() {
        View root = binding.getRoot();
        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {
                    int keyboardBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                    int systemBottom =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                    int extraBottom = Math.max(0, keyboardBottom - systemBottom);
                    view.setPadding(
                            initialLeft, initialTop, initialRight, initialBottom + extraBottom);
                    return insets;
                });

        ViewCompat.requestApplyInsets(root);
    }

    private void displayConversation() {
        if (binding == null || messageAdapter == null) {
            return;
        }

        syncDraftOwner();
        if (draftOwnerId.isEmpty()) {
            showConversationError("Bạn cần đăng nhập để xem hội thoại.");
            return;
        }

        if (conversationId == null || conversationId.isEmpty()) {
            showConversationError("Không tìm thấy hội thoại.");
            return;
        }

        // Tránh xếp chồng yêu cầu nếu kết nối chậm hơn chu kỳ làm mới.
        if (loadingConversation) {
            return;
        }

        loadingConversation = true;
        FragmentChatBinding requestView = binding;
        String requestUserId = draftOwnerId;
        chatRepository.fetchConversation(
                conversationId,
                conversation -> {
                    if (binding != requestView || !requestUserId.equals(currentUserId())) {
                        return;
                    }

                    loadingConversation = false;
                    showConversation(conversation);
                },
                message -> {
                    if (binding != requestView || !requestUserId.equals(currentUserId())) {
                        return;
                    }

                    loadingConversation = false;
                    showConversationError(message);
                });
    }

    private void showConversation(Conversation conversation) {
        binding.tvChatContactName.setText(conversation.getContactName(currentUserId()));
        binding.tvChatRoomTitle.setText(conversation.getRoomTitle());
        binding.edtChatMessage.setEnabled(!sending);
        binding.btnSendMessage.setEnabled(!sending);

        List<ChatMessage> messages = conversation.getMessages();
        boolean messageCountChanged = messages.size() != messageAdapter.getItemCount();
        messageAdapter.updateMessages(messages);
        binding.tvChatEmpty.setText("Chưa có tin nhắn. Hãy bắt đầu cuộc trò chuyện.");
        binding.tvChatEmpty.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);

        if (!messages.isEmpty() && messageCountChanged) {
            binding.rvChatMessages.scrollToPosition(messages.size() - 1);
        }
    }

    private void showConversationError(String message) {
        binding.tvChatContactName.setText("Hội thoại");
        binding.tvChatRoomTitle.setText("");
        binding.tvChatEmpty.setText(message);
        binding.tvChatEmpty.setVisibility(View.VISIBLE);
        binding.edtChatMessage.setEnabled(false);
        binding.btnSendMessage.setEnabled(false);
        messageAdapter.updateMessages(new ArrayList<>());
    }

    private void sendMessage() {
        if (binding == null || sending) {
            return;
        }

        syncDraftOwner();
        String content = binding.edtChatMessage.getText().toString().trim();
        if (content.isEmpty() || !checkSendMessagePermission()) {
            return;
        }

        if (content.length() > 3000) {
            Toast.makeText(requireContext(), "Tin nhắn tối đa 3000 ký tự.", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        // Gửi lại cùng nội dung sau lỗi mạng vẫn dùng cùng mã để tránh tạo tin trùng.
        if (!content.equals(pendingContent)) {
            pendingClientId = UUID.randomUUID().toString();
            pendingContent = content;
        }

        sending = true;
        binding.btnSendMessage.setEnabled(false);
        binding.edtChatMessage.setEnabled(false);
        FragmentChatBinding requestView = binding;
        String requestUserId = draftOwnerId;
        chatRepository.send(
                conversationId,
                content,
                pendingClientId,
                () -> {
                    if (!requestUserId.equals(currentUserId())) {
                        return;
                    }

                    sending = false;
                    pendingContent = "";
                    pendingClientId = "";
                    messageDraft = "";
                    if (binding != requestView) {
                        return;
                    }

                    binding.edtChatMessage.setText("");
                    binding.edtChatMessage.setEnabled(true);
                    binding.btnSendMessage.setEnabled(true);
                    displayConversation();
                },
                message -> {
                    if (!requestUserId.equals(currentUserId())) {
                        return;
                    }

                    sending = false;
                    if (binding != requestView) {
                        return;
                    }

                    binding.btnSendMessage.setEnabled(true);
                    binding.edtChatMessage.setEnabled(true);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                });
    }

    private boolean checkSendMessagePermission() {
        SessionAccess.Result result = SessionAccess.requireVerifiedEmail();
        if (result == SessionAccess.Result.ALLOWED) {
            return true;
        }

        messageDraft = binding.edtChatMessage.getText().toString();
        hideKeyboard();
        Fragment nextFragment;

        if (result == SessionAccess.Result.LOGIN_REQUIRED) {
            Toast.makeText(
                            requireContext(),
                            "Bạn cần đăng nhập để gửi tin nhắn.",
                            Toast.LENGTH_SHORT)
                    .show();
            nextFragment = new LoginFragment();
        } else if (result == SessionAccess.Result.EMAIL_VERIFICATION_REQUIRED) {
            Toast.makeText(
                            requireContext(),
                            "Bạn cần xác minh email để gửi tin nhắn.",
                            Toast.LENGTH_SHORT)
                    .show();
            String email = SessionRepository.getInstance().getCurrentSession().getEmail();
            nextFragment = VerifyEmailFragment.newInstance(email);
        } else {
            Toast.makeText(
                            requireContext(),
                            "Tài khoản chưa có quyền gửi tin nhắn.",
                            Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, nextFragment)
                .addToBackStack(null)
                .commit();
        return false;
    }

    private void hideKeyboard() {
        if (binding == null) {
            return;
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(requireActivity().getWindow(), binding.getRoot());
        controller.hide(WindowInsetsCompat.Type.ime());
        binding.edtChatMessage.clearFocus();
    }

    @Override
    public void onDestroyView() {
        pollHandler.removeCallbacks(poll);
        loadingConversation = false;
        if (binding != null) {
            messageDraft = binding.edtChatMessage.getText().toString();
            ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), null);
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
