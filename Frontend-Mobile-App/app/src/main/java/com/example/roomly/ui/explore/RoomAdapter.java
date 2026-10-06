package com.example.roomly.ui.explore;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.R;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.databinding.ItemRoomBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị danh sách thẻ phòng.
 * Gửi yêu cầu lưu hoặc bỏ lưu cho Fragment kiểm tra và xử lý.
 */
public class RoomAdapter
        extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    // Danh sách hiển thị dùng chung các đối tượng phòng với repository.
    private final List<RoomCard> rooms = new ArrayList<>();

    private OnRoomClickListener onRoomClickListener;
    private OnSaveRequestListener onSaveRequestListener;

    // Giữ tương thích với các màn hình đang đăng ký sự kiện này.
    private OnSaveChangedListener onSaveChangedListener;

    public interface OnRoomClickListener {

        /**
         * Thông báo phòng được chọn để mở màn hình chi tiết.
         */
        void onRoomClick(RoomCard room);
    }

    public interface OnSaveRequestListener {

        /**
         * Yêu cầu Fragment kiểm tra quyền và xử lý lưu hoặc bỏ lưu.
         * Trạng thái của phòng chưa thay đổi khi hàm này được gọi.
         */
        void onSaveRequest(RoomCard room);
    }

    public interface OnSaveChangedListener {

        /**
         * Thông báo trạng thái lưu đã được cập nhật.
         */
        void onSaveChanged(RoomCard room);
    }

    /**
     * Sao chép danh sách ban đầu nhưng giữ chung các đối tượng phòng.
     */
    public RoomAdapter(List<RoomCard> initialRooms) {
        rooms.addAll(initialRooms);
    }

    /**
     * Đăng ký xử lý thao tác bấm thẻ phòng.
     */
    public void setOnRoomClickListener(
            @Nullable OnRoomClickListener listener
    ) {
        onRoomClickListener = listener;
    }

    /**
     * Đăng ký xử lý yêu cầu lưu hoặc bỏ lưu trước khi dữ liệu thay đổi.
     */
    public void setOnSaveRequestListener(
            @Nullable OnSaveRequestListener listener
    ) {
        onSaveRequestListener = listener;
    }

    /**
     * Đăng ký xử lý sau khi trạng thái lưu được cập nhật.
     */
    public void setOnSaveChangedListener(
            @Nullable OnSaveChangedListener listener
    ) {
        onSaveChangedListener = listener;
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
     * Hiển thị phòng và đăng ký các thao tác trên thẻ.
     * Luôn lấy vị trí hiện tại để tránh xử lý thẻ đã bị loại khỏi danh sách.
     */
    @Override
    public void onBindViewHolder(
            @NonNull RoomViewHolder holder,
            int position
    ) {
        holder.bind(rooms.get(position));

        holder.itemView.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            if (onRoomClickListener != null) {
                onRoomClickListener.onRoomClick(
                        rooms.get(currentPosition)
                );
            }
        });

        holder.binding.btnSaveRoom.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            // Không tự đổi trạng thái lưu trong Adapter.
            if (onSaveRequestListener != null) {
                onSaveRequestListener.onSaveRequest(
                        rooms.get(currentPosition)
                );
            }
        });
    }

    /**
     * Trả về số phòng đang hiển thị.
     */
    @Override
    public int getItemCount() {
        return rooms.size();
    }

    /**
     * Cập nhật danh sách sau khi tìm kiếm hoặc lọc phòng.
     * Sao chép danh sách đầu vào trước khi xóa dữ liệu hiện tại.
     */
    public void updateRooms(List<RoomCard> newRooms) {
        List<RoomCard> updatedRooms = new ArrayList<>(newRooms);

        rooms.clear();
        rooms.addAll(updatedRooms);

        notifyDataSetChanged();
    }

    /**
     * Cập nhật thẻ và thông báo sau khi Fragment đã đổi trạng thái lưu.
     * Hàm này không tự lưu hoặc bỏ lưu phòng.
     */
    public void notifyRoomSaveChanged(RoomCard room) {
        int position = rooms.indexOf(room);

        if (position >= 0) {
            notifyItemChanged(position);
        }

        if (onSaveChangedListener != null) {
            onSaveChangedListener.onSaveChanged(room);
        }
    }

    /**
     * Gỡ sự kiện khi RecyclerView đưa thẻ vào vùng tái sử dụng.
     */
    @Override
    public void onViewRecycled(@NonNull RoomViewHolder holder) {
        holder.itemView.setOnClickListener(null);
        holder.binding.btnSaveRoom.setOnClickListener(null);

        super.onViewRecycled(holder);
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
         * Hiển thị thông tin phòng và trạng thái trái tim hiện tại.
         */
        void bind(RoomCard room) {
            binding.tvRoomTitle.setText(room.getTitle());
            binding.tvRoomPrice.setText(room.getPrice());
            binding.tvRoomAddress.setText(room.getAddress());
            binding.tvRoomAmenities.setText(room.getAmenities());
            binding.imgRoom.setImageResource(room.getImageResId());

            updateSaveButton(room);
        }

        /**
         * Chọn icon và mô tả phù hợp với trạng thái lưu phòng.
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