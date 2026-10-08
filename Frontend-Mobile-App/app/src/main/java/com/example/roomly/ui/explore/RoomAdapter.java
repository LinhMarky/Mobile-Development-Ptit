package com.example.roomly.ui.explore;

import android.net.Uri;
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
 * Hiển thị thẻ phòng và gửi yêu cầu lưu phòng cho Fragment xử lý.
 */
public class RoomAdapter
        extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    private final List<RoomCard> rooms = new ArrayList<>();

    private OnRoomClickListener onRoomClickListener;
    private OnSaveRequestListener onSaveRequestListener;
    private OnSaveChangedListener onSaveChangedListener;

    public interface OnRoomClickListener {

        /** Thông báo phòng được chọn để mở chi tiết. */
        void onRoomClick(RoomCard room);
    }

    public interface OnSaveRequestListener {

        /** Yêu cầu Fragment kiểm tra quyền trước khi đổi trạng thái lưu. */
        void onSaveRequest(RoomCard room);
    }

    public interface OnSaveChangedListener {

        /** Thông báo trạng thái lưu đã được cập nhật. */
        void onSaveChanged(RoomCard room);
    }

    /** Sao chép danh sách nhưng giữ chung các đối tượng phòng. */
    public RoomAdapter(List<RoomCard> initialRooms) {
        rooms.addAll(initialRooms);
    }

    /** Đăng ký thao tác mở chi tiết. */
    public void setOnRoomClickListener(
            @Nullable OnRoomClickListener listener
    ) {
        onRoomClickListener = listener;
    }

    /** Đăng ký xử lý yêu cầu lưu hoặc bỏ lưu. */
    public void setOnSaveRequestListener(
            @Nullable OnSaveRequestListener listener
    ) {
        onSaveRequestListener = listener;
    }

    /** Đăng ký xử lý sau khi trạng thái lưu thay đổi. */
    public void setOnSaveChangedListener(
            @Nullable OnSaveChangedListener listener
    ) {
        onSaveChangedListener = listener;
    }

    /** Tạo giao diện thẻ từ item_room.xml. */
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

    /** Hiển thị phòng và dùng vị trí hiện tại khi xử lý thao tác bấm. */
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

            if (onSaveRequestListener != null) {
                onSaveRequestListener.onSaveRequest(
                        rooms.get(currentPosition)
                );
            }
        });
    }

    /** Trả về số phòng đang hiển thị. */
    @Override
    public int getItemCount() {
        return rooms.size();
    }

    /** Cập nhật danh sách sau khi tìm kiếm hoặc lọc. */
    public void updateRooms(List<RoomCard> newRooms) {
        List<RoomCard> updated = new ArrayList<>(newRooms);

        rooms.clear();
        rooms.addAll(updated);

        notifyDataSetChanged();
    }

    /** Cập nhật thẻ sau khi Fragment đã xử lý trạng thái lưu. */
    public void notifyRoomSaveChanged(RoomCard room) {
        int position = rooms.indexOf(room);

        if (position >= 0) {
            notifyItemChanged(position);
        }

        if (onSaveChangedListener != null) {
            onSaveChangedListener.onSaveChanged(room);
        }
    }

    /** Gỡ listener và ảnh khi thẻ được tái sử dụng. */
    @Override
    public void onViewRecycled(@NonNull RoomViewHolder holder) {
        holder.itemView.setOnClickListener(null);
        holder.binding.btnSaveRoom.setOnClickListener(null);
        holder.binding.imgRoom.setImageDrawable(null);

        super.onViewRecycled(holder);
    }

    /** Giữ các thành phần giao diện của một thẻ phòng. */
    static class RoomViewHolder extends RecyclerView.ViewHolder {

        private final ItemRoomBinding binding;

        /** Khởi tạo ViewHolder bằng binding. */
        RoomViewHolder(ItemRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /** Hiển thị thông tin, ảnh và trạng thái trái tim. */
        void bind(RoomCard room) {
            binding.tvRoomTitle.setText(room.getTitle());
            binding.tvRoomPrice.setText(room.getPrice());
            binding.tvRoomAddress.setText(room.getAddress());
            binding.tvRoomAmenities.setText(room.getAmenities());

            displayRoomImage(room);
            updateSaveButton(room);
        }

        /**
         * Ưu tiên ảnh URI và xóa ảnh thẻ cũ trước khi hiển thị.
         * Nếu không có ảnh URI đọc được, dùng ảnh drawable khi có.
         */
        private void displayRoomImage(RoomCard room) {
            binding.imgRoom.setImageDrawable(null);

            if (!room.getImageUri().trim().isEmpty()) {
                try {
                    binding.imgRoom.setImageURI(
                            Uri.parse(room.getImageUri())
                    );
                } catch (
                        SecurityException | IllegalArgumentException exception
                ) {
                    binding.imgRoom.setImageDrawable(null);
                }
            }

            if (binding.imgRoom.getDrawable() == null
                    && room.getImageResId() != 0) {
                binding.imgRoom.setImageResource(room.getImageResId());
            }

            binding.imgRoom.setContentDescription(
                    binding.imgRoom.getDrawable() == null
                            ? "Phòng chưa có ảnh đọc được"
                            : "Ảnh phòng " + room.getTitle()
            );
        }

        /** Chọn icon và mô tả theo trạng thái lưu hiện tại. */
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