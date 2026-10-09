package com.example.roomly.ui.booking;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.roomly.R;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoBookingRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentBookingCreateBinding;
import com.example.roomly.data.model.Booking;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public final class BookingCreateFragment extends Fragment {

    private static final String ARG_ROOM_ID = "booking_room_id";
    private static final String ARG_OWNER_ID = "booking_owner_id";
    private static final String ARG_TITLE = "booking_room_title";
    private static final String ARG_ADDRESS = "booking_room_address";
    private static final String ARG_RENT = "booking_room_rent";
    private static final String ARG_RENT_VND = "booking_rent_vnd";
    private static final String ARG_DEPOSIT = "booking_room_deposit";
    private static final String ARG_DEPOSIT_VND = "booking_deposit_vnd";
    private static final String ARG_AVAILABILITY = "booking_availability";
    private static final String ARG_FEES = "booking_fees";

    private static final String STATE_MOVE_IN = "booking_move_in";

    private FragmentBookingCreateBinding binding;
    private Calendar selectedMoveIn;

    private DatePickerDialog dateDialog;
    private AlertDialog previewDialog;

    private boolean submitting;

    public static BookingCreateFragment newInstance(RoomCard room) {
        BookingCreateFragment fragment = new BookingCreateFragment();

        Bundle arguments = new Bundle();
        String roomId = room.getRoomId();

        if (roomId == null || roomId.trim().isEmpty()) {
            String listingId = room.getListingId();

            if (listingId != null && !listingId.trim().isEmpty()) {
                roomId = "demo-listing-" + listingId.trim();
            } else {
                String title = room.getTitle() == null
                        ? ""
                        : room.getTitle().trim();

                String address = room.getAddress() == null
                        ? ""
                        : room.getAddress().trim();

                roomId = "demo-room-" + java.util.UUID.nameUUIDFromBytes(
                        (title + "\n" + address).getBytes(
                                java.nio.charset.StandardCharsets.UTF_8
                        )
                ).toString();
            }
        }

        arguments.putString(ARG_ROOM_ID, roomId);
        arguments.putString(ARG_OWNER_ID, room.getOwnerId());
        arguments.putString(ARG_TITLE, room.getTitle());
        arguments.putString(ARG_ADDRESS, room.getAddress());
        arguments.putString(ARG_RENT, room.getPrice());
        arguments.putLong(ARG_RENT_VND, room.getMonthlyRent());
        arguments.putString(ARG_DEPOSIT, room.getDepositLabel());

        if (room.getDepositVnd() != null) {
            arguments.putLong(ARG_DEPOSIT_VND, room.getDepositVnd());
        }

        arguments.putString(
                ARG_AVAILABILITY,
                room.getAvailability().name()
        );

        ArrayList<String> fees = new ArrayList<>();

        for (RoomCard.Fee fee : room.getFees()) {
            fees.add(fee.name + ": " + fee.getDisplayAmount());
        }

        arguments.putStringArrayList(ARG_FEES, fees);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_MOVE_IN)) {
            selectedMoveIn = Calendar.getInstance();
            selectedMoveIn.setTimeInMillis(
                    savedInstanceState.getLong(STATE_MOVE_IN)
            );
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentBookingCreateBinding.inflate(
                inflater, container, false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBookingBack.setOnClickListener(
                clickedView ->
                        getParentFragmentManager().popBackStack()
        );

        binding.btnBookingMoveIn.setOnClickListener(
                clickedView -> showDatePicker()
        );

        binding.btnBookingSubmit.setText("Xem trước yêu cầu demo");
        binding.btnBookingSubmit.setOnClickListener(
                clickedView -> previewRequest()
        );

        if (savedInstanceState == null) {
            binding.edtBookingOccupants.setText("1");
        }

        renderSummary();
        updateDateButton();
    }

    private void renderSummary() {
        Bundle arguments = getArguments();

        if (arguments == null
                || arguments.getString(ARG_TITLE, "").trim().isEmpty()) {
            showFormError("Không tìm thấy thông tin phòng.");
            setFormEnabled(false);
            return;
        }

        binding.tvBookingRoomTitle.setText(
                arguments.getString(ARG_TITLE, "")
        );

        String address = arguments.getString(ARG_ADDRESS, "");

        binding.tvBookingRoomAddress.setText(
                address.trim().isEmpty()
                        ? "Chưa cung cấp địa chỉ"
                        : address
        );

        binding.tvBookingRent.setText(
                "Giá thuê: "
                        + arguments.getString(ARG_RENT, "Chưa cung cấp")
        );

        binding.tvBookingDeposit.setText(
                "Tiền cọc: "
                        + arguments.getString(ARG_DEPOSIT, "Chưa cung cấp")
        );

        binding.layoutBookingFees.removeAllViews();

        ArrayList<String> fees =
                arguments.getStringArrayList(ARG_FEES);

        if (fees == null || fees.isEmpty()) {
            addFeeText("Chưa cung cấp thông tin phí dịch vụ.");
        } else {
            for (String fee : fees) {
                addFeeText(fee);
            }
        }

        boolean available = RoomCard.Availability.AVAILABLE.name()
                .equals(arguments.getString(ARG_AVAILABILITY));

        setFormEnabled(available);

        if (!available) {
            showFormError(
                    "Phòng hiện chưa đủ điều kiện tạo yêu cầu thuê."
            );
        } else {
            binding.tvBookingError.setVisibility(View.GONE);
        }
    }

    private void addFeeText(String text) {
        TextView feeView = new TextView(requireContext());

        feeView.setText(text);
        feeView.setTextSize(14);
        feeView.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.roomly_text_secondary
                )
        );

        int padding = Math.round(
                4 * getResources().getDisplayMetrics().density
        );

        feeView.setPadding(0, padding, 0, padding);
        binding.layoutBookingFees.addView(feeView);
    }

    private void setFormEnabled(boolean enabled) {
        binding.btnBookingSubmit.setEnabled(enabled);
        binding.btnBookingMoveIn.setEnabled(enabled);
        binding.inputBookingOccupants.setEnabled(enabled);
        binding.inputBookingNote.setEnabled(enabled);
    }

    private void showDatePicker() {
        if (binding == null || !isAdded() || dateDialog != null) {
            return;
        }

        Calendar tomorrow = getTomorrow();
        Calendar initial = selectedMoveIn == null
                || selectedMoveIn.before(tomorrow)
                ? tomorrow
                : selectedMoveIn;

        dateDialog = new DatePickerDialog(
                requireContext(),
                (picker, year, month, day) -> {
                    selectedMoveIn = Calendar.getInstance();
                    selectedMoveIn.clear();
                    selectedMoveIn.set(year, month, day);

                    if (binding != null) {
                        binding.tvBookingError.setVisibility(View.GONE);
                        updateDateButton();
                    }
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        );

        dateDialog.getDatePicker().setMinDate(
                tomorrow.getTimeInMillis()
        );

        dateDialog.setOnDismissListener(
                dialog -> dateDialog = null
        );

        dateDialog.show();
    }

    private void updateDateButton() {
        if (binding == null) {
            return;
        }

        binding.btnBookingMoveIn.setText(
                selectedMoveIn == null
                        ? "Chọn ngày vào ở"
                        : formatDate(selectedMoveIn)
        );
    }

    private void previewRequest() {
        if (binding == null
                || !isAdded()
                || submitting
                || !binding.btnBookingSubmit.isEnabled()
                || previewDialog != null) {
            return;
        }

        binding.tvBookingError.setVisibility(View.GONE);
        binding.inputBookingOccupants.setError(null);
        binding.inputBookingNote.setError(null);

        if (selectedMoveIn == null
                || selectedMoveIn.before(getTomorrow())) {
            showFormError(
                    "Vui lòng chọn ngày vào ở từ ngày mai trở đi."
            );
            binding.btnBookingMoveIn.requestFocus();
            return;
        }

        String occupantText =
                binding.edtBookingOccupants.getText() == null
                        ? ""
                        : binding.edtBookingOccupants.getText()
                        .toString().trim();

        int occupants;

        try {
            occupants = Integer.parseInt(occupantText);
        } catch (NumberFormatException exception) {
            binding.inputBookingOccupants.setError(
                    "Vui lòng nhập số người ở từ 1 đến 10."
            );
            binding.edtBookingOccupants.requestFocus();
            return;
        }

        if (occupants < 1 || occupants > 10) {
            binding.inputBookingOccupants.setError(
                    "Số người ở phải từ 1 đến 10."
            );
            binding.edtBookingOccupants.requestFocus();
            return;
        }

        String note = binding.edtBookingNote.getText() == null
                ? ""
                : binding.edtBookingNote.getText().toString().trim();

        if (note.codePointCount(0, note.length()) > 1000) {
            binding.inputBookingNote.setError(
                    "Ghi chú tối đa 1.000 ký tự."
            );
            binding.edtBookingNote.requestFocus();
            return;
        }

        Bundle arguments = getArguments();

        if (arguments == null) {
            showFormError("Không tìm thấy thông tin phòng.");
            return;
        }

        long moveInMillis = selectedMoveIn.getTimeInMillis();

        String message =
                "Phòng: " + arguments.getString(ARG_TITLE, "")
                        + "\nNgày dự kiến vào ở: "
                        + formatDate(selectedMoveIn)
                        + "\nSố người ở: " + occupants
                        + "\nGiá thuê: "
                        + arguments.getString(ARG_RENT, "Chưa cung cấp")
                        + "\nTiền cọc: "
                        + arguments.getString(
                        ARG_DEPOSIT, "Chưa cung cấp"
                )
                        + "\n\nGhi chú: "
                        + (note.isEmpty() ? "Không có ghi chú." : note)
                        + "\n\nYêu cầu demo chỉ lưu trong bộ nhớ "
                        + "khi app đang chạy. Không gửi đến chủ trọ "
                        + "và không phát sinh thanh toán."
                        + "\nCác yêu cầu demo dùng chung để thử giao diện.";

        previewDialog = new MaterialAlertDialogBuilder(
                requireContext()
        )
                .setTitle("Xem trước yêu cầu demo")
                .setMessage(message)
                .setNegativeButton("Sửa lại", null)
                .setPositiveButton("Tạo yêu cầu demo", null)
                .create();

        previewDialog.setOnDismissListener(
                dialog -> previewDialog = null
        );

        previewDialog.show();

        previewDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        viewClicked -> createDemoRequest(
                                moveInMillis,
                                occupants,
                                note
                        )
                );
    }

    private void createDemoRequest(
            long moveInMillis,
            int occupants,
            String note
    ) {
        if (binding == null
                || !isAdded()
                || submitting
                || getParentFragmentManager().isStateSaved()) {
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn()
                || !session.hasRole(UserRole.TENANT)) {
            showMessage(
                    "Hãy vào Cá nhân và bật tài khoản Người thuê thử "
                            + "để tạo yêu cầu demo."
            );
            return;
        }

        Bundle arguments = getArguments();

        if (arguments == null) {
            showMessage("Không tìm thấy thông tin phòng.");
            return;
        }

        String ownerId = arguments.getString(ARG_OWNER_ID, "");

        if (!ownerId.isEmpty()
                && ownerId.equals(session.getUserId())) {
            showMessage("Bạn không thể yêu cầu thuê phòng của mình.");
            return;
        }

        if (!RoomCard.Availability.AVAILABLE.name().equals(
                arguments.getString(ARG_AVAILABILITY)
        )) {
            showMessage("Phòng hiện chưa đủ điều kiện tạo yêu cầu.");
            return;
        }

        if (!arguments.containsKey(ARG_RENT_VND)) {
            showMessage(
                    "Vui lòng quay lại và mở lại form từ chi tiết phòng."
            );
            return;
        }

        submitting = true;
        setFormEnabled(false);
        binding.progressBookingSubmit.setVisibility(View.VISIBLE);

        if (previewDialog != null) {
            previewDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(false);
        }

        Booking booking;

        try {
            Long deposit = arguments.containsKey(ARG_DEPOSIT_VND)
                    ? arguments.getLong(ARG_DEPOSIT_VND)
                    : null;

            booking = DemoBookingRepository.getInstance()
                    .createDemoRequest(
                            arguments.getString(ARG_ROOM_ID, ""),
                            arguments.getString(ARG_TITLE, ""),
                            arguments.getString(ARG_ADDRESS, ""),
                            arguments.getLong(ARG_RENT_VND),
                            deposit,
                            moveInMillis,
                            occupants,
                            note
                    );
        } catch (IllegalArgumentException exception) {
            submitting = false;
            setFormEnabled(true);
            binding.progressBookingSubmit.setVisibility(View.GONE);

            if (previewDialog != null) {
                previewDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setEnabled(true);
            }

            showMessage(
                    exception.getMessage() == null
                            ? "Không thể tạo yêu cầu demo."
                            : exception.getMessage()
            );
            return;
        }

        if (previewDialog != null) {
            previewDialog.dismiss();
        }

        binding.progressBookingSubmit.setVisibility(View.GONE);

        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        BookingDetailFragment.newInstance(
                                booking.getId()
                        )
                )
                .addToBackStack(null)
                .commit();
    }

    private void showFormError(String message) {
        if (binding != null) {
            binding.tvBookingError.setText(message);
            binding.tvBookingError.setVisibility(View.VISIBLE);
        }
    }

    private void showMessage(String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private static Calendar getTomorrow() {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        return calendar;
    }

    private static String formatDate(Calendar date) {
        return new SimpleDateFormat(
                "dd/MM/yyyy",
                new Locale("vi", "VN")
        ).format(date.getTime());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (selectedMoveIn != null) {
            outState.putLong(
                    STATE_MOVE_IN,
                    selectedMoveIn.getTimeInMillis()
            );
        }
    }

    @Override
    public void onDestroyView() {
        if (dateDialog != null) {
            dateDialog.setOnDismissListener(null);
            dateDialog.dismiss();
            dateDialog = null;
        }

        if (previewDialog != null) {
            previewDialog.setOnDismissListener(null);
            previewDialog.dismiss();
            previewDialog = null;
        }

        if (binding != null) {
            binding.btnBookingBack.setOnClickListener(null);
            binding.btnBookingMoveIn.setOnClickListener(null);
            binding.btnBookingSubmit.setOnClickListener(null);
        }

        submitting = false;
        binding = null;

        super.onDestroyView();
    }
}