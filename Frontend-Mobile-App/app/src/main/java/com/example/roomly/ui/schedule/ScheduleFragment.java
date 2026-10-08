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

/** Hiển thị lịch của người thuê và kiểm tra quyền trước khi hủy lịch. */
public class ScheduleFragment extends Fragment {

    private FragmentScheduleBinding binding;
    private ViewingAppointmentAdapter appointmentAdapter;
    private AlertDialog cancelDialog;

    /** Tạo giao diện Lịch trình bằng ViewBinding. */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentScheduleBinding.inflate(
                inflater, container, false
        );
        return binding.getRoot();
    }

    /** Thiết lập danh sách và làm mới giao diện khi phiên thay đổi. */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupAppointmentList();

        binding.btnScheduleLogin.setOnClickListener(
                clicked -> openLoginScreen()
        );

        SessionRepository.getInstance().getSessionState().observe(
                getViewLifecycleOwner(),
                session -> {
                    closeCancelDialog();
                    displayAppointments();
                }
        );
    }

    /** Tạo adapter và đăng ký thao tác yêu cầu hủy lịch. */
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

    /** Đọc lại lịch khi trở về màn hình Lịch trình. */
    @Override
    public void onResume() {
        super.onResume();
        displayAppointments();
    }

    /** Hiển thị trạng thái truy cập hoặc lịch thuộc người thuê hiện tại. */
    private void displayAppointments() {
        if (binding == null || appointmentAdapter == null) {
            return;
        }

        SessionAccess.Result result =
                SessionAccess.requireRole(UserRole.TENANT, false);

        if (result != SessionAccess.Result.ALLOWED) {
            appointmentAdapter.updateAppointments(new ArrayList<>());

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
                DemoAppointmentRepository.getInstance().getAppointments();

        appointmentAdapter.updateAppointments(appointments);

        boolean isEmpty = appointments.isEmpty();

        binding.layoutScheduleEmpty.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );
        binding.rvAppointments.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );
    }

    /** Đọc lại lịch theo ID trước khi mở xác nhận hủy. */
    private void confirmCancelAppointment(
            ViewingAppointment appointment
    ) {
        if (binding == null || cancelDialog != null
                || !checkCancelPermission()) {
            return;
        }

        ViewingAppointment current =
                DemoAppointmentRepository.getInstance()
                        .getMyAppointmentById(appointment.getId());

        if (current == null || !current.canCancel()) {
            displayAppointments();
            showMessage("Lịch này không còn có thể hủy.");
            return;
        }

        String requestingUserId = SessionRepository.getInstance()
                .getCurrentSession().getUserId();

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Hủy lịch xem phòng?")
                        .setMessage(
                                "Bạn muốn hủy lịch xem phòng “"
                                        + current.getRoomTitle()
                                        + "”? Lịch sẽ được giữ trong lịch sử."
                        )
                        .setNegativeButton("Giữ lịch", null)
                        .setPositiveButton(
                                "Hủy lịch",
                                (interfaceDialog, which) ->
                                        cancelAppointment(
                                                current.getId(),
                                                requestingUserId
                                        )
                        )
                        .create();

        cancelDialog = dialog;

        dialog.setOnDismissListener(dismissed -> {
            if (cancelDialog == dialog) {
                cancelDialog = null;
            }
        });

        dialog.show();
    }

    /** Kiểm tra lại tài khoản rồi chuyển lịch sang trạng thái Đã hủy. */
    private void cancelAppointment(
            String appointmentId,
            String requestingUserId
    ) {
        if (binding == null || !checkCancelPermission()) {
            return;
        }

        String currentUserId = SessionRepository.getInstance()
                .getCurrentSession().getUserId();

        if (!requestingUserId.equals(currentUserId)) {
            displayAppointments();
            showMessage(
                    "Tài khoản đã thay đổi. Hãy chọn lại lịch cần hủy."
            );
            return;
        }

        try {
            DemoAppointmentRepository.getInstance()
                    .cancelAppointment(appointmentId);

            displayAppointments();
            showMessage("Đã hủy lịch xem phòng.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            displayAppointments();
            showMessage(exception.getMessage());
        }
    }

    /** Kiểm tra quyền người thuê và xác minh trước khi hủy lịch. */
    private boolean checkCancelPermission() {
        SessionAccess.Result result =
                SessionAccess.requireRole(UserRole.TENANT, true);

        if (result == SessionAccess.Result.ALLOWED) {
            return true;
        }

        closeCancelDialog();
        displayAppointments();

        if (result == SessionAccess.Result.LOGIN_REQUIRED) {
            openLoginScreen();
        } else if (
                result == SessionAccess.Result.EMAIL_VERIFICATION_REQUIRED
        ) {
            showMessage("Bạn cần xác minh email trước khi hủy lịch.");
            openScreen(VerifyEmailFragment.newInstance(
                    SessionRepository.getInstance()
                            .getCurrentSession().getEmail()
            ));
        } else {
            showMessage("Tài khoản chưa có quyền người thuê.");
        }

        return false;
    }

    /** Mở Đăng nhập và giữ Lịch trình trong back stack. */
    private void openLoginScreen() {
        openScreen(new LoginFragment());
    }

    /** Mở màn hình xác thực sau khi đóng xác nhận hủy lịch. */
    private void openScreen(Fragment fragment) {
        if (binding == null
                || getParentFragmentManager().isStateSaved()) {
            return;
        }

        closeCancelDialog();

        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /** Hiển thị thông báo khi Fragment còn được gắn. */
    private void showMessage(String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(), message, Toast.LENGTH_LONG
            ).show();
        }
    }

    /** Đóng xác nhận hủy và gỡ listener của hộp thoại. */
    private void closeCancelDialog() {
        if (cancelDialog != null) {
            AlertDialog dialog = cancelDialog;
            cancelDialog = null;
            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
    }

    /** Gỡ thao tác, adapter và binding khi giao diện bị hủy. */
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