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

import com.example.roomly.R;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostAddRoomBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;

import java.math.BigDecimal;

/**
 * Điều khiển biểu mẫu thêm phòng của chủ trọ.
 * Phòng được lưu vào repository mẫu trong bộ nhớ.
 * Ảnh hiện mới dùng để xem trước, chưa được lưu vào dữ liệu phòng.
 */
public class HostAddRoomFragment extends Fragment {

    private static final String STATE_IMAGE_URI =
            "selected_room_image_uri";

    private FragmentHostAddRoomBinding binding;

    // Đường dẫn ảnh đang chọn trên thiết bị.
    private Uri selectedImageUri;

    // Đăng ký trình chọn ảnh trước khi Fragment được khởi tạo.
    private final ActivityResultLauncher<String[]> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::handleSelectedImage
            );

    /**
     * Khôi phục đường dẫn ảnh khi Android tạo lại Fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String savedUri = savedInstanceState.getString(
                    STATE_IMAGE_URI
            );

            if (savedUri != null) {
                selectedImageUri = Uri.parse(savedUri);
            }
        }
    }

    /**
     * Tạo giao diện từ fragment_host_add_room.xml.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHostAddRoomBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký các nút, trình chọn ảnh và thao tác đóng bàn phím.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupKeyboardInsets();

        binding.btnSelectRoomImage.setOnClickListener(clickedView -> {
            hideKeyboard();
            imagePicker.launch(new String[]{"image/*"});
        });

        displaySelectedImage();

        binding.btnAddRoomBack.setOnClickListener(clickedView -> {
            hideKeyboard();
            getParentFragmentManager().popBackStack();
        });

        binding.btnCreateRoom.setOnClickListener(
                clickedView -> handleCreateRoom()
        );

        // Cho phép nhập nhiều dòng và dùng Done để đóng bàn phím.
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
     * Lưu đường dẫn ảnh để giữ lựa chọn khi xoay màn hình.
     * Các ô nhập có ID được Android lưu trạng thái giao diện.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (selectedImageUri != null) {
            outState.putString(
                    STATE_IMAGE_URI,
                    selectedImageUri.toString()
            );
        }
    }

    /**
     * Nhận ảnh đã chọn và giữ quyền đọc ảnh trên thiết bị.
     * Nếu người dùng hủy chọn, giữ nguyên ảnh trước đó.
     */
    private void handleSelectedImage(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }

        try {
            requireContext().getContentResolver()
                    .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

            selectedImageUri = uri;
            displaySelectedImage();

        } catch (SecurityException exception) {
            Toast.makeText(
                    requireContext(),
                    "Không thể giữ quyền đọc ảnh này. "
                            + "Bạn chọn ảnh khác nhé.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Hiển thị ảnh xem trước hoặc hướng dẫn khi chưa chọn ảnh.
     * Nếu ảnh không còn truy cập được, cho phép chọn lại.
     */
    private void displaySelectedImage() {
        if (binding == null) {
            return;
        }

        boolean hasImage = selectedImageUri != null;

        try {
            binding.imgAddRoomPreview.setImageURI(selectedImageUri);

            if (hasImage
                    && binding.imgAddRoomPreview.getDrawable() == null) {
                selectedImageUri = null;
                hasImage = false;

                Toast.makeText(
                        requireContext(),
                        "Không đọc được ảnh. Bạn chọn lại ảnh nhé.",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (SecurityException exception) {
            selectedImageUri = null;
            hasImage = false;
            binding.imgAddRoomPreview.setImageDrawable(null);

            Toast.makeText(
                    requireContext(),
                    "Không còn quyền đọc ảnh. Bạn chọn lại ảnh nhé.",
                    Toast.LENGTH_SHORT
            ).show();
        }

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
     * Thêm khoảng trống khi bàn phím mở.
     * Trừ phần thanh hệ thống đã được MainActivity xử lý.
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

    /**
     * Kiểm tra quyền và biểu mẫu rồi lưu phòng vào repository mẫu.
     * Khi lưu thành công, quay về màn hình Phòng của tôi.
     */
    private void handleCreateRoom() {
        if (binding == null || !binding.btnCreateRoom.isEnabled()) {
            return;
        }

        clearErrors();

        if (!checkCreatePermission()) {
            return;
        }

        if (!validateForm()) {
            return;
        }

        hideKeyboard();

        // Chặn bấm liên tiếp trong lúc xử lý.
        binding.btnCreateRoom.setEnabled(false);

        try {
            String unitCode = readText(binding.edtRoomCode);
            String name = readText(binding.edtRoomName);
            String address = readText(binding.edtRoomAddress);
            String description = readText(binding.edtRoomDescription);

            String areaText = readText(binding.edtRoomArea)
                    .replace(',', '.');

            BigDecimal area = new BigDecimal(areaText);

            // Lưu thông tin phòng cùng đường dẫn ảnh đã chọn.
            DemoHostRoomRepository.getInstance().createRoom(
                    unitCode,
                    name,
                    address,
                    area,
                    description,
                    selectedImageUri == null
                            ? null
                            : selectedImageUri.toString()
            );

            Toast.makeText(
                    requireContext(),
                    "Đã tạo phòng mẫu.",
                    Toast.LENGTH_SHORT
            ).show();

            getParentFragmentManager().popBackStack();

        } catch (IllegalArgumentException | IllegalStateException exception) {
            // Giữ nội dung biểu mẫu để người dùng sửa và thử lại.
            showFormError(exception.getMessage());

        } finally {
            if (binding != null) {
                binding.btnCreateRoom.setEnabled(true);
            }
        }
    }

    /**
     * Yêu cầu đăng nhập, quyền HOST và email đã xác minh.
     * Kiểm tra lại khi bấm nút vì phiên có thể đã thay đổi.
     */
    private boolean checkCreatePermission() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openScreen(new LoginFragment());
            return false;
        }

        if (!session.hasRole(UserRole.HOST)) {
            showFormError(
                    "Tài khoản hiện tại chưa có quyền chủ trọ."
            );
            return false;
        }

        if (!session.isEmailVerified()) {
            openScreen(
                    VerifyEmailFragment.newInstance(session.getEmail())
            );
            return false;
        }

        return true;
    }

    /**
     * Kiểm tra các trường thông tin cơ bản của biểu mẫu.
     * Giới hạn chi tiết sẽ được đồng bộ với API khi tích hợp.
     */
    private boolean validateForm() {
        boolean valid = true;
        EditText firstInvalidField = null;

        if (readText(binding.edtRoomCode).isEmpty()) {
            binding.inputRoomCode.setError(
                    "Vui lòng nhập mã phòng."
            );

            firstInvalidField = binding.edtRoomCode;
            valid = false;
        }

        if (readText(binding.edtRoomName).isEmpty()) {
            binding.inputRoomName.setError(
                    "Vui lòng nhập tên phòng."
            );

            if (firstInvalidField == null) {
                firstInvalidField = binding.edtRoomName;
            }

            valid = false;
        }

        if (readText(binding.edtRoomAddress).isEmpty()) {
            binding.inputRoomAddress.setError(
                    "Vui lòng nhập địa chỉ."
            );

            if (firstInvalidField == null) {
                firstInvalidField = binding.edtRoomAddress;
            }

            valid = false;
        }

        if (!isPositiveArea(readText(binding.edtRoomArea))) {
            binding.inputRoomArea.setError(
                    "Nhập diện tích là số lớn hơn 0."
            );

            if (firstInvalidField == null) {
                firstInvalidField = binding.edtRoomArea;
            }

            valid = false;
        }

        if (firstInvalidField != null) {
            firstInvalidField.requestFocus();
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
     * Kiểm tra diện tích là số thập phân dương.
     * Chấp nhận dấu phẩy hoặc dấu chấm làm dấu thập phân.
     */
    private boolean isPositiveArea(String text) {
        String normalized = text.replace(',', '.');

        if (!normalized.matches("[0-9]+(\\.[0-9]+)?")) {
            return false;
        }

        try {
            return new BigDecimal(normalized)
                    .compareTo(BigDecimal.ZERO) > 0;

        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * Xóa các thông báo lỗi cũ trước lần kiểm tra mới.
     */
    private void clearErrors() {
        binding.inputRoomCode.setError(null);
        binding.inputRoomName.setError(null);
        binding.inputRoomAddress.setError(null);
        binding.inputRoomArea.setError(null);
        binding.inputRoomDescription.setError(null);

        binding.tvAddRoomError.setText("");
        binding.tvAddRoomError.setVisibility(View.GONE);
    }

    /**
     * Hiển thị thông báo lỗi chung bên dưới biểu mẫu.
     */
    private void showFormError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvAddRoomError.setText(
                message == null
                        ? "Không thể tạo phòng mẫu. Bạn thử lại nhé."
                        : message
        );

        binding.tvAddRoomError.setVisibility(View.VISIBLE);
    }

    /**
     * Đóng bàn phím và bỏ focus khỏi ô đang nhập.
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

        View focusedView = binding.getRoot().findFocus();

        if (focusedView != null) {
            focusedView.clearFocus();
        }

        binding.getRoot().requestFocus();
    }

    /**
     * Mở màn hình xác thực và giữ biểu mẫu trong back stack.
     */
    private void openScreen(Fragment fragment) {
        hideKeyboard();

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Gỡ listener, giải phóng ảnh xem trước và binding.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnAddRoomBack.setOnClickListener(null);
            binding.btnCreateRoom.setOnClickListener(null);
            binding.btnSelectRoomImage.setOnClickListener(null);

            binding.edtRoomDescription.setOnEditorActionListener(null);
            binding.imgAddRoomPreview.setImageDrawable(null);

            ViewCompat.setOnApplyWindowInsetsListener(
                    binding.getRoot(),
                    null
            );
        }

        binding = null;

        super.onDestroyView();
    }
}