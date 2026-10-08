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

import com.example.roomly.R;
import com.example.roomly.data.model.RoomCard.RoomType;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostAddRoomBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Biểu mẫu thêm phòng của chủ trọ.
 * Thông tin và URI ảnh được lưu vào repository mẫu trong bộ nhớ.
 */
public class HostAddRoomFragment extends Fragment {

    private static final String STATE_DRAFT = "add_room_draft";

    private static final String[] TYPE_LABELS = {
            "Phòng trọ", "Căn hộ", "Studio"
    };

    private static final RoomType[] ROOM_TYPES = {
            RoomType.ROOM, RoomType.APARTMENT, RoomType.STUDIO
    };

    // Danh mục thử giao diện; chưa phải danh mục ID của backend.
    private static final String[] AMENITY_LABELS = {
            "Wi-Fi",
            "Điều hòa",
            "Có nội thất",
            "Bếp riêng",
            "Chỗ để xe",
            "Máy giặt",
            "Ban công"
    };

    private FragmentHostAddRoomBinding binding;
    private AlertDialog optionsDialog;

    private Bundle draft;
    private Uri selectedImageUri;
    private RoomType selectedRoomType;
    private final List<String> selectedAmenities = new ArrayList<>();

    // Giữ tài khoản bắt đầu nhập để tránh lưu nhầm khi đổi phiên.
    private String draftOwnerId;
    private String imagePickerOwnerId;

