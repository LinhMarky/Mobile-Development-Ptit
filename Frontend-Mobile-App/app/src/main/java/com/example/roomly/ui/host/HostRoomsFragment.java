package com.example.roomly.ui.host;

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
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostRoomsBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.example.roomly.ui.host.viewing.HostAppointmentsFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị danh sách phòng mẫu thuộc tài khoản chủ trọ hiện tại.
 */
public class HostRoomsFragment extends Fragment {

    private FragmentHostRoomsBinding binding;
    private HostRoomAdapter roomAdapter;

    /**
     * Tạo giao diện quản lý phòng bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHostRoomsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách, các nút và quan sát phiên đăng nhập.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvHostRooms.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        roomAdapter = new HostRoomAdapter();
        roomAdapter.setOnRoomClickListener(this::openRoomDetail);
        binding.rvHostRooms.setAdapter(roomAdapter);

        binding.btnHostAddRoom.setOnClickListener(
                clickedView -> handleAddRoom()
        );

        binding.btnHostRoomsAccess.setOnClickListener(
                clickedView -> handleAccess()
        );

        // Mở danh sách yêu cầu xem phòng gửi đến chủ trọ.
        binding.btnHostOpenAppointments.setOnClickListener(
                clickedView -> openScreen(new HostAppointmentsFragment())
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(getViewLifecycleOwner(), this::renderSession);
    }

    /**
     * Cập nhật danh sách khi trở lại từ màn hình thêm phòng.
     */
    @Override
    public void onResume() {
        super.onResume();

        renderSession(
                SessionRepository.getInstance().getCurrentSession()
        );
    }

    /**
     * Kiểm tra quyền trước khi đọc danh sách phòng.
     * Chỉ hiện nút thêm phòng và xem yêu cầu cho tài khoản có quyền chủ trọ.
     * Xóa dữ liệu đang hiển thị nếu phiên không còn quyền truy cập.
     */
    private void renderSession(@Nullable SessionState session) {
        if (binding == null || roomAdapter == null) {
            return;
        }

        SessionState currentSession = session == null
                ? SessionState.guest()
                : session;

        // Mặc định ẩn các nút trước khi kiểm tra quyền.
        binding.btnHostAddRoom.setVisibility(View.GONE);
        binding.btnHostRoomsAccess.setVisibility(View.GONE);
        binding.btnHostOpenAppointments.setVisibility(View.GONE);

        if (!currentSession.isLoggedIn()) {
            roomAdapter.updateRooms(new ArrayList<>());

            showStatus(
                    "Đăng nhập để quản lý phòng",
                    "Bạn cần đăng nhập bằng tài khoản có quyền chủ trọ."
            );

            binding.btnHostRoomsAccess.setText("Đăng nhập");
            binding.btnHostRoomsAccess.setVisibility(View.VISIBLE);
            return;
        }

        if (!currentSession.hasRole(UserRole.HOST)) {
            roomAdapter.updateRooms(new ArrayList<>());

            showStatus(
                    "Tài khoản chưa có quyền chủ trọ",
                    "Chức năng quản lý phòng dành cho tài khoản có quyền chủ trọ."
            );
            return;
        }

        // Tài khoản có quyền chủ trọ được mở các màn hình quản lý.
        binding.btnHostAddRoom.setVisibility(View.VISIBLE);
        binding.btnHostOpenAppointments.setVisibility(View.VISIBLE);

        displayRooms(currentSession);
    }

    /**
     * Hiển thị các phòng mẫu thuộc tài khoản hiện tại.
     * Nếu chưa có phòng, hiển thị hướng dẫn thêm phòng hoặc xác minh email.
     */
    private void displayRooms(SessionState session) {
        List<HostRoom> rooms = DemoHostRoomRepository.getInstance()
                .getMyRooms();

        roomAdapter.updateRooms(rooms);

        if (!rooms.isEmpty()) {
            binding.layoutHostRoomsStatus.setVisibility(View.GONE);
            binding.rvHostRooms.setVisibility(View.VISIBLE);
            return;
        }

        if (!session.isEmailVerified()) {
            showStatus(
                    "Xác minh email của bạn",
                    "Bạn cần xác minh email trước khi thêm phòng."
            );

            binding.btnHostRoomsAccess.setText("Xác minh email");
            binding.btnHostRoomsAccess.setVisibility(View.VISIBLE);
            return;
        }

        showStatus(
                "Bạn chưa có phòng",
                "Bấm “Thêm phòng” để tạo phòng đầu tiên trong dữ liệu mẫu."
        );
    }

    /**
     * Hiển thị trạng thái và ẩn danh sách phòng.
     */
    private void showStatus(String title, String description) {
        if (binding == null) {
            return;
        }

        binding.rvHostRooms.setVisibility(View.GONE);
        binding.layoutHostRoomsStatus.setVisibility(View.VISIBLE);

        binding.tvHostRoomsStatusTitle.setText(title);
        binding.tvHostRoomsStatusDescription.setText(description);
    }

    /**
     * Mở đăng nhập hoặc xác minh email theo phiên hiện tại.
     */
    private void handleAccess() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openScreen(new LoginFragment());
            return;
        }

        if (session.hasRole(UserRole.HOST)
                && !session.isEmailVerified()) {
            openScreen(
                    VerifyEmailFragment.newInstance(session.getEmail())
            );
        }
    }

    /**
     * Kiểm tra quyền và xác minh email trước khi mở biểu mẫu thêm phòng.
     */
    private void handleAddRoom() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()) {
            openScreen(new LoginFragment());
            return;
        }

        if (!session.hasRole(UserRole.HOST)) {
            renderSession(session);

            Toast.makeText(
                    requireContext(),
                    "Tài khoản chưa có quyền chủ trọ.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!session.isEmailVerified()) {
            openScreen(
                    VerifyEmailFragment.newInstance(session.getEmail())
            );
            return;
        }

        openScreen(new HostAddRoomFragment());
    }

    /**
     * Mở màn hình mới và giữ danh sách phòng trong back stack.
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
     * Kiểm tra phòng còn thuộc tài khoản hiện tại trước khi mở chi tiết.
     * Chỉ truyền ID để màn hình chi tiết đọc lại dữ liệu và quyền.
     */
    private void openRoomDetail(HostRoom room) {
        HostRoom currentRoom = DemoHostRoomRepository.getInstance()
                .getMyRoomById(room.getId());

        if (currentRoom == null) {
            renderSession(
                    SessionRepository.getInstance().getCurrentSession()
            );

            Toast.makeText(
                    requireContext(),
                    "Không thể mở phòng này.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        openScreen(
                HostRoomDetailFragment.newInstance(currentRoom.getId())
        );
    }

    /**
     * Gỡ listener, adapter và binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnHostAddRoom.setOnClickListener(null);
            binding.btnHostRoomsAccess.setOnClickListener(null);
            binding.rvHostRooms.setAdapter(null);
        }
        if (roomAdapter != null) {
            roomAdapter.setOnRoomClickListener(null);
        }

        roomAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}