package com.example.roomly.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.roomly.data.model.RoomCard;
import com.example.roomly.databinding.FragmentRoomDetailBinding;

public class RoomDetailFragment extends Fragment {

    // Các khóa dùng để truyền dữ liệu phòng qua Bundle.
    private static final String ARG_TITLE = "room_title";
    private static final String ARG_PRICE = "room_price";
    private static final String ARG_ADDRESS = "room_address";
    private static final String ARG_AMENITIES = "room_amenities";
    private static final String ARG_IMAGE = "room_image";

    // Binding liên kết với fragment_room_detail.xml.
    private FragmentRoomDetailBinding binding;

    /**
     * Tạo màn hình chi tiết và đính kèm dữ liệu của phòng được chọn.
     * Bundle giúp Android giữ lại dữ liệu khi tạo lại Fragment.
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
     * Hiển thị dữ liệu phòng và gắn thao tác cho nút quay lại.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        displayRoomDetails();
        setupBackButton();
    }

    /**
     * Đọc dữ liệu đã truyền và cập nhật thông tin trên giao diện.
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
     * Quay về màn hình trước bằng cách lấy Fragment khỏi back stack.
     * Bước nối màn hình tiếp theo sẽ thêm giao dịch vào back stack.
     */
    private void setupBackButton() {
        binding.btnBack.setOnClickListener(
                view -> getParentFragmentManager().popBackStack()
        );
    }

    /**
     * Giải phóng binding khi giao diện bị hủy để tránh giữ View cũ.
     */
    @Override
    public void onDestroyView() {
        binding = null;

        super.onDestroyView();
    }
}