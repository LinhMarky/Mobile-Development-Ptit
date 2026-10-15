package com.example.roomly.ui.messages;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.example.roomly.data.repository.DemoChatRepository;
import com.example.roomly.data.repository.LocalMessageDraftRepository;
import com.example.roomly.data.repository.SessionAccess;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentChatBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hiển thị hội thoại mẫu.
 * Nội dung chưa gửi được lưu cục bộ bằng SQLite.
 */
public class ChatFragment extends Fragment {

    private static final String ARG_CONVERSATION_ID =
            "conversation_id";

    private static final String STATE_DRAFT = "chat_draft";
    private static final String STATE_DRAFT_OWNER = "chat_draft_owner";
    private static final String TAG = "ROOMLY_MESSAGE_DRAFT";

    private static final ExecutorService DRAFT_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable saveDraftRunnable =
            this::saveLocalDraftNow;

    private FragmentChatBinding binding;
    private ChatMessageAdapter messageAdapter;
    private LocalMessageDraftRepository localDraftRepository;
    private TextWatcher draftTextWatcher;

    private String conversationId;
    private String messageDraft = "";
    private String draftOwnerId = "";

    private boolean hasMemoryDraft = false;
    private boolean viewStateReady = false;
    private boolean draftReady = false;
    private boolean draftLoading = false;
    private boolean draftDirty = false;
    private boolean applyingDraft = false;

    private int draftLoadVersion = 0;

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

        localDraftRepository = new LocalMessageDraftRepository(
                requireContext().getApplicationContext()
        );

        if (getArguments() != null) {
            conversationId = getArguments().getString(
                    ARG_CONVERSATION_ID
            );
        }

