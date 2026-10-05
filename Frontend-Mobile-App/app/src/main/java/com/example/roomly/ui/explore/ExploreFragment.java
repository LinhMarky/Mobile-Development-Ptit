package com.example.roomly.ui.explore;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.repository.DemoRoomRepository;
import com.example.roomly.databinding.FragmentExploreBinding;
import com.example.roomly.ui.detail.RoomDetailFragment;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.view.inputmethod.EditorInfo;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class ExploreFragment extends Fragment {

    // Binding liên kết với giao diện fragment_explore.xml.
    private FragmentExploreBinding binding;

    // Adapter hiển thị danh sách và kết quả tìm kiếm.
    private RoomAdapter roomAdapter;

    // Theo dõi nội dung người dùng nhập vào ô tìm kiếm.
    private TextWatcher searchWatcher;

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
     * Thiết lập danh sách phòng và xử lý ô tìm kiếm.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupRoomList();
        setupSearch();
    }

    /**
     * Áp dụng từ khóa sau khi Android khôi phục nội dung ô tìm kiếm,
     * ví dụ khi quay lại từ màn hình chi tiết hoặc xoay máy.
     */
    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        filterRooms(binding.edtSearch.getText().toString());
    }

    /**
     * Hiển thị phòng từ repository dùng chung
     * và đăng ký thao tác mở chi tiết.
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
     * Theo dõi ô tìm kiếm để lọc danh sách ngay khi nhập hoặc xóa chữ.
     */
    private void setupSearch() {
        searchWatcher = new TextWatcher() {

            /**
             * Được gọi trước khi nội dung thay đổi.
             * Bước này chưa cần xử lý.
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
             * Lọc phòng theo nội dung mới trong ô tìm kiếm.
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
             * Được gọi sau khi nội dung thay đổi.
             * Việc lọc đã được xử lý trong onTextChanged.
             */
            @Override
            public void afterTextChanged(Editable text) {
                // Không cần xử lý thêm.
            }
        };

        binding.edtSearch.addTextChangedListener(searchWatcher);

        // Tìm phòng và đóng bàn phím khi bấm Search.
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
     * Đóng bàn phím và bỏ trạng thái nhập của ô tìm kiếm
     * để người dùng xem danh sách kết quả.
     */
    private void hideSearchKeyboard() {
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        requireActivity().getWindow(),
                        binding.getRoot()
                );

        controller.hide(WindowInsetsCompat.Type.ime());
        binding.edtSearch.clearFocus();
    }

    /**
     * Tìm phòng theo tên hoặc địa chỉ.
     * Hiện thông báo nếu không có kết quả phù hợp.
     * Khi từ khóa trống, hiển thị toàn bộ phòng.
     */
    private void filterRooms(String keyword) {
        if (binding == null || roomAdapter == null) {
            return;
        }

        String query = normalizeSearchText(keyword);

        List<RoomCard> allRooms =
                DemoRoomRepository.getInstance().getRooms();

        List<RoomCard> results = new ArrayList<>();

        if (query.isEmpty()) {
            // Không nhập từ khóa thì hiển thị tất cả phòng.
            results.addAll(allRooms);
        } else {
            for (RoomCard room : allRooms) {
                String title = normalizeSearchText(room.getTitle());
                String address = normalizeSearchText(room.getAddress());

                if (title.contains(query) || address.contains(query)) {
                    results.add(room);
                }
            }
        }

        // Cập nhật các thẻ phòng theo kết quả tìm kiếm.
        roomAdapter.updateRooms(results);

        // Hiện thông báo khi danh sách kết quả trống.
        boolean isEmpty = results.isEmpty();

        binding.layoutSearchEmpty.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        binding.rvRooms.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );
    }
    /**
     * Chuyển chữ thành chữ thường, bỏ dấu tiếng Việt
     * và chuẩn hóa khoảng trắng để tìm kiếm dễ hơn.
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
     * Mở chi tiết phòng và lưu màn hình Khám phá
     * vào back stack để người dùng có thể quay lại.
     */
    private void openRoomDetail(RoomCard room) {
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
     * Gỡ bộ theo dõi tìm kiếm, adapter và binding
     * khi giao diện Fragment bị hủy.
     */
    @Override
    public void onDestroyView() {
        if (searchWatcher != null) {
            binding.edtSearch.removeTextChangedListener(searchWatcher);
        }

        binding.rvRooms.setAdapter(null);

        searchWatcher = null;
        roomAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}