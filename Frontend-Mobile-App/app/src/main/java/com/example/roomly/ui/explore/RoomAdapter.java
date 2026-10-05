package com.example.roomly.ui.explore;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.R;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.databinding.ItemRoomBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị các thẻ phòng và xử lý thao tác mở chi tiết,
 * lưu phòng hoặc bỏ lưu phòng.
 */
public class RoomAdapter
        extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    // Danh sách đang hiển thị, dùng chung các đối tượng phòng.
    private final List<RoomCard> rooms = new ArrayList<>();

    private OnRoomClickListener onRoomClickListener;
    private OnSaveChangedListener onSaveChangedListener;

    public interface OnRoomClickListener {

        /**
         * Thông báo phòng được chọn để mở màn hình chi tiết.
         */
        void onRoomClick(RoomCard room);
    }

    public interface OnSaveChangedListener {

        /**
         * Thông báo phòng vừa được lưu hoặc bỏ lưu.
         */
        void onSaveChanged(RoomCard room);
    }

    /**
     * Sao chép danh sách ban đầu vào Adapter.
     * Các đối tượng phòng vẫn được dùng chung với repository.
     */
    public RoomAdapter(List<RoomCard> initialRooms) {
        rooms.addAll(initialRooms);
    }

    /**
     * Đăng ký xử lý khi người dùng bấm thẻ phòng.
     */
    public void setOnRoomClickListener(OnRoomClickListener listener) {
        this.onRoomClickListener = listener;
    }

    /**
     * Đăng ký xử lý khi trạng thái lưu phòng thay đổi.
     */
    public void setOnSaveChangedListener(OnSaveChangedListener listener) {
        this.onSaveChangedListener = listener;
    }

    /**
     * Tạo giao diện một thẻ phòng từ item_room.xml.
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
     * Hiển thị dữ liệu phòng và đăng ký thao tác bấm thẻ.
     */
    @Override
    public void onBindViewHolder(
            @NonNull RoomViewHolder holder,
            int position
    ) {
        RoomCard room = rooms.get(position);

        holder.bind(room, onSaveChangedListener);

        holder.itemView.setOnClickListener(view -> {
            if (onRoomClickListener != null) {
                onRoomClickListener.onRoomClick(room);
            }
        });
    }

    /**
     * Trả về số phòng trong danh sách đang hiển thị.
     */
    @Override
    public int getItemCount() {
        return rooms.size();
    }

    /**
     * Cập nhật danh sách hiển thị bằng kết quả tìm kiếm.
     * Giữ nguyên các đối tượng phòng để bảo toàn trạng thái lưu.
     */
    public void updateRooms(List<RoomCard> newRooms) {
        // Sao chép trước khi xóa danh sách hiện tại.
        List<RoomCard> updatedRooms = new ArrayList<>(newRooms);

        rooms.clear();
        rooms.addAll(updatedRooms);

        // Hiển thị lại danh sách dữ liệu mẫu sau khi cập nhật.
        notifyDataSetChanged();
    }

    /**
     * Giữ các thành phần giao diện của một thẻ phòng.
     */
    static class RoomViewHolder extends RecyclerView.ViewHolder {

        private final ItemRoomBinding binding;

        /**
         * Khởi tạo ViewHolder bằng binding của thẻ phòng.
         */
        RoomViewHolder(ItemRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Hiển thị thông tin phòng và trạng thái trái tim.
         * Khi bấm trái tim, cập nhật dữ liệu và thông báo cho màn hình.
         */
        void bind(
                RoomCard room,
                OnSaveChangedListener saveListener
        ) {
            binding.tvRoomTitle.setText(room.getTitle());
            binding.tvRoomPrice.setText(room.getPrice());
            binding.tvRoomAddress.setText(room.getAddress());
            binding.tvRoomAmenities.setText(room.getAmenities());
            binding.imgRoom.setImageResource(room.getImageResId());

            // Cập nhật icon khi RecyclerView tái sử dụng thẻ phòng.
            updateSaveButton(room);

            binding.btnSaveRoom.setOnClickListener(view -> {
                // Đảo trạng thái lưu của phòng.
                room.setSaved(!room.isSaved());

                updateSaveButton(room);

                // Báo cho màn hình cập nhật danh sách nếu cần.
                if (saveListener != null) {
                    saveListener.onSaveChanged(room);
                }
            });
        }

        /**
         * Cập nhật icon và mô tả trái tim theo trạng thái lưu phòng.
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