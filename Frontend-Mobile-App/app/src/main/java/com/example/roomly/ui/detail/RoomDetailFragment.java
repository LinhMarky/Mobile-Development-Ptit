package com.example.roomly.ui.detail;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.R;
import com.example.roomly.data.model.Conversation;
import com.example.roomly.data.model.HostViewingSlot;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.RoomImage;
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
import com.example.roomly.ui.booking.BookingCreateFragment;
import com.example.roomly.ui.messages.ChatFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private static final String ARG_DEPOSIT = "room_deposit";
    private static final String ARG_AVAILABILITY = "room_availability";
    private static final String ARG_FEES = "room_fees";
    private static final String ARG_IMAGES = "room_images";

    private static final String STATE_IMAGE_POSITION = "image_position";

    private FragmentRoomDetailBinding binding;

    private RoomImageAdapter imageAdapter;
    private LinearLayoutManager imageLayoutManager;
    private PagerSnapHelper imageSnapHelper;
    private RecyclerView.OnScrollListener imageScrollListener;

    private int imagePosition;
    private List<RoomImage> displayedImages = new ArrayList<>();

    private BottomSheetDialog bookingDialog;
    private AlertDialog slotDialog;
    private String bookingUserId;

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

        if (room.getDepositVnd() != null) {
            args.putLong(ARG_DEPOSIT, room.getDepositVnd());
        }

        args.putString(
                ARG_AVAILABILITY,
                room.getAvailability().name()
        );

        ArrayList<Bundle> feeBundles = new ArrayList<>();

        for (RoomCard.Fee fee : room.getFees()) {
            Bundle item = new Bundle();
            item.putString("name", fee.name);
            item.putLong("amount", fee.amountVnd);
            item.putString("unit", fee.unit);
            feeBundles.add(item);
        }

        args.putParcelableArrayList(ARG_FEES, feeBundles);

        ArrayList<Bundle> imageBundles = new ArrayList<>();

        for (RoomImage image : room.getImages()) {
            Bundle item = new Bundle();
            item.putInt("drawable", image.getDrawableResId());
            item.putString("uri", image.getUri());
            item.putString("description", image.getDescription());
            imageBundles.add(item);
        }

        args.putParcelableArrayList(ARG_IMAGES, imageBundles);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            imagePosition = savedInstanceState.getInt(
                    STATE_IMAGE_POSITION,
                    0
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
        binding = FragmentRoomDetailBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupImageGallery();

        binding.btnBack.setOnClickListener(
                clicked -> getParentFragmentManager().popBackStack()
        );

        binding.btnDetailSchedule.setOnClickListener(
                clicked -> showBookingDialog()
        );

        binding.btnDetailMessage.setOnClickListener(
                clicked -> openConversation()
        );

        binding.btnDetailRequestBooking.setOnClickListener(
                clicked -> openBookingForm()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(getViewLifecycleOwner(), session -> {
                    if (bookingDialog != null) {
                        SessionState current = SessionRepository
                                .getInstance()
                                .getCurrentSession();

                        if (!current.isLoggedIn()
                                || !current.hasRole(UserRole.TENANT)
                                || !current.isEmailVerified()
                                || bookingUserId == null
                                || !bookingUserId.equals(current.getUserId())) {
                            closeBookingDialog();
                        }
                    }

                    displayRoomDetails();
                });

        displayRoomDetails();
    }

    @Override
    public void onResume() {
        super.onResume();
        displayRoomDetails();
    }

    private void setupImageGallery() {
        imageAdapter = new RoomImageAdapter();

        imageLayoutManager = new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        );

        binding.rvDetailImages.setLayoutManager(imageLayoutManager);
        binding.rvDetailImages.setAdapter(imageAdapter);
        binding.rvDetailImages.setItemAnimator(null);

        imageSnapHelper = new PagerSnapHelper();
        imageSnapHelper.attachToRecyclerView(binding.rvDetailImages);

        imageScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(
                    @NonNull RecyclerView recyclerView,
                    int newState
            ) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    rememberImagePosition();
                    updateImageCounter();
                }
            }
        };

        binding.rvDetailImages.addOnScrollListener(
                imageScrollListener
        );
    }

    private void rememberImagePosition() {
        if (imageLayoutManager == null || imageSnapHelper == null) {
            return;
        }

        View snappedView = imageSnapHelper.findSnapView(
                imageLayoutManager
        );

        if (snappedView != null) {
            int position = imageLayoutManager.getPosition(
                    snappedView
            );

            if (position != RecyclerView.NO_POSITION) {
                imagePosition = position;
            }
        }
    }

    private void updateImageCounter() {
        if (binding == null || imageAdapter == null) {
            return;
        }

        int count = imageAdapter.getItemCount();

        if (count == 0) {
            binding.tvDetailImageCounter.setVisibility(View.GONE);
            binding.tvDetailImageCounter.setText("");
            return;
        }

        imagePosition = Math.max(
                0,
                Math.min(imagePosition, count - 1)
        );

        binding.tvDetailImageCounter.setText(
                (imagePosition + 1) + "/" + count
        );

        binding.tvDetailImageCounter.setContentDescription(
                "Ảnh " + (imagePosition + 1) + " trên " + count
        );

        binding.tvDetailImageCounter.setVisibility(View.VISIBLE);
    }

    private boolean sameImages(
            List<RoomImage> first,
            List<RoomImage> second
    ) {
        if (first.size() != second.size()) {
            return false;
        }

        for (int index = 0; index < first.size(); index++) {
            RoomImage a = first.get(index);
            RoomImage b = second.get(index);

            if (a.getDrawableResId() != b.getDrawableResId()
                    || !a.getUri().equals(b.getUri())
                    || !a.getDescription().equals(b.getDescription())) {
                return false;
            }
        }

        return true;
    }

    private void displayRoomImages(RoomCard room) {
        List<RoomImage> images = room.getImages();

        binding.imgRoomDetail.setImageDrawable(null);
        binding.imgRoomDetail.setVisibility(View.GONE);

        boolean hasImages = !images.isEmpty();

        binding.rvDetailImages.setVisibility(
                hasImages ? View.VISIBLE : View.GONE
        );

        binding.tvDetailNoImages.setVisibility(
                hasImages ? View.GONE : View.VISIBLE
        );

        binding.tvDetailNoImages.setText("Phòng chưa có ảnh");

        if (!sameImages(displayedImages, images)) {
            displayedImages = new ArrayList<>(images);
            imageAdapter.submitImages(images);

            imagePosition = images.isEmpty()
                    ? 0
                    : Math.max(
                    0,
                    Math.min(imagePosition, images.size() - 1)
            );

            imageLayoutManager.scrollToPositionWithOffset(
                    imagePosition,
                    0
            );
        }

        updateImageCounter();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        rememberImagePosition();
        outState.putInt(STATE_IMAGE_POSITION, imagePosition);
        super.onSaveInstanceState(outState);
    }

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
            // Giữ loại phòng mặc định.
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

        if (args.containsKey(ARG_DEPOSIT)) {
            room.setDepositVnd(args.getLong(ARG_DEPOSIT));
        }

        try {
            room.setAvailability(
                    RoomCard.Availability.valueOf(
                            args.getString(ARG_AVAILABILITY, "UNKNOWN")
                    )
            );
        } catch (IllegalArgumentException ignored) {
            room.setAvailability(RoomCard.Availability.UNKNOWN);
        }

        ArrayList<Bundle> feeBundles =
                args.getParcelableArrayList(ARG_FEES);

        List<RoomCard.Fee> restoredFees = new ArrayList<>();

        if (feeBundles != null) {
            for (Bundle item : feeBundles) {
                restoredFees.add(
                        new RoomCard.Fee(
                                item.getString("name", "Phí khác"),
                                item.getLong("amount"),
                                item.getString("unit", "")
                        )
                );
            }
        }

        room.setFees(restoredFees);

        ArrayList<Bundle> imageBundles =
                args.getParcelableArrayList(ARG_IMAGES);

        List<RoomImage> restoredImages = new ArrayList<>();

        if (imageBundles != null) {
            for (Bundle item : imageBundles) {
                int drawable = item.getInt("drawable", 0);
                String uri = item.getString("uri", "");
                String description = item.getString(
                        "description",
                        "Ảnh phòng"
                );

                if (drawable > 0) {
                    restoredImages.add(
                            RoomImage.fromDrawable(
                                    drawable,
                                    description
                            )
                    );
                } else if (!uri.isEmpty()) {
                    restoredImages.add(
                            RoomImage.fromUri(
                                    uri,
                                    description
                            )
                    );
                }
            }
        }

        room.setImages(restoredImages);

        String areaText = args.getString(ARG_AREA, "");

        if (!areaText.isEmpty()) {
            try {
                room.setArea(new BigDecimal(areaText));
            } catch (NumberFormatException ignored) {
                // Không tự tạo diện tích.
            }
        }

        return room;
    }

    private boolean isOwnRoom(RoomCard room) {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        return session.isLoggedIn()
                && !room.getOwnerId().isEmpty()
                && session.getUserId().equals(room.getOwnerId());
    }

    private void displayRoomDetails() {
        if (binding == null) {
            return;
        }

        RoomCard room = getCurrentRoom();

        if (room == null) {
            showUnavailableRoom();
            return;
        }

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

        binding.tvDetailAvailability.setText(
                room.getAvailabilityLabel()
        );

        binding.tvDetailDeposit.setText(
                "Tiền cọc: " + room.getDepositLabel()
        );

        renderFees(room);
        displayRoomImages(room);

        boolean blocked = isViewingBlocked(room);
        boolean ownRoom = isOwnRoom(room);

        binding.btnDetailMessage.setEnabled(!ownRoom);
        binding.btnDetailSchedule.setEnabled(!blocked && !ownRoom);

        binding.btnDetailSchedule.setText(
                blocked
                        ? room.getAvailabilityLabel()
                        : "Hẹn xem phòng"
        );

        boolean canRequest =
                room.getAvailability() == RoomCard.Availability.AVAILABLE
                        && !ownRoom;

        binding.btnDetailRequestBooking.setEnabled(canRequest);

        if (ownRoom) {
            binding.btnDetailRequestBooking.setText(
                    "Đây là phòng của bạn"
            );
        } else if (canRequest) {
            binding.btnDetailRequestBooking.setText("Yêu cầu thuê");
        } else {
            binding.btnDetailRequestBooking.setText(
                    room.getAvailabilityLabel()
            );
        }

        if (blocked || ownRoom) {
            closeBookingDialog();
        }
    }

    /**
     * Mở biểu mẫu xem trước.
     * Chưa gửi booking nên cho phép khách thử giao diện.
     */
    private void openBookingForm() {
        RoomCard room = getCurrentRoom();

        if (room == null) {
            displayRoomDetails();
            return;
        }

        if (room.getAvailability() != RoomCard.Availability.AVAILABLE) {
            showMessage(
                    "Phòng chưa ở trạng thái còn trống."
            );
            displayRoomDetails();
            return;
        }

        if (isOwnRoom(room)) {
            showMessage(
                    "Bạn không thể yêu cầu thuê phòng của mình."
            );
            return;
        }

        openScreen(BookingCreateFragment.newInstance(room));
    }

    private boolean isViewingBlocked(RoomCard room) {
        return room.getAvailability() == RoomCard.Availability.HELD
                || room.getAvailability() == RoomCard.Availability.RENTED;
    }

    private void renderFees(RoomCard room) {
        binding.layoutDetailFees.removeAllViews();

        if (room.getFees().isEmpty()) {
            addFeeLine(
                    "Chủ trọ chưa cung cấp thông tin các khoản phí."
            );
            return;
        }

        for (RoomCard.Fee fee : room.getFees()) {
            addFeeLine(fee.name + ": " + fee.getDisplayAmount());
        }
    }

    private void addFeeLine(String text) {
        TextView line = new TextView(requireContext());

        line.setText(text);
        line.setTextSize(14);
        line.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.roomly_text_primary
                )
        );

        int padding = Math.round(
                8 * getResources().getDisplayMetrics().density
        );

        line.setPadding(0, padding, 0, padding);
        binding.layoutDetailFees.addView(line);
    }

    private void showUnavailableRoom() {
        closeBookingDialog();

        binding.imgRoomDetail.setImageDrawable(null);
        binding.imgRoomDetail.setVisibility(View.GONE);
        binding.rvDetailImages.setVisibility(View.GONE);
        binding.tvDetailImageCounter.setVisibility(View.GONE);
        binding.tvDetailNoImages.setVisibility(View.VISIBLE);
        binding.tvDetailNoImages.setText("Bài đăng không còn hiển thị");

        imageAdapter.submitImages(null);
        displayedImages.clear();
        imagePosition = 0;

        binding.tvDetailTitle.setText("Bài đăng không còn hiển thị");
        binding.tvDetailPrice.setText("");
        binding.tvDetailAddress.setText("");
        binding.tvDetailAmenities.setText("");

        binding.tvDetailDescription.setText(
                "Bài đăng có thể đã bị ẩn hoặc dữ liệu mẫu đã mất. "
                        + "Bạn quay lại Khám phá để chọn phòng khác."
        );

        binding.tvDetailLandlordName.setText("Chưa cung cấp");
        binding.tvDetailAvailability.setText(
                "Bài đăng không còn hiển thị"
        );
        binding.tvDetailDeposit.setText("Tiền cọc: Chưa cung cấp");
        binding.layoutDetailFees.removeAllViews();

        binding.btnDetailSchedule.setText("Không thể đặt lịch");
        binding.btnDetailMessage.setEnabled(false);
        binding.btnDetailSchedule.setEnabled(false);

        binding.btnDetailRequestBooking.setText("Không thể yêu cầu thuê");
        binding.btnDetailRequestBooking.setEnabled(false);
    }

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

            openScreen(
                    VerifyEmailFragment.newInstance(
                            SessionRepository.getInstance()
                                    .getCurrentSession()
                                    .getEmail()
                    )
            );

        } else {
            showMessage("Tài khoản chưa có quyền người thuê.");
        }

        return false;
    }

    private void showBookingDialog() {
        if (binding == null
                || bookingDialog != null
                || !checkViewingPermission()) {
            return;
        }

        RoomCard room = getCurrentRoom();

        if (room == null) {
            displayRoomDetails();
            return;
        }

        if (isViewingBlocked(room)) {
            showMessage(room.getAvailabilityLabel());
            return;
        }

        if (isOwnRoom(room)) {
            showMessage("Bạn không thể đặt lịch xem phòng của mình.");
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

        sheet.btnConfirmBooking.setOnClickListener(
                clicked -> confirmBooking(
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

    private void showSlotPicker(
            RoomCard room,
            BottomSheetBookAppointmentBinding sheet,
            String[] selectedSlotId
    ) {
        if (slotDialog != null
                || bookingDialog == null
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
                    slot.getStartTimeMillis(),
                    "dd/MM/yyyy HH:mm"
            ) + " → " + formatTime(
                    slot.getEndTimeMillis(),
                    "dd/MM/yyyy HH:mm"
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

                            sheet.btnChooseDate.setText(
                                    formatTime(
                                            slot.getStartTimeMillis(),
                                            "dd/MM/yyyy"
                                    )
                            );

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

    private void confirmBooking(
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
                .getCurrentSession()
                .getUserId();

        if (!requestingUserId.equals(currentUserId)) {
            closeBookingDialog();
            showMessage(
                    "Tài khoản đã thay đổi. Hãy mở lại bảng đặt lịch."
            );
            return;
        }

        RoomCard room = getCurrentRoom();

        if (room == null || isViewingBlocked(room) || isOwnRoom(room)) {
            dialog.dismiss();
            displayRoomDetails();
            showMessage("Phòng không còn phù hợp để đặt lịch.");
            return;
        }

        hideBookingKeyboard(dialog, sheet.edtBookingNote);

        if (slotId == null || slotId.trim().isEmpty()) {
            showBookingError(
                    sheet,
                    "Bạn hãy chọn khung giờ xem phòng."
            );
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
            DemoAppointmentRepository.getInstance()
                    .createRequest(
                            room.getListingId(),
                            slotId,
                            note
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

    private void showBookingError(
            BottomSheetBookAppointmentBinding sheet,
            @Nullable String message
    ) {
        sheet.tvBookingError.setText(
                message == null ? "Không thể gửi yêu cầu." : message
        );
        sheet.tvBookingError.setVisibility(View.VISIBLE);
    }

    private String formatTime(long timeMillis, String pattern) {
        return new SimpleDateFormat(
                pattern,
                new Locale("vi", "VN")
        ).format(new Date(timeMillis));
    }

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

        if (isOwnRoom(room)) {
            showMessage("Bạn không thể nhắn tin với chính mình.");
            return;
        }

        try {
            Conversation conversation =
                    DemoChatRepository.getInstance()
                            .getOrCreateConversation(room);

            openScreen(
                    ChatFragment.newInstance(conversation.getId())
            );

        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void openScreen(Fragment fragment) {
        if (binding == null
                || getParentFragmentManager().isStateSaved()) {
            return;
        }

        closeBookingDialog();

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

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
                dialog.getWindow(),
                view
        ).hide(WindowInsetsCompat.Type.ime());
    }

    private void closeSlotDialog() {
        if (slotDialog != null) {
            AlertDialog dialog = slotDialog;
            slotDialog = null;
            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
    }

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

    private void showMessage(@Nullable String message) {
        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    message == null
                            ? "Không thể thực hiện thao tác."
                            : message,
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onDestroyView() {
        closeBookingDialog();
        rememberImagePosition();

        if (binding != null) {
            binding.btnBack.setOnClickListener(null);
            binding.btnDetailSchedule.setOnClickListener(null);
            binding.btnDetailMessage.setOnClickListener(null);
            binding.btnDetailRequestBooking.setOnClickListener(null);

            if (imageScrollListener != null) {
                binding.rvDetailImages.removeOnScrollListener(
                        imageScrollListener
                );
            }

            if (imageSnapHelper != null) {
                imageSnapHelper.attachToRecyclerView(null);
            }

            binding.rvDetailImages.setAdapter(null);
            binding.imgRoomDetail.setImageDrawable(null);
        }

        imageAdapter = null;
        imageLayoutManager = null;
        imageSnapHelper = null;
        imageScrollListener = null;
        displayedImages.clear();
        binding = null;

        super.onDestroyView();
    }
}