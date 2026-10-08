package com.example.roomly.ui.profile;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.DebugSessionHelper;
import com.example.roomly.data.repository.DemoProfileRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.BottomSheetEditProfileBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Biểu mẫu chỉnh sửa hồ sơ.
 * Khôi phục nội dung đang nhập khi Android tạo lại hộp thoại.
 */
public class EditProfileDialogFragment
        extends BottomSheetDialogFragment {

    public static final String TAG = "edit_profile_dialog";

    private static final String ARG_OWNER_ID = "profile_owner_id";
    private static final String STATE_NAME = "profile_draft_name";
    private static final String STATE_PHONE = "profile_draft_phone";

    private BottomSheetEditProfileBinding binding;
    private String ownerId;

    /** Tạo biểu mẫu gắn với tài khoản mở nó. */
    public static EditProfileDialogFragment newInstance(String userId) {
        EditProfileDialogFragment fragment =
                new EditProfileDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARG_OWNER_ID, userId);
        fragment.setArguments(args);

        return fragment;
    }

    /** Đọc ID tài khoản cần chỉnh sửa. */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            ownerId = getArguments().getString(ARG_OWNER_ID);
        }
    }

    /** Tạo bảng chỉnh sửa bằng layout đã có trong dự án. */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext(), getTheme());

        binding = BottomSheetEditProfileBinding.inflate(
                getLayoutInflater()
        );

        dialog.setContentView(binding.getRoot());

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        String name = session.getFullName();
        String phone = DemoProfileRepository.getInstance().getPhone();

        if (savedInstanceState != null) {
            name = savedInstanceState.getString(STATE_NAME, name);
            phone = savedInstanceState.getString(STATE_PHONE, phone);
        }

        binding.edtProfileName.setText(name);
        binding.edtProfileEmail.setText(session.getEmail());
        binding.edtProfilePhone.setText(phone);

        binding.edtProfileEmail.setKeyListener(null);
        binding.edtProfileEmail.setFocusable(false);
        binding.edtProfileEmail.setCursorVisible(false);

        binding.btnSaveProfile.setText(
                DebugSessionHelper.isDemoSession()
                        ? "Lưu thay đổi mẫu"
                        : "Lưu thay đổi"
        );

        binding.btnSaveProfile.setOnClickListener(
                view -> saveProfile()
        );

        binding.edtProfilePhone.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideKeyboard();
                        return true;
                    }
                    return false;
                }
        );

        return dialog;
    }

    /** Điều chỉnh bàn phím và đóng biểu mẫu nếu tài khoản thay đổi. */
    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        Window window = dialog == null ? null : dialog.getWindow();

        if (window != null) {
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }

        SessionRepository.getInstance()
                .getSessionState()
                .observe(this, session -> {
                    if (binding != null && !canEdit(session)) {
                        hideKeyboard();
                        dismissAllowingStateLoss();
                    }
                });
    }

    /** Chỉ cho chỉnh sửa khi vẫn là tài khoản đã mở biểu mẫu. */
    private boolean canEdit(@Nullable SessionState session) {
        return session != null
                && session.isLoggedIn()
                && session.isEmailVerified()
                && ownerId != null
                && ownerId.equals(session.getUserId());
    }

    /** Kiểm tra dữ liệu rồi lưu hồ sơ mẫu của đúng tài khoản. */
    private void saveProfile() {
        if (binding == null
                || !binding.btnSaveProfile.isEnabled()) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!canEdit(session)) {
            showMessage("Phiên đã thay đổi. Hãy mở lại biểu mẫu.");
            dismissAllowingStateLoss();
            return;
        }

        binding.layoutProfileName.setError(null);
        binding.layoutProfilePhone.setError(null);

        String name = readText(binding.edtProfileName);
        String phone = normalizePhone(
                readText(binding.edtProfilePhone)
        );

        EditText firstInvalid = null;

        if (name.isEmpty()) {
            binding.layoutProfileName.setError(
                    "Vui lòng nhập họ và tên."
            );
            firstInvalid = binding.edtProfileName;
        } else if (name.codePointCount(0, name.length()) > 100) {
            binding.layoutProfileName.setError(
                    "Họ tên tối đa 100 ký tự."
            );
            firstInvalid = binding.edtProfileName;
        }

        // Kiểm tra cơ bản cho giao diện; sẽ đối chiếu lại với API.
        // Cho phép để trống nếu người dùng chưa cung cấp số điện thoại.
        if (!phone.isEmpty()
                && !phone.matches("\\+?[0-9]{8,15}")) {
            binding.layoutProfilePhone.setError(
                    "Nhập 8–15 chữ số, có thể bắt đầu bằng dấu +."
            );

            if (firstInvalid == null) {
                firstInvalid = binding.edtProfilePhone;
            }
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocus();
            return;
        }

        binding.btnSaveProfile.setEnabled(false);

        try {
            DemoProfileRepository.getInstance().updateProfile(
                    ownerId,
                    name,
                    phone
            );

            hideKeyboard();
            showMessage(
                    "Đã cập nhật hồ sơ mẫu, chưa gửi đến server."
            );
            dismiss();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        } finally {
            if (binding != null) {
                binding.btnSaveProfile.setEnabled(true);
            }
        }
    }

    /** Đọc nội dung và bỏ khoảng trắng ở hai đầu. */
    private String readText(EditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().trim();
    }

    /** Bỏ dấu phân cách thông dụng trong số điện thoại. */
    private String normalizePhone(String phone) {
        return phone.replaceAll("[\\s()\\-]", "");
    }

    /** Đóng bàn phím bên trong hộp thoại. */
    private void hideKeyboard() {
        Dialog dialog = getDialog();

        if (binding == null
                || dialog == null
                || dialog.getWindow() == null) {
            return;
        }

        new WindowInsetsControllerCompat(
                dialog.getWindow(),
                binding.getRoot()
        ).hide(WindowInsetsCompat.Type.ime());

        View focused = binding.getRoot().findFocus();
        if (focused != null) {
            focused.clearFocus();
        }
    }

    /** Hiển thị lỗi mà vẫn giữ nội dung người dùng đang nhập. */
    private void showMessage(@Nullable String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    message == null
                            ? "Không thể cập nhật hồ sơ."
                            : message,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /** Lưu bản nháp khi Android tạo lại hộp thoại. */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (binding != null) {
            outState.putString(
                    STATE_NAME,
                    readText(binding.edtProfileName)
            );
            outState.putString(
                    STATE_PHONE,
                    readText(binding.edtProfilePhone)
            );
        }

        super.onSaveInstanceState(outState);
    }

    /** Gỡ các listener và giải phóng binding của biểu mẫu. */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnSaveProfile.setOnClickListener(null);
            binding.edtProfilePhone.setOnEditorActionListener(null);
        }

        binding = null;
        super.onDestroyView();
    }
}