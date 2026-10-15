package com.example.roomly.ui.auth;

import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.example.roomly.data.repository.AuthRepository;
import com.example.roomly.ui.NavigationHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

final class AuthDialogs {
    private AuthDialogs() {}

    static void resetPassword(Fragment screen, String email, Runnable finished) {
        LifecycleOwner owner = screen.getViewLifecycleOwner();
        new AuthRepository()
                .requestCode(
                        email,
                        true,
                        response -> {
                            if (!isCurrentView(screen, owner)) {
                                finished.run();
                                return;
                            }
                            showResetForm(screen, owner, email, response, finished);
                        },
                        error -> {
                            finished.run();
                            if (isCurrentView(screen, owner)) {
                                Toast.makeText(screen.requireContext(), error, Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
    }

    private static boolean isCurrentView(Fragment screen, LifecycleOwner owner) {
        return screen.isAdded()
                && screen.getViewLifecycleOwnerLiveData().getValue() == owner
                && owner.getLifecycle().getCurrentState() != Lifecycle.State.DESTROYED;
    }

    private static void showResetForm(
            Fragment screen,
            LifecycleOwner owner,
            String email,
            JSONObject response,
            Runnable finished) {
        LinearLayout form = new LinearLayout(screen.requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * screen.getResources().getDisplayMetrics().density);
        form.setPadding(padding, 0, padding, 0);
        EditText code = new EditText(screen.requireContext());
        code.setHint("Mã xác nhận 6 chữ số");
        code.setInputType(InputType.TYPE_CLASS_NUMBER);
        code.setFilters(new InputFilter[] {new InputFilter.LengthFilter(6)});
        EditText password = new EditText(screen.requireContext());
        password.setHint("Mật khẩu mới (ít nhất 12 ký tự)");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(code);
        form.addView(password);
        String message = response.optString("message", "Hãy kiểm tra hộp thư của bạn.");
        String demoCode = response.optString("demo_code", "");
        if (!demoCode.isEmpty()) message += "\nMã thử (email mô phỏng): " + demoCode;
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(screen.requireContext())
                        .setTitle("Đặt lại mật khẩu")
                        .setMessage(message)
                        .setView(form)
                        .setNegativeButton("Quay lại", null)
                        .setPositiveButton("Đổi mật khẩu", null)
                        .create();
        DefaultLifecycleObserver observer =
                new DefaultLifecycleObserver() {
                    @Override
                    public void onDestroy(@NonNull LifecycleOwner destroyed) {
                        dialog.dismiss();
                    }
                };
        owner.getLifecycle().addObserver(observer);
        dialog.setOnDismissListener(
                ignored -> {
                    owner.getLifecycle().removeObserver(observer);
                    finished.run();
                });
        dialog.setOnShowListener(
                ignored ->
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setOnClickListener(
                                        v -> {
                                            String enteredCode = code.getText().toString().trim();
                                            String newPassword = password.getText().toString();
                                            if (!enteredCode.matches("[0-9]{6}")) {
                                                code.setError("Nhập đủ 6 chữ số.");
                                                return;
                                            }
                                            if (newPassword.length() < 12
                                                    || newPassword.getBytes(StandardCharsets.UTF_8)
                                                                    .length
                                                            > 72) {
                                                password.setError(
                                                        "Mật khẩu cần ít nhất 12 ký tự, tối đa 72"
                                                            + " byte UTF-8.");
                                                return;
                                            }
                                            dialog.setCancelable(false);
                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                    .setEnabled(false);
                                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                                    .setEnabled(false);
                                            new AuthRepository()
                                                    .resetPassword(
                                                            email,
                                                            enteredCode,
                                                            newPassword,
                                                            () -> {
                                                                if (!isCurrentView(screen, owner))
                                                                    return;
                                                                NavigationHelper.whenResumed(
                                                                        screen,
                                                                        () -> {
                                                                            dialog.dismiss();
                                                                            Toast.makeText(
                                                                                            screen
                                                                                                    .requireContext(),
                                                                                            "Đã đổi"
                                                                                                + " mật khẩu."
                                                                                                + " Hãy đăng"
                                                                                                + " nhập"
                                                                                                + " lại.",
                                                                                            Toast
                                                                                                    .LENGTH_LONG)
                                                                                    .show();
                                                                            screen.getParentFragmentManager()
                                                                                    .popBackStack();
                                                                        });
                                                            },
                                                            error -> {
                                                                if (!isCurrentView(screen, owner))
                                                                    return;
                                                                dialog.setCancelable(true);
                                                                dialog.getButton(
                                                                                AlertDialog
                                                                                        .BUTTON_POSITIVE)
                                                                        .setEnabled(true);
                                                                dialog.getButton(
                                                                                AlertDialog
                                                                                        .BUTTON_NEGATIVE)
                                                                        .setEnabled(true);
                                                                code.setError(error);
                                                            });
                                        }));
        dialog.show();
    }
}
