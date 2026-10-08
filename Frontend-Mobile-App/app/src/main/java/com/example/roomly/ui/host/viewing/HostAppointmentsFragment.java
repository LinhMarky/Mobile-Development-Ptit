package com.example.roomly.ui.host.viewing;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.data.model.HostViewingAppointment;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostAppointmentRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostAppointmentsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị và xử lý yêu cầu xem phòng mẫu của chủ trọ.
 * Mọi thao tác đọc lại dữ liệu và quyền tại thời điểm thực hiện.
 */
public class HostAppointmentsFragment extends Fragment {

    private FragmentHostAppointmentsBinding binding;
    private HostAppointmentAdapter appointmentAdapter;

    private AlertDialog activeDialog;

    // Xác định chủ sở hữu của dữ liệu đang mở trong hộp thoại.
    private String dialogOwnerId;

    /** Tạo giao diện từ fragment_host_appointments.xml. */
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

    /** Thiết lập danh sách, thao tác bấm thẻ và quan sát phiên. */
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

        appointmentAdapter.setOnAppointmentClickListener(
                this::showAppointmentDetails
        );

        binding.rvHostAppointments.setAdapter(appointmentAdapter);

        binding.btnHostAppointmentsBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> {
                            closeDialogIfSessionChanged();
                            displayAppointments();
                        }
                );
    }

    /** Đọc lại danh sách khi màn hình hoạt động trở lại. */
    @Override
    public void onResume() {
        super.onResume();

        closeDialogIfSessionChanged();
        displayAppointments();
    }

    /** Kiểm tra phiên và hiển thị các yêu cầu của đúng chủ trọ. */
    private void displayAppointments() {
        if (binding == null || appointmentAdapter == null) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

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

        if (!session.hasRole(UserRole.HOST)) {
            appointmentAdapter.updateAppointments(
                    new ArrayList<>()
            );

            showStatus(
                    "Tài khoản chưa có quyền chủ trọ",
                    "Yêu cầu xem phòng chỉ hiển thị "
                            + "cho chủ trọ sở hữu phòng."
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

        binding.layoutHostAppointmentsStatus.setVisibility(
                View.GONE
        );

        binding.rvHostAppointments.setVisibility(View.VISIBLE);
    }

    /**
     * Đọc lại yêu cầu rồi mở đầy đủ thông tin.
     * Chỉ hiện thao tác xử lý khi yêu cầu đang chờ,
     * lịch chưa bắt đầu và email chủ trọ đã xác minh.
     */
    private void showAppointmentDetails(String appointmentId) {
        if (binding == null || activeDialog != null) {
            return;
        }

        HostViewingAppointment appointment =
                DemoHostAppointmentRepository.getInstance()
                        .getMyAppointmentById(appointmentId);

        if (appointment == null) {
            displayAppointments();
            showMessage("Yêu cầu không còn tồn tại hoặc bạn không có quyền.");
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        boolean pending = appointment.getStatus()
                == HostViewingAppointment.Status.PENDING;

        boolean future = appointment.getStartTimeMillis()
                > System.currentTimeMillis();

        boolean canProcess = pending
                && future
                && session.isEmailVerified();

        String message = buildAppointmentMessage(appointment);

        if (pending && !future) {
            message += "\n\nLịch đã bắt đầu hoặc đã qua, "
                    + "không thể xác nhận hay từ chối.";
        } else if (pending && !session.isEmailVerified()) {
            message += "\n\nBạn cần xác minh email "
                    + "trước khi xử lý yêu cầu.";
        }

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Yêu cầu xem phòng")
                        .setMessage(message)
                        .setNegativeButton("Đóng", null);

        if (canProcess) {
            builder.setPositiveButton(
                    "Xác nhận",
                    (dialog, which) -> {
                        closeActiveDialog();
                        showConfirmDialog(appointmentId);
                    }
            );

            builder.setNeutralButton(
                    "Từ chối",
                    (dialog, which) -> {
                        closeActiveDialog();
                        showRejectDialog(appointmentId);
                    }
            );
        }

        showTrackedDialog(
                builder.create(),
                appointment.getOwnerId()
        );
    }

    /** Ghép đầy đủ thông tin yêu cầu để hiển thị trong hộp thoại. */
    private String buildAppointmentMessage(
            HostViewingAppointment appointment
    ) {
        StringBuilder message = new StringBuilder();

        message.append("Phòng: ")
                .append(appointment.getRoomName())
                .append("\nMã phòng: ")
                .append(appointment.getUnitCode())
                .append("\nNgười gửi: ")
                .append(appointment.getGuestName())
                .append("\nTrạng thái: ")
                .append(appointment.getStatusLabel())
                .append("\n\nBắt đầu: ")
                .append(formatTime(appointment.getStartTimeMillis()))
                .append("\nKết thúc: ")
                .append(formatTime(appointment.getEndTimeMillis()));

        String note = appointment.getNote().trim();

        message.append("\n\nGhi chú: ")
                .append(note.isEmpty() ? "Không có" : note);

        String reason = appointment.getDecisionReason().trim();

        if (!reason.isEmpty()) {
            message.append("\n\nLý do xử lý: ").append(reason);
        }

        return message.toString();
    }

    /** Hỏi lại trước khi xác nhận yêu cầu đang chờ. */
    private void showConfirmDialog(String appointmentId) {
        HostViewingAppointment appointment =
                getProcessableAppointment(appointmentId);

        if (appointment == null) {
            return;
        }

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xác nhận lịch xem phòng?")
                        .setMessage(
                                "Người xem: "
                                        + appointment.getGuestName()
                                        + "\nPhòng: "
                                        + appointment.getRoomName()
                                        + "\nBắt đầu: "
                                        + formatTime(
                                        appointment.getStartTimeMillis()
                                )
                        )
                        .setNegativeButton("Quay lại", null)
                        .setPositiveButton(
                                "Xác nhận",
                                (clickedDialog, which) ->
                                        processDecision(
                                                appointmentId,
                                                true,
                                                ""
                                        )
                        )
                        .create();

        showTrackedDialog(dialog, appointment.getOwnerId());
    }

    /**
     * Yêu cầu nhập lý do từ chối.
     * Không đóng hộp thoại nếu người dùng chưa nhập lý do.
     */
    private void showRejectDialog(String appointmentId) {
        HostViewingAppointment appointment =
                getProcessableAppointment(appointmentId);

        if (appointment == null) {
            return;
        }

        EditText reasonInput = new EditText(requireContext());
        reasonInput.setHint("Nhập lý do từ chối");
        reasonInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        reasonInput.setMinLines(3);
        reasonInput.setMaxLines(5);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(8), dp(20), dp(8));

        container.addView(
                reasonInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Từ chối yêu cầu?")
                        .setMessage(
                                "Phòng: " + appointment.getRoomName()
                                        + "\nNgười gửi: "
                                        + appointment.getGuestName()
                        )
                        .setView(container)
                        .setNegativeButton("Quay lại", null)
                        .setPositiveButton("Từ chối", null)
                        .create();

        showTrackedDialog(dialog, appointment.getOwnerId());

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(clickedView -> {
                    String reason = reasonInput.getText()
                            .toString()
                            .trim();

                    if (reason.isEmpty()) {
                        reasonInput.setError(
                                "Bạn cần nhập lý do từ chối."
                        );
                        reasonInput.requestFocus();
                        return;
                    }

                    processDecision(
                            appointmentId,
                            false,
                            reason
                    );
                });
    }

    /**
     * Kiểm tra lại yêu cầu trước khi mở hộp thoại xử lý.
     * Repository vẫn kiểm tra lại một lần nữa khi lưu.
     */
    @Nullable
    private HostViewingAppointment getProcessableAppointment(
            String appointmentId
    ) {
        if (binding == null || activeDialog != null) {
            return null;
        }

        HostViewingAppointment appointment =
                DemoHostAppointmentRepository.getInstance()
                        .getMyAppointmentById(appointmentId);

        if (appointment == null) {
            displayAppointments();
            showMessage("Không tìm thấy yêu cầu thuộc phòng của bạn.");
            return null;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isEmailVerified()) {
            showMessage("Bạn cần xác minh email trước khi xử lý.");
            return null;
        }

        if (appointment.getStatus()
                != HostViewingAppointment.Status.PENDING) {
            displayAppointments();
            showMessage("Yêu cầu này đã được xử lý.");
            return null;
        }

        if (appointment.getStartTimeMillis()
                <= System.currentTimeMillis()) {
            showMessage("Lịch đã bắt đầu hoặc đã qua.");
            return null;
        }

        return appointment;
    }

    /**
     * Gọi repository xác nhận hoặc từ chối,
     * sau đó đóng hộp thoại và cập nhật danh sách.
     */
    private void processDecision(
            String appointmentId,
            boolean confirmed,
            String reason
    ) {
        if (binding == null) {
            return;
        }

        try {
            if (confirmed) {
                DemoHostAppointmentRepository.getInstance()
                        .confirmAppointment(appointmentId);
            } else {
                DemoHostAppointmentRepository.getInstance()
                        .rejectAppointment(appointmentId, reason);
            }

            closeActiveDialog();
            displayAppointments();

            showMessage(
                    confirmed
                            ? "Đã xác nhận yêu cầu mẫu."
                            : "Đã từ chối yêu cầu mẫu."
            );
        } catch (
                IllegalArgumentException | IllegalStateException exception
        ) {
            closeActiveDialog();
            displayAppointments();
            showMessage(exception.getMessage());
        }
    }

    /**
     * Theo dõi hộp thoại đang mở.
     * Kiểm tra đúng đối tượng để hộp thoại cũ không xóa tham chiếu mới.
     */
    private void showTrackedDialog(
            AlertDialog dialog,
            String ownerId
    ) {
        activeDialog = dialog;
        dialogOwnerId = ownerId;

        dialog.setOnDismissListener(dismissedDialog -> {
            if (activeDialog == dialog) {
                activeDialog = null;
                dialogOwnerId = null;
            }
        });

        dialog.show();
    }

    /** Đóng hộp thoại khi đổi tài khoản hoặc mất quyền chủ trọ. */
    private void closeDialogIfSessionChanged() {
        if (activeDialog == null) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.HOST)
                || !session.getUserId().equals(dialogOwnerId)) {
            closeActiveDialog();
        }
    }

    /** Đóng hộp thoại đang mở và giải phóng tham chiếu. */
    private void closeActiveDialog() {
        AlertDialog dialog = activeDialog;

        activeDialog = null;
        dialogOwnerId = null;

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /** Hiển thị thông báo trạng thái thay cho danh sách. */
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

    /** Hiển thị thông báo ngắn khi Fragment vẫn còn giao diện. */
    private void showMessage(@Nullable String message) {
        if (binding == null || !isAdded()) {
            return;
        }

        Toast.makeText(
                requireContext(),
                message == null ? "Không thể xử lý yêu cầu." : message,
                Toast.LENGTH_SHORT
        ).show();
    }

    /** Hiển thị ngày giờ theo múi giờ hiện tại của thiết bị. */
    private String formatTime(long timeMillis) {
        return new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                new Locale("vi", "VN")
        ).format(new Date(timeMillis));
    }

    /** Chuyển dp sang pixel theo mật độ màn hình. */
    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    /** Đóng hộp thoại, gỡ listener và giải phóng giao diện. */
    @Override
    public void onDestroyView() {
        closeActiveDialog();

        if (appointmentAdapter != null) {
            appointmentAdapter.setOnAppointmentClickListener(null);
        }

        if (binding != null) {
            binding.btnHostAppointmentsBack.setOnClickListener(null);
            binding.rvHostAppointments.setAdapter(null);
        }

        appointmentAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}