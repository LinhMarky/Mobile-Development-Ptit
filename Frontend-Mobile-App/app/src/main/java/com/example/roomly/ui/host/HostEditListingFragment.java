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

import com.example.roomly.data.model.HostListing;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.repository.HostRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostEditListingBinding;

import org.json.JSONObject;
import org.json.JSONArray;

/** Chỉnh sửa bài đăng và phụ phí rồi lưu lên backend; tin cần được duyệt lại. */
public class HostEditListingFragment extends Fragment {
    private HostSaveViewModel saveViewModel;
    private ListingFeeEditor feeEditor;

    private static final String ARG_LISTING_ID = "edit_listing_id";
    private static final String STATE_DRAFT = "edit_listing_draft";

    private FragmentHostEditListingBinding binding;

    private String listingId;
    private Bundle draft;

    /** Tạo màn hình chỉnh sửa và truyền mã bản nháp qua Bundle. */
    public static HostEditListingFragment newInstance(String listingId) {
        HostEditListingFragment fragment = new HostEditListingFragment();

        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        fragment.setArguments(args);

        return fragment;
    }

    /** Đọc mã bài đăng và khôi phục nội dung đang nhập nếu có. */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            listingId = args.getString(ARG_LISTING_ID);
        }

        if (savedInstanceState != null) {
            draft = savedInstanceState.getBundle(STATE_DRAFT);
        }
    }

    /** Tạo giao diện từ fragment_host_edit_listing.xml. */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHostEditListingBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    /** Hiển thị nội dung và đăng ký thao tác quay lại, lưu, đóng bàn phím. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        feeEditor = new ListingFeeEditor(binding.listingFees.getRoot());
        observeSaveState();

        displayInitialData();
        if (getAccessibleListing() == null) {
            HostRepository.getInstance().fetchListings(rows -> {
                if (binding != null) displayInitialData();
            }, message -> { if (binding != null) showError(message); });
        }
        setupKeyboardInsets();

        binding.btnEditListingBack.setOnClickListener(
                clickedView -> {
                    hideKeyboard();
                    getParentFragmentManager().popBackStack();
                });

        binding.btnEditListingSave.setOnClickListener(clickedView -> saveChanges());

        binding.edtEditListingDescription.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        hideKeyboard();
                        return true;
                    }

                    return false;
                });

        SessionRepository.getInstance()
                .getSessionState()
                .observe(getViewLifecycleOwner(), session -> renderAccess());
    }

    /**
     * Hiển thị dữ liệu ban đầu hoặc nội dung đang nhập đã được giữ lại. Không điền lại dữ liệu mỗi
     * khi trạng thái phiên thay đổi.
     */
    private void displayInitialData() {
        HostListing listing = getAccessibleListing();

        if (listing == null) {
            renderAccess();
            return;
        }

        String savedDeposit =
                listing.getDepositVnd() == null ? "" : String.valueOf(listing.getDepositVnd());

        if (draft != null) {
            binding.edtEditListingTitle.setText(draft.getString("title", ""));

            binding.edtEditListingPrice.setText(draft.getString("price", ""));

            binding.edtEditListingDeposit.setText(draft.getString("deposit", savedDeposit));

            binding.edtEditListingDescription.setText(draft.getString("description", ""));
        } else {
            binding.edtEditListingTitle.setText(listing.getTitle());

            binding.edtEditListingPrice.setText(String.valueOf(listing.getMonthlyRent()));

            binding.edtEditListingDeposit.setText(savedDeposit);

            binding.edtEditListingDescription.setText(listing.getDescription());
        }

        try {
            String savedFees = draft == null ? null : draft.getString("fees");
            feeEditor.load(savedFees == null ? HostRepository.getInstance().getListingFees(listingId) : new JSONArray(savedFees));
        } catch (org.json.JSONException error) {
            feeEditor.load(null);
        }
        renderAccess();
    }

    /** Lấy bản nháp thuộc tài khoản có quyền chủ trọ hiện tại. */
    @Nullable
    private HostListing getAccessibleListing() {
        SessionState session = SessionRepository.getInstance().getCurrentSession();
        if (!session.isLoggedIn() || !session.hasRole(UserRole.HOST)) return null;
        HostListing listing = HostRepository.getInstance().getMyListingById(listingId);
        if (listing == null || !listing.getStatus().canEdit()) return null;
        HostRoom room = HostRepository.getInstance().getMyRoomById(listing.getRoomId());
        return room != null && room.getAvailability() == RoomCard.Availability.AVAILABLE
                ? listing
                : null;
    }

    /**
     * Kiểm tra quyền truy cập và hiển thị thông tin phòng liên kết. Ẩn biểu mẫu nếu bản nháp hoặc
     * phòng không còn truy cập được.
     */
    private void renderAccess() {
        if (binding == null) {
            return;
        }

        HostListing listing = getAccessibleListing();

        HostRoom room =
                listing == null
                        ? null
                        : HostRepository.getInstance().getMyRoomById(listing.getRoomId());

        if (listing == null || room == null) {
            binding.layoutEditListingForm.setVisibility(View.GONE);
            binding.tvEditListingRoomCode.setText("");
            binding.tvEditListingRoomName.setText("");

            hideKeyboard();

            showError("Không tìm thấy bản nháp hoặc phòng " + "thuộc tài khoản hiện tại.");
            return;
        }

        binding.layoutEditListingForm.setVisibility(View.VISIBLE);
        binding.tvEditListingRoomCode.setText(room.getUnitCode());
        binding.tvEditListingRoomName.setText(room.getName());
    }

    /**
     * Kiểm tra tiêu đề, giá thuê, mô tả và lưu nội dung bản nháp. Repository kiểm tra lại quyền sở
     * hữu trước khi cập nhật.
     */
    private void saveChanges() {
        if (binding == null || !binding.btnEditListingSave.isEnabled()) {
            return;
        }

        clearErrors();
        binding.inputEditListingDeposit.setError(null);

        SessionState session = SessionRepository.getInstance().getCurrentSession();

        if (!session.isLoggedIn()) {
            showError("Bạn cần đăng nhập để chỉnh sửa bản nháp.");
            return;
        }

        HostListing listing = getAccessibleListing();

        if (listing == null) {
            renderAccess();
            return;
        }

        if (!session.isEmailVerified()) {
            showError("Bạn hãy quay về trang Cá nhân " + "để xác minh email trước khi lưu.");
            hideKeyboard();
            return;
        }

        String title = readText(binding.edtEditListingTitle).trim();
        String priceText = readText(binding.edtEditListingPrice).trim();
        String depositText = readText(binding.edtEditListingDeposit).trim();
        String description = readText(binding.edtEditListingDescription).trim();

        if (title.isEmpty() || title.length() > 150) {
            binding.inputEditListingTitle.setError("Tiêu đề phải có từ 1 đến 150 ký tự.");
            binding.edtEditListingTitle.requestFocus();
            return;
        }

        long monthlyRent;

        try {
            if (!priceText.matches("[0-9]+")) {
                throw new NumberFormatException();
            }

            monthlyRent = Long.parseLong(priceText);

            if (monthlyRent <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            binding.inputEditListingPrice.setError("Nhập giá lớn hơn 0, ví dụ 3500000.");
            binding.edtEditListingPrice.requestFocus();
            return;
        }

        if (depositText.isEmpty()) {
            binding.inputEditListingDeposit.setError("Nhập 0 nếu không yêu cầu cọc.");
            binding.edtEditListingDeposit.requestFocus();
            return;
        }
        long depositVnd;

        if (!depositText.matches("[0-9]+")) {
            binding.inputEditListingDeposit.setError("Tiền cọc phải là số nguyên không âm.");
            binding.edtEditListingDeposit.requestFocus();
            return;
        }
        try {
            depositVnd = Long.parseLong(depositText);
        } catch (NumberFormatException exception) {
            binding.inputEditListingDeposit.setError("Số tiền cọc quá lớn. Bạn hãy nhập lại.");
            binding.edtEditListingDeposit.requestFocus();
            return;
        }

        if (description.isEmpty() || description.length() > 5000) {
            binding.inputEditListingDescription.setError(
                    "Nội dung bài đăng phải có từ 1 đến 5000 ký tự.");
            binding.edtEditListingDescription.requestFocus();
            return;
        }

        if (!feeEditor.isLoaded()) {
            showError("Chưa tải được phụ phí. Hãy quay lại và thử lại.");
            return;
        }
        hideKeyboard();

        JSONObject body =
                HostRepository.getInstance()
                        .listingBody(title, monthlyRent, depositVnd, description, feeEditor.values());
        if (!saveViewModel.begin(SessionRepository.getInstance().getCurrentSession().getUserId())) return;
        HostRepository.getInstance()
                .saveListing(
                        listing.getRoomId(),
                        body,
                        saveViewModel::listingSaved,
                        saveViewModel::failed);
    }

    /** Đọc nội dung ô nhập và loại bỏ khoảng trắng ở hai đầu. */
    private String readText(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    /** Xóa các thông báo lỗi trước khi kiểm tra lại dữ liệu. */
    private void clearErrors() {
        binding.inputEditListingTitle.setError(null);
        binding.inputEditListingPrice.setError(null);
        binding.inputEditListingDeposit.setError(null);
        binding.inputEditListingDescription.setError(null);

        binding.tvEditListingError.setText("");
        binding.tvEditListingError.setVisibility(View.GONE);
    }

    /** Hiển thị thông báo lỗi chung của màn hình. */
    private void showError(@Nullable String message) {
        if (binding == null) {
            return;
        }

        binding.tvEditListingError.setText(
                message == null || message.trim().isEmpty()
                        ? "Không thể cập nhật bản nháp."
                        : message);

        binding.tvEditListingError.setVisibility(View.VISIBLE);
    }

    /** Hiển thị tiến trình và khóa nút lưu trong lúc cập nhật. */
    private void setSaving(boolean saving) {
        if (binding == null) {
            return;
        }

        binding.progressEditListing.setVisibility(saving ? View.VISIBLE : View.GONE);

        binding.btnEditListingSave.setEnabled(!saving);
    }

    /**
     * Thêm khoảng trống khi bàn phím mở để không che biểu mẫu. Giữ nguyên khoảng đệm ban đầu của
     * giao diện.
     */
    private void setupKeyboardInsets() {
        View root = binding.getRoot();

        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {
                    int keyboardBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

                    int systemBottom =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

                    view.setPadding(
                            initialLeft,
                            initialTop,
                            initialRight,
                            initialBottom + Math.max(0, keyboardBottom - systemBottom));

                    return insets;
                });

        ViewCompat.requestApplyInsets(root);
    }

    /** Đóng bàn phím và bỏ trạng thái nhập của ô đang được chọn. */
    private void hideKeyboard() {
        if (binding == null) {
            return;
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(requireActivity().getWindow(), binding.getRoot());

        controller.hide(WindowInsetsCompat.Type.ime());

        View focusedView = binding.getRoot().findFocus();

        if (focusedView != null) {
            focusedView.clearFocus();
        }
    }

    /** Giữ lại nội dung đang nhập để khôi phục khi giao diện tạo lại. */
    private void captureDraft() {
        if (binding == null) {
            return;
        }

        draft = new Bundle();

        draft.putString("title", readText(binding.edtEditListingTitle));

        draft.putString("price", readText(binding.edtEditListingPrice));

        draft.putString("deposit", readText(binding.edtEditListingDeposit));

        draft.putString("description", readText(binding.edtEditListingDescription));
        if (feeEditor != null && feeEditor.isLoaded()) draft.putString("fees", feeEditor.values().toString());
    }

    /** Lưu nội dung đang nhập khi Android cần khôi phục Fragment. */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        captureDraft();

        if (draft != null) {
            outState.putBundle(STATE_DRAFT, draft);
        }

        super.onSaveInstanceState(outState);
    }

    /** Giữ nội dung đang nhập và giải phóng các tham chiếu giao diện. */
    @Override
    public void onDestroyView() {
        captureDraft();
        if (feeEditor != null) {
            feeEditor.close();
            feeEditor = null;
        }

        if (binding != null) {
            binding.btnEditListingBack.setOnClickListener(null);
            binding.btnEditListingSave.setOnClickListener(null);

            binding.edtEditListingDescription.setOnEditorActionListener(null);

            ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), null);
        }

        binding = null;

        super.onDestroyView();
    }

    private void observeSaveState() {
        saveViewModel = new ViewModelProvider(this).get(HostSaveViewModel.class);
        saveViewModel.phase().observe(getViewLifecycleOwner(), phase -> {
            if (binding == null) return;
            setSaving(HostSaveViewModel.SAVING.equals(phase));
            binding.btnEditListingSave.setEnabled(!saveViewModel.blocksNewSave());
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
