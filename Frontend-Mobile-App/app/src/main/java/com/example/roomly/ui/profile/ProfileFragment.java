package com.example.roomly.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.repository.DemoProfileRepository;
import com.example.roomly.databinding.BottomSheetEditProfileBinding;
import com.example.roomly.databinding.FragmentProfileBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.example.roomly.R;
import com.example.roomly.ui.auth.LoginFragment;

/**
 * Hiển thị và chỉnh sửa hồ sơ bằng dữ liệu mẫu.
 */
public class ProfileFragment extends Fragment {

    // Binding của màn hình Cá nhân.
    private FragmentProfileBinding binding;

    // Bảng chỉnh sửa đang mở.
    private BottomSheetDialog editProfileDialog;

    /**
     * Tạo giao diện Cá nhân từ file XML.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentProfileBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Hiển thị hồ sơ và đăng ký thao tác chỉnh sửa thông tin.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        displayProfile();

        binding.btnEditProfile.setOnClickListener(
                clickedView -> showEditProfileDialog()
        );

        binding.btnOpenLogin.setOnClickListener(
                clickedView -> openLoginScreen()
        );
    }

    /**
     * Lấy thông tin trong repository và cập nhật lên giao diện.
     */
    private void displayProfile() {
        if (binding == null) {
            return;
        }

        DemoProfileRepository repository =
                DemoProfileRepository.getInstance();

        binding.tvProfileName.setText(repository.getFullName());
        binding.tvProfileEmail.setText(repository.getEmail());
        binding.tvProfilePhone.setText(repository.getPhone());
    }

    /**
     * Mở bảng chỉnh sửa với thông tin đang được lưu.
     * Nút Xong trên bàn phím sẽ đóng bàn phím của ô điện thoại.
     */
    private void showEditProfileDialog() {
        if (editProfileDialog != null) {
            return;
        }

        BottomSheetEditProfileBinding sheetBinding =
                BottomSheetEditProfileBinding.inflate(
                        getLayoutInflater()
                );

        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());

        editProfileDialog = dialog;
        dialog.setContentView(sheetBinding.getRoot());

        DemoProfileRepository repository =
                DemoProfileRepository.getInstance();

        sheetBinding.edtProfileName.setText(
                repository.getFullName()
        );

        sheetBinding.edtProfileEmail.setText(
                repository.getEmail()
        );

        sheetBinding.edtProfilePhone.setText(
                repository.getPhone()
        );

        // Email chỉ hiển thị, không cho chỉnh sửa.
        sheetBinding.edtProfileEmail.setKeyListener(null);

        sheetBinding.edtProfilePhone.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideProfileKeyboard(
                                dialog,
                                sheetBinding.edtProfilePhone
                        );

                        return true;
                    }

                    return false;
                }
        );

        sheetBinding.btnSaveProfile.setOnClickListener(
                view -> saveProfile(sheetBinding, dialog)
        );

        dialog.setOnDismissListener(dismissedDialog -> {
            if (editProfileDialog == dialog) {
                editProfileDialog = null;
            }
        });

        dialog.show();
    }

    /**
     * Kiểm tra họ tên và số điện thoại trước khi lưu hồ sơ mẫu.
     * Bản demo chấp nhận số điện thoại gồm 10 chữ số, bắt đầu bằng 0.
     */
    private void saveProfile(
            BottomSheetEditProfileBinding sheetBinding,
            BottomSheetDialog dialog
    ) {
        sheetBinding.layoutProfileName.setError(null);
        sheetBinding.layoutProfilePhone.setError(null);

        String fullName = "";

        if (sheetBinding.edtProfileName.getText() != null) {
            fullName = sheetBinding.edtProfileName
                    .getText()
                    .toString()
                    .trim()
                    .replaceAll("\\s+", " ");
        }

        String phone = "";

        if (sheetBinding.edtProfilePhone.getText() != null) {
            phone = sheetBinding.edtProfilePhone
                    .getText()
                    .toString()
                    .trim()
                    .replaceAll("\\s+", "");
        }

        boolean isValid = true;

        if (fullName.isEmpty()) {
            sheetBinding.layoutProfileName.setError(
                    "Bạn hãy nhập họ và tên."
            );
            isValid = false;
        }

        if (!phone.matches("0[0-9]{9}")) {
            sheetBinding.layoutProfilePhone.setError(
                    "Nhập 10 chữ số, bắt đầu bằng 0."
            );
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        DemoProfileRepository.getInstance().updateProfile(
                fullName,
                phone
        );

        displayProfile();
        hideProfileKeyboard(dialog, sheetBinding.getRoot());
        dialog.dismiss();

        Toast.makeText(
                requireContext(),
                "Đã cập nhật hồ sơ mẫu.",
                Toast.LENGTH_SHORT
        ).show();
    }

    /**
     * Bỏ focus khỏi ô nhập và đóng bàn phím của bảng chỉnh sửa.
     */
    private void hideProfileKeyboard(
            BottomSheetDialog dialog,
            View view
    ) {
        if (dialog.getWindow() == null) {
            return;
        }

        View focusedView = dialog.getCurrentFocus();

        if (focusedView != null) {
            focusedView.clearFocus();
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        dialog.getWindow(),
                        view
                );

        controller.hide(WindowInsetsCompat.Type.ime());
    }

    /**
     * Mở màn hình đăng nhập và giữ trang Cá nhân trong back stack.
     */
    private void openLoginScreen() {
        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new LoginFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Đóng bảng chỉnh sửa và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        if (editProfileDialog != null) {
            editProfileDialog.dismiss();
            editProfileDialog = null;
        }

        binding = null;

        super.onDestroyView();
    }
}