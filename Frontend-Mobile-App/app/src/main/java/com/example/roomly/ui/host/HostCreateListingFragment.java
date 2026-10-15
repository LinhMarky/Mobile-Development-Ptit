package com.example.roomly.ui.host;

import android.net.Uri;
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
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.ListingDraft;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostListingRepository;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.data.repository.LocalListingDraftRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostCreateListingBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tạo bài đăng từ phòng của chủ trọ.
 * Nội dung đang soạn được lưu riêng trong SQLite.
 */
public class HostCreateListingFragment extends Fragment {

    private static final String ARG_ROOM_ID = "listing_room_id";
    private static final String STATE_DRAFT_OWNER = "local_draft_owner";
    private static final String TAG = "ROOMLY_LOCAL_DRAFT";

    private static final ExecutorService LOCAL_DRAFT_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable saveLocalDraftRunnable =
            this::saveLocalDraftNow;

    private FragmentHostCreateListingBinding binding;
    private LocalListingDraftRepository localDraftRepository;
    private TextWatcher localDraftTextWatcher;

    private String roomId;
    private String localDraftOwnerId = "";

    private int localDraftLoadVersion = 0;
    private boolean viewStateReady = false;
    private boolean localDraftReady = false;
    private boolean localDraftLoading = false;
    private boolean localDraftDirty = false;
    private boolean applyingLocalDraft = false;
    private boolean listingSavedSuccessfully = false;

    public static HostCreateListingFragment newInstance(String roomId) {
        HostCreateListingFragment fragment =
                new HostCreateListingFragment();

        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        localDraftRepository = new LocalListingDraftRepository(
                requireContext().getApplicationContext()
        );

        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHostCreateListingBinding.inflate(
                inflater,
                container,
                false
        );

        viewStateReady = false;
        localDraftReady = false;
        localDraftLoading = false;
        localDraftDirty = false;
        localDraftOwnerId = "";

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupKeyboardInsets();
        setFormEnabled(false);

        binding.btnCreateListingBack.setOnClickListener(clickedView -> {
            saveLocalDraftNow();
            hideKeyboard();
            getParentFragmentManager().popBackStack();
        });

        binding.btnSaveListing.setOnClickListener(
                clickedView -> saveDraft()
        );

        binding.edtListingDescription.setSingleLine(false);
        binding.edtListingDescription.setImeOptions(
                EditorInfo.IME_ACTION_DONE
        );

        binding.edtListingDescription.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        saveLocalDraftNow();
                        hideKeyboard();
                        return true;
                    }

