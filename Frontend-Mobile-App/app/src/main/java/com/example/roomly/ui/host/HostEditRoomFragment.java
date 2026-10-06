package com.example.roomly.ui.host;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostEditRoomBinding;

import java.math.BigDecimal;

/**
 * Chỉnh sửa thông tin và ảnh phòng mẫu.
 * ID, chủ sở hữu và mã phòng được giữ nguyên.
 */
public class HostEditRoomFragment extends Fragment {

    private static final String ARG_ROOM_ID = "edit_room_id";
    private static final String STATE_DRAFT = "edit_room_draft";
    private static final String STATE_IMAGE = "edit_room_image";

    private FragmentHostEditRoomBinding binding;

    private String roomId;
    private String selectedImageUri;
    private Bundle restoredDraft;

    private final ActivityResultLauncher<String[]> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::handleSelectedImage
            );

    /**
     * Tạo màn hình chỉnh sửa với ID phòng được chọn.
     */
    public static HostEditRoomFragment newInstance(String roomId) {
        HostEditRoomFragment fragment = new HostEditRoomFragment();

        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Đọc ID phòng và bản nháp khi Android tạo lại Fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }

        if (savedInstanceState != null) {
            restoredDraft = savedInstanceState.getBundle(STATE_DRAFT);
            selectedImageUri = savedInstanceState.getString(STATE_IMAGE);
        }
    }

    /**
     * Tạo giao diện chỉnh sửa phòng bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHostEditRoomBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập các nút, bàn phím và theo dõi quyền truy cập.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupKeyboardInsets();

        binding.btnEditRoomBack.setOnClickListener(clickedView -> {
            hideKeyboard();
            getParentFragmentManager().popBackStack();
        });

        binding.btnEditRoomSelectImage.setOnClickListener(clickedView -> {
            if (getAccessibleRoom() == null) {
                renderAccess();
                return;
            }

            hideKeyboard();
            imagePicker.launch(new String[]{"image/*"});
        });

        binding.btnEditRoomSave.setOnClickListener(
                clickedView -> saveChanges()
        );

        binding.edtEditRoomDescription.setSingleLine(false);
        binding.edtEditRoomDescription.setImeOptions(
                EditorInfo.IME_ACTION_DONE
        );

        binding.edtEditRoomDescription.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
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
                        session -> renderAccess()
                );
    }

    /**
     * Điền dữ liệu sau khi Android khôi phục trạng thái các ô nhập.
     * Ưu tiên bản nháp để không mất nội dung khi xoay màn hình.
     */
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        HostRoom room = getAccessibleRoom();

        if (room == null) {
            renderAccess();
            return;
        }

        if (restoredDraft != null) {
            binding.edtEditRoomName.setText(
                    restoredDraft.getString("name", "")
            );

            binding.edtEditRoomAddress.setText(
                    restoredDraft.getString("address", "")
            );

            binding.edtEditRoomArea.setText(
                    restoredDraft.getString("area", "")
            );

            binding.edtEditRoomDescription.setText(
                    restoredDraft.getString("description", "")
            );
        } else {
            binding.edtEditRoomName.setText(room.getName());
            binding.edtEditRoomAddress.setText(room.getAddress());

            binding.edtEditRoomArea.setText(
                    room.getArea().stripTrailingZeros().toPlainString()
            );

            binding.edtEditRoomDescription.setText(
                    room.getDescription()
            );

            selectedImageUri = room.getImageUri();
        }

        displayImage();
        renderAccess();
    }

    /**
     * Lưu bản nháp và đường dẫn ảnh khi Android tạo lại màn hình.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (binding != null) {
            restoredDraft = captureDraft();
        }

        if (restoredDraft != null) {
            outState.putBundle(STATE_DRAFT, restoredDraft);
        }

        outState.putString(STATE_IMAGE, selectedImageUri);
    }

    /**
     * Đọc nội dung đang nhập để giữ lại bản nháp.
     */
    private Bundle captureDraft() {
        Bundle draft = new Bundle();

        draft.putString("name", readText(binding.edtEditRoomName));
        draft.putString("address", readText(binding.edtEditRoomAddress));
        draft.putString("area", readText(binding.edtEditRoomArea));
        draft.putString(
                "description",
                readText(binding.edtEditRoomDescription)
        );

        return draft;
    }

    /**
     * Chỉ trả về phòng thuộc tài khoản chủ trọ hiện tại.
     */
    private HostRoom getAccessibleRoom() {
        return DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);
    }

    /**
     * Hiển thị biểu mẫu khi còn quyền truy cập.
     * Việc lưu yêu cầu email đã xác minh và được kiểm tra lại khi bấm nút.
     */
    private void renderAccess() {
        if (binding == null) {
            return;
        }

        HostRoom room = getAccessibleRoom();

        if (room == null) {
            binding.layoutEditRoomForm.setVisibility(View.GONE);
            hideKeyboard();

            showError(
                    "Không tìm thấy phòng thuộc tài khoản của bạn. "
                            + "Bạn quay lại danh sách nhé."
            );
            return;
        }

        binding.layoutEditRoomForm.setVisibility(View.VISIBLE);
        binding.tvEditRoomCode.setText(
                "Mã phòng: " + room.getUnitCode()
        );
    }

    /**
     * Giữ quyền đọc và hiển thị ảnh mới.
     * Hủy trình chọn ảnh sẽ giữ nguyên ảnh hiện tại.
     */
    private void handleSelectedImage(@Nullable Uri uri) {
        if (uri == null || getAccessibleRoom() == null) {
            return;
        }

        try {
            requireContext().getContentResolver()
                    .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

            selectedImageUri = uri.toString();
            displayImage();

        } catch (SecurityException exception) {
            Toast.makeText(
                    requireContext(),
                    "Không đọc được ảnh này. Bạn chọn ảnh khác nhé.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Hiển thị ảnh hiện tại hoặc phần thay thế.
     * Không xóa đường dẫn cũ chỉ vì ảnh tạm thời không đọc được.
     */
    private void displayImage() {
        if (binding == null) {
            return;
        }

        binding.imgEditRoomPreview.setImageDrawable(null);

        boolean imageLoaded = false;

        if (selectedImageUri != null && !selectedImageUri.isEmpty()) {
            try {
                binding.imgEditRoomPreview.setImageURI(
                        Uri.parse(selectedImageUri)
                );

                imageLoaded =
                        binding.imgEditRoomPreview.getDrawable() != null;

            } catch (SecurityException exception) {
                binding.imgEditRoomPreview.setImageDrawable(null);
            }
        }

        binding.imgEditRoomPreview.setVisibility(
                imageLoaded ? View.VISIBLE : View.GONE
        );

        binding.tvEditRoomImagePlaceholder.setVisibility(
                imageLoaded ? View.GONE : View.VISIBLE
        );

        binding.btnEditRoomSelectImage.setText(
                selectedImageUri == null ? "Chọn ảnh" : "Đổi ảnh"
        );
    }

    /**
     * Kiểm tra quyền và thông tin trước khi cập nhật phòng mẫu.
     * Giữ nội dung biểu mẫu nếu thao tác thất bại.
     */
    private void saveChanges() {
        if (binding == null || !binding.btnEditRoomSave.isEnabled()) {
            return;
        }

        clearErrors();

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || getAccessibleRoom() == null) {
            renderAccess();
            return;
        }

        if (!session.isEmailVerified()) {
            showError(
                    "Bạn cần xác minh email ở trang Cá nhân "
                            + "trước khi lưu thay đổi."
            );
            return;
        }

        if (!validateForm()) {
            return;
        }

        hideKeyboard();
        binding.btnEditRoomSave.setEnabled(false);

        try {
            BigDecimal area = new BigDecimal(
                    readText(binding.edtEditRoomArea).replace(',', '.')
            );

            DemoHostRoomRepository.getInstance().updateRoom(
                    roomId,
                    readText(binding.edtEditRoomName),
                    readText(binding.edtEditRoomAddress),
                    area,
                    readText(binding.edtEditRoomDescription),
                    selectedImageUri
            );

            Toast.makeText(
                    requireContext(),
                    "Đã cập nhật phòng mẫu.",
                    Toast.LENGTH_SHORT
            ).show();

            getParentFragmentManager().popBackStack();

        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());

        } finally {
            if (binding != null) {
                binding.btnEditRoomSave.setEnabled(true);
            }
        }
    }

    /**
     * Kiểm tra tên, địa chỉ và diện tích trước khi lưu.
     */
    private boolean validateForm() {
        boolean valid = true;
        EditText firstInvalid = null;

        if (readText(binding.edtEditRoomName).isEmpty()) {
            binding.inputEditRoomName.setError("Vui lòng nhập tên phòng.");
            firstInvalid = binding.edtEditRoomName;
            valid = false;
        }

        if (readText(binding.edtEditRoomAddress).isEmpty()) {
            binding.inputEditRoomAddress.setError("Vui lòng nhập địa chỉ.");

            if (firstInvalid == null) {
                firstInvalid = binding.edtEditRoomAddress;
            }

            valid = false;
        }

        String areaText = readText(binding.edtEditRoomArea)
                .replace(',', '.');

        boolean validArea = false;

        if (areaText.matches("[0-9]+(\\.[0-9]+)?")) {
            try {
                validArea = new BigDecimal(areaText)
                        .compareTo(BigDecimal.ZERO) > 0;
            } catch (NumberFormatException exception) {
                validArea = false;
            }
        }

        if (!validArea) {
            binding.inputEditRoomArea.setError(
                    "Nhập diện tích là số lớn hơn 0."
            );

            if (firstInvalid == null) {
                firstInvalid = binding.edtEditRoomArea;
            }

            valid = false;
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocus();
        }

        return valid;
    }

    /**
     * Đọc nội dung ô nhập và loại bỏ khoảng trắng ở hai đầu.
     */
    private String readText(EditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().trim();
    }

    /**
     * Xóa lỗi cũ trước khi kiểm tra lại biểu mẫu.
     */
    private void clearErrors() {
        binding.inputEditRoomName.setError(null);
        binding.inputEditRoomAddress.setError(null);
        binding.inputEditRoomArea.setError(null);
        binding.inputEditRoomDescription.setError(null);

        binding.tvEditRoomError.setText("");
        binding.tvEditRoomError.setVisibility(View.GONE);
    }

    /**
     * Hiển thị thông báo lỗi chung.
     */
    private void showError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvEditRoomError.setText(
                message == null ? "Không thể cập nhật phòng." : message
        );

        binding.tvEditRoomError.setVisibility(View.VISIBLE);
    }

    /**
     * Thêm khoảng trống để bàn phím không che biểu mẫu.
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
     * Đóng bàn phím và bỏ focus khỏi ô nhập.
     */
    private void hideKeyboard() {
        if (binding == null) {
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

    /**
     * Giữ bản nháp và giải phóng các tham chiếu giao diện.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            restoredDraft = captureDraft();

            binding.btnEditRoomBack.setOnClickListener(null);
            binding.btnEditRoomSave.setOnClickListener(null);
            binding.btnEditRoomSelectImage.setOnClickListener(null);
            binding.edtEditRoomDescription.setOnEditorActionListener(null);
            binding.imgEditRoomPreview.setImageDrawable(null);

            ViewCompat.setOnApplyWindowInsetsListener(
                    binding.getRoot(),
                    null
            );
        }

        binding = null;

        super.onDestroyView();
    }
}