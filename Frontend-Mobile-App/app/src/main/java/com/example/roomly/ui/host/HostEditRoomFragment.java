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
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.RoomCard.RoomType;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostEditRoomBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Chỉnh sửa thông tin và ảnh phòng trong repository mẫu.
 * ID, chủ sở hữu và mã phòng được giữ nguyên.
 */
public class HostEditRoomFragment extends Fragment {

    private static final String ARG_ROOM_ID = "edit_room_id";
    private static final String STATE_DRAFT = "edit_room_draft";

    private static final String[] TYPE_LABELS = {
            "Phòng trọ", "Căn hộ", "Studio"
    };

    private static final RoomType[] ROOM_TYPES = {
            RoomType.ROOM, RoomType.APARTMENT, RoomType.STUDIO
    };

    // Danh mục thử giao diện, chưa phải amenity_ids của API.
    private static final String[] AMENITY_LABELS = {
            "Wi-Fi",
            "Điều hòa",
            "Có nội thất",
            "Bếp riêng",
            "Chỗ để xe",
            "Máy giặt",
            "Ban công"
    };

    private FragmentHostEditRoomBinding binding;
    private AlertDialog optionsDialog;

    private String roomId;
    private String selectedImageUri;
    private String imagePickerOwnerId;
    private RoomType selectedRoomType;
    private Bundle restoredDraft;

    private final List<String> selectedAmenities = new ArrayList<>();

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
            imagePickerOwnerId = savedInstanceState.getString(
                    "image_picker_owner_id"
            );
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
                inflater, container, false
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
            HostRoom room = getAccessibleRoom();

            if (room == null) {
                renderAccess();
                return;
            }

