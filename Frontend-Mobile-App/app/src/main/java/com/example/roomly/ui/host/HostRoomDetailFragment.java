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
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostRoomDetailBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.example.roomly.ui.host.viewing.HostViewingFragment;

/**
 * Hiển thị chi tiết phòng thuộc tài khoản chủ trọ hiện tại.
 * Chỉ truyền ID qua Bundle, rồi lấy lại dữ liệu từ repository.
 */
public class HostRoomDetailFragment extends Fragment {

    private static final String ARG_ROOM_ID = "host_room_id";

    private FragmentHostRoomDetailBinding binding;
    private String roomId;

    /**
     * Tạo màn hình chi tiết với ID của phòng được chọn.
     */
    public static HostRoomDetailFragment newInstance(String roomId) {
        HostRoomDetailFragment fragment = new HostRoomDetailFragment();

        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Đọc ID phòng từ Bundle khi Fragment được tạo.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            roomId = args.getString(ARG_ROOM_ID);
        }
    }

    /**
     * Tạo giao diện chi tiết phòng bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHostRoomDetailBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Đăng ký thao tác và cập nhật thông tin khi phiên thay đổi.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnHostDetailBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack()
        );

        binding.btnHostDetailEdit.setOnClickListener(
                clickedView -> handleEditRoom()
        );

        binding.btnHostDetailCreateListing.setOnClickListener(
                clickedView -> handleCreateListing()
        );

        binding.btnHostDetailViewing.setOnClickListener(
                clickedView -> openViewingScreen()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayRoom()
                );
    }

    /**
     * Đọc lại thông tin khi trở về từ màn hình khác.
     */
    @Override
    public void onResume() {
        super.onResume();

        displayRoom();
    }

    /**
     * Kiểm tra phiên và chủ sở hữu trước khi hiển thị dữ liệu.
     * Nếu dữ liệu mẫu đã mất, hiện thông báo để quay lại.
     */
    private void displayRoom() {
        if (binding == null) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            showError("Bạn cần đăng nhập để xem phòng của mình.");
            return;
        }

        if (!session.hasRole(UserRole.HOST)) {
            showError("Tài khoản hiện tại chưa có quyền chủ trọ.");
            return;
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (room == null) {
            showError(
                    "Không tìm thấy phòng thuộc tài khoản của bạn. "
                            + "Dữ liệu mẫu có thể đã mất khi ứng dụng khởi động lại."
            );
            return;
        }

        binding.tvHostDetailError.setText("");
        binding.tvHostDetailError.setVisibility(View.GONE);
        binding.layoutHostDetailContent.setVisibility(View.VISIBLE);

        binding.tvHostDetailCode.setText(
                "Mã phòng: " + room.getUnitCode()
        );

        binding.tvHostDetailName.setText(room.getName());
        binding.tvHostDetailAddress.setText(room.getAddress());
        binding.tvHostDetailArea.setText(room.getFormattedArea());

        String description = room.getDescription();

        binding.tvHostDetailDescription.setText(
                description == null || description.trim().isEmpty()
                        ? "Chưa có mô tả."
                        : description
        );

        displayRoomImage(room);
    }

    /**
     * Hiển thị ảnh từ đường dẫn đã lưu.
     * Dùng phần thay thế nếu ảnh không còn đọc được.
     */
    private void displayRoomImage(HostRoom room) {
        clearRoomImage();

        String imageUri = room.getImageUri();

        if (imageUri == null || imageUri.trim().isEmpty()) {
            return;
        }

        try {
            binding.imgHostDetailRoom.setImageURI(
                    Uri.parse(imageUri)
            );

            boolean imageLoaded =
                    binding.imgHostDetailRoom.getDrawable() != null;

            binding.imgHostDetailRoom.setVisibility(
                    imageLoaded ? View.VISIBLE : View.GONE
            );

            binding.tvHostDetailImagePlaceholder.setVisibility(
                    imageLoaded ? View.GONE : View.VISIBLE
            );

            binding.imgHostDetailRoom.setContentDescription(
                    "Ảnh phòng " + room.getName()
            );

        } catch (SecurityException exception) {
            clearRoomImage();
        }
    }

    /**
     * Xóa ảnh cũ và hiển thị phần thay thế.
     */
    private void clearRoomImage() {
        binding.imgHostDetailRoom.setImageDrawable(null);
        binding.imgHostDetailRoom.setVisibility(View.GONE);
        binding.tvHostDetailImagePlaceholder.setVisibility(View.VISIBLE);
    }

    /**
     * Ẩn và xóa thông tin phòng khi không còn quyền truy cập.
     */
    private void showError(String message) {
        binding.layoutHostDetailContent.setVisibility(View.GONE);

        binding.tvHostDetailCode.setText("");
        binding.tvHostDetailName.setText("");
        binding.tvHostDetailAddress.setText("");
        binding.tvHostDetailArea.setText("");
        binding.tvHostDetailDescription.setText("");

        clearRoomImage();

        binding.tvHostDetailError.setText(message);
        binding.tvHostDetailError.setVisibility(View.VISIBLE);
    }

    /**
     * Kiểm tra quyền, chủ sở hữu và xác minh email.
     * Khi đủ điều kiện, mở biểu mẫu chỉnh sửa phòng.
     */
    private void handleEditRoom() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openScreen(new LoginFragment());
            return;
        }

        if (!session.hasRole(UserRole.HOST)) {
            displayRoom();
            return;
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (room == null) {
            displayRoom();
            return;
        }

        if (!session.isEmailVerified()) {
            openScreen(
                    VerifyEmailFragment.newInstance(session.getEmail())
            );
            return;
        }

        openScreen(
                HostEditRoomFragment.newInstance(room.getId())
        );
    }
    /**
     * Mở màn hình xác thực và giữ chi tiết phòng trong back stack.
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
     * Kiểm tra quyền, chủ sở hữu và xác minh email.
     * Mở biểu mẫu tạo bài đăng cho phòng đang xem.
     */
    private void handleCreateListing() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openScreen(new LoginFragment());
            return;
        }

        if (!session.hasRole(UserRole.HOST)) {
            displayRoom();
            return;
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (room == null) {
            displayRoom();
            return;
        }

        if (!session.isEmailVerified()) {
            openScreen(
                    VerifyEmailFragment.newInstance(session.getEmail())
            );
            return;
        }

        openScreen(
                HostCreateListingFragment.newInstance(room.getId())
        );
    }

    /**
     * Mở màn hình quản lý khung giờ của phòng đang xem.
     * Kiểm tra lại quyền sở hữu trước khi chuyển màn hình.
     */
    private void openViewingScreen() {
        if (DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId) == null) {
            return;
        }

        openScreen(
                HostViewingFragment.newInstance(roomId)
        );
    }

    /**
     * Gỡ listener, giải phóng ảnh và binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnHostDetailBack.setOnClickListener(null);
            binding.btnHostDetailEdit.setOnClickListener(null);
            binding.imgHostDetailRoom.setImageDrawable(null);
            binding.btnHostDetailCreateListing.setOnClickListener(null);
            binding.btnHostDetailViewing.setOnClickListener(null);
        }

        binding = null;

        super.onDestroyView();
    }
}