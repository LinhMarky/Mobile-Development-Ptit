package com.example.roomly.ui.detail;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.roomly.R;
import com.example.roomly.data.model.Conversation;
import com.example.roomly.data.model.HostViewingSlot;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoAdminListingRepository;
import com.example.roomly.data.repository.DemoAppointmentRepository;
import com.example.roomly.data.repository.DemoChatRepository;
import com.example.roomly.data.repository.DemoHostViewingRepository;
import com.example.roomly.data.repository.SessionAccess;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.BottomSheetBookAppointmentBinding;
import com.example.roomly.databinding.FragmentRoomDetailBinding;
import com.example.roomly.ui.auth.LoginFragment;
import com.example.roomly.ui.auth.VerifyEmailFragment;
import com.example.roomly.ui.messages.ChatFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị dữ liệu chi tiết của bài đăng công khai.
 * Đọc lại bài đăng trước khi hiển thị hoặc thực hiện thao tác.
 */
public class RoomDetailFragment extends Fragment {

    private static final String ARG_TITLE = "room_title";
    private static final String ARG_PRICE = "room_price";
    private static final String ARG_ADDRESS = "room_address";
    private static final String ARG_AMENITIES = "room_amenities";
    private static final String ARG_IMAGE = "room_image";
    private static final String ARG_ROOM_ID = "room_id";
    private static final String ARG_OWNER_ID = "room_owner_id";
    private static final String ARG_UNIT_CODE = "room_unit_code";
    private static final String ARG_ROOM_TYPE = "room_type";
    private static final String ARG_LISTING_ID = "room_listing_id";
    private static final String ARG_IMAGE_URI = "room_image_uri";
    private static final String ARG_DESCRIPTION = "room_description";
    private static final String ARG_HOST_NAME = "room_host_name";
    private static final String ARG_AREA = "room_area";
    private static final String ARG_MONTHLY_RENT = "room_monthly_rent";

    private FragmentRoomDetailBinding binding;

    private BottomSheetDialog bookingDialog;
    private AlertDialog slotDialog;
    private String bookingUserId;

    /**
     * Truyền ID bài đăng và thông tin dự phòng qua Bundle.
     * Thông tin dự phòng dùng cho phòng minh họa chưa có bài đăng.
     */
    public static RoomDetailFragment newInstance(RoomCard room) {
        RoomDetailFragment fragment = new RoomDetailFragment();
        Bundle args = new Bundle();

        args.putString(ARG_TITLE, room.getTitle());
        args.putString(ARG_PRICE, room.getPrice());
        args.putString(ARG_ADDRESS, room.getAddress());
        args.putString(ARG_AMENITIES, room.getAmenities());
        args.putInt(ARG_IMAGE, room.getImageResId());

        args.putString(ARG_ROOM_ID, room.getRoomId());
        args.putString(ARG_OWNER_ID, room.getOwnerId());
        args.putString(ARG_UNIT_CODE, room.getUnitCode());
        args.putString(ARG_ROOM_TYPE, room.getRoomType().name());
        args.putString(ARG_LISTING_ID, room.getListingId());
        args.putString(ARG_IMAGE_URI, room.getImageUri());

        args.putString(ARG_DESCRIPTION, room.getDescription());
        args.putString(ARG_HOST_NAME, room.getHostName());
        args.putLong(ARG_MONTHLY_RENT, room.getMonthlyRent());

        if (room.getArea() != null) {
            args.putString(ARG_AREA, room.getArea().toPlainString());
        }

        fragment.setArguments(args);
        return fragment;
    }