                    return false;
                }
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> {
                            if (viewStateReady) {
                                displayRoom();
                            }
                        }
                );
    }

    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        String currentOwner = getCurrentHostId();

        String restoredOwner = savedInstanceState == null
                ? ""
                : savedInstanceState.getString(
                STATE_DRAFT_OWNER,
                ""
        );

        // Chỉ giữ nội dung Android khôi phục nếu đúng tài khoản.
        boolean keepRestoredContent =
                !currentOwner.isEmpty()
                        && currentOwner.equals(restoredOwner);

        if (!keepRestoredContent) {
            clearDraftInputs();
        }

        setupLocalDraftTracking();
        viewStateReady = true;

        displayRoom(keepRestoredContent);
    }

    private String getCurrentHostId() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || session.getUserId() == null) {
            return "";
        }

        return session.getUserId().trim();
    }

    private void displayRoom() {
        displayRoom(false);
    }

    private void displayRoom(boolean keepRestoredContent) {
        if (binding == null || !viewStateReady) {
            return;
        }

        String currentOwner = getCurrentHostId();

        HostRoom room = currentOwner.isEmpty()
                || roomId == null
                || roomId.trim().isEmpty()
                ? null
                : DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (room == null) {
            mainHandler.removeCallbacks(saveLocalDraftRunnable);
            localDraftLoadVersion++;
            localDraftReady = false;
            localDraftLoading = false;
            localDraftDirty = false;
            localDraftOwnerId = "";

            clearDraftInputs();
            setFormEnabled(false);

            binding.layoutCreateListingForm.setVisibility(View.GONE);
            binding.imgListingRoom.setImageDrawable(null);
            binding.imgListingRoom.setVisibility(View.GONE);

            binding.tvListingRoomCode.setText("");
            binding.tvListingRoomName.setText("");
            binding.tvListingRoomAddress.setText("");

            hideKeyboard();

            showError(
                    "Không tìm thấy phòng thuộc tài khoản của bạn. "
                            + "Bạn quay lại danh sách nhé."
            );
            return;
        }

        binding.layoutCreateListingForm.setVisibility(View.VISIBLE);
        binding.tvListingRoomCode.setText(
                "Mã phòng: " + room.getUnitCode()
        );
        binding.tvListingRoomName.setText(room.getName());
        binding.tvListingRoomAddress.setText(room.getAddress());

        displayRoomImage(room);

        if (!currentOwner.equals(localDraftOwnerId)) {
            mainHandler.removeCallbacks(saveLocalDraftRunnable);
            localDraftLoadVersion++;

            if (!keepRestoredContent) {
                clearDraftInputs();
            }

            localDraftOwnerId = currentOwner;
            localDraftReady = false;
            localDraftLoading = false;
            localDraftDirty = false;
            listingSavedSuccessfully = false;

            clearErrors();
            restoreLocalDraft(keepRestoredContent);
        } else if (!localDraftReady && !localDraftLoading) {
            restoreLocalDraft(false);
        }
    }

    private void restoreLocalDraft(boolean keepRestoredContent) {
        if (binding == null || localDraftOwnerId.isEmpty()) {
            return;
        }

        localDraftReady = false;
        localDraftLoading = true;
        setFormEnabled(false);
        binding.progressCreateListing.setVisibility(View.VISIBLE);

        final String ownerId = localDraftOwnerId;
        final String selectedRoomId = roomId;
        final int loadVersion = ++localDraftLoadVersion;
        final View originalRoot = binding.getRoot();
        final LocalListingDraftRepository repository =
                localDraftRepository;

        LOCAL_DRAFT_EXECUTOR.execute(() -> {
            try {
                ListingDraft draft = repository.getDraft(
                        ownerId,
                        selectedRoomId
                );

                mainHandler.post(() -> {
                    if (!isCurrentDraftView(
                            originalRoot,
                            loadVersion,
                            ownerId
                    )) {
                        return;
                    }

                    if (!keepRestoredContent) {
                        applyLocalDraft(draft);
                    }

                    localDraftLoading = false;
                    localDraftReady = true;

                    binding.progressCreateListing.setVisibility(View.GONE);
                    setFormEnabled(true);

                    if (keepRestoredContent) {
                        localDraftDirty = true;
                        scheduleLocalDraftSave();
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

                    localDraftLoading = false;
                    binding.progressCreateListing.setVisibility(View.GONE);

                    showError(
                            "Không đọc được bản nháp trên điện thoại. "
                                    + "Bạn hãy quay lại rồi mở lại biểu mẫu."
                    );
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
                && localDraftLoadVersion == loadVersion
                && ownerId.equals(localDraftOwnerId)
                && ownerId.equals(getCurrentHostId())
                && !listingSavedSuccessfully;
    }

    private void applyLocalDraft(@Nullable ListingDraft draft) {
        applyingLocalDraft = true;

        try {
            binding.edtListingTitle.setText(
                    draft == null ? "" : draft.getTitle()
            );
            binding.edtListingPrice.setText(
                    draft == null ? "" : draft.getPriceText()
            );
            binding.edtListingDeposit.setText(
                    draft == null ? "" : draft.getDepositText()
            );
            binding.edtListingDescription.setText(
                    draft == null ? "" : draft.getDescription()
            );
        } finally {
            applyingLocalDraft = false;
        }

        localDraftDirty = false;
    }

    private void clearDraftInputs() {
        if (binding != null) {
            applyLocalDraft(null);
        }
    }

    private void setupLocalDraftTracking() {
        localDraftTextWatcher = new TextWatcher() {
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
                if (!applyingLocalDraft && localDraftReady) {
                    localDraftDirty = true;
                }
            }

            @Override
            public void afterTextChanged(Editable text) {
                if (!applyingLocalDraft && localDraftReady) {
                    scheduleLocalDraftSave();
                }
            }
        };

        binding.edtListingTitle.addTextChangedListener(
                localDraftTextWatcher
        );
        binding.edtListingPrice.addTextChangedListener(
                localDraftTextWatcher
        );
        binding.edtListingDeposit.addTextChangedListener(
                localDraftTextWatcher
        );
        binding.edtListingDescription.addTextChangedListener(
                localDraftTextWatcher
        );
    }

    private void scheduleLocalDraftSave() {
        mainHandler.removeCallbacks(saveLocalDraftRunnable);

        if (binding == null
                || !localDraftReady
                || !localDraftDirty
                || listingSavedSuccessfully) {
            return;
        }

        mainHandler.postDelayed(saveLocalDraftRunnable, 500);
    }

    private void saveLocalDraftNow() {
        mainHandler.removeCallbacks(saveLocalDraftRunnable);

        if (binding == null
                || !localDraftReady
                || !localDraftDirty
                || applyingLocalDraft
                || listingSavedSuccessfully
                || localDraftOwnerId.isEmpty()
                || !localDraftOwnerId.equals(getCurrentHostId())) {
            return;
        }

        ListingDraft draft = new ListingDraft(
                localDraftOwnerId,
                roomId,
                readRawText(binding.edtListingTitle),
                readRawText(binding.edtListingPrice),
                readRawText(binding.edtListingDeposit),
                readRawText(binding.edtListingDescription),
                System.currentTimeMillis()
        );

        localDraftDirty = false;

        final View originalRoot = binding.getRoot();
        final int loadVersion = localDraftLoadVersion;
        final String ownerId = localDraftOwnerId;
        final LocalListingDraftRepository repository =
                localDraftRepository;

        LOCAL_DRAFT_EXECUTOR.execute(() -> {
            try {
                repository.saveDraft(draft);
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

                    localDraftDirty = true;

                    showError(
                            "Chưa lưu được bản nháp trên điện thoại. "
                                    + "Bạn hãy giữ lại nội dung đang nhập."
                    );
                });
            }
        });
    }

    private void deleteLocalDraftAfterSuccess(String ownerId) {
        listingSavedSuccessfully = true;
        localDraftReady = false;
        localDraftDirty = false;
        localDraftLoadVersion++;

        mainHandler.removeCallbacks(saveLocalDraftRunnable);

        final String selectedRoomId = roomId;
        final LocalListingDraftRepository repository =
                localDraftRepository;

        // Cùng hàng đợi: xóa sau các lần ghi đã được gửi trước đó.
        LOCAL_DRAFT_EXECUTOR.execute(() -> {
            try {
                repository.deleteDraft(ownerId, selectedRoomId);
            } catch (RuntimeException exception) {
                Log.e(
                        TAG,
                        "Đã tạo bài đăng nhưng chưa xóa được bản soạn SQLite.",
                        exception
                );
            }
        });
    }

    private void displayRoomImage(HostRoom room) {
        binding.imgListingRoom.setImageDrawable(null);
        binding.imgListingRoom.setVisibility(View.GONE);

        String imageUri = room.getImageUri();

        if (imageUri == null || imageUri.trim().isEmpty()) {
            return;
        }

        try {
            binding.imgListingRoom.setImageURI(Uri.parse(imageUri));

            if (binding.imgListingRoom.getDrawable() != null) {
                binding.imgListingRoom.setVisibility(View.VISIBLE);
                binding.imgListingRoom.setContentDescription(
                        "Ảnh phòng " + room.getName()
                );
            }
        } catch (SecurityException exception) {
            binding.imgListingRoom.setImageDrawable(null);
        }
    }

    private void saveDraft() {
        if (binding == null
                || !binding.btnSaveListing.isEnabled()
                || !localDraftReady
                || listingSavedSuccessfully) {
            return;
        }

        clearErrors();

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !localDraftOwnerId.equals(getCurrentHostId())
                || DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId) == null) {
            displayRoom();
            return;
        }

        if (!session.isEmailVerified()) {
            showError(
                    "Bạn cần xác minh email ở trang Cá nhân "
                            + "trước khi lưu bài đăng."
            );
            return;
        }

        if (!validateForm()) {
            return;
        }

        String depositText = readText(binding.edtListingDeposit);
        Long depositVnd = null;

        if (!depositText.isEmpty()) {
            try {
                if (!depositText.matches("[0-9]+")) {
                    throw new NumberFormatException();
                }

                depositVnd = Long.parseLong(depositText);
            } catch (NumberFormatException exception) {
                binding.inputListingDeposit.setError(
                        "Nhập tiền cọc là số nguyên không âm "
                                + "và trong phạm vi hợp lệ."
                );
                binding.edtListingDeposit.requestFocus();
                return;
            }
        }

        hideKeyboard();
        setFormEnabled(false);
        binding.progressCreateListing.setVisibility(View.VISIBLE);

        try {
            long monthlyRent = Long.parseLong(
                    readText(binding.edtListingPrice)
            );

            // Khi nối API, chỉ xóa bản soạn SQLite
            // sau khi backend xác nhận tạo bài thành công.
            DemoHostListingRepository.getInstance().createDraft(
                    roomId,
                    readText(binding.edtListingTitle),
                    monthlyRent,
                    depositVnd,
                    readText(binding.edtListingDescription)
            );

            deleteLocalDraftAfterSuccess(localDraftOwnerId);

            Toast.makeText(
                    requireContext(),
                    "Đã lưu bản nháp mẫu, chưa đăng công khai.",
                    Toast.LENGTH_SHORT
            ).show();

            getParentFragmentManager().popBackStack();

        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
        } finally {
            if (binding != null) {
                binding.progressCreateListing.setVisibility(View.GONE);

                setFormEnabled(
                        localDraftReady && !listingSavedSuccessfully
                );
            }
        }
    }

    private boolean validateForm() {
        boolean valid = true;
        EditText firstInvalid = null;

        String title = readText(binding.edtListingTitle);

        if (title.isEmpty() || title.length() > 150) {
            binding.inputListingTitle.setError(
                    "Tiêu đề cần từ 1 đến 150 ký tự."
            );
            firstInvalid = binding.edtListingTitle;
            valid = false;
        }

        String priceText = readText(binding.edtListingPrice);
        boolean validPrice = false;

        if (priceText.matches("[0-9]+")) {
            try {
                validPrice = Long.parseLong(priceText) > 0;
            } catch (NumberFormatException exception) {
                validPrice = false;
            }
        }

        if (!validPrice) {
            binding.inputListingPrice.setError(
                    "Nhập giá thuê là số nguyên lớn hơn 0 và hợp lệ."
            );

            if (firstInvalid == null) {
                firstInvalid = binding.edtListingPrice;
            }

            valid = false;
        }

        String description = readText(
                binding.edtListingDescription
        );

        if (description.isEmpty() || description.length() > 5000) {
            binding.inputListingDescription.setError(
                    "Nội dung cần từ 1 đến 5000 ký tự."
            );

            if (firstInvalid == null) {
                firstInvalid = binding.edtListingDescription;
            }

            valid = false;
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocus();
        }

        return valid;
    }

    private void setFormEnabled(boolean enabled) {
        if (binding == null) {
            return;
        }

        binding.edtListingTitle.setEnabled(enabled);
        binding.edtListingPrice.setEnabled(enabled);
        binding.edtListingDeposit.setEnabled(enabled);
        binding.edtListingDescription.setEnabled(enabled);
        binding.btnSaveListing.setEnabled(enabled);
    }

    private String readRawText(EditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString();
    }

    private String readText(EditText input) {
        return readRawText(input).trim();
    }

    private void clearErrors() {
        if (binding == null) {
            return;
        }

        binding.inputListingTitle.setError(null);
        binding.inputListingPrice.setError(null);
        binding.inputListingDeposit.setError(null);
        binding.inputListingDescription.setError(null);

        binding.tvCreateListingError.setText("");
        binding.tvCreateListingError.setVisibility(View.GONE);
    }

    private void showError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvCreateListingError.setText(
                message == null || message.trim().isEmpty()
                        ? "Không thể xử lý bản nháp."
                        : message
        );
        binding.tvCreateListingError.setVisibility(View.VISIBLE);
    }

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

    private void hideKeyboard() {
        if (binding == null || !isAdded()) {
            return;
        }

        new WindowInsetsControllerCompat(
                requireActivity().getWindow(),
                binding.getRoot()
        ).hide(WindowInsetsCompat.Type.ime());

        View focusedView = binding.getRoot().findFocus();

        if (focusedView != null) {
            focusedView.clearFocus();
        }

        binding.getRoot().requestFocus();
    }

    @Override
    public void onPause() {
        saveLocalDraftNow();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        saveLocalDraftNow();

        boolean canKeepRestoredContent =
                localDraftReady
                        && !listingSavedSuccessfully
                        && !localDraftOwnerId.isEmpty()
                        && localDraftOwnerId.equals(getCurrentHostId());

        if (canKeepRestoredContent) {
            outState.putString(
                    STATE_DRAFT_OWNER,
                    localDraftOwnerId
            );
        } else {
            // SQLite chưa đọc xong: lần tạo lại phải đọc từ database,
            // không ưu tiên biểu mẫu đang trống trên giao diện.
            outState.remove(STATE_DRAFT_OWNER);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        saveLocalDraftNow();

        mainHandler.removeCallbacks(saveLocalDraftRunnable);
        localDraftLoadVersion++;
        viewStateReady = false;
        localDraftReady = false;
        localDraftLoading = false;

        if (binding != null) {
            if (localDraftTextWatcher != null) {
                binding.edtListingTitle.removeTextChangedListener(
                        localDraftTextWatcher
                );
                binding.edtListingPrice.removeTextChangedListener(
                        localDraftTextWatcher
                );
                binding.edtListingDeposit.removeTextChangedListener(
                        localDraftTextWatcher
                );
                binding.edtListingDescription.removeTextChangedListener(
                        localDraftTextWatcher
                );
            }

            binding.btnCreateListingBack.setOnClickListener(null);
            binding.btnSaveListing.setOnClickListener(null);
            binding.edtListingDescription.setOnEditorActionListener(null);
            binding.imgListingRoom.setImageDrawable(null);

            ViewCompat.setOnApplyWindowInsetsListener(
                    binding.getRoot(),
                    null
            );
        }

        localDraftTextWatcher = null;
        binding = null;

        super.onDestroyView();
    }
}