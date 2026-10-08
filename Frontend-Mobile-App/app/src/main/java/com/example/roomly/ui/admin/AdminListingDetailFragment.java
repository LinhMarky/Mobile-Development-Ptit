package com.example.roomly.ui.admin;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.model.AdminListing;
import com.example.roomly.data.repository.DemoAdminListingRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentAdminListingDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Hiển thị chi tiết và xử lý kiểm duyệt trên bài đăng mẫu.
 */
public class AdminListingDetailFragment extends Fragment {

    private static final String ARG_LISTING_ID = "admin_listing_id";

    private FragmentAdminListingDetailBinding binding;
    private String listingId;
    private AlertDialog moderationDialog;

    private enum Action {
        APPROVE,
        REJECT,
        HIDE,
        RESTORE
    }

    /**
     * Tạo màn hình chi tiết với mã bài đăng cần xem.
     */
    public static AdminListingDetailFragment newInstance(String listingId) {
        AdminListingDetailFragment fragment =
                new AdminListingDetailFragment();

        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Đọc mã bài đăng từ Bundle.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            listingId = args.getString(ARG_LISTING_ID);
        }
    }

    /**
     * Tạo giao diện chi tiết kiểm duyệt bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminListingDetailBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký thao tác và cập nhật nội dung theo phiên hiện tại.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnAdminDetailBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack()
        );

        binding.btnAdminDetailApprove.setOnClickListener(
                clickedView -> showModerationDialog(Action.APPROVE)
        );

        binding.btnAdminDetailReject.setOnClickListener(
                clickedView -> showModerationDialog(Action.REJECT)
        );

        binding.btnAdminDetailHide.setOnClickListener(
                clickedView -> showModerationDialog(Action.HIDE)
        );

        binding.btnAdminDetailRestore.setOnClickListener(
                clickedView -> showModerationDialog(Action.RESTORE)
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayListing()
                );
    }

    /**
     * Đọc lại bài đăng khi màn hình hoạt động trở lại.
     */
    @Override
    public void onResume() {
        super.onResume();
        displayListing();
    }

    /**
     * Hiển thị thông tin và các nút phù hợp trạng thái bài đăng.
     * Repository chỉ trả dữ liệu cho tài khoản có quyền ADMIN.
     */
    private void displayListing() {
        if (binding == null) {
            return;
        }

        AdminListing listing = DemoAdminListingRepository.getInstance()
                .getListingById(listingId);

        if (listing == null) {
            closeModerationDialog();

            binding.layoutAdminDetailContent.setVisibility(View.GONE);
            binding.imgAdminDetail.setImageDrawable(null);
            binding.tvAdminDetailTitle.setText("");
            binding.tvAdminDetailPrice.setText("");
            binding.tvAdminDetailHost.setText("");
            binding.tvAdminDetailRoomCode.setText("");
            binding.tvAdminDetailAddress.setText("");
            binding.tvAdminDetailDescription.setText("");
            binding.tvAdminDetailReason.setText("");
            binding.tvAdminDetailStatus.setText("");

            showError(
                    "Không tìm thấy bài đăng hoặc tài khoản "
                            + "không có quyền quản trị."
            );
            return;
        }

        binding.layoutAdminDetailContent.setVisibility(View.VISIBLE);
        binding.tvAdminDetailError.setVisibility(View.GONE);

        binding.tvAdminDetailStatus.setText(listing.getStatusLabel());
        binding.tvAdminDetailTitle.setText(listing.getTitle());
        binding.tvAdminDetailPrice.setText(listing.getFormattedPrice());

        binding.tvAdminDetailHost.setText(
                "Chủ trọ: " + listing.getHostName()
        );

        binding.tvAdminDetailRoomCode.setText(
                "Mã phòng: " + listing.getUnitCode()
        );

        binding.tvAdminDetailAddress.setText(listing.getAddress());
        binding.tvAdminDetailDescription.setText(listing.getDescription());

        String reason = listing.getModerationReason().trim();

        binding.tvAdminDetailReason.setText(
                reason.isEmpty() ? "" : "Lý do kiểm duyệt:\n" + reason
        );

        binding.tvAdminDetailReason.setVisibility(
                reason.isEmpty() ? View.GONE : View.VISIBLE
        );

        boolean pending = listing.getStatus()
                == AdminListing.Status.PENDING;

        boolean published = listing.getStatus()
                == AdminListing.Status.PUBLISHED;

        boolean hidden = listing.getStatus()
                == AdminListing.Status.HIDDEN;

        binding.btnAdminDetailApprove.setVisibility(
                pending ? View.VISIBLE : View.GONE
        );

        binding.btnAdminDetailReject.setVisibility(
                pending ? View.VISIBLE : View.GONE
        );

        binding.btnAdminDetailHide.setVisibility(
                published ? View.VISIBLE : View.GONE
        );

        binding.btnAdminDetailRestore.setVisibility(
                hidden ? View.VISIBLE : View.GONE
        );

        displayImage(listing.getImageUri());
    }

    /**
     * Hiển thị ảnh URI cục bộ của dữ liệu mẫu hoặc nội dung thay thế.
     */
    private void displayImage(@Nullable String imageUri) {
        binding.imgAdminDetail.setImageDrawable(null);

        boolean hasImage = false;

        if (imageUri != null && !imageUri.trim().isEmpty()) {
            try {
                binding.imgAdminDetail.setImageURI(Uri.parse(imageUri));
                hasImage = binding.imgAdminDetail.getDrawable() != null;
            } catch (SecurityException | IllegalArgumentException exception) {
                binding.imgAdminDetail.setImageDrawable(null);
            }
        }

        binding.imgAdminDetail.setVisibility(
                hasImage ? View.VISIBLE : View.GONE
        );

        binding.tvAdminDetailImagePlaceholder.setVisibility(
                hasImage ? View.GONE : View.VISIBLE
        );
    }

    /**
     * Hỏi xác nhận và yêu cầu lý do khi từ chối, ẩn hoặc khôi phục.
     * Không tự đóng hộp thoại nếu lý do còn trống.
     */
    private void showModerationDialog(Action action) {
        if (binding == null || moderationDialog != null) {
            return;
        }

        AdminListing listing = DemoAdminListingRepository.getInstance()
                .getListingById(listingId);

        if (listing == null) {
            displayListing();
            return;
        }

        boolean needsReason = action != Action.APPROVE;

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getActionTitle(action))
                        .setMessage(
                                "Bài đăng: " + listing.getTitle()
                                        + "\nThao tác hiện chỉ áp dụng "
                                        + "cho dữ liệu mẫu."
                        )
                        .setNegativeButton("Quay lại", null)
                        .setPositiveButton("Xác nhận", null);

        EditText reasonInput = new EditText(requireContext());
        reasonInput.setHint("Nhập lý do");
        reasonInput.setMinLines(3);
        reasonInput.setGravity(android.view.Gravity.TOP);
        reasonInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        if (needsReason) {
            LinearLayout container = new LinearLayout(requireContext());

            int padding = Math.round(
                    20 * getResources().getDisplayMetrics().density
            );

            container.setPadding(padding, padding, padding, 0);
            container.addView(
                    reasonInput,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            builder.setView(container);
        }

        AlertDialog dialog = builder.create();
        moderationDialog = dialog;

        dialog.setOnDismissListener(dismissedDialog -> {
            if (moderationDialog == dialog) {
                moderationDialog = null;
            }
        });

        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(clickedView -> {
                    String reason = reasonInput.getText()
                            .toString()
                            .trim();

                    if (needsReason && reason.isEmpty()) {
                        reasonInput.setError("Bạn hãy nhập lý do.");
                        reasonInput.requestFocus();
                        return;
                    }

                    dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                            .setEnabled(false);

                    if (performModeration(action, reason)) {
                        dialog.dismiss();
                    } else {
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                                .setEnabled(true);
                    }
                });
    }

    /**
     * Trả về tiêu đề xác nhận phù hợp với thao tác.
     */
    private String getActionTitle(Action action) {
        switch (action) {
            case APPROVE:
                return "Duyệt bài đăng?";

            case REJECT:
                return "Từ chối bài đăng?";

            case HIDE:
                return "Ẩn bài đăng?";

            case RESTORE:
                return "Khôi phục hiển thị?";

            default:
                return "Xác nhận kiểm duyệt";
        }
    }

    /**
     * Gọi repository để kiểm tra quyền, trạng thái và cập nhật bài đăng.
     * Trả về true khi thao tác thành công.
     */
    private boolean performModeration(Action action, String reason) {
        if (binding == null) {
            return false;
        }

        DemoAdminListingRepository repository =
                DemoAdminListingRepository.getInstance();

        try {
            switch (action) {
                case APPROVE:
                    repository.approveListing(listingId);
                    break;

                case REJECT:
                    repository.rejectListing(listingId, reason);
                    break;

                case HIDE:
                    repository.hideListing(listingId, reason);
                    break;

                case RESTORE:
                    repository.restoreListing(listingId, reason);
                    break;

                default:
                    throw new IllegalStateException(
                            "Thao tác không hợp lệ."
                    );
            }

            displayListing();

            Toast.makeText(
                    requireContext(),
                    "Đã cập nhật trạng thái bài đăng mẫu.",
                    Toast.LENGTH_SHORT
            ).show();

            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            displayListing();
            showError(exception.getMessage());
            return false;
        }
    }

    /**
     * Hiển thị lỗi thao tác hoặc lỗi truy cập.
     */
    private void showError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvAdminDetailError.setText(
                message == null ? "Không thể thực hiện thao tác." : message
        );

        binding.tvAdminDetailError.setVisibility(View.VISIBLE);
    }

    /**
     * Đóng hộp thoại kiểm duyệt nếu đang mở.
     */
    private void closeModerationDialog() {
        AlertDialog dialog = moderationDialog;
        moderationDialog = null;

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Đóng hộp thoại và giải phóng sự kiện, ảnh, binding.
     */
    @Override
    public void onDestroyView() {
        closeModerationDialog();

        if (binding != null) {
            binding.btnAdminDetailBack.setOnClickListener(null);
            binding.btnAdminDetailApprove.setOnClickListener(null);
            binding.btnAdminDetailReject.setOnClickListener(null);
            binding.btnAdminDetailHide.setOnClickListener(null);
            binding.btnAdminDetailRestore.setOnClickListener(null);
            binding.imgAdminDetail.setImageDrawable(null);
        }

        binding = null;
        super.onDestroyView();
    }
}