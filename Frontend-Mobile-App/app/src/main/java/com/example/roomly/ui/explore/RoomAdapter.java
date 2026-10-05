package com.example.roomly.ui.explore;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.RoomCard;
import com.example.roomly.databinding.ItemRoomBinding;

import java.util.ArrayList;
import java.util.List;

import com.example.roomly.R;

/**
 * Hiển thị danh sách phòng bằng mẫu giao diện item_room.xml.
 */
public class RoomAdapter
        extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private final List<RoomCard> rooms = new ArrayList<>();

    // Nơi nhận sự kiện khi người dùng bấm một thẻ phòng.
    private OnRoomClickListener onRoomClickListener;

    public interface OnRoomClickListener {

        /**
         * Báo cho màn hình Khám phá biết phòng nào vừa được bấm.
         */
        void onRoomClick(RoomCard room);
    }

    /**
     * Cho phép màn hình Khám phá đăng ký xử lý sự kiện bấm phòng.
     */
    public void setOnRoomClickListener(OnRoomClickListener listener) {
        this.onRoomClickListener = listener;
    }

    /**
     * Nhận danh sách phòng ban đầu và sao chép vào Adapter.
     */
    public RoomAdapter(List<RoomCard> initialRooms) {
        rooms.addAll(initialRooms);
    }

    /**
     * Tạo giao diện cho một thẻ phòng bằng ViewBinding.
     */
    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemRoomBinding binding = ItemRoomBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new RoomViewHolder(binding);
    }

    /**
     * Hiển thị dữ liệu phòng trên thẻ và xử lý thao tác bấm thẻ.
     */
    @Override
    public void onBindViewHolder(
            @NonNull RoomViewHolder holder,
            int position
    ) {
        RoomCard room = rooms.get(position);

        holder.bind(room);

        holder.itemView.setOnClickListener(view -> {
            if (onRoomClickListener != null) {
                onRoomClickListener.onRoomClick(room);
            }
        });
    }

    /**
     * Trả về tổng số phòng cần hiển thị.
     */
    @Override
    public int getItemCount() {
        return rooms.size();
    }

    /**
     * Giữ các thành phần giao diện của một thẻ phòng.
     */
    static class RoomViewHolder extends RecyclerView.ViewHolder {

        private final com.example.roomly.databinding.ItemRoomBinding binding;

        /**
         * Khởi tạo ViewHolder với giao diện thẻ đã tạo.
         */
        RoomViewHolder(ItemRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Gán tên, giá, địa chỉ, tiện ích và ảnh vào thẻ phòng.
         */
        /**
         * Hiển thị thông tin phòng và trạng thái trái tim.
         * Khi bấm trái tim, đổi trạng thái lưu của dữ liệu mẫu.
         */
        void bind(RoomCard room) {
            binding.tvRoomTitle.setText(room.getTitle());
            binding.tvRoomPrice.setText(room.getPrice());
            binding.tvRoomAddress.setText(room.getAddress());
            binding.tvRoomAmenities.setText(room.getAmenities());
            binding.imgRoom.setImageResource(room.getImageResId());

            // Luôn cập nhật icon khi tái sử dụng thẻ trong RecyclerView.
            updateSaveButton(room);

            binding.btnSaveRoom.setOnClickListener(view -> {
                room.setSaved(!room.isSaved());

                updateSaveButton(room);
            });
        }

        /**
         * Chọn icon và mô tả phù hợp với trạng thái lưu của phòng.
         */
        private void updateSaveButton(RoomCard room) {
            binding.btnSaveRoom.setImageResource(
                    room.isSaved()
                            ? R.drawable.ic_favorite_filled
                            : R.drawable.ic_favorite
            );

            binding.btnSaveRoom.setContentDescription(
                    room.isSaved() ? "Bỏ lưu phòng" : "Lưu phòng"
            );
        }
    }
}