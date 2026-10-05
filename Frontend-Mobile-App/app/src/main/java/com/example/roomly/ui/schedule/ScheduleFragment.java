package com.example.roomly.ui.schedule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.data.model.ViewingAppointment;
import com.example.roomly.data.repository.DemoAppointmentRepository;
import com.example.roomly.databinding.FragmentScheduleBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị các lịch hẹn xem phòng trong dữ liệu mẫu.
 */
public class ScheduleFragment extends Fragment {

    private FragmentScheduleBinding binding;
    private ViewingAppointmentAdapter appointmentAdapter;

    // Giữ hộp thoại để đóng khi giao diện bị hủy.
    private androidx.appcompat.app.AlertDialog cancelDialog;

    /**
     * Tạo giao diện từ fragment_schedule.xml.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentScheduleBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách lịch hẹn và thao tác hủy lịch.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvAppointments.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        appointmentAdapter = new ViewingAppointmentAdapter(
                new ArrayList<>()
        );

        appointmentAdapter.setOnCancelAppointmentListener(
                this::confirmCancelAppointment
        );

        binding.rvAppointments.setAdapter(appointmentAdapter);
    }

    /**
     * Cập nhật danh sách mỗi khi màn hình Lịch trình hoạt động.
     */
    @Override
    public void onResume() {
        super.onResume();

        displayAppointments();
    }

    /**
     * Hiển thị lịch hẹn và chuyển sang thông báo trống nếu chưa có lịch.
     */
    private void displayAppointments() {
        if (binding == null || appointmentAdapter == null) {
            return;
        }

        List<ViewingAppointment> appointments =
                DemoAppointmentRepository.getInstance()
                        .getAppointments();

        appointmentAdapter.updateAppointments(appointments);

        boolean isEmpty = appointments.isEmpty();

        binding.layoutScheduleEmpty.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        binding.rvAppointments.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );
    }

    /**
     * Hỏi xác nhận trước khi xóa lịch hẹn khỏi dữ liệu mẫu.
     */
    private void confirmCancelAppointment(
            ViewingAppointment appointment
    ) {
        if (cancelDialog != null) {
            return;
        }

        cancelDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Hủy lịch xem phòng?")
                .setMessage(
                        "Bạn muốn hủy lịch xem phòng “"
                                + appointment.getRoomTitle()
                                + "”?"
                )
                .setNegativeButton("Giữ lịch", null)
                .setPositiveButton("Hủy lịch", (dialog, which) -> {
                    DemoAppointmentRepository.getInstance()
                            .removeAppointment(appointment);

                    displayAppointments();
                })
                .create();

        cancelDialog.setOnDismissListener(dialog -> {
            cancelDialog = null;
        });

        cancelDialog.show();
    }

    /**
     * Đóng hộp thoại và giải phóng các tham chiếu giao diện.
     */
    @Override
    public void onDestroyView() {
        if (cancelDialog != null) {
            cancelDialog.dismiss();
            cancelDialog = null;
        }

        if (binding != null) {
            binding.rvAppointments.setAdapter(null);
        }

        appointmentAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}