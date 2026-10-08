package com.example.roomly.ui.admin;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.roomly.R;
import com.example.roomly.data.model.AdminAccount;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoAdminAccountRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentAdminAccountsBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Hiển thị, tìm kiếm và khóa tài khoản trong dữ liệu quản trị mẫu.
 * Mọi thao tác kiểm tra lại quyền admin tại thời điểm thực hiện.
 */
public class AdminAccountsFragment extends Fragment {

    private static final String STATE_FILTER = "accounts_filter";

    private FragmentAdminAccountsBinding binding;
    private TextWatcher searchWatcher;
    private AlertDialog suspensionDialog;

    private int selectedFilterId = R.id.chip_admin_accounts_all;

    /** Khôi phục bộ lọc khi Android tạo lại Fragment. */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            selectedFilterId = savedInstanceState.getInt(
                    STATE_FILTER,
                    R.id.chip_admin_accounts_all
            );
        }
    }

    /** Tạo giao diện quản lý tài khoản bằng ViewBinding. */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminAccountsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký nút quay lại, tìm kiếm, bộ lọc
     * và quan sát quyền truy cập theo vòng đời giao diện.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnAdminAccountsBack.setOnClickListener(
                clickedView -> {
                    hideKeyboard();
                    getParentFragmentManager().popBackStack();
                }
        );

        setupSearch();

        binding.chipGroupAdminAccounts.check(selectedFilterId);

        binding.chipGroupAdminAccounts.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    if (!checkedIds.isEmpty()) {
                        selectedFilterId = checkedIds.get(0);
                        displayAccounts();
                    }
                }
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayAccounts()
                );
    }

    /** Áp dụng tìm kiếm sau khi Android khôi phục nội dung ô nhập. */
    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        binding.chipGroupAdminAccounts.check(selectedFilterId);
        displayAccounts();
    }

    /** Cập nhật danh sách khi màn hình hoạt động trở lại. */
    @Override
    public void onResume() {
        super.onResume();

        displayAccounts();
    }

    /** Lưu bộ lọc hiện tại khi Android tạo lại màn hình. */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_FILTER, selectedFilterId);

        super.onSaveInstanceState(outState);
    }

    /**
     * Lọc tài khoản ngay khi nhập và đóng bàn phím
     * khi người dùng bấm Search.
     */
    private void setupSearch() {
        searchWatcher = new TextWatcher() {

            /** Không cần xử lý trước khi nội dung thay đổi. */
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after
            ) {
            }

            /** Cập nhật danh sách theo từ khóa mới. */
            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count
            ) {
                displayAccounts();
            }

            /** Việc tìm kiếm đã được xử lý trong onTextChanged. */
            @Override
            public void afterTextChanged(Editable text) {
            }
        };

        binding.edtAdminAccountsSearch.addTextChangedListener(
                searchWatcher
        );

        binding.edtAdminAccountsSearch.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        displayAccounts();
                        hideKeyboard();
                        return true;
                    }

                    return false;
                }
        );
    }

    /**
     * Kiểm tra quyền trước khi đọc dữ liệu.
     * Xóa các thẻ và đóng hộp thoại nếu phiên mất quyền admin.
     */
    private void displayAccounts() {
        if (binding == null) {
            return;
        }

        binding.layoutAdminAccountsList.removeAllViews();

        if (!hasAdminAccess()) {
            closeSuspensionDialog();

            binding.layoutAdminAccountsContent.setVisibility(
                    View.GONE
            );
            binding.tvAdminAccountsAccessError.setVisibility(
                    View.VISIBLE
            );
            binding.tvAdminAccountsCount.setText("");
            return;
        }

        binding.layoutAdminAccountsContent.setVisibility(
                View.VISIBLE
        );
        binding.tvAdminAccountsAccessError.setVisibility(
                View.GONE
        );

        String keyword = binding.edtAdminAccountsSearch
                .getText() == null
                ? ""
                : binding.edtAdminAccountsSearch
                .getText()
                .toString();

        List<AdminAccount> accounts =
                DemoAdminAccountRepository.getInstance()
                        .getAccounts(getSelectedStatus(), keyword);

        binding.tvAdminAccountsCount.setText(
                "Tìm thấy " + accounts.size() + " tài khoản"
        );

        binding.tvAdminAccountsEmpty.setVisibility(
                accounts.isEmpty() ? View.VISIBLE : View.GONE
        );

        for (AdminAccount account : accounts) {
            addAccountCard(account);
        }
    }

    /** Chuyển chip đang chọn thành trạng thái cần lọc. */
    @Nullable
    private AdminAccount.Status getSelectedStatus() {
        if (selectedFilterId == R.id.chip_admin_accounts_active) {
            return AdminAccount.Status.ACTIVE;
        }

        if (selectedFilterId == R.id.chip_admin_accounts_suspended) {
            return AdminAccount.Status.SUSPENDED;
        }

        return null;
    }

    /**
     * Tạo thẻ hiển thị thông tin tài khoản.
     * Chỉ hiện nút khóa với tài khoản đang hoạt động được phép xử lý.
     */
    private void addAccountCard(AdminAccount account) {
        MaterialCardView card =
                new MaterialCardView(requireContext());

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(0);
        card.setCardBackgroundColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.roomly_surface
                )
        );
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.roomly_border
                )
        );

        LinearLayout content =
                new LinearLayout(requireContext());

        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        card.addView(
                content,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        addText(
                content,
                account.getStatusLabel(),
                13,
                R.color.roomly_primary,
                true,
                0
        );

        addText(
                content,
                account.getFullName(),
                18,
                R.color.roomly_text_primary,
                true,
                8
        );

        addText(
                content,
                account.getEmail(),
                14,
                R.color.roomly_text_secondary,
                false,
                6
        );

        addText(
                content,
                account.getRolesLabel(),
                14,
                R.color.roomly_text_secondary,
                false,
                8
        );

        addText(
                content,
                account.isEmailVerified()
                        ? "Email đã xác minh"
                        : "Email chưa xác minh",
                13,
                R.color.roomly_text_secondary,
                false,
                6
        );

        if (account.getStatus() == AdminAccount.Status.SUSPENDED) {
            addText(
                    content,
                    "Lý do khóa: " + account.getSuspensionReason(),
                    14,
                    R.color.roomly_text_primary,
                    false,
                    12
            );
        } else if (canSuspendAccount(account)) {
            MaterialButton button =
                    new MaterialButton(requireContext());

            LinearLayout.LayoutParams buttonParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            buttonParams.topMargin = dp(12);
            button.setLayoutParams(buttonParams);
            button.setText("Khóa tài khoản");
            button.setAllCaps(false);
            button.setMinHeight(dp(48));
            button.setCornerRadius(dp(12));
            button.setTextColor(
                    ContextCompat.getColor(
                            requireContext(),
                            R.color.roomly_surface
                    )
            );
            button.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(
                                    requireContext(),
                                    R.color.roomly_primary
                            )
                    )
            );

            button.setOnClickListener(
                    clickedView -> showSuspensionDialog(
                            account.getId()
                    )
            );

            content.addView(button);
        }

        binding.layoutAdminAccountsList.addView(card);
    }

    /** Thêm một dòng thông tin có màu, cỡ chữ và khoảng cách thống nhất. */
    private void addText(
            LinearLayout parent,
            String text,
            int textSize,
            int colorResId,
            boolean bold,
            int marginTop
    ) {
        TextView textView = new TextView(requireContext());

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.topMargin = dp(marginTop);

        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(
                ContextCompat.getColor(requireContext(), colorResId)
        );
        textView.setTypeface(
                textView.getTypeface(),
                bold
                        ? android.graphics.Typeface.BOLD
                        : android.graphics.Typeface.NORMAL
        );

        parent.addView(textView);
    }

    /**
     * Đọc lại tài khoản và yêu cầu nhập lý do trước khi khóa.
     * Hộp thoại giữ nguyên khi lý do chưa hợp lệ.
     */
    private void showSuspensionDialog(String accountId) {
        if (binding == null || suspensionDialog != null) {
            return;
        }

        AdminAccount account =
                DemoAdminAccountRepository.getInstance()
                        .getAccountById(accountId);

        if (account == null || !canSuspendAccount(account)) {
            displayAccounts();
            return;
        }

        hideKeyboard();

        EditText reasonInput = new EditText(requireContext());
        reasonInput.setHint("Nhập lý do khóa tài khoản");
        reasonInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        reasonInput.setMinLines(3);
        reasonInput.setMaxLines(5);

        LinearLayout inputContainer =
                new LinearLayout(requireContext());

        inputContainer.setOrientation(LinearLayout.VERTICAL);
        inputContainer.setPadding(
                dp(20), dp(8), dp(20), dp(8)
        );

        inputContainer.addView(
                reasonInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Khóa tài khoản?")
                        .setMessage(
                                account.getFullName()
                                        + "\n"
                                        + account.getEmail()
                                        + "\n\nThao tác chỉ cập nhật "
                                        + "dữ liệu mẫu trên thiết bị."
                        )
                        .setView(inputContainer)
                        .setNegativeButton("Quay lại", null)
                        .setPositiveButton("Khóa tài khoản", null)
                        .create();

        suspensionDialog = dialog;

        dialog.setOnDismissListener(dismissedDialog -> {
            if (suspensionDialog == dialog) {
                suspensionDialog = null;
            }
        });

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(clickedView -> {
                    String reason = reasonInput.getText()
                            .toString()
                            .trim();

                    if (reason.isEmpty()) {
                        reasonInput.setError(
                                "Bạn cần nhập lý do khóa."
                        );
                        reasonInput.requestFocus();
                        return;
                    }

                    try {
                        DemoAdminAccountRepository.getInstance()
                                .suspendAccount(accountId, reason);

                        dialog.dismiss();
                        displayAccounts();

                        Toast.makeText(
                                requireContext(),
                                "Đã khóa tài khoản mẫu.",
                                Toast.LENGTH_SHORT
                        ).show();
                    } catch (
                            IllegalArgumentException
                            | IllegalStateException exception
                    ) {
                        dialog.dismiss();
                        displayAccounts();

                        Toast.makeText(
                                requireContext(),
                                exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    /** Kiểm tra quyền admin từ phiên hiện tại. */
    private boolean hasAdminAccess() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        return session.isLoggedIn()
                && session.hasRole(UserRole.ADMIN);
    }

    /** Kiểm tra điều kiện hiện nút khóa tài khoản mẫu. */
    private boolean canSuspendAccount(AdminAccount account) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        return hasAdminAccess()
                && account.getStatus() == AdminAccount.Status.ACTIVE
                && !account.hasRole(UserRole.ADMIN)
                && !account.getId().equals(session.getUserId());
    }

    /** Đóng bàn phím tìm kiếm trước khi mở hộp thoại hoặc quay lại. */
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
        binding.edtAdminAccountsSearch.clearFocus();
    }

    /** Chuyển kích thước dp thành pixel theo mật độ màn hình. */
    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    /** Đóng hộp thoại khi mất quyền hoặc giao diện bị hủy. */
    private void closeSuspensionDialog() {
        AlertDialog dialog = suspensionDialog;
        suspensionDialog = null;

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /** Gỡ các listener, đóng hộp thoại và giải phóng binding. */
    @Override
    public void onDestroyView() {
        closeSuspensionDialog();

        if (binding != null) {
            if (searchWatcher != null) {
                binding.edtAdminAccountsSearch
                        .removeTextChangedListener(searchWatcher);
            }

            binding.edtAdminAccountsSearch
                    .setOnEditorActionListener(null);

            binding.chipGroupAdminAccounts
                    .setOnCheckedStateChangeListener(null);

            binding.btnAdminAccountsBack
                    .setOnClickListener(null);

            binding.layoutAdminAccountsList.removeAllViews();
        }

        searchWatcher = null;
        binding = null;

        super.onDestroyView();
    }
}