            imagePickerOwnerId = room.getOwnerId();
            hideKeyboard();
            imagePicker.launch(new String[]{"image/*"});
        });

        binding.btnEditRoomType.setOnClickListener(
                clickedView -> showRoomTypeDialog()
        );

        binding.btnEditRoomAmenities.setOnClickListener(
                clickedView -> showAmenitiesDialog()
        );

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
     * Điền dữ liệu sau khi Android khôi phục trạng thái View.
     * Ưu tiên bản nháp để giữ nội dung người dùng đang sửa.
     */
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        HostRoom room = getAccessibleRoom();

        if (room == null) {
            renderAccess();
            return;
        }

        selectedAmenities.clear();

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

            binding.edtEditRoomMaxOccupants.setText(
                    restoredDraft.getString("occupants", "")
            );

            binding.edtEditRoomDescription.setText(
                    restoredDraft.getString("description", "")
            );

            // Giữ ảnh mới nhận nếu callback đã chạy trước khi khôi phục View.
            if (selectedImageUri == null) {
                selectedImageUri = restoredDraft.getString("image_uri");
            }

            String restoredType = restoredDraft.getString("room_type");

            try {
                selectedRoomType = restoredType == null
                        ? room.getRoomType()
                        : RoomType.valueOf(restoredType);
            } catch (IllegalArgumentException exception) {
                selectedRoomType = room.getRoomType();
            }

            ArrayList<String> amenities =
                    restoredDraft.getStringArrayList("amenities");

            selectedAmenities.addAll(
                    amenities == null ? room.getAmenities() : amenities
            );
        } else {
            binding.edtEditRoomName.setText(room.getName());
            binding.edtEditRoomAddress.setText(room.getAddress());

            binding.edtEditRoomArea.setText(
                    room.getArea() == null
                            ? ""
                            : room.getArea()
                            .stripTrailingZeros()
                            .toPlainString()
            );

            binding.edtEditRoomMaxOccupants.setText(
                    room.getMaxOccupants() > 0
                            ? String.valueOf(room.getMaxOccupants())
                            : ""
            );

            binding.edtEditRoomDescription.setText(
                    room.getDescription()
            );

            if (selectedImageUri == null) {
                selectedImageUri = room.getImageUri();
            }

            selectedRoomType = room.getRoomType();
            selectedAmenities.addAll(room.getAmenities());
        }

        renderRoomOptions();
        displayImage();
        renderAccess();
    }

    /**
     * Lưu bản nháp và tài khoản chọn ảnh khi Android tạo lại màn hình.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding != null) {
            restoredDraft = captureDraft();
        }

        if (restoredDraft != null) {
            outState.putBundle(STATE_DRAFT, restoredDraft);
        }

        outState.putString(
                "image_picker_owner_id", imagePickerOwnerId
        );

        super.onSaveInstanceState(outState);
    }

    /**
     * Chụp toàn bộ nội dung và lựa chọn của biểu mẫu.
     */
    private Bundle captureDraft() {
        Bundle draft = new Bundle();

        draft.putString("name", readText(binding.edtEditRoomName));
        draft.putString("address", readText(binding.edtEditRoomAddress));
        draft.putString("area", readText(binding.edtEditRoomArea));

        draft.putString(
                "occupants",
                readText(binding.edtEditRoomMaxOccupants)
        );

        draft.putString(
                "description",
                readText(binding.edtEditRoomDescription)
        );

        draft.putString("image_uri", selectedImageUri);

        draft.putString(
                "room_type",
                selectedRoomType == null
                        ? null
                        : selectedRoomType.name()
        );

        draft.putStringArrayList(
                "amenities", new ArrayList<>(selectedAmenities)
        );

        return draft;
    }

    /**
     * Chỉ trả về phòng thuộc tài khoản chủ trọ hiện tại.
     */
    @Nullable
    private HostRoom getAccessibleRoom() {
        if (roomId == null || roomId.trim().isEmpty()) {
            return null;
        }

        return DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);
    }

    /**
     * Hiển thị biểu mẫu khi tài khoản còn quyền truy cập phòng.
     */
    private void renderAccess() {
        if (binding == null) {
            return;
        }

        HostRoom room = getAccessibleRoom();

        if (room == null) {
            closeOptionsDialog();
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
     * Nhận ảnh khi tài khoản vẫn là tài khoản đã mở trình chọn ảnh.
     */
    private void handleSelectedImage(@Nullable Uri uri) {
        if (uri == null || !isAdded()) {
            return;
        }

        HostRoom room = getAccessibleRoom();
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (room == null
                || !session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || imagePickerOwnerId == null
                || !imagePickerOwnerId.equals(session.getUserId())) {
            return;
        }

        try {
            requireContext().getContentResolver()
                    .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

            selectedImageUri = uri.toString();

            if (restoredDraft != null) {
                restoredDraft.putString("image_uri", selectedImageUri);
            }

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
     * Hiển thị ảnh hoặc nội dung thay thế nếu không đọc được ảnh.
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
                selectedImageUri == null || selectedImageUri.isEmpty()
                        ? "Chọn ảnh"
                        : "Đổi ảnh"
        );
    }

    /**
     * Hiển thị loại phòng và danh sách tiện ích đang chọn.
     */
    private void renderRoomOptions() {
        if (binding == null) {
            return;
        }

        String typeLabel = "Chọn loại phòng";

        for (int index = 0; index < ROOM_TYPES.length; index++) {
            if (ROOM_TYPES[index] == selectedRoomType) {
                typeLabel = "Loại phòng: " + TYPE_LABELS[index];
                break;
            }
        }

        binding.btnEditRoomType.setText(typeLabel);

        binding.tvEditRoomAmenities.setText(
                selectedAmenities.isEmpty()
                        ? "Chưa chọn tiện ích"
                        : android.text.TextUtils.join(
                        " • ", selectedAmenities
                )
        );
    }

    /**
     * Chọn loại phòng và chỉ cập nhật khi bấm Áp dụng.
     */
    private void showRoomTypeDialog() {
        if (binding == null
                || optionsDialog != null
                || getAccessibleRoom() == null) {
            return;
        }

        hideKeyboard();

        int currentIndex = -1;

        for (int index = 0; index < ROOM_TYPES.length; index++) {
            if (ROOM_TYPES[index] == selectedRoomType) {
                currentIndex = index;
                break;
            }
        }

        final int[] chosenIndex = {currentIndex};

        optionsDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Loại phòng")
                .setSingleChoiceItems(
                        TYPE_LABELS,
                        currentIndex,
                        (dialog, which) -> chosenIndex[0] = which
                )
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    if (binding == null
                            || getAccessibleRoom() == null
                            || chosenIndex[0] < 0) {
                        return;
                    }

                    selectedRoomType = ROOM_TYPES[chosenIndex[0]];
                    renderRoomOptions();
                })
                .create();

        optionsDialog.setOnDismissListener(
                dialog -> optionsDialog = null
        );

        optionsDialog.show();
    }

    /**
     * Chọn nhiều tiện ích và giữ lựa chọn cũ nếu đóng hộp thoại.
     */
    private void showAmenitiesDialog() {
        if (binding == null
                || optionsDialog != null
                || getAccessibleRoom() == null) {
            return;
        }

        hideKeyboard();

        boolean[] checked = new boolean[AMENITY_LABELS.length];

        for (int index = 0; index < AMENITY_LABELS.length; index++) {
            checked[index] = selectedAmenities.contains(
                    AMENITY_LABELS[index]
            );
        }

        optionsDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tiện ích phòng")
                .setMultiChoiceItems(
                        AMENITY_LABELS,
                        checked,
                        (dialog, which, isChecked) ->
                                checked[which] = isChecked
                )
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    if (binding == null || getAccessibleRoom() == null) {
                        return;
                    }

                    // Giữ tiện ích cũ ngoài danh mục thử giao diện.
                    for (int index = 0; index < checked.length; index++) {
                        selectedAmenities.remove(AMENITY_LABELS[index]);

                        if (checked[index]) {
                            selectedAmenities.add(
                                    AMENITY_LABELS[index]
                            );
                        }
                    }

                    renderRoomOptions();
                })
                .create();

        optionsDialog.setOnDismissListener(
                dialog -> optionsDialog = null
        );

        optionsDialog.show();
    }

    /**
     * Đóng hộp thoại lựa chọn khi giao diện hoặc quyền truy cập thay đổi.
     */
    private void closeOptionsDialog() {
        if (optionsDialog != null) {
            optionsDialog.dismiss();
            optionsDialog = null;
        }
    }

    /**
     * Kiểm tra quyền và dữ liệu rồi lưu thay đổi vào repository mẫu.
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
        binding.progressEditRoom.setVisibility(View.VISIBLE);

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
                    selectedImageUri,
                    selectedRoomType,
                    Integer.parseInt(
                            readText(binding.edtEditRoomMaxOccupants)
                    ),
                    new ArrayList<>(selectedAmenities)
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
                binding.progressEditRoom.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Kiểm tra dữ liệu và đưa focus đến ô sai đầu tiên.
     */
    private boolean validateForm() {
        boolean valid = true;
        EditText firstInvalid = null;

        if (readText(binding.edtEditRoomName).isEmpty()) {
            binding.inputEditRoomName.setError(
                    "Vui lòng nhập tên phòng."
            );
            firstInvalid = binding.edtEditRoomName;
            valid = false;
        }

        String address = readText(binding.edtEditRoomAddress);
        int addressLength = address.codePointCount(0, address.length());

        if (addressLength < 5 || addressLength > 300) {
            binding.inputEditRoomAddress.setError(
                    "Địa chỉ phải có từ 5 đến 300 ký tự."
            );

            if (firstInvalid == null) {
                firstInvalid = binding.edtEditRoomAddress;
            }

            valid = false;
        }

        if (!isValidArea(readText(binding.edtEditRoomArea))) {
            binding.inputEditRoomArea.setError(
                    "Diện tích phải từ 2 đến 1.000 m²."
            );

            if (firstInvalid == null) {
                firstInvalid = binding.edtEditRoomArea;
            }

            valid = false;
        }

        if (!readText(binding.edtEditRoomMaxOccupants)
                .matches("(?:[1-9]|10)")) {
            binding.inputEditRoomMaxOccupants.setError(
                    "Nhập số người tối đa từ 1 đến 10."
            );

            if (firstInvalid == null) {
                firstInvalid = binding.edtEditRoomMaxOccupants;
            }

            valid = false;
        }

        if (selectedRoomType == null) {
            showError("Vui lòng chọn loại phòng.");
            valid = false;
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocus();
        } else if (!valid) {
            binding.btnEditRoomType.requestFocus();
        }

        return valid;
    }

    /**
     * Kiểm tra diện tích và chấp nhận dấu phẩy hoặc dấu chấm.
     */
    private boolean isValidArea(String text) {
        String normalized = text.replace(',', '.');

        if (!normalized.matches("[0-9]+(\\.[0-9]+)?")) {
            return false;
        }

        try {
            BigDecimal area = new BigDecimal(normalized);

            return area.compareTo(new BigDecimal("2")) >= 0
                    && area.compareTo(new BigDecimal("1000")) <= 0;
        } catch (NumberFormatException exception) {
            return false;
        }
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
     * Xóa lỗi cũ trước lần kiểm tra mới.
     */
    private void clearErrors() {
        binding.inputEditRoomName.setError(null);
        binding.inputEditRoomAddress.setError(null);
        binding.inputEditRoomArea.setError(null);
        binding.inputEditRoomMaxOccupants.setError(null);
        binding.inputEditRoomDescription.setError(null);

        binding.tvEditRoomError.setText("");
        binding.tvEditRoomError.setVisibility(View.GONE);
    }

    /**
     * Hiển thị lỗi chung của biểu mẫu.
     */
    private void showError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvEditRoomError.setText(
                message == null || message.trim().isEmpty()
                        ? "Không thể cập nhật phòng."
                        : message
        );

        binding.tvEditRoomError.setVisibility(View.VISIBLE);
    }

    /**
     * Thêm khoảng trống bàn phím và giữ padding ban đầu.
     */
    private void setupKeyboardInsets() {
        View root = binding.getRoot();

        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int keyboardBottom = insets.getInsets(
                    WindowInsetsCompat.Type.ime()
            ).bottom;

            int systemBottom = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            ).bottom;

            view.setPadding(
                    left,
                    top,
                    right,
                    bottom + Math.max(0, keyboardBottom - systemBottom)
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
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
     * Giữ bản nháp, đóng hộp thoại và giải phóng ViewBinding.
     */
    @Override
    public void onDestroyView() {
        closeOptionsDialog();

        if (binding != null) {
            restoredDraft = captureDraft();

            binding.btnEditRoomBack.setOnClickListener(null);
            binding.btnEditRoomSave.setOnClickListener(null);
            binding.btnEditRoomSelectImage.setOnClickListener(null);
            binding.btnEditRoomType.setOnClickListener(null);
            binding.btnEditRoomAmenities.setOnClickListener(null);
            binding.edtEditRoomDescription.setOnEditorActionListener(null);
            binding.imgEditRoomPreview.setImageDrawable(null);

            ViewCompat.setOnApplyWindowInsetsListener(
                    binding.getRoot(), null
            );
        }

        binding = null;
        super.onDestroyView();
    }
}