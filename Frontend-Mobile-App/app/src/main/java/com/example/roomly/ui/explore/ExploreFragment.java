package com.example.roomly.ui.explore;

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
import com.example.roomly.databinding.FragmentExploreBinding;
import com.example.roomly.ui.detail.RoomDetailFragment;

public class ExploreFragment extends Fragment {

    // Binding liên kết với giao diện fragment_explore.xml.
    private FragmentExploreBinding binding;

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
     * Thiết lập danh sách phòng sau khi giao diện được tạo.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        setupRoomList();
    }

    /**
     * Hiển thị danh sách từ repository dùng chung
     * và đăng ký thao tác bấm thẻ phòng.
     */
    private void setupRoomList() {
        binding.rvRooms.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        RoomAdapter adapter = new RoomAdapter(
                DemoRoomRepository.getInstance().getRooms()
        );

        adapter.setOnRoomClickListener(this::openRoomDetail);

        binding.rvRooms.setAdapter(adapter);
    }

    /**
     * Mở chi tiết phòng được chọn và lưu màn hình trước
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
     * Gỡ adapter và giải phóng binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        binding.rvRooms.setAdapter(null);
        binding = null;

        super.onDestroyView();
    }
}