    private final ActivityResultLauncher<String[]> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::handleSelectedImage
            );

    /**
     * Khôi phục bản nháp khi Android tạo lại Fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            draft = savedInstanceState.getBundle(STATE_DRAFT);
        }

        if (draft != null) {
            draftOwnerId = draft.getString("owner_id");

            String image = draft.getString("image_uri");
            if (image != null && !image.isEmpty()) {
                selectedImageUri = Uri.parse(image);
            }

            String type = draft.getString("room_type");
            if (type != null) {
                try {
                    selectedRoomType = RoomType.valueOf(type);
                } catch (IllegalArgumentException exception) {
                    selectedRoomType = null;
                }
            }

            ArrayList<String> amenities =
                    draft.getStringArrayList("amenities");

            if (amenities != null) {
                selectedAmenities.addAll(amenities);
            }
        } else {
            SessionState session = SessionRepository.getInstance()
                    .getCurrentSession();

            if (session.isLoggedIn()) {
                draftOwnerId = session.getUserId();
            }
        }
    }

    /**
     * Tạo giao diện thêm phòng bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHostAddRoomBinding.inflate(
                inflater, container, false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký thao tác chọn ảnh, loại phòng, tiện ích và tạo phòng.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupKeyboardInsets();

        binding.btnAddRoomBack.setOnClickListener(clickedView -> {
            hideKeyboard();
            getParentFragmentManager().popBackStack();
        });

        binding.btnSelectRoomImage.setOnClickListener(clickedView -> {
            if (!checkCreatePermission()) {
                return;
            }

            imagePickerOwnerId = draftOwnerId;
            hideKeyboard();
            imagePicker.launch(new String[]{"image/*"});
        });

        binding.btnChooseRoomType.setOnClickListener(
                clickedView -> showRoomTypeDialog()
        );

        binding.btnChooseRoomAmenities.setOnClickListener(
                clickedView -> showAmenitiesDialog()
        );

        binding.btnCreateRoom.setOnClickListener(
                clickedView -> handleCreateRoom()
        );

        binding.edtRoomDescription.setSingleLine(false);
        binding.edtRoomDescription.setImeOptions(
                EditorInfo.IME_ACTION_DONE
        );

        binding.edtRoomDescription.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideKeyboard();
                        return true;
                    }
                    return false;
                }
        );
    }

    /**
     * Khôi phục nội dung sau khi Android khôi phục trạng thái View.
     */
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (draft != null) {
            binding.edtRoomCode.setText(draft.getString("code", ""));
            binding.edtRoomName.setText(draft.getString("name", ""));
            binding.edtRoomAddress.setText(draft.getString("address", ""));
            binding.edtRoomArea.setText(draft.getString("area", ""));
            binding.edtRoomMaxOccupants.setText(
                    draft.getString("occupants", "")
            );
            binding.edtRoomDescription.setText(
                    draft.getString("description", "")
            );
        }

        renderRoomOptions();
        displaySelectedImage();
    }

    /**
     * Lưu nội dung nhập và các lựa chọn khi Activity được tạo lại.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        captureDraft();
        outState.putBundle(STATE_DRAFT, draft);
        super.onSaveInstanceState(outState);
    }

    /**
     * Chụp bản nháp để giữ dữ liệu khi giao diện bị hủy.
     */
    private void captureDraft() {
        if (binding == null) {
            return;
        }

        draft = new Bundle();
        draft.putString("owner_id", draftOwnerId);
        draft.putString("code", readText(binding.edtRoomCode));
        draft.putString("name", readText(binding.edtRoomName));
        draft.putString("address", readText(binding.edtRoomAddress));
        draft.putString("area", readText(binding.edtRoomArea));
        draft.putString(
                "occupants", readText(binding.edtRoomMaxOccupants)
        );
        draft.putString(
                "description", readText(binding.edtRoomDescription)
        );
        draft.putString(
                "image_uri",
                selectedImageUri == null ? null : selectedImageUri.toString()
        );
        draft.putString(
                "room_type",
                selectedRoomType == null ? null : selectedRoomType.name()
        );
        draft.putStringArrayList(
                "amenities", new ArrayList<>(selectedAmenities)
        );
    }

    /**
     * Mở danh sách loại phòng và chỉ lưu khi bấm Áp dụng.
     */
    private void showRoomTypeDialog() {
        if (optionsDialog != null || !checkCreatePermission()) {
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
                    if (chosenIndex[0] >= 0 && binding != null) {
                        selectedRoomType = ROOM_TYPES[chosenIndex[0]];
                        renderRoomOptions();
                    }
                })
                .create();

        optionsDialog.setOnDismissListener(dialog -> optionsDialog = null);
        optionsDialog.show();
    }

    /**
     * Chọn nhiều tiện ích, giữ lựa chọn cũ nếu đóng hộp thoại.
     */
    private void showAmenitiesDialog() {
        if (optionsDialog != null || !checkCreatePermission()) {
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
                    if (binding == null) {
                        return;
                    }

                    selectedAmenities.clear();

                    for (int index = 0; index < checked.length; index++) {
                        if (checked[index]) {
                            selectedAmenities.add(AMENITY_LABELS[index]);
                        }
                    }

                    renderRoomOptions();
                })
                .create();

        optionsDialog.setOnDismissListener(dialog -> optionsDialog = null);
        optionsDialog.show();
    }

    /**
     * Cập nhật loại phòng và danh sách tiện ích đang chọn.
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

        binding.btnChooseRoomType.setText(typeLabel);
        binding.tvRoomAmenities.setText(
                selectedAmenities.isEmpty()
                        ? "Chưa chọn tiện ích"
                        : android.text.TextUtils.join(" • ", selectedAmenities)
        );
    }

    /**
     * Nhận ảnh và giữ quyền đọc nếu phiên vẫn là tài khoản chọn ảnh.
     */
    private void handleSelectedImage(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.isEmailVerified()
                || imagePickerOwnerId == null
                || !imagePickerOwnerId.equals(session.getUserId())) {
            return;
        }

        try {
            requireContext().getContentResolver()
                    .takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

            selectedImageUri = uri;
            displaySelectedImage();
        } catch (SecurityException exception) {
            Toast.makeText(
                    requireContext(),
                    "Không thể giữ quyền đọc ảnh. Bạn chọn ảnh khác nhé.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Hiển thị ảnh xem trước hoặc phần thay thế khi không đọc được ảnh.
     */
    private void displaySelectedImage() {
        if (binding == null) {
            return;
        }

        binding.imgAddRoomPreview.setImageDrawable(null);

        try {
            if (selectedImageUri != null) {
                binding.imgAddRoomPreview.setImageURI(selectedImageUri);

                if (binding.imgAddRoomPreview.getDrawable() == null) {
                    selectedImageUri = null;
                }
            }
        } catch (SecurityException exception) {
            selectedImageUri = null;
            binding.imgAddRoomPreview.setImageDrawable(null);
        }

        boolean hasImage = selectedImageUri != null;

        binding.imgAddRoomPreview.setVisibility(
                hasImage ? View.VISIBLE : View.GONE
        );
        binding.tvAddRoomImagePlaceholder.setVisibility(
                hasImage ? View.GONE : View.VISIBLE
        );
        binding.btnSelectRoomImage.setText(
                hasImage ? "Đổi ảnh" : "Chọn ảnh"
        );
    }

    /**
     * Kiểm tra quyền và dữ liệu rồi tạo phòng mẫu.
     */
    private void handleCreateRoom() {
        if (binding == null || !binding.btnCreateRoom.isEnabled()) {
            return;
        }

        clearErrors();

        if (!checkCreatePermission() || !validateForm()) {
            return;
        }

        hideKeyboard();
        binding.btnCreateRoom.setEnabled(false);
        binding.progressAddRoom.setVisibility(View.VISIBLE);

        try {
            DemoHostRoomRepository.getInstance().createRoom(
                    readText(binding.edtRoomCode),
                    readText(binding.edtRoomName),
                    readText(binding.edtRoomAddress),
                    new BigDecimal(
                            readText(binding.edtRoomArea).replace(',', '.')
                    ),
                    readText(binding.edtRoomDescription),
                    selectedImageUri == null
                            ? null : selectedImageUri.toString(),
                    selectedRoomType,
                    Integer.parseInt(
                            readText(binding.edtRoomMaxOccupants)
                    ),
                    new ArrayList<>(selectedAmenities)
            );

            Toast.makeText(
                    requireContext(),
                    "Đã tạo phòng mẫu.",
                    Toast.LENGTH_SHORT
            ).show();

            getParentFragmentManager().popBackStack();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showFormError(exception.getMessage());
        } finally {
            if (binding != null) {
                binding.btnCreateRoom.setEnabled(true);
                binding.progressAddRoom.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Kiểm tra đăng nhập, quyền chủ trọ, xác minh và tài khoản bản nháp.
     */
    private boolean checkCreatePermission() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openScreen(new LoginFragment());
            return false;
        }

        if (!session.hasRole(UserRole.HOST)) {
            showFormError("Tài khoản hiện tại chưa có quyền chủ trọ.");
            return false;
        }

        if (draftOwnerId == null) {
            draftOwnerId = session.getUserId();
        } else if (!draftOwnerId.equals(session.getUserId())) {
            showFormError(
                    "Tài khoản đã thay đổi. Hãy quay lại và mở biểu mẫu mới."
            );
            return false;
        }

        if (!session.isEmailVerified()) {
            openScreen(VerifyEmailFragment.newInstance(session.getEmail()));
            return false;
        }

        return true;
    }

    /**
     * Kiểm tra dữ liệu và đưa focus đến ô nhập sai đầu tiên.
     */
    private boolean validateForm() {
        EditText firstInvalid = null;
        boolean valid = true;

        if (!readText(binding.edtRoomCode)
                .matches("[A-Z0-9_-]{1,30}")) {
            binding.inputRoomCode.setError(
                    "Dùng 1–30 ký tự: chữ hoa, số, dấu _ hoặc -."
            );
            firstInvalid = binding.edtRoomCode;
            valid = false;
        }

        if (readText(binding.edtRoomName).isEmpty()) {
            binding.inputRoomName.setError("Vui lòng nhập tên phòng.");
            if (firstInvalid == null) {
                firstInvalid = binding.edtRoomName;
            }
            valid = false;
        }

        String address = readText(binding.edtRoomAddress);
        int addressLength = address.codePointCount(0, address.length());

        if (addressLength < 5 || addressLength > 300) {
            binding.inputRoomAddress.setError(
                    "Địa chỉ phải có từ 5 đến 300 ký tự."
            );
            if (firstInvalid == null) {
                firstInvalid = binding.edtRoomAddress;
            }
            valid = false;
        }

        if (!isValidArea(readText(binding.edtRoomArea))) {
            binding.inputRoomArea.setError(
                    "Diện tích phải từ 2 đến 1.000 m²."
            );
            if (firstInvalid == null) {
                firstInvalid = binding.edtRoomArea;
            }
            valid = false;
        }

        if (!readText(binding.edtRoomMaxOccupants)
                .matches("(?:[1-9]|10)")) {
            binding.inputRoomMaxOccupants.setError(
                    "Nhập số người tối đa từ 1 đến 10."
            );
            if (firstInvalid == null) {
                firstInvalid = binding.edtRoomMaxOccupants;
            }
            valid = false;
        }

        if (selectedRoomType == null) {
            showFormError("Vui lòng chọn loại phòng.");
            valid = false;
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocus();
        } else if (!valid) {
            binding.scrollAddRoom.smoothScrollTo(
                    0, binding.btnChooseRoomType.getTop()
            );
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
                ? "" : input.getText().toString().trim();
    }

    /**
     * Xóa lỗi cũ trước lần kiểm tra mới.
     */
    private void clearErrors() {
        binding.inputRoomCode.setError(null);
        binding.inputRoomName.setError(null);
        binding.inputRoomAddress.setError(null);
        binding.inputRoomArea.setError(null);
        binding.inputRoomMaxOccupants.setError(null);
        binding.inputRoomDescription.setError(null);
        binding.tvAddRoomError.setText("");
        binding.tvAddRoomError.setVisibility(View.GONE);
    }

    /**
     * Hiển thị lỗi chung của biểu mẫu.
     */
    private void showFormError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvAddRoomError.setText(
                message == null ? "Không thể tạo phòng." : message
        );
        binding.tvAddRoomError.setVisibility(View.VISIBLE);
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
            int ime = insets.getInsets(
                    WindowInsetsCompat.Type.ime()
            ).bottom;
            int bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            ).bottom;

            view.setPadding(
                    left, top, right, bottom + Math.max(0, ime - bars)
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
                requireActivity().getWindow(), binding.getRoot()
        ).hide(WindowInsetsCompat.Type.ime());

        View focused = binding.getRoot().findFocus();
        if (focused != null) {
            focused.clearFocus();
        }

        binding.getRoot().requestFocus();
    }

    /**
     * Mở màn hình xác thực và giữ biểu mẫu trong back stack.
     */
    private void openScreen(Fragment fragment) {
        captureDraft();
        hideKeyboard();

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Lưu bản nháp, đóng hộp thoại và giải phóng giao diện.
     */
    @Override
    public void onDestroyView() {
        captureDraft();

        if (optionsDialog != null) {
            optionsDialog.dismiss();
            optionsDialog = null;
        }

        if (binding != null) {
            binding.btnAddRoomBack.setOnClickListener(null);
            binding.btnCreateRoom.setOnClickListener(null);
            binding.btnSelectRoomImage.setOnClickListener(null);
            binding.btnChooseRoomType.setOnClickListener(null);
            binding.btnChooseRoomAmenities.setOnClickListener(null);
            binding.edtRoomDescription.setOnEditorActionListener(null);
            binding.imgAddRoomPreview.setImageDrawable(null);
            ViewCompat.setOnApplyWindowInsetsListener(
                    binding.getRoot(), null
            );
        }

        binding = null;
        super.onDestroyView();
    }
}