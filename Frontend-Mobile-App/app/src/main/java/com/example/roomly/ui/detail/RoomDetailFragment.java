package com.example.roomly.ui.detail;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.ViewingAppointment;
import com.example.roomly.data.repository.DemoAppointmentRepository;
import com.example.roomly.databinding.BottomSheetBookAppointmentBinding;
import com.example.roomly.databinding.FragmentRoomDetailBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.view.inputmethod.EditorInfo;

import com.example.roomly.R;
import com.example.roomly.data.model.Conversation;
import com.example.roomly.data.repository.DemoChatRepository;
import com.example.roomly.ui.messages.ChatFragment;

import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.SessionAccess;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;

public class RoomDetailFragment extends Fragment {

    // Các khóa truyền thông tin phòng qua Bundle.
    private static final String ARG_TITLE = "room_title";
    private static final String ARG_PRICE = "room_price";
    private static final String ARG_ADDRESS = "room_address";
    private static final String ARG_AMENITIES = "room_amenities";
    private static final String ARG_IMAGE = "room_image";

    // Binding của màn hình chi tiết.
    private FragmentRoomDetailBinding binding;

    // Các bảng đang mở, được đóng khi giao diện bị hủy.
    private BottomSheetDialog bookingDialog;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;

    /**
     * Tạo màn hình chi tiết và truyền thông tin phòng qua Bundle.
     * Android có thể khôi phục thông tin này khi tạo lại Fragment.
     */
    public static RoomDetailFragment newInstance(RoomCard room) {
        RoomDetailFragment fragment = new RoomDetailFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, room.getTitle());
        args.putString(ARG_PRICE, room.getPrice());
        args.putString(ARG_ADDRESS, room.getAddress());
        args.putString(ARG_AMENITIES, room.getAmenities());
        args.putInt(ARG_IMAGE, room.getImageResId());

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Tạo giao diện chi tiết phòng từ file XML.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentRoomDetailBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Hiển thị thông tin phòng và đăng ký các thao tác trên màn hình.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        displayRoomDetails();
        setupBackButton();
        setupBookingButton();
        setupMessageButton();
    }

    /**
     * Đọc thông tin phòng trong Bundle và hiển thị lên giao diện.
     */
    private void displayRoomDetails() {
        Bundle args = getArguments();

        if (args == null) {
            return;
        }

        binding.tvDetailTitle.setText(
                args.getString(ARG_TITLE, "")
        );

        binding.tvDetailPrice.setText(
                args.getString(ARG_PRICE, "")
        );

        binding.tvDetailAddress.setText(
                args.getString(ARG_ADDRESS, "")
        );

        binding.tvDetailAmenities.setText(
                args.getString(ARG_AMENITIES, "")
        );

        int imageResId = args.getInt(ARG_IMAGE, 0);

        if (imageResId != 0) {
            binding.imgRoomDetail.setImageResource(imageResId);
        }
    }

    /**
     * Quay lại màn hình đã mở chi tiết phòng.
     */
    private void setupBackButton() {
        binding.btnBack.setOnClickListener(
                view -> getParentFragmentManager().popBackStack()
        );
    }

    /**
     * Mở bảng đặt lịch khi người dùng bấm Đặt lịch xem phòng.
     */
    private void setupBookingButton() {
        binding.btnDetailSchedule.setOnClickListener(
                view -> showBookingDialog()
        );
    }

    /**
     * Lấy thông tin phòng hiện tại để tạo dữ liệu lịch hẹn mẫu.
     * Lịch hẹn dùng tên, địa chỉ và ảnh từ phòng này.
     */
    @Nullable
    private RoomCard getCurrentRoom() {
        Bundle args = getArguments();

        if (args == null) {
            return null;
        }

        return new RoomCard(
                args.getString(ARG_TITLE, ""),
                args.getString(ARG_PRICE, ""),
                args.getString(ARG_ADDRESS, ""),
                args.getString(ARG_AMENITIES, ""),
                args.getInt(ARG_IMAGE, 0),
                false
        );
    }

    /**
     * Kiểm tra quyền hẹn xem phòng:
     * cần đăng nhập, có vai trò TENANT và đã xác minh email.
     * Nếu chưa đủ điều kiện, mở màn hình hướng dẫn tương ứng.
     */
    private boolean checkViewingPermission() {
        SessionAccess.Result result = SessionAccess.requireRole(
                UserRole.TENANT,
                true
        );

        if (result == SessionAccess.Result.ALLOWED) {
            return true;
        }

        // Đóng bảng đặt lịch trước khi chuyển sang màn hình khác.
        if (bookingDialog != null) {
            hideBookingKeyboard(
                    bookingDialog,
                    bookingDialog.getWindow() != null
                            ? bookingDialog.getWindow().getDecorView()
                            : binding.getRoot()
            );

            bookingDialog.dismiss();
        }

        Fragment nextFragment;

        if (result == SessionAccess.Result.LOGIN_REQUIRED) {
            Toast.makeText(
                    requireContext(),
                    "Bạn cần đăng nhập để hẹn xem phòng.",
                    Toast.LENGTH_SHORT
            ).show();

            nextFragment = new LoginFragment();

        } else if (
                result == SessionAccess.Result.EMAIL_VERIFICATION_REQUIRED
        ) {
            Toast.makeText(
                    requireContext(),
                    "Bạn cần xác minh email để hẹn xem phòng.",
                    Toast.LENGTH_SHORT
            ).show();

            String email = SessionRepository.getInstance()
                    .getCurrentSession()
                    .getEmail();

            nextFragment = VerifyEmailFragment.newInstance(email);

        } else {
            Toast.makeText(
                    requireContext(),
                    "Tài khoản chưa có quyền người thuê để hẹn xem phòng.",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        nextFragment
                )
                .addToBackStack(null)
                .commit();

        return false;
    }

    /**
     * Kiểm tra quyền rồi mở bảng đặt lịch xem phòng mẫu.
     * Thiết lập chọn ngày, giờ, ghi chú và xác nhận lịch hẹn.
     */
    private void showBookingDialog() {
        if (!checkViewingPermission()) {
            return;
        }

        // Không mở thêm bảng nếu bảng đặt lịch đang hiển thị.
        if (bookingDialog != null) {
            return;
        }

        RoomCard room = getCurrentRoom();

        if (room == null) {
            Toast.makeText(
                    requireContext(),
                    "Không tìm thấy thông tin phòng.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        BottomSheetBookAppointmentBinding sheetBinding =
                BottomSheetBookAppointmentBinding.inflate(
                        getLayoutInflater()
                );

        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());

        bookingDialog = dialog;
        dialog.setContentView(sheetBinding.getRoot());

        sheetBinding.tvBookingRoomTitle.setText(room.getTitle());

        // Cho phép nhập ghi chú nhiều dòng.
        sheetBinding.edtBookingNote.setRawInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        sheetBinding.edtBookingNote.setImeOptions(
                EditorInfo.IME_ACTION_DONE
        );

        // Đóng bàn phím khi người dùng bấm Xong.
        sheetBinding.edtBookingNote.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideBookingKeyboard(
                                dialog,
                                sheetBinding.edtBookingNote
                        );

                        return true;
                    }

                    return false;
                }
        );

        // Lưu ngày giờ được chọn, bỏ giây và mili giây.
        Calendar appointmentTime = Calendar.getInstance();
        appointmentTime.set(Calendar.SECOND, 0);
        appointmentTime.set(Calendar.MILLISECOND, 0);

        // Phần tử 0 là đã chọn ngày, phần tử 1 là đã chọn giờ.
        boolean[] selected = {false, false};

        sheetBinding.btnChooseDate.setOnClickListener(view -> {
            hideBookingKeyboard(
                    dialog,
                    sheetBinding.edtBookingNote
            );

            showDatePicker(
                    sheetBinding,
                    appointmentTime,
                    selected
            );
        });

        sheetBinding.btnChooseTime.setOnClickListener(view -> {
            hideBookingKeyboard(
                    dialog,
                    sheetBinding.edtBookingNote
            );

            showTimePicker(
                    sheetBinding,
                    appointmentTime,
                    selected
            );
        });

        sheetBinding.btnConfirmBooking.setOnClickListener(view ->
                confirmBooking(
                        room,
                        sheetBinding,
                        dialog,
                        appointmentTime,
                        selected
                )
        );

        // Gỡ các sự kiện và đóng bảng chọn ngày giờ khi đóng bảng.
        dialog.setOnDismissListener(dismissedDialog -> {
            sheetBinding.edtBookingNote
                    .setOnEditorActionListener(null);

            sheetBinding.btnChooseDate.setOnClickListener(null);
            sheetBinding.btnChooseTime.setOnClickListener(null);
            sheetBinding.btnConfirmBooking.setOnClickListener(null);

            closePickerDialogs();

            if (bookingDialog == dialog) {
                bookingDialog = null;
            }
        });

        dialog.show();
    }
    /**
     * Mở bảng chọn ngày và không cho chọn ngày trước hôm nay.
     * Cập nhật nội dung nút sau khi người dùng xác nhận ngày.
     */
    private void showDatePicker(
            BottomSheetBookAppointmentBinding sheetBinding,
            Calendar appointmentTime,
            boolean[] selected
    ) {
        if (datePickerDialog != null) {
            return;
        }

        DatePickerDialog picker = new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, dayOfMonth) -> {
                    appointmentTime.set(Calendar.YEAR, year);
                    appointmentTime.set(Calendar.MONTH, month);
                    appointmentTime.set(
                            Calendar.DAY_OF_MONTH,
                            dayOfMonth
                    );

                    selected[0] = true;

                    SimpleDateFormat dateFormat =
                            new SimpleDateFormat(
                                    "dd/MM/yyyy",
                                    new Locale("vi", "VN")
                            );

                    sheetBinding.btnChooseDate.setText(
                            dateFormat.format(appointmentTime.getTime())
                    );

                    sheetBinding.tvBookingError.setVisibility(
                            View.GONE
                    );
                },
                appointmentTime.get(Calendar.YEAR),
                appointmentTime.get(Calendar.MONTH),
                appointmentTime.get(Calendar.DAY_OF_MONTH)
        );

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        picker.getDatePicker().setMinDate(
                today.getTimeInMillis()
        );

        datePickerDialog = picker;

        picker.setOnDismissListener(dialog -> {
            if (datePickerDialog == picker) {
                datePickerDialog = null;
            }
        });

        picker.show();
    }

    /**
     * Mở bảng chọn giờ theo định dạng 24 giờ.
     * Cập nhật nội dung nút sau khi người dùng xác nhận giờ.
     */
    private void showTimePicker(
            BottomSheetBookAppointmentBinding sheetBinding,
            Calendar appointmentTime,
            boolean[] selected
    ) {
        if (timePickerDialog != null) {
            return;
        }

        TimePickerDialog picker = new TimePickerDialog(
                requireContext(),
                (timePicker, hourOfDay, minute) -> {
                    appointmentTime.set(
                            Calendar.HOUR_OF_DAY,
                            hourOfDay
                    );

                    appointmentTime.set(Calendar.MINUTE, minute);
                    appointmentTime.set(Calendar.SECOND, 0);
                    appointmentTime.set(Calendar.MILLISECOND, 0);

                    selected[1] = true;

                    sheetBinding.btnChooseTime.setText(
                            String.format(
                                    Locale.ROOT,
                                    "%02d:%02d",
                                    hourOfDay,
                                    minute
                            )
                    );

                    sheetBinding.tvBookingError.setVisibility(
                            View.GONE
                    );
                },
                appointmentTime.get(Calendar.HOUR_OF_DAY),
                appointmentTime.get(Calendar.MINUTE),
                true
        );

        timePickerDialog = picker;

        picker.setOnDismissListener(dialog -> {
            if (timePickerDialog == picker) {
                timePickerDialog = null;
            }
        });

        picker.show();
    }

    /**
     * Kiểm tra lại quyền, ngày giờ và ghi chú trước khi lưu lịch mẫu.
     * Chỉ chấp nhận lịch trong tương lai và ghi chú tối đa 300 ký tự.
     */
    private void confirmBooking(
            RoomCard room,
            BottomSheetBookAppointmentBinding sheetBinding,
            BottomSheetDialog dialog,
            Calendar appointmentTime,
            boolean[] selected
    ) {
        // Không xử lý nếu bảng đã đóng hoặc nút đã được bấm trước đó.
        if (!dialog.isShowing()
                || !sheetBinding.btnConfirmBooking.isEnabled()) {
            return;
        }

        // Kiểm tra lại vì phiên đăng nhập có thể đã thay đổi.
        if (!checkViewingPermission()) {
            return;
        }

        hideBookingKeyboard(
                dialog,
                sheetBinding.edtBookingNote
        );

        if (!selected[0]) {
            showBookingError(
                    sheetBinding,
                    "Bạn hãy chọn ngày xem phòng."
            );

            return;
        }

        if (!selected[1]) {
            showBookingError(
                    sheetBinding,
                    "Bạn hãy chọn giờ xem phòng."
            );

            return;
        }

        long appointmentTimeMillis =
                appointmentTime.getTimeInMillis();

        if (appointmentTimeMillis <= System.currentTimeMillis()) {
            showBookingError(
                    sheetBinding,
                    "Thời gian xem phòng phải sau thời điểm hiện tại."
            );

            return;
        }

        String note = "";

        if (sheetBinding.edtBookingNote.getText() != null) {
            note = sheetBinding.edtBookingNote
                    .getText()
                    .toString()
                    .trim();
        }

        int noteLength = note.codePointCount(0, note.length());

        if (noteLength > 300) {
            showBookingError(
                    sheetBinding,
                    "Ghi chú không được vượt quá 300 ký tự."
            );

            return;
        }

        sheetBinding.tvBookingError.setVisibility(View.GONE);

        // Khóa nút để tránh lưu nhiều lần trong cùng một bảng.
        sheetBinding.btnConfirmBooking.setEnabled(false);

        ViewingAppointment appointment =
                new ViewingAppointment(
                        room,
                        appointmentTimeMillis,
                        note
                );

        DemoAppointmentRepository.getInstance()
                .addAppointment(appointment);

        dialog.dismiss();

        Toast.makeText(
                requireContext(),
                "Đã lưu lịch hẹn mẫu. Bạn xem trong tab Lịch trình nhé.",
                Toast.LENGTH_LONG
        ).show();
    }
    /**
     * Hiển thị lý do chưa thể xác nhận đặt lịch.
     */
    private void showBookingError(
            BottomSheetBookAppointmentBinding sheetBinding,
            String message
    ) {
        sheetBinding.tvBookingError.setText(message);
        sheetBinding.tvBookingError.setVisibility(View.VISIBLE);
    }

    /**
     * Đóng bàn phím của bảng đặt lịch và bỏ focus khỏi ô đang nhập.
     */
    private void hideBookingKeyboard(
            BottomSheetDialog dialog,
            View view
    ) {
        if (dialog.getWindow() == null) {
            return;
        }

        View focusedView = dialog.getCurrentFocus();

        if (focusedView != null) {
            focusedView.clearFocus();
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        dialog.getWindow(),
                        view
                );

        controller.hide(WindowInsetsCompat.Type.ime());
    }
    /**
     * Đóng các bảng chọn ngày và giờ nếu chúng còn mở.
     */
    private void closePickerDialogs() {
        if (datePickerDialog != null) {
            datePickerDialog.dismiss();
            datePickerDialog = null;
        }

        if (timePickerDialog != null) {
            timePickerDialog.dismiss();
            timePickerDialog = null;
        }
    }

    /**
     * Kiểm tra đăng nhập trước khi mở hội thoại của phòng.
     * Khách được chuyển sang màn hình Đăng nhập.
     * Hội thoại hiện vẫn sử dụng dữ liệu mẫu.
     */
    private void setupMessageButton() {
        binding.btnDetailMessage.setOnClickListener(view -> {
            SessionAccess.Result result =
                    SessionAccess.requireLogin();

            if (result != SessionAccess.Result.ALLOWED) {
                Toast.makeText(
                        requireContext(),
                        "Bạn cần đăng nhập để nhắn tin với chủ trọ.",
                        Toast.LENGTH_SHORT
                ).show();

                getParentFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(
                                R.id.fragment_container,
                                new LoginFragment()
                        )
                        .addToBackStack(null)
                        .commit();

                return;
            }

            RoomCard room = getCurrentRoom();

            if (room == null) {
                Toast.makeText(
                        requireContext(),
                        "Không tìm thấy thông tin phòng.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Conversation conversation =
                    DemoChatRepository.getInstance()
                            .getOrCreateConversation(room);

            getParentFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                            R.id.fragment_container,
                            ChatFragment.newInstance(
                                    conversation.getId()
                            )
                    )
                    .addToBackStack(null)
                    .commit();
        });
    }
    /**
     * Đóng các bảng và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        closePickerDialogs();

        if (bookingDialog != null) {
            bookingDialog.dismiss();
            bookingDialog = null;
        }

        binding = null;

        super.onDestroyView();
    }
}