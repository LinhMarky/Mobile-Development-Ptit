package com.example.roomly.ui.saved;

import android.os.Bundle;
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
import com.example.roomly.databinding.FragmentSavedBinding;
import com.example.roomly.ui.detail.RoomDetailFragment;
import com.example.roomly.ui.explore.RoomAdapter;

import java.util.List;

public class SavedFragment extends Fragment {

    // Binding liên kết với fragment_saved.xml.
    private com.example.roomly.databinding.FragmentSavedBinding binding;

    /**
     * Tạo giao diện màn hình Đã lưu từ file XML.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSavedBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách phòng cuộn theo chiều dọc.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvSavedRooms.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
    }

    /**
     * Cập nhật danh sách mỗi khi màn hình được mở hoặc trở lại
     * từ màn hình chi tiết phòng.
     */
    @Override
    public void onResume() {
        super.onResume();

        displaySavedRooms();
    }

    /**
     * Hiển thị phòng đã lưu; nếu danh sách trống,
     * hiện thông báo hướng dẫn bấm trái tim ở trang Khám phá.
     */
    private void displaySavedRooms() {
        List<RoomCard> savedRooms =
                DemoRoomRepository.getInstance().getSavedRooms();

        boolean isEmpty = savedRooms.isEmpty();

        binding.layoutSavedEmpty.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        binding.rvSavedRooms.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );

        RoomAdapter adapter = new RoomAdapter(savedRooms);
        adapter.setOnRoomClickListener(this::openRoomDetail);

        binding.rvSavedRooms.setAdapter(adapter);
    }

    /**
     * Mở chi tiết phòng và lưu màn hình Đã lưu vào back stack
     * để nút quay lại đưa người dùng về đúng màn hình.
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
     * Gỡ adapter và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        binding.rvSavedRooms.setAdapter(null);
        binding = null;

        super.onDestroyView();
    }
}