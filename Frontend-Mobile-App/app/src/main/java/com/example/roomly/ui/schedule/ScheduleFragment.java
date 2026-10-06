package com.example.roomly.ui.schedule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.model.ViewingAppointment;
import com.example.roomly.data.repository.DemoAppointmentRepository;
import com.example.roomly.data.repository.SessionAccess;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentScheduleBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị lịch hẹn xem phòng trong dữ liệu mẫu.
 * Kiểm tra quyền xem và quyền hủy trước khi thay đổi dữ liệu.
 */
public class ScheduleFragment extends Fragment {

    private FragmentScheduleBinding binding;
    private ViewingAppointmentAdapter appointmentAdapter;

    // Hộp thoại xác nhận đang mở, nếu có.
    private AlertDialog cancelDialog;

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
     * Thiết lập danh sách, nút đăng nhập và theo dõi trạng thái phiên.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupAppointmentList();

        binding.btnScheduleLogin.setOnClickListener(
                clickedView -> openLoginScreen()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> {
                            // Đóng xác nhận cũ khi phiên thay đổi.
                            closeCancelDialog();
                            displayAppointments();
                        }
                );
    }

    /**
     * Thiết lập danh sách lịch hẹn và đăng ký yêu cầu hủy.
     */
    private void setupAppointmentList() {
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
     * Làm mới danh sách khi màn hình Lịch trình hoạt động.
     */
    @Override
    public void onResume() {
        super.onResume();

        displayAppointments();
    }

    /**
     * Kiểm tra đăng nhập và vai trò TENANT trước khi đọc lịch mẫu.
     * Hiển thị trạng thái khách, thiếu quyền, danh sách hoặc trống.
     */
    private void displayAppointments() {
        if (binding == null || appointmentAdapter == null) {
            return;
        }

        SessionAccess.Result result = SessionAccess.requireRole(
                UserRole.TENANT,
                false
        );

        if (result != SessionAccess.Result.ALLOWED) {
            appointmentAdapter.updateAppointments(
                    new ArrayList<>()
            );

            binding.rvAppointments.setVisibility(View.GONE);
            binding.layoutScheduleEmpty.setVisibility(View.GONE);
            binding.layoutScheduleAccess.setVisibility(View.VISIBLE);

            boolean needsLogin =
                    result == SessionAccess.Result.LOGIN_REQUIRED;

            binding.tvScheduleAccessTitle.setText(
                    needsLogin
                            ? "Đăng nhập để xem lịch trình"
                            : "Lịch trình dành cho người thuê"
            );

            binding.tvScheduleAccessDescription.setText(
                    needsLogin
                            ? "Đăng nhập để theo dõi và quản lý "
                              + "các lịch hẹn xem phòng của bạn."
                            : "Tài khoản hiện tại chưa có vai trò "
                              + "người thuê để xem lịch trình này."
            );

            binding.btnScheduleLogin.setVisibility(
                    needsLogin ? View.VISIBLE : View.GONE
            );

            return;
        }

        binding.layoutScheduleAccess.setVisibility(View.GONE);

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
     * Kiểm tra quyền trước khi mở hộp thoại xác nhận hủy.
     * Lưu mã người dùng để phát hiện đổi tài khoản trong lúc xác nhận.
     */
    private void confirmCancelAppointment(
            ViewingAppointment appointment
    ) {
        if (binding == null || cancelDialog != null) {
            return;
        }

        if (!checkCancelPermission()) {
            return;
        }

        String requestingUserId = SessionRepository.getInstance()
                .getCurrentSession()
                .getUserId();

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Hủy lịch xem phòng?")
                        .setMessage(
                                "Bạn muốn hủy lịch xem phòng “"
                                        + appointment.getRoomTitle()
                                        + "”?"
                        )
                        .setNegativeButton("Giữ lịch", null)
                        .setPositiveButton(
                                "Hủy lịch",
                                (dialogInterface, which) ->
                                        cancelAppointment(
                                                appointment,
                                                requestingUserId
                                        )
                        )
                        .create();

        cancelDialog = dialog;

        dialog.setOnDismissListener(dismissedDialog -> {
            if (cancelDialog == dialog) {
                cancelDialog = null;
            }
        });

        dialog.show();
    }

    /**
     * Kiểm tra lại phiên và quyền trước khi xóa lịch khỏi dữ liệu mẫu.
     * Không xử lý xác nhận được mở bởi một tài khoản khác.
     */
    private void cancelAppointment(
            ViewingAppointment appointment,
            @Nullable String requestingUserId
    ) {
        if (binding == null) {
            return;
        }

        if (!checkCancelPermission()) {
            return;
        }

        String currentUserId = SessionRepository.getInstance()
                .getCurrentSession()
                .getUserId();

        if (requestingUserId == null
                || !requestingUserId.equals(currentUserId)) {
            displayAppointments();

            Toast.makeText(
                    requireContext(),
                    "Tài khoản đã thay đổi. Bạn hãy chọn lại lịch cần hủy.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Kiểm tra lịch còn tồn tại trong repository mẫu.
        boolean stillExists = DemoAppointmentRepository.getInstance()
                .getAppointments()
                .contains(appointment);

        if (!stillExists) {
            displayAppointments();

            Toast.makeText(
                    requireContext(),
                    "Lịch hẹn không còn trong danh sách.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        DemoAppointmentRepository.getInstance()
                .removeAppointment(appointment);

        displayAppointments();

        Toast.makeText(
                requireContext(),
                "Đã hủy lịch hẹn mẫu.",
                Toast.LENGTH_SHORT
        ).show();
    }

    /**
     * Kiểm tra đăng nhập, vai trò TENANT và xác minh email.
     * Mở màn hình phù hợp nếu chưa đủ điều kiện hủy lịch.
     */
    private boolean checkCancelPermission() {
        SessionAccess.Result result = SessionAccess.requireRole(
                UserRole.TENANT,
                true
        );

        if (result == SessionAccess.Result.ALLOWED) {
            return true;
        }

        closeCancelDialog();

        if (result == SessionAccess.Result.LOGIN_REQUIRED) {
            displayAppointments();
            openLoginScreen();

            return false;
        }

        if (result
                == SessionAccess.Result.EMAIL_VERIFICATION_REQUIRED) {
            Toast.makeText(
                    requireContext(),
                    "Bạn cần xác minh email để hủy lịch hẹn.",
                    Toast.LENGTH_SHORT
            ).show();

            openVerifyEmailScreen();

            return false;
        }

        displayAppointments();

        Toast.makeText(
                requireContext(),
                "Tài khoản chưa có quyền người thuê để hủy lịch.",
                Toast.LENGTH_LONG
        ).show();

        return false;
    }

    /**
     * Mở Đăng nhập và giữ màn hình Lịch trình trong back stack.
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
     * Đóng hộp thoại xác nhận nếu còn mở.
     */
    private void closeCancelDialog() {
        AlertDialog dialog = cancelDialog;
        cancelDialog = null;

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Đóng hộp thoại và giải phóng các tham chiếu giao diện.
     * Observer phiên tự được gỡ theo vòng đời giao diện.
     */
    @Override
    public void onDestroyView() {
        closeCancelDialog();

        if (binding != null) {
            binding.btnScheduleLogin.setOnClickListener(null);
            binding.rvAppointments.setAdapter(null);
        }

        if (appointmentAdapter != null) {
            appointmentAdapter.setOnCancelAppointmentListener(null);
        }

        appointmentAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}