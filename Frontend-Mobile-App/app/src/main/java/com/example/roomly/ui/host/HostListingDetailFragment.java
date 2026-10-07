package com.example.roomly.ui.host;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

/**
 * Hiển thị chi tiết bản nháp thuộc tài khoản chủ trọ hiện tại.
 * Chỉ truyền ID qua Bundle và đọc lại dữ liệu từ repository.
 */
public class HostListingDetailFragment extends Fragment {

    private static final String ARG_LISTING_ID = "host_listing_id";

    private FragmentHostListingDetailBinding binding;
    private String listingId;

    /**
     * Tạo màn hình chi tiết với ID bản nháp được chọn.
     */
    public static HostListingDetailFragment newInstance(String listingId) {
        HostListingDetailFragment fragment =
                new HostListingDetailFragment();

        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Đọc ID bản nháp khi Fragment được tạo.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            listingId = getArguments().getString(ARG_LISTING_ID);
        }
    }

    /**
     * Tạo giao diện chi tiết bài đăng bằng ViewBinding.
     */
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

    /**
     * Đăng ký thao tác và theo dõi phiên để cập nhật quyền truy cập.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnListingDetailBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack()
        );

        binding.btnListingDetailEdit.setOnClickListener(
                clickedView -> handleEditListing()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayListing()
                );
    }

    /**
     * Đọc lại dữ liệu khi trở về từ màn hình khác.
     */
    @Override
    public void onResume() {
        super.onResume();

        displayListing();
    }

    /**
     * Kiểm tra quyền và hiển thị bản nháp cùng phòng liên kết.
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
                    "Không tìm thấy bản nháp thuộc tài khoản của bạn. "
                            + "Dữ liệu mẫu có thể đã mất khi app khởi động lại."
            );
            return;
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(listing.getRoomId());

        binding.tvListingDetailError.setText("");
        binding.tvListingDetailError.setVisibility(View.GONE);
        binding.layoutListingDetailContent.setVisibility(View.VISIBLE);

        binding.tvListingDetailStatus.setText("Bản nháp mẫu");
        binding.tvListingDetailTitle.setText(listing.getTitle());
        binding.tvListingDetailPrice.setText(
                listing.getFormattedPrice()
        );

        binding.tvListingDetailDescription.setText(
                listing.getDescription()
        );

        // Chỉ cho phép chỉnh sửa khi phòng liên kết còn truy cập được.
        binding.btnListingDetailEdit.setEnabled(room != null);

        clearImage();

        if (room == null) {
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
     * Hiển thị ảnh của phòng liên kết.
     * Giữ phần thay thế khi ảnh không còn đọc được.
     */
    private void displayImage(HostRoom room) {
        String imageUri = room.getImageUri();

        if (imageUri == null || imageUri.trim().isEmpty()) {
            return;
        }

        try {
            binding.imgListingDetailRoom.setImageURI(
                    Uri.parse(imageUri)
            );

            boolean imageLoaded =
                    binding.imgListingDetailRoom.getDrawable() != null;

            binding.imgListingDetailRoom.setVisibility(
                    imageLoaded ? View.VISIBLE : View.GONE
            );

            binding.tvListingDetailImagePlaceholder.setVisibility(
                    imageLoaded ? View.GONE : View.VISIBLE
            );

            binding.imgListingDetailRoom.setContentDescription(
                    "Ảnh phòng " + room.getName()
            );

        } catch (SecurityException exception) {
            clearImage();
        }
    }

    /**
     * Xóa ảnh cũ và hiển thị phần thay thế.
     */
    private void clearImage() {
        binding.imgListingDetailRoom.setImageDrawable(null);
        binding.imgListingDetailRoom.setVisibility(View.GONE);

        binding.tvListingDetailImagePlaceholder.setVisibility(
                View.VISIBLE
        );
    }

    /**
     * Xóa nội dung riêng tư và hiển thị lỗi khi không còn quyền truy cập.
     */
    private void showError(String message) {
        binding.layoutListingDetailContent.setVisibility(View.GONE);
        binding.btnListingDetailEdit.setEnabled(false);

        binding.tvListingDetailTitle.setText("");
        binding.tvListingDetailPrice.setText("");
        binding.tvListingDetailDescription.setText("");
        binding.tvListingDetailRoomCode.setText("");
        binding.tvListingDetailAddress.setText("");

        clearImage();

        binding.tvListingDetailError.setText(message);
        binding.tvListingDetailError.setVisibility(View.VISIBLE);
    }

    /**
     * Kiểm tra quyền, bản nháp, phòng liên kết và xác minh email.
     * Màn hình chỉnh sửa bản nháp sẽ được nối ở bước tiếp theo.
     */
    private void handleEditListing() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openScreen(new LoginFragment());
            return;
        }

        if (!session.hasRole(UserRole.HOST)) {
            displayListing();
            return;
        }

        HostListing listing = DemoHostListingRepository.getInstance()
                .getMyListingById(listingId);

        if (listing == null) {
            displayListing();
            return;
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(listing.getRoomId());

        if (room == null) {
            displayListing();
            return;
        }

        if (!session.isEmailVerified()) {
            openScreen(
                    VerifyEmailFragment.newInstance(session.getEmail())
            );
            return;
        }

        // Mở màn hình chỉnh sửa bản nháp đang xem.
        openScreen(
                HostEditListingFragment.newInstance(listingId)
        );
    }

    /**
     * Mở màn hình xác thực và giữ chi tiết bài đăng trong back stack.
     */
    private void openScreen(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Gỡ listener, giải phóng ảnh và binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnListingDetailBack.setOnClickListener(null);
            binding.btnListingDetailEdit.setOnClickListener(null);
            binding.imgListingDetailRoom.setImageDrawable(null);
        }

        binding = null;

        super.onDestroyView();
    }
}