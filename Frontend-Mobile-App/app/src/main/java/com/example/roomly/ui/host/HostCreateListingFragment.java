package com.example.roomly.ui.host;

import android.net.Uri;
import android.os.Bundle;
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
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostListingRepository;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostCreateListingBinding;

/**
 * Tạo bản nháp bài đăng mẫu cho phòng thuộc chủ trọ hiện tại.
 * Bản nháp chưa được đăng công khai.
 */
public class HostCreateListingFragment extends Fragment {

    private static final String ARG_ROOM_ID = "listing_room_id";

    private FragmentHostCreateListingBinding binding;
    private String roomId;

    /**
     * Tạo biểu mẫu bài đăng với ID phòng được chọn.
     */
    public static HostCreateListingFragment newInstance(String roomId) {
        HostCreateListingFragment fragment =
                new HostCreateListingFragment();

        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Đọc ID phòng từ Bundle.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
    }

    /**
     * Tạo giao diện biểu mẫu bằng ViewBinding.
     */
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

        return binding.getRoot();
    }

    /**
     * Đăng ký thao tác, xử lý bàn phím và quan sát quyền truy cập.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupKeyboardInsets();

        binding.btnCreateListingBack.setOnClickListener(clickedView -> {
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
                        session -> displayRoom()
                );
    }

    /**
     * Kiểm tra quyền và hiển thị phòng được chọn.
     * Không điền lại các ô bài đăng để tránh ghi đè nội dung đang nhập.
     */
    private void displayRoom() {
        if (binding == null) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || room == null) {
            binding.layoutCreateListingForm.setVisibility(View.GONE);
            binding.imgListingRoom.setImageDrawable(null);

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
    }

    /**
     * Hiển thị ảnh của phòng được chọn.
     * Ẩn ảnh nếu đường dẫn không còn đọc được.
     */
    private void displayRoomImage(HostRoom room) {
        binding.imgListingRoom.setImageDrawable(null);
        binding.imgListingRoom.setVisibility(View.GONE);

        String imageUri = room.getImageUri();

        if (imageUri == null || imageUri.trim().isEmpty()) {
            return;
        }

        try {
            binding.imgListingRoom.setImageURI(
                    Uri.parse(imageUri)
            );

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

    /**
     * Kiểm tra quyền và biểu mẫu rồi lưu bản nháp mẫu.
     * Giữ nguyên nội dung nếu thao tác thất bại.
     */
    private void saveDraft() {
        if (binding == null || !binding.btnSaveListing.isEnabled()) {
            return;
        }

        clearErrors();

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
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

        hideKeyboard();
        binding.btnSaveListing.setEnabled(false);

        try {
            long monthlyRent = Long.parseLong(
                    readText(binding.edtListingPrice)
            );

            DemoHostListingRepository.getInstance().createDraft(
                    roomId,
                    readText(binding.edtListingTitle),
                    monthlyRent,
                    readText(binding.edtListingDescription)
            );

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
                binding.btnSaveListing.setEnabled(true);
            }
        }
    }

    /**
     * Kiểm tra tiêu đề, giá thuê và nội dung bài đăng.
     * Xử lý cả trường hợp số nhập vượt phạm vi kiểu long.
     */
    private boolean validateForm() {
        boolean valid = true;
        EditText firstInvalid = null;

        if (readText(binding.edtListingTitle).isEmpty()) {
            binding.inputListingTitle.setError(
                    "Vui lòng nhập tiêu đề bài đăng."
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

        if (readText(binding.edtListingDescription).isEmpty()) {
            binding.inputListingDescription.setError(
                    "Vui lòng nhập nội dung bài đăng."
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

    /**
     * Đọc nội dung ô nhập và loại bỏ khoảng trắng ở hai đầu.
     */
    private String readText(EditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().trim();
    }

    /**
     * Xóa các thông báo lỗi cũ trước khi kiểm tra lại.
     */
    private void clearErrors() {
        binding.inputListingTitle.setError(null);
        binding.inputListingPrice.setError(null);
        binding.inputListingDescription.setError(null);

        binding.tvCreateListingError.setText("");
        binding.tvCreateListingError.setVisibility(View.GONE);
    }

    /**
     * Hiển thị thông báo lỗi chung.
     */
    private void showError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvCreateListingError.setText(
                message == null
                        ? "Không thể lưu bản nháp."
                        : message
        );

        binding.tvCreateListingError.setVisibility(View.VISIBLE);
    }

    /**
     * Thêm khoảng trống khi bàn phím mở để biểu mẫu không bị che.
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
     * Đóng bàn phím và bỏ focus khỏi ô đang nhập.
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
     * Gỡ listener, giải phóng ảnh và binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnCreateListingBack.setOnClickListener(null);
            binding.btnSaveListing.setOnClickListener(null);
            binding.edtListingDescription.setOnEditorActionListener(null);
            binding.imgListingRoom.setImageDrawable(null);

            ViewCompat.setOnApplyWindowInsetsListener(
                    binding.getRoot(),
                    null
            );
        }

        binding = null;

        super.onDestroyView();
    }
}