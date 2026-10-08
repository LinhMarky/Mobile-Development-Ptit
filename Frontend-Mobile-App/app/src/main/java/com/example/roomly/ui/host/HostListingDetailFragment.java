package com.example.roomly.ui.host;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.roomly.R;
import com.example.roomly.data.model.HostListing;
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostListingRepository;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostListingDetailBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Hiển thị bài đăng của chủ trọ cùng kết quả kiểm duyệt mới nhất.
 * Chỉ truyền ID qua Bundle và đọc lại dữ liệu từ repository.
 */
public class HostListingDetailFragment extends Fragment {

    private static final String ARG_LISTING_ID = "host_listing_id";

    private FragmentHostListingDetailBinding binding;
    private String listingId;

    private AlertDialog submitDialog;
    private String submitDialogOwnerId;

    /** Tạo màn hình chi tiết với mã bài đăng được chọn. */
    public static HostListingDetailFragment newInstance(
            String listingId
    ) {
        HostListingDetailFragment fragment =
                new HostListingDetailFragment();

        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        fragment.setArguments(args);

        return fragment;
    }

    /** Đọc mã bài đăng khi Fragment được tạo. */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            listingId = getArguments().getString(ARG_LISTING_ID);
        }
    }

    /** Tạo giao diện chi tiết bằng ViewBinding. */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHostListingDetailBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /** Đăng ký nút thao tác và theo dõi thay đổi phiên. */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnListingDetailBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        binding.btnListingDetailEdit.setOnClickListener(
                clickedView -> handleEditListing()
        );

        binding.btnListingDetailSubmit.setOnClickListener(
                clickedView -> handleSubmitListing()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> {
                            closeSubmitDialogIfAccessChanged();
                            displayListing();
                        }
                );
    }

    /** Đọc lại thông tin khi trở về từ màn hình khác. */
    @Override
    public void onResume() {
        super.onResume();

        closeSubmitDialogIfAccessChanged();
        displayListing();
    }

    /**
     * Kiểm tra quyền và hiển thị bài đăng cùng trạng thái mới nhất.
     * Chỉ hiện nút chỉnh sửa và gửi duyệt khi bài còn là bản nháp.
     */
    private void displayListing() {
        if (binding == null) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)) {
            showError(
                    "Bạn cần đăng nhập bằng tài khoản chủ trọ "
                            + "để xem bài đăng của mình."
            );
            return;
        }

        HostListing listing = DemoHostListingRepository.getInstance()
                .getMyListingById(listingId);

        if (listing == null) {
            showError(
                    "Không tìm thấy bài đăng thuộc tài khoản của bạn. "
                            + "Dữ liệu mẫu có thể đã mất khi app khởi động lại."
            );
            return;
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(listing.getRoomId());

        binding.tvListingDetailError.setText("");
        binding.tvListingDetailError.setVisibility(View.GONE);
        binding.layoutListingDetailContent.setVisibility(View.VISIBLE);

        binding.tvListingDetailStatus.setText(
                listing.getStatusLabel()
        );
        binding.tvListingDetailTitle.setText(listing.getTitle());
        binding.tvListingDetailPrice.setText(
                listing.getFormattedPrice()
        );
        binding.tvListingDetailDescription.setText(
                listing.getDescription()
        );

        String reason = listing.getModerationReason().trim();

        binding.tvListingDetailModerationReason.setText(
                reason.isEmpty()
                        ? ""
                        : "Thông tin kiểm duyệt:\n" + reason
        );

        binding.tvListingDetailModerationReason.setVisibility(
                reason.isEmpty() ? View.GONE : View.VISIBLE
        );

        boolean draft = listing.getStatus() == HostListing.Status.DRAFT;

        binding.btnListingDetailEdit.setVisibility(
                draft ? View.VISIBLE : View.GONE
        );
        binding.btnListingDetailSubmit.setVisibility(
                draft ? View.VISIBLE : View.GONE
        );

        binding.btnListingDetailEdit.setEnabled(draft && room != null);
        binding.btnListingDetailSubmit.setEnabled(
                draft && room != null && submitDialog == null
        );

        clearImage();

        if (room == null) {
            closeSubmitDialog();

            binding.tvListingDetailRoomCode.setText(
                    "Không tìm thấy phòng liên kết"
            );
            binding.tvListingDetailAddress.setText("");
            return;
        }

        binding.tvListingDetailRoomCode.setText(
                "Mã phòng: " + room.getUnitCode()
        );
        binding.tvListingDetailAddress.setText(room.getAddress());

        displayImage(room);
    }

    /**
     * Kiểm tra quyền sở hữu, trạng thái nháp và xác minh email
     * trước khi cho chỉnh sửa hoặc gửi duyệt.
     */
    @Nullable
    private HostListing requireEditableDraft() {
        if (binding == null) {
            return null;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openScreen(new LoginFragment());
            return null;
        }

        if (!session.hasRole(UserRole.HOST)) {
            displayListing();
            showMessage("Tài khoản chưa có quyền chủ trọ.");
            return null;
        }

        HostListing listing = DemoHostListingRepository.getInstance()
                .getMyListingById(listingId);

        if (listing == null) {
            displayListing();
            return null;
        }

        if (listing.getStatus() != HostListing.Status.DRAFT) {
            displayListing();
            showMessage("Bài đăng không còn ở trạng thái Bản nháp.");
            return null;
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(listing.getRoomId());

        if (room == null) {
            displayListing();
            showMessage("Không tìm thấy phòng liên kết.");
            return null;
        }

        if (!session.isEmailVerified()) {
            openScreen(
                    VerifyEmailFragment.newInstance(session.getEmail())
            );
            return null;
        }

        return listing;
    }

    /** Mở chỉnh sửa sau khi kiểm tra lại bản nháp hiện tại. */
    private void handleEditListing() {
        HostListing listing = requireEditableDraft();

        if (listing != null) {
            openScreen(
                    HostEditListingFragment.newInstance(listing.getId())
            );
        }
    }

    /** Hỏi xác nhận trước khi chuyển bản nháp sang hàng chờ admin. */
    private void handleSubmitListing() {
        if (submitDialog != null) {
            return;
        }

        HostListing listing = requireEditableDraft();

        if (listing == null) {
            return;
        }

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Gửi bài đăng để duyệt?")
                        .setMessage(
                                listing.getTitle()
                                        + "\n\nSau khi gửi, bài chuyển sang "
                                        + "Chờ duyệt và không thể chỉnh sửa "
                                        + "bằng chức năng sửa bản nháp."
                        )
                        .setNegativeButton("Quay lại", null)
                        .setPositiveButton("Gửi duyệt", null)
                        .create();

        submitDialog = dialog;
        submitDialogOwnerId = listing.getOwnerId();

        dialog.setOnDismissListener(dismissedDialog -> {
            if (submitDialog == dialog) {
                submitDialog = null;
                submitDialogOwnerId = null;

                if (binding != null) {
                    displayListing();
                }
            }
        });

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(clickedView ->
                        submitCurrentDraft(dialog)
                );
    }

    /**
     * Kiểm tra lại tài khoản mở hộp thoại và gửi bản nháp.
     * Repository tiếp tục kiểm tra quyền, phòng và dữ liệu khi lưu.
     */
    private void submitCurrentDraft(AlertDialog dialog) {
        if (binding == null
                || submitDialog != dialog
                || !dialog.isShowing()
                || !dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
        ).isEnabled()) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.getUserId().equals(submitDialogOwnerId)) {
            closeSubmitDialog();
            displayListing();
            showMessage("Phiên tài khoản đã thay đổi.");
            return;
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        try {
            DemoHostListingRepository.getInstance()
                    .submitDraft(listingId);

            closeSubmitDialog();
            displayListing();

            showMessage("Đã gửi bài đăng mẫu sang hàng chờ admin.");
        } catch (
                IllegalArgumentException | IllegalStateException exception
        ) {
            closeSubmitDialog();
            displayListing();
            showMessage(exception.getMessage());
        }
    }

    /** Hiển thị ảnh phòng; dùng phần thay thế nếu không đọc được ảnh. */
    private void displayImage(HostRoom room) {
        String imageUri = room.getImageUri();

        if (imageUri == null || imageUri.trim().isEmpty()) {
            return;
        }

        try {
            binding.imgListingDetailRoom.setImageURI(
                    Uri.parse(imageUri)
            );

            boolean loaded =
                    binding.imgListingDetailRoom.getDrawable() != null;

            binding.imgListingDetailRoom.setVisibility(
                    loaded ? View.VISIBLE : View.GONE
            );
            binding.tvListingDetailImagePlaceholder.setVisibility(
                    loaded ? View.GONE : View.VISIBLE
            );
            binding.imgListingDetailRoom.setContentDescription(
                    "Ảnh phòng " + room.getName()
            );
        } catch (SecurityException | IllegalArgumentException exception) {
            clearImage();
        }
    }

    /** Xóa ảnh cũ và hiển thị phần thay thế. */
    private void clearImage() {
        binding.imgListingDetailRoom.setImageDrawable(null);
        binding.imgListingDetailRoom.setVisibility(View.GONE);
        binding.tvListingDetailImagePlaceholder.setVisibility(
                View.VISIBLE
        );
    }

    /** Xóa thông tin riêng tư khi không còn quyền truy cập. */
    private void showError(String message) {
        closeSubmitDialog();

        binding.layoutListingDetailContent.setVisibility(View.GONE);

        binding.btnListingDetailEdit.setEnabled(false);
        binding.btnListingDetailEdit.setVisibility(View.GONE);
        binding.btnListingDetailSubmit.setEnabled(false);
        binding.btnListingDetailSubmit.setVisibility(View.GONE);

        binding.tvListingDetailStatus.setText("");
        binding.tvListingDetailTitle.setText("");
        binding.tvListingDetailPrice.setText("");
        binding.tvListingDetailDescription.setText("");
        binding.tvListingDetailRoomCode.setText("");
        binding.tvListingDetailAddress.setText("");
        binding.tvListingDetailModerationReason.setText("");
        binding.tvListingDetailModerationReason.setVisibility(View.GONE);

        clearImage();

        binding.tvListingDetailError.setText(message);
        binding.tvListingDetailError.setVisibility(View.VISIBLE);
    }

    /** Đóng hộp thoại nếu tài khoản hoặc điều kiện truy cập thay đổi. */
    private void closeSubmitDialogIfAccessChanged() {
        if (submitDialog == null) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.isEmailVerified()
                || !session.getUserId().equals(submitDialogOwnerId)) {
            closeSubmitDialog();
        }
    }

    /** Đóng hộp thoại và gỡ listener trước khi giải phóng tham chiếu. */
    private void closeSubmitDialog() {
        AlertDialog dialog = submitDialog;

        submitDialog = null;
        submitDialogOwnerId = null;

        if (dialog != null) {
            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
    }

    /** Mở màn hình tiếp theo và giữ chi tiết bài đăng trong back stack. */
    private void openScreen(Fragment fragment) {
        closeSubmitDialog();

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /** Hiển thị thông báo khi giao diện vẫn còn hoạt động. */
    private void showMessage(@Nullable String message) {
        if (binding == null || !isAdded()) {
            return;
        }

        Toast.makeText(
                requireContext(),
                message == null ? "Không thể xử lý bài đăng." : message,
                Toast.LENGTH_LONG
        ).show();
    }

    /** Đóng hộp thoại, gỡ listener và giải phóng ảnh, binding. */
    @Override
    public void onDestroyView() {
        closeSubmitDialog();

        if (binding != null) {
            binding.btnListingDetailBack.setOnClickListener(null);
            binding.btnListingDetailEdit.setOnClickListener(null);
            binding.btnListingDetailSubmit.setOnClickListener(null);
            binding.imgListingDetailRoom.setImageDrawable(null);
        }

        binding = null;

        super.onDestroyView();
    }
}