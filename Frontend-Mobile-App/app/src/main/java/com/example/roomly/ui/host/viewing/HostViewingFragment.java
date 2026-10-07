package com.example.roomly.ui.host.viewing;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.HostViewingSlot;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.data.repository.DemoHostViewingRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostViewingBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.roomly.BuildConfig;
import com.example.roomly.data.repository.DemoHostAppointmentRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị và quản lý khung giờ xem phòng trong dữ liệu mẫu.
 */
public class HostViewingFragment extends Fragment {

    private static final String ARG_ROOM_ID = "viewing_room_id";

    private FragmentHostViewingBinding binding;
    private HostViewingSlotAdapter slotAdapter;

    private String roomId;

    // Giữ hộp thoại đang mở để đóng khi giao diện bị hủy.
    private Dialog activeDialog;

    private interface OnDateTimeSelectedListener {

        /**
         * Nhận thời điểm sau khi người dùng chọn ngày và giờ.
         */
        void onSelected(long timeMillis);
    }

    /**
     * Tạo màn hình quản lý khung giờ cho một phòng.
     */
    public static HostViewingFragment newInstance(String roomId) {
        HostViewingFragment fragment = new HostViewingFragment();

        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Đọc mã phòng từ dữ liệu truyền vào Fragment.
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
     * Tạo giao diện từ fragment_host_viewing.xml.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHostViewingBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách, nút thao tác và theo dõi phiên đăng nhập.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvHostViewingSlots.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        slotAdapter = new HostViewingSlotAdapter(new ArrayList<>());

        slotAdapter.setOnToggleSlotListener(this::confirmToggleSlot);

        binding.rvHostViewingSlots.setAdapter(slotAdapter);

        binding.btnHostViewingBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack()
        );

        binding.btnHostViewingAdd.setOnClickListener(
                clickedView -> startAddingSlot()
        );

        // Chỉ hiện nút thử trong bản debug.
        binding.btnHostViewingDemoRequest.setVisibility(
                BuildConfig.DEBUG ? View.VISIBLE : View.GONE
        );

