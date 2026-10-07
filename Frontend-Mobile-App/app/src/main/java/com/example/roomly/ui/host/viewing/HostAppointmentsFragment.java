package com.example.roomly.ui.host.viewing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.data.model.HostViewingAppointment;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.DemoHostAppointmentRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostAppointmentsBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị các yêu cầu xem phòng mẫu gửi đến chủ trọ.
 */
public class HostAppointmentsFragment extends Fragment {

    private FragmentHostAppointmentsBinding binding;
    private HostAppointmentAdapter appointmentAdapter;

    /**
     * Tạo giao diện từ fragment_host_appointments.xml.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHostAppointmentsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách, nút quay lại và theo dõi phiên đăng nhập.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvHostAppointments.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        appointmentAdapter = new HostAppointmentAdapter(
                new ArrayList<>()
        );

        binding.rvHostAppointments.setAdapter(appointmentAdapter);

        binding.btnHostAppointmentsBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayAppointments()
                );
    }

    /**
     * Cập nhật danh sách khi màn hình được mở hoặc trở lại.
     */
    @Override
    public void onResume() {
        super.onResume();
        displayAppointments();
    }

    /**
     * Lấy các yêu cầu thuộc những phòng của chủ trọ hiện tại.
     * Repository kiểm tra quyền truy cập thông qua repository phòng.
     * Xóa danh sách đang hiển thị khi phiên đăng nhập thay đổi.
     */
    private void displayAppointments() {
        if (binding == null || appointmentAdapter == null) {
            return;
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()) {
            appointmentAdapter.updateAppointments(
                    new ArrayList<>()
            );

            showStatus(
                    "Bạn chưa đăng nhập",
                    "Hãy đăng nhập tài khoản chủ trọ "
                            + "để xem các yêu cầu gửi đến bạn."
            );
            return;
        }

        List<HostViewingAppointment> appointments =
                DemoHostAppointmentRepository.getInstance()
                        .getMyAppointments();

        appointmentAdapter.updateAppointments(appointments);

        if (appointments.isEmpty()) {
            showStatus(
                    "Chưa có yêu cầu xem phòng",
                    "Các yêu cầu gửi đến những phòng "
                            + "thuộc tài khoản của bạn sẽ xuất hiện tại đây."
            );
            return;
        }

        binding.layoutHostAppointmentsStatus.setVisibility(View.GONE);
        binding.rvHostAppointments.setVisibility(View.VISIBLE);
    }

    /**
     * Hiển thị thông báo trạng thái thay cho danh sách.
     */
    private void showStatus(String title, String description) {
        if (binding == null) {
            return;
        }

        binding.tvHostAppointmentsStatusTitle.setText(title);

        binding.tvHostAppointmentsStatusDescription.setText(
                description
        );

        binding.layoutHostAppointmentsStatus.setVisibility(
                View.VISIBLE
        );

        binding.rvHostAppointments.setVisibility(View.GONE);
    }

    /**
     * Gỡ sự kiện, adapter và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnHostAppointmentsBack.setOnClickListener(null);
            binding.rvHostAppointments.setAdapter(null);
        }

        appointmentAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}