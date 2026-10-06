package com.example.roomly.ui.saved;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoRoomRepository;
import com.example.roomly.data.repository.SessionAccess;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentSavedBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.example.roomly.ui.detail.RoomDetailFragment;
import com.example.roomly.ui.explore.RoomAdapter;


import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị phòng đã lưu bằng dữ liệu mẫu.
 * Kiểm tra quyền xem danh sách và quyền thay đổi trạng thái lưu.
 */
public class SavedFragment extends Fragment {

    // Giữ tên đầy đủ của lớp Binding như file hiện tại.
    private FragmentSavedBinding binding;

    private RoomAdapter roomAdapter;

    /**
     * Tạo giao diện màn hình Đã lưu từ file XML.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSavedBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách và đăng ký nút mở Đăng nhập.
     * Theo dõi phiên để cập nhật giao diện khi trạng thái thay đổi.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupRoomList();

        binding.btnSavedLogin.setOnClickListener(
                clickedView -> openLoginScreen()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displaySavedRooms()
                );
    }

    /**
     * Tạo Adapter và đăng ký mở chi tiết, yêu cầu lưu hoặc bỏ lưu.
     */
    private void setupRoomList() {
        binding.rvSavedRooms.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        roomAdapter = new RoomAdapter(new ArrayList<>());

        roomAdapter.setOnRoomClickListener(this::openRoomDetail);

        roomAdapter.setOnSaveRequestListener(
                this::handleSaveRoomRequest
        );

        binding.rvSavedRooms.setAdapter(roomAdapter);
    }

    /**
     * Làm mới danh sách khi mở màn hình hoặc quay lại từ chi tiết.
     */
    @Override
    public void onResume() {
        super.onResume();

        displaySavedRooms();
    }

    /**
     * Kiểm tra quyền xem trước khi đọc danh sách phòng đã lưu.
     * Xem danh sách yêu cầu đăng nhập và vai trò TENANT.
     * Việc thay đổi trạng thái lưu được kiểm tra xác minh email riêng.
     */
    private void displaySavedRooms() {
        if (binding == null || roomAdapter == null) {
            return;
        }

        SessionAccess.Result result = SessionAccess.requireRole(
                UserRole.TENANT,
                false
        );

        if (result != SessionAccess.Result.ALLOWED) {
            // Xóa dữ liệu khỏi Adapter khi phiên không có quyền xem.
            roomAdapter.updateRooms(new ArrayList<>());

            binding.rvSavedRooms.setVisibility(View.GONE);
            binding.layoutSavedEmpty.setVisibility(View.GONE);
            binding.layoutSavedAccess.setVisibility(View.VISIBLE);

            boolean needsLogin =
                    result == SessionAccess.Result.LOGIN_REQUIRED;

            binding.tvSavedAccessTitle.setText(
                    needsLogin
                            ? "Đăng nhập để xem phòng đã lưu"
                            : "Danh sách dành cho người thuê"
            );

            binding.tvSavedAccessDescription.setText(
                    needsLogin
                            ? "Lưu những căn phòng bạn quan tâm "
                              + "để dễ dàng xem lại."
                            : "Tài khoản hiện tại chưa có vai trò "
                              + "người thuê để xem danh sách này."
            );

            binding.btnSavedLogin.setVisibility(
                    needsLogin ? View.VISIBLE : View.GONE
            );

            return;
        }

        binding.layoutSavedAccess.setVisibility(View.GONE);

        List<RoomCard> savedRooms =
                DemoRoomRepository.getInstance().getSavedRooms();

        roomAdapter.updateRooms(savedRooms);

        boolean isEmpty = savedRooms.isEmpty();

        binding.layoutSavedEmpty.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        binding.rvSavedRooms.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );
    }

    /**
     * Kiểm tra đăng nhập, vai trò và xác minh email trước khi bỏ lưu.
     * Chỉ thay đổi dữ liệu mẫu khi người dùng đủ điều kiện.
     */
    private void handleSaveRoomRequest(RoomCard room) {
        if (binding == null || roomAdapter == null) {
            return;
        }

        SessionAccess.Result result = SessionAccess.requireRole(
                UserRole.TENANT,
                true
        );

        if (result == SessionAccess.Result.ALLOWED) {
            room.setSaved(!room.isSaved());

            roomAdapter.notifyRoomSaveChanged(room);

            // Phòng vừa bỏ lưu được loại khỏi danh sách ngay.
            displaySavedRooms();

            Toast.makeText(
                    requireContext(),
                    room.isSaved()
                            ? "Đã lưu phòng vào danh sách mẫu."
                            : "Đã bỏ lưu phòng khỏi danh sách mẫu.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (result == SessionAccess.Result.LOGIN_REQUIRED) {
            displaySavedRooms();
            openLoginScreen();

            return;
        }

        if (result
                == SessionAccess.Result.EMAIL_VERIFICATION_REQUIRED) {
            Toast.makeText(
                    requireContext(),
                    "Bạn cần xác minh email để thay đổi phòng đã lưu.",
                    Toast.LENGTH_SHORT
            ).show();

            openVerifyEmailScreen();

            return;
        }

        displaySavedRooms();

        Toast.makeText(
                requireContext(),
                "Tài khoản chưa có quyền người thuê để bỏ lưu phòng.",
                Toast.LENGTH_LONG
        ).show();
    }

    /**
     * Mở Đăng nhập và giữ màn hình Đã lưu trong back stack.
     * Không tự chuyển màn hình khi khách chỉ mở tab Đã lưu.
     */
    private void openLoginScreen() {
        if (binding == null) {
            return;
        }

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
     * Mở xác minh email với địa chỉ của phiên đăng nhập hiện tại.
     */
    private void openVerifyEmailScreen() {
        String email = SessionRepository.getInstance()
                .getCurrentSession()
                .getEmail();

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        VerifyEmailFragment.newInstance(email)
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Kiểm tra lại quyền xem danh sách trước khi mở phòng được chọn.
     * Giữ màn hình Đã lưu trong back stack để quay lại.
     */
    private void openRoomDetail(RoomCard room) {
        if (binding == null) {
            return;
        }

        SessionAccess.Result result = SessionAccess.requireRole(
                UserRole.TENANT,
                false
        );

        if (result != SessionAccess.Result.ALLOWED) {
            displaySavedRooms();

            if (result == SessionAccess.Result.LOGIN_REQUIRED) {
                openLoginScreen();
            }

            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        RoomDetailFragment.newInstance(room)
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Gỡ sự kiện, Adapter và Binding khi giao diện bị hủy.
     * Observer phiên tự được gỡ theo vòng đời của giao diện.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnSavedLogin.setOnClickListener(null);
            binding.rvSavedRooms.setAdapter(null);
        }

        if (roomAdapter != null) {
            roomAdapter.setOnRoomClickListener(null);
            roomAdapter.setOnSaveRequestListener(null);
            roomAdapter.setOnSaveChangedListener(null);
        }

        roomAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}