        if (savedInstanceState != null) {
            messageDraft = savedInstanceState.getString(
                    STATE_DRAFT,
                    ""
            );

            draftOwnerId = savedInstanceState.getString(
                    STATE_DRAFT_OWNER,
                    ""
            );

            hasMemoryDraft = savedInstanceState.containsKey(
                    STATE_DRAFT
            );
        }
    }

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

        viewStateReady = false;
        draftReady = false;
        draftLoading = false;
        draftDirty = false;

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupMessageList();
        setupKeyboardInsets();
        setupActionButtons();
        setComposerEnabled(false);

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayConversation()
                );
    }

    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        // Không dùng trực tiếp nội dung View do Android khôi phục.
        // Nội dung phải được kiểm tra đúng tài khoản trước.
        setDraftText("");

        setupDraftTracking();
        viewStateReady = true;
        displayConversation();
    }

    @Override
    public void onResume() {
        super.onResume();
        displayConversation();
    }

    private String getCurrentUserId() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn() || session.getUserId() == null) {
            return "";
        }

        return session.getUserId().trim();
    }

    @Nullable
    private Conversation getAccessibleConversation() {
        if (conversationId == null
                || conversationId.trim().isEmpty()) {
            return null;
        }

        // Repository hiện tại kiểm tra thành viên hội thoại.
        return DemoChatRepository.getInstance()
                .getConversation(conversationId);
    }

    private void setupMessageList() {
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext());

        layoutManager.setStackFromEnd(true);
        binding.rvChatMessages.setLayoutManager(layoutManager);

        messageAdapter = new ChatMessageAdapter(new ArrayList<>());
        binding.rvChatMessages.setAdapter(messageAdapter);
    }

    private void setupActionButtons() {
        binding.btnChatBack.setOnClickListener(view -> {
            saveLocalDraftNow();
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

    private void setupDraftTracking() {
        draftTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count
            ) {
                if (applyingDraft || !draftReady) {
                    return;
                }

                messageDraft = text.toString();
                hasMemoryDraft = true;
                draftDirty = true;
            }

            @Override
            public void afterTextChanged(Editable text) {
                if (!applyingDraft && draftReady) {
                    scheduleDraftSave();
                }
            }
        };

        binding.edtChatMessage.addTextChangedListener(
                draftTextWatcher
        );
    }

    private void displayConversation() {
        if (binding == null
                || messageAdapter == null
                || !viewStateReady) {
            return;
        }

        String currentUserId = getCurrentUserId();

        if (!currentUserId.equals(draftOwnerId)) {
            mainHandler.removeCallbacks(saveDraftRunnable);
            draftLoadVersion++;

            messageDraft = "";
            draftOwnerId = currentUserId;
            hasMemoryDraft = false;
            draftReady = false;
            draftLoading = false;
            draftDirty = false;

            setDraftText("");
        }

        Conversation conversation = getAccessibleConversation();

        if (currentUserId.isEmpty() || conversation == null) {
            mainHandler.removeCallbacks(saveDraftRunnable);
            draftLoadVersion++;

            draftReady = false;
            draftLoading = false;
            draftDirty = false;
            hasMemoryDraft = false;
            messageDraft = "";

            setDraftText("");
            setComposerEnabled(false);
            hideKeyboard();

            binding.tvChatContactName.setText("Hội thoại");
            binding.tvChatRoomTitle.setText("");

            binding.tvChatEmpty.setText(
                    "Hội thoại không còn dữ liệu hoặc tài khoản hiện tại "
                            + "không có quyền truy cập."
            );
            binding.tvChatEmpty.setVisibility(View.VISIBLE);

            messageAdapter.updateMessages(new ArrayList<>());
            return;
        }

        binding.tvChatContactName.setText(
                conversation.getContactName(currentUserId)
        );
        binding.tvChatRoomTitle.setText(
                conversation.getRoomTitle()
        );

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

        if (!draftReady && !draftLoading) {
            restoreLocalDraft();
        }

        setComposerEnabled(draftReady);
    }

    private void restoreLocalDraft() {
        if (binding == null || draftOwnerId.isEmpty()) {
            return;
        }

        draftLoading = true;
        setComposerEnabled(false);

        final String ownerId = draftOwnerId;
        final String selectedConversationId = conversationId;
        final int loadVersion = ++draftLoadVersion;
        final View originalRoot = binding.getRoot();

        final boolean keepMemoryDraft = hasMemoryDraft;
        final String memoryContent = messageDraft;

        final LocalMessageDraftRepository repository =
                localDraftRepository;

        DRAFT_EXECUTOR.execute(() -> {
            try {
                String storedContent = repository.getDraft(
                        ownerId,
                        selectedConversationId
                );

                mainHandler.post(() -> {
                    if (!isCurrentDraftView(
                            originalRoot,
                            loadVersion,
                            ownerId
                    )) {
                        return;
                    }

                    messageDraft = keepMemoryDraft
                            ? memoryContent
                            : storedContent;

                    hasMemoryDraft = true;
                    setDraftText(messageDraft);

                    draftLoading = false;
                    draftReady = true;
                    setComposerEnabled(true);

                    // Nội dung giữ trong Fragment có thể mới hơn SQLite.
                    if (keepMemoryDraft) {
                        draftDirty = true;
                        scheduleDraftSave();
                    }
                });

            } catch (RuntimeException exception) {
                Log.e(TAG, "Không đọc được bản nháp.", exception);

                mainHandler.post(() -> {
                    if (!isCurrentDraftView(
                            originalRoot,
                            loadVersion,
                            ownerId
                    )) {
                        return;
                    }

                    draftLoading = false;

                    Toast.makeText(
                            requireContext(),
                            "Không đọc được bản nháp. "
                                    + "Bạn quay lại rồi mở lại hội thoại nhé.",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private boolean isCurrentDraftView(
            View originalRoot,
            int loadVersion,
            String ownerId
    ) {
        return binding != null
                && binding.getRoot() == originalRoot
                && draftLoadVersion == loadVersion
                && ownerId.equals(draftOwnerId)
                && ownerId.equals(getCurrentUserId())
                && getAccessibleConversation() != null;
    }

    private void setDraftText(String content) {
        if (binding == null) {
            return;
        }

        applyingDraft = true;

        try {
            binding.edtChatMessage.setText(content);
            binding.edtChatMessage.setSelection(
                    binding.edtChatMessage.length()
            );
        } finally {
            applyingDraft = false;
        }
    }

    private void setComposerEnabled(boolean enabled) {
        if (binding == null) {
            return;
        }

        binding.edtChatMessage.setEnabled(enabled);
        binding.btnSendMessage.setEnabled(enabled);
    }

    private String readMessageText() {
        return binding.edtChatMessage.getText() == null
                ? ""
                : binding.edtChatMessage.getText().toString();
    }

    private void scheduleDraftSave() {
        mainHandler.removeCallbacks(saveDraftRunnable);

        if (binding != null && draftReady && draftDirty) {
            mainHandler.postDelayed(saveDraftRunnable, 500);
        }
    }

    private void saveLocalDraftNow() {
        mainHandler.removeCallbacks(saveDraftRunnable);

        if (binding == null
                || !draftReady
                || !draftDirty
                || draftOwnerId.isEmpty()
                || !draftOwnerId.equals(getCurrentUserId())
                || getAccessibleConversation() == null) {
            return;
        }

        messageDraft = readMessageText();
        hasMemoryDraft = true;
        draftDirty = false;

        final String ownerId = draftOwnerId;
        final String selectedConversationId = conversationId;
        final String content = messageDraft;

        final View originalRoot = binding.getRoot();
        final int loadVersion = draftLoadVersion;
        final LocalMessageDraftRepository repository =
                localDraftRepository;

        // Một hàng đợi chung bảo đảm ghi/xóa đúng thứ tự.
        DRAFT_EXECUTOR.execute(() -> {
            try {
                // Repository tự xóa bản nháp nếu nội dung trống.
                repository.saveDraft(
                        ownerId,
                        selectedConversationId,
                        content
                );
            } catch (RuntimeException exception) {
                Log.e(TAG, "Không lưu được bản nháp.", exception);

                mainHandler.post(() -> {
                    if (!isCurrentDraftView(
                            originalRoot,
                            loadVersion,
                            ownerId
                    )) {
                        return;
                    }

                    draftDirty = true;

                    Toast.makeText(
                            requireContext(),
                            "Chưa lưu được bản nháp trên điện thoại.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }

    private void sendMessage() {
        if (binding == null
                || !draftReady
                || !binding.btnSendMessage.isEnabled()) {
            return;
        }

        if (!draftOwnerId.equals(getCurrentUserId())
                || getAccessibleConversation() == null) {
            displayConversation();
            return;
        }

        String content = readMessageText().trim();

        if (content.isEmpty()) {
            return;
        }

        if (content.length() > 5000) {
            binding.edtChatMessage.setError(
                    "Tin nhắn không được vượt quá 5000 ký tự."
            );
            return;
        }

        if (!checkSendMessagePermission()) {
            return;
        }

        binding.btnSendMessage.setEnabled(false);

        try {
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

            // Khi nối API: chỉ thực hiện phần này sau phản hồi thành công.
            messageDraft = "";
            hasMemoryDraft = true;
            setDraftText("");

            draftDirty = true;
            saveLocalDraftNow();

            displayConversation();

        } catch (IllegalArgumentException | IllegalStateException exception) {
            String message = exception.getMessage();

            Toast.makeText(
                    requireContext(),
                    message == null
                            ? "Không thể gửi tin nhắn."
                            : message,
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            if (binding != null) {
                binding.btnSendMessage.setEnabled(
                        draftReady
                                && draftOwnerId.equals(getCurrentUserId())
                                && getAccessibleConversation() != null
                );
            }
        }
    }

    private boolean checkSendMessagePermission() {
        SessionAccess.Result result =
                SessionAccess.requireVerifiedEmail();

        if (result == SessionAccess.Result.ALLOWED) {
            return true;
        }

        saveLocalDraftNow();
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

        if (getParentFragmentManager().isStateSaved()) {
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

                    view.setPadding(
                            initialLeft,
                            initialTop,
                            initialRight,
                            initialBottom + Math.max(
                                    0,
                                    keyboardBottom - systemBottom
                            )
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(root);
    }

    private void hideKeyboard() {
        if (binding == null || !isAdded()) {
            return;
        }

        new WindowInsetsControllerCompat(
                requireActivity().getWindow(),
                binding.getRoot()
        ).hide(WindowInsetsCompat.Type.ime());

        binding.edtChatMessage.clearFocus();
    }

    @Override
    public void onPause() {
        saveLocalDraftNow();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        saveLocalDraftNow();

        // Không ghi nội dung thuộc tài khoản trước vào Bundle mới.
        boolean sameOwner = !draftOwnerId.isEmpty()
                && draftOwnerId.equals(getCurrentUserId());

        if (sameOwner && hasMemoryDraft) {
            outState.putString(STATE_DRAFT, messageDraft);
            outState.putString(STATE_DRAFT_OWNER, draftOwnerId);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        saveLocalDraftNow();

        mainHandler.removeCallbacks(saveDraftRunnable);
        draftLoadVersion++;

        viewStateReady = false;
        draftReady = false;
        draftLoading = false;

        if (binding != null) {
            if (draftTextWatcher != null) {
                binding.edtChatMessage.removeTextChangedListener(
                        draftTextWatcher
                );
            }

            ViewCompat.setOnApplyWindowInsetsListener(
                    binding.getRoot(),
                    null
            );

            binding.btnChatBack.setOnClickListener(null);
            binding.btnSendMessage.setOnClickListener(null);
            binding.edtChatMessage.setOnEditorActionListener(null);
            binding.rvChatMessages.setAdapter(null);
        }

        draftTextWatcher = null;
        messageAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}