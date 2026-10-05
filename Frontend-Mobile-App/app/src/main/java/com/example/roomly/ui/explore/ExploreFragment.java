package com.example.roomly.ui.explore;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.repository.DemoRoomRepository;
import com.example.roomly.databinding.BottomSheetRoomFilterBinding;
import com.example.roomly.databinding.FragmentExploreBinding;
import com.example.roomly.ui.detail.RoomDetailFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExploreFragment extends Fragment {

    private static final String KEY_ROOM_TYPE = "selected_room_type";
    private static final String KEY_MIN_PRICE = "min_price";
    private static final String KEY_MAX_PRICE = "max_price";
    private static final String KEY_MIN_AREA = "min_area";
    private static final String KEY_MAX_AREA = "max_area";

    // Binding của màn hình Khám phá.
    private FragmentExploreBinding binding;

    private RoomAdapter roomAdapter;
    private TextWatcher searchWatcher;

    // Bảng lọc đang mở, nếu có.
    private BottomSheetDialog filterDialog;

    // null nghĩa là chọn Tất cả hoặc không giới hạn.
    private RoomCard.RoomType selectedRoomType = null;
    private Long minPrice = null;
    private Long maxPrice = null;
    private Long minArea = null;
    private Long maxArea = null;

    /**
     * Khôi phục các điều kiện lọc khi Android tạo lại Fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            return;
        }

        String savedType =
                savedInstanceState.getString(KEY_ROOM_TYPE);

        if (savedType != null) {
            try {
                selectedRoomType =
                        RoomCard.RoomType.valueOf(savedType);
            } catch (IllegalArgumentException exception) {
                selectedRoomType = null;
            }
        }

        minPrice = restoreOptionalNumber(
                savedInstanceState, KEY_MIN_PRICE
        );
        maxPrice = restoreOptionalNumber(
                savedInstanceState, KEY_MAX_PRICE
        );
        minArea = restoreOptionalNumber(
                savedInstanceState, KEY_MIN_AREA
        );
        maxArea = restoreOptionalNumber(
                savedInstanceState, KEY_MAX_AREA
        );
    }

    /**
     * Tạo giao diện màn hình Khám phá từ file XML.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentExploreBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách, tìm kiếm và các bộ lọc.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupRoomList();
        setupSearch();
        setupRoomTypeFilters();
        setupAdvancedFilter();
    }

    /**
     * Áp dụng bộ lọc sau khi Android khôi phục nội dung ô tìm kiếm.
     */
    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        filterRooms(binding.edtSearch.getText().toString());
    }

    /**
     * Lưu các điều kiện đã áp dụng để khôi phục khi xoay màn hình.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(
                KEY_ROOM_TYPE,
                selectedRoomType == null
                        ? null
                        : selectedRoomType.name()
        );

        saveOptionalNumber(outState, KEY_MIN_PRICE, minPrice);
        saveOptionalNumber(outState, KEY_MAX_PRICE, maxPrice);
        saveOptionalNumber(outState, KEY_MIN_AREA, minArea);
        saveOptionalNumber(outState, KEY_MAX_AREA, maxArea);
    }

    /**
     * Lưu một giới hạn dạng số; xóa khóa nếu không có giới hạn.
     */
    private void saveOptionalNumber(
            Bundle state,
            String key,
            Long value
    ) {
        if (value == null) {
            state.remove(key);
        } else {
            state.putLong(key, value);
        }
    }

    /**
     * Đọc giới hạn đã lưu; trả về null nếu không có giới hạn.
     */
    private Long restoreOptionalNumber(Bundle state, String key) {
        return state.containsKey(key) ? state.getLong(key) : null;
    }

    /**
     * Tạo danh sách phòng và đăng ký thao tác mở chi tiết.
     */
    private void setupRoomList() {
        binding.rvRooms.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        roomAdapter = new RoomAdapter(
                DemoRoomRepository.getInstance().getRooms()
        );

        roomAdapter.setOnRoomClickListener(this::openRoomDetail);

        binding.rvRooms.setAdapter(roomAdapter);
    }

    /**
     * Lọc khi nhập từ khóa và đóng bàn phím khi bấm Search.
     */
    private void setupSearch() {
        searchWatcher = new TextWatcher() {

            /**
             * Được gọi trước khi nội dung thay đổi.
             */
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after
            ) {
                // Không cần xử lý.
            }

            /**
             * Lọc danh sách theo từ khóa vừa nhập.
             */
            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count
            ) {
                filterRooms(text.toString());
            }

            /**
             * Được gọi sau khi nhập; việc lọc đã thực hiện ở trên.
             */
            @Override
            public void afterTextChanged(Editable text) {
                // Không cần xử lý thêm.
            }
        };

        binding.edtSearch.addTextChangedListener(searchWatcher);

        binding.edtSearch.setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        filterRooms(textView.getText().toString());
                        hideSearchKeyboard();
                        return true;
                    }

                    return false;
                }
        );
    }

    /**
     * Đăng ký thao tác chọn Tất cả, Phòng trọ, Căn hộ hoặc Studio.
     */
    private void setupRoomTypeFilters() {
        binding.btnFilterAll.setOnClickListener(
                view -> selectRoomType(null)
        );

        binding.btnFilterRoom.setOnClickListener(
                view -> selectRoomType(RoomCard.RoomType.ROOM)
        );

        binding.btnFilterApartment.setOnClickListener(
                view -> selectRoomType(RoomCard.RoomType.APARTMENT)
        );

        binding.btnFilterStudio.setOnClickListener(
                view -> selectRoomType(RoomCard.RoomType.STUDIO)
        );

        updateRoomTypeButtons();
    }

    /**
     * Ghi nhận loại phòng, đổi màu nút và lọc lại danh sách.
     */
    private void selectRoomType(RoomCard.RoomType roomType) {
        selectedRoomType = roomType;

        updateRoomTypeButtons();
        hideSearchKeyboard();
        filterRooms(binding.edtSearch.getText().toString());
    }

    /**
     * Cập nhật trạng thái chọn của cả bốn nút loại phòng.
     */
    private void updateRoomTypeButtons() {
        updateFilterButton(
                binding.btnFilterAll,
                selectedRoomType == null
        );

        updateFilterButton(
                binding.btnFilterRoom,
                selectedRoomType == RoomCard.RoomType.ROOM
        );

        updateFilterButton(
                binding.btnFilterApartment,
                selectedRoomType == RoomCard.RoomType.APARTMENT
        );

        updateFilterButton(
                binding.btnFilterStudio,
                selectedRoomType == RoomCard.RoomType.STUDIO
        );
    }

    /**
     * Đổi nền và màu chữ của một nút theo trạng thái chọn.
     */
    private void updateFilterButton(Button button, boolean selected) {
        button.setBackgroundResource(
                selected
                        ? R.drawable.bg_filter_selected
                        : R.drawable.bg_filter_unselected
        );

        button.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        selected
                                ? R.color.roomly_surface
                                : R.color.roomly_text_secondary
                )
        );

        button.setSelected(selected);
    }

    /**
     * Đăng ký thao tác mở bảng lọc giá thuê và diện tích.
     */
    private void setupAdvancedFilter() {
        binding.btnFilter.setOnClickListener(view -> {
            hideSearchKeyboard();
            showRoomFilterDialog();
        });
    }

    /**
     * Mở bảng lọc với các điều kiện hiện tại.
     * Chỉ áp dụng thay đổi khi bấm nút Áp dụng.
     */
    private void showRoomFilterDialog() {
        // Tránh mở nhiều bảng lọc cùng lúc.
        if (filterDialog != null) {
            return;
        }

        BottomSheetRoomFilterBinding filterBinding =
                BottomSheetRoomFilterBinding.inflate(
                        getLayoutInflater()
                );

        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());

        filterDialog = dialog;
        dialog.setContentView(filterBinding.getRoot());

        // Khôi phục các giới hạn đã áp dụng.
        filterBinding.edtMinPrice.setText(
                minPrice == null ? "" : String.valueOf(minPrice)
        );
        filterBinding.edtMaxPrice.setText(
                maxPrice == null ? "" : String.valueOf(maxPrice)
        );
        filterBinding.edtMinArea.setText(
                minArea == null ? "" : String.valueOf(minArea)
        );
        filterBinding.edtMaxArea.setText(
                maxArea == null ? "" : String.valueOf(maxArea)
        );

        // Đặt lại các ô trong bảng, chờ Áp dụng để xác nhận.
        filterBinding.btnResetFilter.setOnClickListener(view -> {
            filterBinding.edtMinPrice.setText("");
            filterBinding.edtMaxPrice.setText("");
            filterBinding.edtMinArea.setText("");
            filterBinding.edtMaxArea.setText("");

            clearFilterErrors(filterBinding);
        });

        filterBinding.btnApplyFilter.setOnClickListener(view -> {
            clearFilterErrors(filterBinding);

            Long newMinPrice;
            Long newMaxPrice;
            Long newMinArea;
            Long newMaxArea;

            try {
                newMinPrice = readOptionalNumber(
                        filterBinding.edtMinPrice.getText().toString()
                );
                newMaxPrice = readOptionalNumber(
                        filterBinding.edtMaxPrice.getText().toString()
                );
                newMinArea = readOptionalNumber(
                        filterBinding.edtMinArea.getText().toString()
                );
                newMaxArea = readOptionalNumber(
                        filterBinding.edtMaxArea.getText().toString()
                );
            } catch (NumberFormatException exception) {
                Toast.makeText(
                        requireContext(),
                        "Vui lòng nhập số nguyên không âm hợp lệ.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (newMinPrice != null && newMaxPrice != null
                    && newMinPrice > newMaxPrice) {
                filterBinding.edtMaxPrice.setError(
                        "Giá đến phải lớn hơn hoặc bằng giá từ"
                );
                return;
            }

            if (newMinArea != null && newMaxArea != null
                    && newMinArea > newMaxArea) {
                filterBinding.edtMaxArea.setError(
                        "Diện tích đến phải lớn hơn hoặc bằng diện tích từ"
                );
                return;
            }

            // Chỉ lưu điều kiện sau khi kiểm tra hợp lệ.
            minPrice = newMinPrice;
            maxPrice = newMaxPrice;
            minArea = newMinArea;
            maxArea = newMaxArea;

            if (binding != null) {
                filterRooms(binding.edtSearch.getText().toString());
            }

            // Đóng bàn phím thuộc cửa sổ bảng lọc.
            if (dialog.getWindow() != null) {
                WindowInsetsControllerCompat controller =
                        new WindowInsetsControllerCompat(
                                dialog.getWindow(),
                                filterBinding.getRoot()
                        );

                controller.hide(WindowInsetsCompat.Type.ime());
            }

            dialog.dismiss();
        });

        dialog.setOnDismissListener(dismissedDialog -> {
            if (filterDialog == dialog) {
                filterDialog = null;
            }
        });

        dialog.show();
    }

    /**
     * Xóa các thông báo lỗi trên bốn ô nhập của bảng lọc.
     */
    private void clearFilterErrors(
            BottomSheetRoomFilterBinding filterBinding
    ) {
        filterBinding.edtMinPrice.setError(null);
        filterBinding.edtMaxPrice.setError(null);
        filterBinding.edtMinArea.setError(null);
        filterBinding.edtMaxArea.setError(null);
    }

    /**
     * Chuyển nội dung thành số nguyên không âm.
     * Ô trống trả về null, nghĩa là không giới hạn.
     */
    private Long readOptionalNumber(String text) {
        String value = text.trim();

        if (value.isEmpty()) {
            return null;
        }

        long number = Long.parseLong(value);

        if (number < 0) {
            throw new NumberFormatException("Giá trị âm");
        }

        return number;
    }

    /**
     * Đóng bàn phím của màn hình Khám phá và bỏ focus ô tìm kiếm.
     */
    private void hideSearchKeyboard() {
        if (binding == null) {
            return;
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        requireActivity().getWindow(),
                        binding.getRoot()
                );

        controller.hide(WindowInsetsCompat.Type.ime());
        binding.edtSearch.clearFocus();
    }

    /**
     * Kết hợp từ khóa, loại phòng, giá thuê và diện tích.
     * Hiện thông báo khi không có phòng đáp ứng mọi điều kiện.
     */
    private void filterRooms(String keyword) {
        if (binding == null || roomAdapter == null) {
            return;
        }

        String query = normalizeSearchText(keyword);

        List<RoomCard> allRooms =
                DemoRoomRepository.getInstance().getRooms();

        List<RoomCard> results = new ArrayList<>();

        for (RoomCard room : allRooms) {
            boolean matchesType =
                    selectedRoomType == null
                            || room.getRoomType() == selectedRoomType;

            String title = normalizeSearchText(room.getTitle());
            String address = normalizeSearchText(room.getAddress());

            boolean matchesKeyword =
                    query.isEmpty()
                            || title.contains(query)
                            || address.contains(query);

            long price = room.getMonthlyRent();
            int area = room.getAreaSquareMeters();

            boolean matchesPrice =
                    (minPrice == null || price >= minPrice)
                            && (maxPrice == null || price <= maxPrice);

            boolean matchesArea =
                    (minArea == null || area >= minArea)
                            && (maxArea == null || area <= maxArea);

            if (matchesType && matchesKeyword
                    && matchesPrice && matchesArea) {
                results.add(room);
            }

            // Kiểm tra có giới hạn giá hoặc diện tích nào đang áp dụng không.
            boolean hasAdvancedFilter =
                    minPrice != null
                            || maxPrice != null
                            || minArea != null
                            || maxArea != null;

            // Đổi nền nút lọc để người dùng nhận biết điều kiện đang bật.
            binding.btnFilter.setBackgroundResource(
                    hasAdvancedFilter
                            ? R.drawable.bg_filter_selected
                            : 0
            );

            // Khi nền xanh, icon trắng; khi không lọc, icon xanh.
            binding.btnFilter.setImageTintList(
                    android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(
                                    requireContext(),
                                    hasAdvancedFilter
                                            ? R.color.roomly_surface
                                            : R.color.roomly_primary
                            )
                    )
            );

            binding.btnFilter.setContentDescription(
                    hasAdvancedFilter
                            ? "Lọc phòng, đang áp dụng giới hạn giá hoặc diện tích"
                            : "Lọc phòng"
            );
        }

        // Giữ các đối tượng phòng dùng chung để bảo toàn trạng thái lưu.
        roomAdapter.updateRooms(results);

        boolean isEmpty = results.isEmpty();

        binding.layoutSearchEmpty.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        binding.rvRooms.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );
    }

    /**
     * Bỏ dấu tiếng Việt, chuyển thành chữ thường
     * và chuẩn hóa khoảng trắng để tìm kiếm.
     */
    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(
                text,
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * Mở chi tiết phòng và lưu màn hình trước vào back stack.
     */
    private void openRoomDetail(RoomCard room) {
        hideSearchKeyboard();

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        RoomDetailFragment.newInstance(room)
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Đóng bảng lọc và giải phóng các tham chiếu giao diện.
     */
    @Override
    public void onDestroyView() {
        if (filterDialog != null) {
            filterDialog.dismiss();
            filterDialog = null;
        }

        if (binding != null) {
            if (searchWatcher != null) {
                binding.edtSearch.removeTextChangedListener(
                        searchWatcher
                );
            }

            binding.edtSearch.setOnEditorActionListener(null);
            binding.rvRooms.setAdapter(null);
        }

        searchWatcher = null;
        roomAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}