    /** Tạo giao diện chi tiết phòng bằng ViewBinding. */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentRoomDetailBinding.inflate(
                inflater, container, false
        );
        return binding.getRoot();
    }

    /** Đăng ký thao tác và theo dõi phiên trong lúc bảng đặt lịch mở. */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(
                clicked -> getParentFragmentManager().popBackStack()
        );

        binding.btnDetailSchedule.setOnClickListener(
                clicked -> showBookingDialog()
        );

        binding.btnDetailMessage.setOnClickListener(
                clicked -> openConversation()
        );

        SessionRepository.getInstance().getSessionState().observe(
                getViewLifecycleOwner(),
                session -> {
                    if (bookingDialog == null) {
                        return;
                    }

                    SessionState current = SessionRepository.getInstance()
                            .getCurrentSession();

                    if (!current.isLoggedIn()
                            || !current.hasRole(UserRole.TENANT)
                            || !current.isEmailVerified()
                            || bookingUserId == null
                            || !bookingUserId.equals(current.getUserId())) {
                        closeBookingDialog();
                    }
                }
        );

        displayRoomDetails();
    }

    /** Đọc lại bài đăng khi trở về từ một màn hình khác. */
    @Override
    public void onResume() {
        super.onResume();
        displayRoomDetails();
    }

    /**
     * Đọc dữ liệu mới nhất nếu phòng có ID bài đăng.
     * Không dùng dữ liệu cũ khi bài đăng đã bị ẩn hoặc không còn tồn tại.
     */
    @Nullable
    private RoomCard getCurrentRoom() {
        Bundle args = getArguments();

        if (args == null) {
            return null;
        }

        String listingId = args.getString(ARG_LISTING_ID, "");

        if (!listingId.trim().isEmpty()) {
            return DemoAdminListingRepository.getInstance()
                    .getPublishedRoomCardById(listingId);
        }

        RoomCard.RoomType type = RoomCard.RoomType.ROOM;

        try {
            type = RoomCard.RoomType.valueOf(
                    args.getString(ARG_ROOM_TYPE, "ROOM")
            );
        } catch (IllegalArgumentException ignored) {
            // Giữ loại phòng mặc định nếu Bundle có giá trị không hợp lệ.
        }

        RoomCard room = new RoomCard(
                args.getString(ARG_TITLE, ""),
                args.getString(ARG_PRICE, ""),
                args.getString(ARG_ADDRESS, ""),
                args.getString(ARG_AMENITIES, ""),
                args.getInt(ARG_IMAGE, 0),
                false,
                type,
                args.getString(ARG_ROOM_ID, ""),
                args.getString(ARG_OWNER_ID, ""),
                args.getString(ARG_UNIT_CODE, "")
        );

        room.setImageUri(args.getString(ARG_IMAGE_URI, ""));
        room.setDescription(args.getString(ARG_DESCRIPTION, ""));
        room.setHostName(args.getString(ARG_HOST_NAME, ""));
        room.setMonthlyRent(args.getLong(ARG_MONTHLY_RENT, 0));

        String areaText = args.getString(ARG_AREA, "");

        if (!areaText.isEmpty()) {
            try {
                room.setArea(new BigDecimal(areaText));
            } catch (NumberFormatException ignored) {
                // Không tự tạo diện tích khi dữ liệu dự phòng không hợp lệ.
            }
        }

        return room;
    }

    /** Hiển thị thông tin đúng theo bài đăng và xóa nội dung mẫu cố định. */
    private void displayRoomDetails() {
        if (binding == null) {
            return;
        }

        RoomCard room = getCurrentRoom();

        if (room == null) {
            showUnavailableRoom();
            return;
        }

        binding.btnDetailMessage.setEnabled(true);
        binding.btnDetailSchedule.setEnabled(true);

        binding.tvDetailTitle.setText(room.getTitle());
        binding.tvDetailPrice.setText(room.getPrice());
        binding.tvDetailAddress.setText(room.getAddress());

        binding.tvDetailAmenities.setText(
                room.getAmenities().isEmpty()
                        ? "Thông tin phòng chưa cung cấp"
                        : room.getAmenities()
        );

        binding.tvDetailDescription.setText(
                room.getDescription().isEmpty()
                        ? "Chủ trọ chưa cung cấp mô tả."
                        : room.getDescription()
        );

        binding.tvDetailLandlordName.setText(
                room.getHostName().isEmpty()
                        ? "Chưa cung cấp tên chủ trọ"
                        : room.getHostName()
        );

        // Nguồn dữ liệu hiện chưa có các khoản phí này.
        binding.tvDetailElectricity.setText("Chưa cung cấp");
        binding.tvDetailWater.setText("Chưa cung cấp");
        binding.tvDetailInternet.setText("Chưa cung cấp");

        displayRoomImage(room);
    }

    /** Hiển thị ảnh phòng và dùng ảnh drawable nếu có. */
    private void displayRoomImage(RoomCard room) {
        binding.imgRoomDetail.setImageDrawable(null);

        String imageUri = room.getImageUri();

        if (!imageUri.isEmpty()) {
            try {
                binding.imgRoomDetail.setImageURI(Uri.parse(imageUri));
            } catch (SecurityException | IllegalArgumentException ignored) {
                binding.imgRoomDetail.setImageDrawable(null);
            }
        }

        if (binding.imgRoomDetail.getDrawable() == null
                && room.getImageResId() != 0) {
            binding.imgRoomDetail.setImageResource(room.getImageResId());
        }

        binding.imgRoomDetail.setContentDescription(
                binding.imgRoomDetail.getDrawable() == null
                        ? "Phòng chưa có ảnh"
                        : "Ảnh phòng " + room.getTitle()
        );
    }

    /** Xóa dữ liệu cũ và khóa thao tác khi bài đăng không còn công khai. */
    private void showUnavailableRoom() {
        closeBookingDialog();

        binding.imgRoomDetail.setImageDrawable(null);
        binding.imgRoomDetail.setContentDescription("Không có ảnh phòng");

        binding.tvDetailTitle.setText("Bài đăng không còn hiển thị");
        binding.tvDetailPrice.setText("");
        binding.tvDetailAddress.setText("");
        binding.tvDetailAmenities.setText("");

        binding.tvDetailDescription.setText(
                "Bài đăng có thể đã bị ẩn hoặc dữ liệu mẫu đã mất. "
                        + "Bạn quay lại Khám phá để chọn phòng khác."
        );

        binding.tvDetailLandlordName.setText("Chưa cung cấp");
        binding.tvDetailElectricity.setText("Chưa cung cấp");
        binding.tvDetailWater.setText("Chưa cung cấp");
        binding.tvDetailInternet.setText("Chưa cung cấp");

        binding.btnDetailMessage.setEnabled(false);
        binding.btnDetailSchedule.setEnabled(false);
    }

    /** Kiểm tra đăng nhập, quyền người thuê và xác minh email. */
    private boolean checkViewingPermission() {
        SessionAccess.Result result =
                SessionAccess.requireRole(UserRole.TENANT, true);

        if (result == SessionAccess.Result.ALLOWED) {
            return true;
        }

        closeBookingDialog();

        if (result == SessionAccess.Result.LOGIN_REQUIRED) {
            showMessage("Bạn cần đăng nhập để hẹn xem phòng.");
            openScreen(new LoginFragment());

        } else if (
                result == SessionAccess.Result.EMAIL_VERIFICATION_REQUIRED
        ) {
            showMessage("Bạn cần xác minh email để hẹn xem phòng.");

            openScreen(VerifyEmailFragment.newInstance(
                    SessionRepository.getInstance()
                            .getCurrentSession().getEmail()
            ));

        } else {
            showMessage("Tài khoản chưa có quyền người thuê.");
        }

        return false;
    }

    /** Mở bảng gửi yêu cầu xem phòng theo khung giờ có sẵn. */
    private void showBookingDialog() {
        if (binding == null || bookingDialog != null
                || !checkViewingPermission()) {
            return;
        }

        RoomCard room = getCurrentRoom();

        if (room == null) {
            displayRoomDetails();
            return;
        }

        if (room.getListingId().isEmpty()) {
            showMessage(
                    "Phòng minh họa chưa hỗ trợ đặt lịch. "
                            + "Hãy chọn bài đăng đã được duyệt."
            );
            return;
        }

        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (session.getUserId().equals(room.getOwnerId())) {
            showMessage("Bạn không thể đặt lịch xem phòng của mình.");
            return;
        }

        BottomSheetBookAppointmentBinding sheet =
                BottomSheetBookAppointmentBinding.inflate(
                        getLayoutInflater()
                );

        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());

        bookingDialog = dialog;
        bookingUserId = session.getUserId();

        final String requestingUserId = bookingUserId;
        final String[] selectedSlotId = {null};

        dialog.setContentView(sheet.getRoot());

        sheet.tvBookingRoomTitle.setText(room.getTitle());
        sheet.btnChooseDate.setText("Chọn khung giờ");
        sheet.btnChooseTime.setText("Chưa chọn giờ");
        sheet.btnChooseTime.setEnabled(false);
        sheet.btnConfirmBooking.setText("Gửi yêu cầu");
        sheet.tvBookingError.setVisibility(View.GONE);

        sheet.edtBookingNote.setRawInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );
        sheet.edtBookingNote.setImeOptions(EditorInfo.IME_ACTION_DONE);

        sheet.edtBookingNote.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideBookingKeyboard(dialog, sheet.edtBookingNote);
                        return true;
                    }
                    return false;
                }
        );

        sheet.btnChooseDate.setOnClickListener(clicked -> {
            hideBookingKeyboard(dialog, sheet.edtBookingNote);
            showSlotPicker(room, sheet, selectedSlotId);
        });

        sheet.btnConfirmBooking.setOnClickListener(clicked ->
                confirmBooking(
                        room,
                        sheet,
                        dialog,
                        selectedSlotId[0],
                        requestingUserId
                )
        );

        dialog.setOnDismissListener(dismissed -> {
            closeSlotDialog();

            sheet.edtBookingNote.setOnEditorActionListener(null);
            sheet.btnChooseDate.setOnClickListener(null);
            sheet.btnConfirmBooking.setOnClickListener(null);

            if (bookingDialog == dialog) {
                bookingDialog = null;
                bookingUserId = null;
            }
        });

        dialog.show();
    }

    /** Đọc khung giờ đang mở trước mỗi lần chọn. */
    private void showSlotPicker(
            RoomCard room,
            BottomSheetBookAppointmentBinding sheet,
            String[] selectedSlotId
    ) {
        if (slotDialog != null || bookingDialog == null
                || !bookingDialog.isShowing()) {
            return;
        }

        List<HostViewingSlot> slots =
                DemoHostViewingRepository.getInstance()
                        .getOpenSlotsForListing(room.getListingId());

        if (slots.isEmpty()) {
            selectedSlotId[0] = null;
            sheet.btnChooseDate.setText("Chọn khung giờ");
            sheet.btnChooseTime.setText("Chưa chọn giờ");

            showBookingError(
                    sheet,
                    "Không có khung giờ đang mở trong tương lai. "
                            + "Bài đăng cũng có thể không còn hiển thị."
            );
            return;
        }

        String[] labels = new String[slots.size()];

        for (int index = 0; index < slots.size(); index++) {
            HostViewingSlot slot = slots.get(index);

            labels[index] = formatTime(
                    slot.getStartTimeMillis(), "dd/MM/yyyy HH:mm"
            ) + " → " + formatTime(
                    slot.getEndTimeMillis(), "dd/MM/yyyy HH:mm"
            );
        }

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Khung giờ xem phòng")
                        .setItems(labels, (interfaceDialog, which) -> {
                            if (bookingDialog == null
                                    || !bookingDialog.isShowing()) {
                                return;
                            }

                            HostViewingSlot slot = slots.get(which);
                            selectedSlotId[0] = slot.getId();

                            sheet.btnChooseDate.setText(formatTime(
                                    slot.getStartTimeMillis(),
                                    "dd/MM/yyyy"
                            ));

                            sheet.btnChooseTime.setText(
                                    formatTime(
                                            slot.getStartTimeMillis(),
                                            "HH:mm"
                                    ) + " → " + formatTime(
                                            slot.getEndTimeMillis(),
                                            "HH:mm dd/MM"
                                    )
                            );

                            sheet.tvBookingError.setVisibility(View.GONE);
                        })
                        .setNegativeButton("Đóng", null)
                        .create();

        slotDialog = dialog;

        dialog.setOnDismissListener(dismissed -> {
            if (slotDialog == dialog) {
                slotDialog = null;
            }
        });

        dialog.show();
    }

    /** Kiểm tra lại tài khoản và gửi yêu cầu qua repository dùng chung. */
    private void confirmBooking(
            RoomCard room,
            BottomSheetBookAppointmentBinding sheet,
            BottomSheetDialog dialog,
            @Nullable String slotId,
            String requestingUserId
    ) {
        if (!dialog.isShowing()
                || !sheet.btnConfirmBooking.isEnabled()
                || !checkViewingPermission()) {
            return;
        }

        String currentUserId = SessionRepository.getInstance()
                .getCurrentSession().getUserId();

        if (!requestingUserId.equals(currentUserId)) {
            closeBookingDialog();
            showMessage("Tài khoản đã thay đổi. Hãy mở lại bảng đặt lịch.");
            return;
        }

        hideBookingKeyboard(dialog, sheet.edtBookingNote);

        if (slotId == null || slotId.trim().isEmpty()) {
            showBookingError(sheet, "Bạn hãy chọn khung giờ xem phòng.");
            return;
        }

        String note = sheet.edtBookingNote.getText() == null
                ? ""
                : sheet.edtBookingNote.getText().toString().trim();

        if (note.codePointCount(0, note.length()) > 300) {
            showBookingError(
                    sheet,
                    "Ghi chú không được vượt quá 300 ký tự."
            );
            return;
        }

        sheet.btnConfirmBooking.setEnabled(false);

        try {
            DemoAppointmentRepository.getInstance().createRequest(
                    room.getListingId(), slotId, note
            );

            dialog.dismiss();

            showMessage(
                    "Đã gửi yêu cầu xem phòng. "
                            + "Bạn theo dõi trạng thái trong Lịch trình."
            );

        } catch (IllegalArgumentException | IllegalStateException exception) {
            sheet.btnConfirmBooking.setEnabled(true);
            showBookingError(sheet, exception.getMessage());
        }
    }

    /** Hiển thị lý do chưa thể gửi yêu cầu xem phòng. */
    private void showBookingError(
            BottomSheetBookAppointmentBinding sheet,
            @Nullable String message
    ) {
        sheet.tvBookingError.setText(
                message == null ? "Không thể gửi yêu cầu." : message
        );
        sheet.tvBookingError.setVisibility(View.VISIBLE);
    }

    /** Định dạng thời gian theo múi giờ hiện tại của thiết bị. */
    private String formatTime(long timeMillis, String pattern) {
        return new SimpleDateFormat(
                pattern, new Locale("vi", "VN")
        ).format(new Date(timeMillis));
    }

    /** Kiểm tra đăng nhập, bài đăng và chủ sở hữu trước khi mở chat. */
    private void openConversation() {
        if (SessionAccess.requireLogin()
                != SessionAccess.Result.ALLOWED) {
            showMessage("Bạn cần đăng nhập để nhắn tin với chủ trọ.");
            openScreen(new LoginFragment());
            return;
        }

        RoomCard room = getCurrentRoom();

        if (room == null) {
            displayRoomDetails();
            return;
        }

        String userId = SessionRepository.getInstance()
                .getCurrentSession().getUserId();

        if (userId.equals(room.getOwnerId())) {
            showMessage("Bạn không thể nhắn tin với chính mình.");
            return;
        }

        try {
            Conversation conversation = DemoChatRepository.getInstance()
                    .getOrCreateConversation(room);

            openScreen(ChatFragment.newInstance(conversation.getId()));

        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    /** Mở màn hình mới và giữ chi tiết phòng trong back stack. */
    private void openScreen(Fragment fragment) {
        if (binding == null
                || getParentFragmentManager().isStateSaved()) {
            return;
        }

        closeBookingDialog();

        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /** Đóng bàn phím của bảng đặt lịch và bỏ focus ô nhập. */
    private void hideBookingKeyboard(
            BottomSheetDialog dialog,
            View view
    ) {
        if (dialog.getWindow() == null) {
            return;
        }

        View focused = dialog.getCurrentFocus();

        if (focused != null) {
            focused.clearFocus();
        }

        new WindowInsetsControllerCompat(
                dialog.getWindow(), view
        ).hide(WindowInsetsCompat.Type.ime());
    }

    /** Đóng hộp thoại chọn khung giờ và gỡ listener. */
    private void closeSlotDialog() {
        if (slotDialog != null) {
            AlertDialog dialog = slotDialog;
            slotDialog = null;
            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
    }

    /** Đóng bảng gửi yêu cầu cùng hộp thoại chọn khung giờ. */
    private void closeBookingDialog() {
        closeSlotDialog();

        if (bookingDialog != null) {
            BottomSheetDialog dialog = bookingDialog;

            if (dialog.getWindow() != null) {
                hideBookingKeyboard(
                        dialog,
                        dialog.getWindow().getDecorView()
                );
            }

            dialog.dismiss();
        }

        bookingDialog = null;
        bookingUserId = null;
    }

    /** Hiển thị thông báo khi Fragment còn được gắn. */
    private void showMessage(@Nullable String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    message == null ? "Không thể thực hiện thao tác." : message,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /** Đóng hộp thoại và giải phóng tham chiếu giao diện. */
    @Override
    public void onDestroyView() {
        closeBookingDialog();

        if (binding != null) {
            binding.btnBack.setOnClickListener(null);
            binding.btnDetailSchedule.setOnClickListener(null);
            binding.btnDetailMessage.setOnClickListener(null);
            binding.imgRoomDetail.setImageDrawable(null);
        }

        binding = null;
        super.onDestroyView();
    }
}