        binding.btnHostViewingDemoRequest.setOnClickListener(
                clickedView -> createDemoRequest()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayRoomAndSlots()
                );
    }

    /**
     * Cập nhật dữ liệu và trạng thái thời gian khi trở lại màn hình.
     */
    @Override
    public void onResume() {
        super.onResume();
        displayRoomAndSlots();
    }

    /**
     * Hiển thị phòng và các khung giờ thuộc chủ trọ hiện tại.
     * Xóa nội dung riêng tư nếu không còn quyền truy cập phòng.
     */
    private void displayRoomAndSlots() {
        if (binding == null || slotAdapter == null) {
            return;
        }

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId);

        if (room == null) {
            closeActiveDialog();

            binding.layoutHostViewingContent.setVisibility(View.GONE);
            binding.tvHostViewingRoomCode.setText("");
            binding.tvHostViewingRoomName.setText("");
            binding.tvHostViewingRoomAddress.setText("");

            slotAdapter.updateSlots(new ArrayList<>());

            showError(
                    "Không tìm thấy phòng thuộc tài khoản hiện tại."
            );
            return;
        }

        binding.layoutHostViewingContent.setVisibility(View.VISIBLE);
        binding.tvHostViewingError.setVisibility(View.GONE);

        binding.tvHostViewingRoomCode.setText(room.getUnitCode());
        binding.tvHostViewingRoomName.setText(room.getName());
        binding.tvHostViewingRoomAddress.setText(room.getAddress());

        List<HostViewingSlot> slots =
                DemoHostViewingRepository.getInstance()
                        .getMySlots(roomId);

        slotAdapter.updateSlots(slots);

        boolean empty = slots.isEmpty();

        binding.layoutHostViewingEmpty.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );

        binding.rvHostViewingSlots.setVisibility(
                empty ? View.GONE : View.VISIBLE
        );
    }

    /**
     * Kiểm tra đăng nhập, xác minh email và quyền sở hữu phòng.
     */
    private boolean canManageSlots() {
        if (binding == null) {
            return false;
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()) {
            showError("Bạn cần đăng nhập để quản lý khung giờ.");
            return false;
        }

        if (DemoHostRoomRepository.getInstance()
                .getMyRoomById(roomId) == null) {
            displayRoomAndSlots();
            return false;
        }

        if (!session.isEmailVerified()) {
            showError(
                    "Bạn hãy về trang Cá nhân để xác minh email "
                            + "trước khi quản lý khung giờ."
            );
            return false;
        }

        binding.tvHostViewingError.setVisibility(View.GONE);
        return true;
    }

    /**
     * Bắt đầu chọn thời gian cho khung giờ mới.
     */
    private void startAddingSlot() {
        if (activeDialog != null || !canManageSlots()) {
            return;
        }

        long suggestedStart =
                System.currentTimeMillis() + 60 * 60 * 1000L;

        pickDateTime(
                "Ngày bắt đầu",
                "Giờ bắt đầu",
                suggestedStart,
                startTime -> pickDateTime(
                        "Ngày kết thúc",
                        "Giờ kết thúc",
                        startTime + 60 * 60 * 1000L,
                        endTime -> confirmCreateSlot(startTime, endTime)
                )
        );
    }

    /**
     * Cho chọn ngày rồi chọn giờ theo múi giờ hiện tại của thiết bị.
     * Đặt giây và mili giây về 0 để lưu đúng thời điểm theo phút.
     */
    private void pickDateTime(
            String dateTitle,
            String timeTitle,
            long initialTime,
            OnDateTimeSelectedListener listener
    ) {
        if (binding == null) {
            return;
        }

        Calendar selected = Calendar.getInstance();
        selected.setTimeInMillis(initialTime);

        DatePickerDialog dateDialog = new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, day) -> {
                    if (binding == null) {
                        return;
                    }

                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, day);

                    TimePickerDialog timeDialog = new TimePickerDialog(
                            requireContext(),
                            (timePicker, hour, minute) -> {
                                if (binding == null) {
                                    return;
                                }

                                selected.set(Calendar.HOUR_OF_DAY, hour);
                                selected.set(Calendar.MINUTE, minute);
                                selected.set(Calendar.SECOND, 0);
                                selected.set(Calendar.MILLISECOND, 0);

                                // Đóng hộp thoại trước khi mở bước tiếp theo.
                                closeActiveDialog();

                                listener.onSelected(
                                        selected.getTimeInMillis()
                                );
                            },
                            selected.get(Calendar.HOUR_OF_DAY),
                            selected.get(Calendar.MINUTE),
                            true
                    );

                    timeDialog.setTitle(timeTitle);
                    showTrackedDialog(timeDialog);
                },
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)
        );

        dateDialog.setTitle(dateTitle);
        showTrackedDialog(dateDialog);
    }

    /**
     * Kiểm tra thời gian và hỏi xác nhận trước khi tạo khung giờ.
     */
    private void confirmCreateSlot(long startTime, long endTime) {
        if (!canManageSlots()) {
            return;
        }

        if (startTime <= System.currentTimeMillis()) {
            showError("Thời gian bắt đầu phải ở trong tương lai.");
            return;
        }

        if (endTime <= startTime) {
            showError("Giờ kết thúc phải sau giờ bắt đầu.");
            return;
        }

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tạo khung giờ xem phòng?")
                .setMessage(
                        "Bắt đầu: " + formatTime(startTime)
                                + "\nKết thúc: " + formatTime(endTime)
                )
                .setNegativeButton("Quay lại", null)
                .setPositiveButton(
                        "Tạo khung giờ",
                        (dialogInterface, which) ->
                                createSlot(startTime, endTime)
                )
                .create();

        showTrackedDialog(dialog);
    }

    /**
     * Lưu khung giờ vào repository mẫu và cập nhật danh sách.
     * Repository kiểm tra lại quyền và thời gian tại lúc lưu.
     */
    private void createSlot(long startTime, long endTime) {
        if (!canManageSlots()) {
            return;
        }

        try {
            DemoHostViewingRepository.getInstance().createSlot(
                    roomId,
                    startTime,
                    endTime
            );

            displayRoomAndSlots();

            Toast.makeText(
                    requireContext(),
                    "Đã tạo khung giờ mẫu.",
                    Toast.LENGTH_SHORT
            ).show();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
        }
    }

    /**
     * Hỏi xác nhận trước khi mở hoặc đóng nhận lịch.
     */
    private void confirmToggleSlot(HostViewingSlot slot) {
        if (activeDialog != null || !canManageSlots()) {
            return;
        }

        boolean open = !slot.isOpen();

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(
                        open ? "Mở nhận lịch?" : "Đóng nhận lịch?"
                )
                .setMessage(
                        "Khung giờ bắt đầu: "
                                + formatTime(slot.getStartTimeMillis())
                                + (open
                                ? "\nMở khung giờ để nhận lịch mới."
                                : "\nĐóng nhận lịch mới không hủy "
                                  + "các lịch hẹn đã có.")
                )
                .setNegativeButton("Quay lại", null)
                .setPositiveButton(
                        open ? "Mở nhận lịch" : "Đóng nhận lịch",
                        (dialogInterface, which) ->
                                updateSlotStatus(slot.getId(), open)
                )
                .create();

        showTrackedDialog(dialog);
    }

    /**
     * Cập nhật trạng thái khung giờ và hiển thị lại danh sách.
     */
    private void updateSlotStatus(String slotId, boolean open) {
        if (!canManageSlots()) {
            return;
        }

        try {
            DemoHostViewingRepository.getInstance()
                    .setSlotOpen(slotId, open);

            displayRoomAndSlots();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
        }
    }

    /**
     * Định dạng thời gian để hiển thị trong hộp thoại.
     */
    private String formatTime(long timeMillis) {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                new Locale("vi", "VN")
        );

        return formatter.format(new Date(timeMillis));
    }

    /**
     * Hiển thị lỗi chung mà không xóa nội dung danh sách.
     */
    private void showError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvHostViewingError.setText(
                message == null
                        ? "Không thể thực hiện thao tác."
                        : message
        );

        binding.tvHostViewingError.setVisibility(View.VISIBLE);
    }

    /**
     * Hiển thị một hộp thoại và theo dõi để giải phóng đúng vòng đời.
     */
    private void showTrackedDialog(Dialog dialog) {
        closeActiveDialog();

        if (binding == null) {
            return;
        }

        activeDialog = dialog;

        dialog.setOnDismissListener(dismissedDialog -> {
            if (activeDialog == dialog) {
                activeDialog = null;
            }
        });

        dialog.show();
    }

    /**
     * Đóng hộp thoại đang mở nếu có.
     */
    private void closeActiveDialog() {
        Dialog dialog = activeDialog;
        activeDialog = null;

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Tạo yêu cầu xem phòng mẫu để kiểm tra giao diện chủ trọ.
     * Chỉ chạy trong bản debug và sử dụng khung giờ đang mở của phòng.
     */
    private void createDemoRequest() {
        if (!BuildConfig.DEBUG || !canManageSlots()) {
            return;
        }

        try {
            DemoHostAppointmentRepository.getInstance()
                    .createDemoRequest(roomId);

            Toast.makeText(
                    requireContext(),
                    "Đã tạo yêu cầu thử. Mở Yêu cầu xem phòng để kiểm tra.",
                    Toast.LENGTH_LONG
            ).show();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
        }
    }

    /**
     * Đóng hộp thoại, gỡ sự kiện và giải phóng giao diện.
     */
    @Override
    public void onDestroyView() {
        closeActiveDialog();

        if (binding != null) {
            binding.btnHostViewingBack.setOnClickListener(null);
            binding.btnHostViewingAdd.setOnClickListener(null);
            binding.rvHostViewingSlots.setAdapter(null);
            binding.btnHostViewingDemoRequest.setOnClickListener(null);
        }

        if (slotAdapter != null) {
            slotAdapter.setOnToggleSlotListener(null);
        }

        slotAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}