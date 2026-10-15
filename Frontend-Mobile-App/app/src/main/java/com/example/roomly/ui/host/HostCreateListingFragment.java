package com.example.roomly.ui.host;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.roomly.ui.NavigationHelper;

import com.example.roomly.data.api.ApiClient;
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.HostRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostCreateListingBinding;
import com.example.roomly.util.ImageLoader;

import org.json.JSONArray;
import org.json.JSONObject;

/** Tạo bản nháp bài đăng cho phòng thuộc chủ trọ hiện tại. Bản nháp chưa được đăng công khai. */
public class HostCreateListingFragment extends Fragment {
    private HostSaveViewModel saveViewModel;
    private ListingFeeEditor feeEditor;
    private String draftFees;

    private static final String ARG_ROOM_ID = "listing_room_id";

    private FragmentHostCreateListingBinding binding;
    private String roomId;

    /** Tạo biểu mẫu bài đăng với ID phòng được chọn. */
    public static HostCreateListingFragment newInstance(String roomId) {
        HostCreateListingFragment fragment = new HostCreateListingFragment();

        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);

        return fragment;
    }

    /** Đọc ID phòng từ Bundle. */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) draftFees = savedInstanceState.getString("draft_fees");
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
    }

    /** Tạo giao diện biểu mẫu bằng ViewBinding. */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHostCreateListingBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    /** Đăng ký thao tác, xử lý bàn phím và quan sát quyền truy cập. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        feeEditor = new ListingFeeEditor(binding.listingFees.getRoot());
        try {
            feeEditor.load(draftFees == null ? new JSONArray() : new JSONArray(draftFees));
        } catch (org.json.JSONException error) {
            feeEditor.load(null);
        }
        observeSaveState();

        setupKeyboardInsets();

        binding.btnCreateListingBack.setOnClickListener(
                clickedView -> {
                    hideKeyboard();
                    getParentFragmentManager().popBackStack();
                });

        binding.btnSaveListing.setOnClickListener(clickedView -> saveDraft());

        binding.edtListingDescription.setSingleLine(false);
        binding.edtListingDescription.setImeOptions(EditorInfo.IME_ACTION_DONE);

        binding.edtListingDescription.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideKeyboard();
                        return true;
                    }

                    return false;
                });

        SessionRepository.getInstance()
                .getSessionState()
                .observe(getViewLifecycleOwner(), session -> displayRoom());
    }

    /**
     * Kiểm tra quyền và hiển thị phòng được chọn. Không điền lại các ô bài đăng để tránh ghi đè nội
     * dung đang nhập.
     */
    private void displayRoom() {
        if (binding == null) {
            return;
        }

        SessionState session = SessionRepository.getInstance().getCurrentSession();

        HostRoom room = HostRepository.getInstance().getMyRoomById(roomId);

        if (!session.isLoggedIn() || !session.hasRole(UserRole.HOST) || room == null) {
            binding.layoutCreateListingForm.setVisibility(View.GONE);
            binding.imgListingRoom.setImageDrawable(null);

            binding.tvListingRoomCode.setText("");
            binding.tvListingRoomName.setText("");
            binding.tvListingRoomAddress.setText("");

            hideKeyboard();

            showError(
                    "Không tìm thấy phòng thuộc tài khoản của bạn. "
                            + "Bạn quay lại danh sách nhé.");
            return;
        }

        binding.layoutCreateListingForm.setVisibility(View.VISIBLE);

        binding.tvListingRoomCode.setText("Mã phòng: " + room.getUnitCode());

        binding.tvListingRoomName.setText(room.getName());
        binding.tvListingRoomAddress.setText(room.getAddress());

        displayRoomImage(room);
    }

    /** Hiển thị ảnh của phòng được chọn. Ẩn ảnh nếu đường dẫn không còn đọc được. */
    private void displayRoomImage(HostRoom room) {
        binding.imgListingRoom.setImageDrawable(null);
        binding.imgListingRoom.setVisibility(View.GONE);

        String imageUri = room.getImageUri();

        if (imageUri == null || imageUri.trim().isEmpty()) {
            return;
        }

        try {
            ImageLoader.load(binding.imgListingRoom, imageUri, android.R.drawable.ic_menu_gallery);

            if (binding.imgListingRoom.getDrawable() != null) {
                binding.imgListingRoom.setVisibility(View.VISIBLE);
                binding.imgListingRoom.setContentDescription("Ảnh phòng " + room.getName());
            }

        } catch (SecurityException exception) {
            binding.imgListingRoom.setImageDrawable(null);
        }
    }

    /**
     * Kiểm tra quyền và biểu mẫu rồi lưu bản nháp lên máy chủ. Giữ nguyên nội dung nếu thao tác
     * thất bại.
     */
    private void saveDraft() {
        if (binding == null || !binding.btnSaveListing.isEnabled()) return;
        clearErrors();
        SessionState session = SessionRepository.getInstance().getCurrentSession();
        HostRoom room = HostRepository.getInstance().getMyRoomById(roomId);
        if (!session.isLoggedIn() || !session.hasRole(UserRole.HOST) || room == null) {
            displayRoom();
            return;
        }
        if (!session.isEmailVerified()) {
            showError("Bạn cần xác minh email ở trang Cá nhân trước khi lưu bài đăng.");
            return;
        }
        if (room.getAvailability() != RoomCard.Availability.AVAILABLE) {
            showError("Chỉ tạo bài đăng cho phòng đang trống.");
            return;
        }
        if (!validateForm()) return;
        if (!feeEditor.isLoaded()) {
            showError("Chưa tải được phụ phí. Hãy quay lại và thử lại.");
            return;
        }
        hideKeyboard();
        binding.btnSaveListing.setEnabled(false);
        JSONObject body =
                ApiClient.json(
                        "title",
                        readText(binding.edtListingTitle),
                        "description",
                        readText(binding.edtListingDescription),
                        "rent_vnd",
                        readText(binding.edtListingPrice),
                        "deposit_vnd",
                        readText(binding.edtListingDeposit),
                        "fees",
                        feeEditor.values());
        if (!saveViewModel.begin(SessionRepository.getInstance().getCurrentSession().getUserId())) return;
        HostRepository.getInstance()
                .saveListing(
                        roomId,
                        body,
                        saveViewModel::listingSaved,
                        saveViewModel::failed);
    }

    /**
     * Kiểm tra tiêu đề, giá thuê và nội dung bài đăng. Xử lý cả trường hợp số nhập vượt phạm vi
     * kiểu long.
     */
    private boolean validateForm() {
        boolean valid = true;
        EditText firstInvalid = null;

        if (readText(binding.edtListingTitle).isEmpty()
                || readText(binding.edtListingTitle).length() > 150) {
            binding.inputListingTitle.setError("Tiêu đề phải có từ 1 đến 150 ký tự.");

            firstInvalid = binding.edtListingTitle;
            valid = false;
        }

        String priceText = readText(binding.edtListingPrice);
        boolean validPrice = false;

        if (priceText.matches("[0-9]+")) {
            try {
                validPrice = Long.parseLong(priceText) > 0;
            } catch (NumberFormatException exception) {
                validPrice = false;
            }
        }

        if (!validPrice) {
            binding.inputListingPrice.setError("Nhập giá thuê là số nguyên lớn hơn 0 và hợp lệ.");

            if (firstInvalid == null) {
                firstInvalid = binding.edtListingPrice;
            }

            valid = false;
        }

        String depositText = readText(binding.edtListingDeposit);
        boolean validDeposit = false;
        if (depositText.matches("[0-9]+")) {
            try {
                validDeposit = Long.parseLong(depositText) >= 0;
            } catch (NumberFormatException exception) {
                validDeposit = false;
            }
        }
        if (!validDeposit) {
            binding.inputListingDeposit.setError(
                    "Nhập tiền cọc không âm; nhập 0 nếu không yêu cầu cọc.");
            if (firstInvalid == null) firstInvalid = binding.edtListingDeposit;
            valid = false;
        }

        if (readText(binding.edtListingDescription).isEmpty()
                || readText(binding.edtListingDescription).length() > 5000) {
            binding.inputListingDescription.setError("Nội dung phải có từ 1 đến 5000 ký tự.");

            if (firstInvalid == null) {
                firstInvalid = binding.edtListingDescription;
            }

            valid = false;
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocus();
        }

        return valid;
    }

    /** Đọc nội dung ô nhập và loại bỏ khoảng trắng ở hai đầu. */
    private String readText(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /** Xóa các thông báo lỗi cũ trước khi kiểm tra lại. */
    private void clearErrors() {
        binding.inputListingTitle.setError(null);
        binding.inputListingPrice.setError(null);
        binding.inputListingDeposit.setError(null);
        binding.inputListingDescription.setError(null);

        binding.tvCreateListingError.setText("");
        binding.tvCreateListingError.setVisibility(View.GONE);
    }

    /** Hiển thị thông báo lỗi chung. */
    private void showError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvCreateListingError.setText(message == null ? "Không thể lưu bản nháp." : message);

        binding.tvCreateListingError.setVisibility(View.VISIBLE);
    }

    /** Thêm khoảng trống khi bàn phím mở để biểu mẫu không bị che. */
    private void setupKeyboardInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                binding.getRoot(),
                (view, insets) -> {
                    int keyboardBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

                    int systemBottom =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

                    view.setPadding(0, 0, 0, Math.max(0, keyboardBottom - systemBottom));

                    return insets;
                });

        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    /** Đóng bàn phím và bỏ focus khỏi ô đang nhập. */
    private void hideKeyboard() {
        if (binding == null) {
            return;
        }

        new WindowInsetsControllerCompat(requireActivity().getWindow(), binding.getRoot())
                .hide(WindowInsetsCompat.Type.ime());

        View focusedView = binding.getRoot().findFocus();

        if (focusedView != null) {
            focusedView.clearFocus();
        }

        binding.getRoot().requestFocus();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        if (feeEditor != null && feeEditor.isLoaded()) draftFees = feeEditor.values().toString();
        state.putString("draft_fees", draftFees);
        super.onSaveInstanceState(state);
    }

    /** Gỡ listener, giải phóng ảnh và binding khi giao diện bị hủy. */
    @Override
    public void onDestroyView() {
        if (feeEditor != null) {
            if (feeEditor.isLoaded()) draftFees = feeEditor.values().toString();
            feeEditor.close();
            feeEditor = null;
        }
        if (binding != null) {
            binding.btnCreateListingBack.setOnClickListener(null);
            binding.btnSaveListing.setOnClickListener(null);
            binding.edtListingDescription.setOnEditorActionListener(null);
            binding.imgListingRoom.setImageDrawable(null);

            ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), null);
        }

        binding = null;

        super.onDestroyView();
    }

    private void observeSaveState() {
        saveViewModel = new ViewModelProvider(this).get(HostSaveViewModel.class);
        saveViewModel.phase().observe(getViewLifecycleOwner(), phase -> {
            if (binding == null) return;
            binding.btnSaveListing.setEnabled(!saveViewModel.blocksNewSave());
            binding.progressCreateListing.setVisibility(HostSaveViewModel.SAVING.equals(phase) ? View.VISIBLE : View.GONE);
            feeEditor.setEnabled(!saveViewModel.blocksNewSave());

            if (HostSaveViewModel.SAVED.equals(phase)) {
                NavigationHelper.whenResumed(this, () -> {
                    String message = saveViewModel.consumeSuccess(
                            SessionRepository.getInstance().getCurrentSession().getUserId());
                    if (message == null || binding == null) return;
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    getParentFragmentManager().popBackStack();
                });
            } else if (HostSaveViewModel.ERROR.equals(phase) || HostSaveViewModel.UNCERTAIN.equals(phase)) {
                showError(saveViewModel.message());
            }
        });
    }

}
