package com.example.roomly.ui.host;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.HostRoom;
import com.example.roomly.databinding.ItemHostRoomBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị thông tin, ảnh phòng và xử lý bấm thẻ để mở chi tiết.
 */
public class HostRoomAdapter
        extends RecyclerView.Adapter<HostRoomAdapter.HostRoomViewHolder> {

    private final List<HostRoom> rooms = new ArrayList<>();

    private OnRoomClickListener onRoomClickListener;

    public interface OnRoomClickListener {

        /**
         * Thông báo phòng được chọn để mở màn hình chi tiết.
         */
        void onRoomClick(HostRoom room);
    }

    /**
     * Khởi tạo adapter với danh sách rỗng.
     */
    public HostRoomAdapter() {
    }

    /**
     * Đăng ký xử lý thao tác bấm thẻ phòng.
     * Truyền null để gỡ listener khi màn hình bị hủy.
     */
    public void setOnRoomClickListener(OnRoomClickListener listener) {
        this.onRoomClickListener = listener;
    }

    /**
     * Tạo giao diện một thẻ phòng bằng ViewBinding.
     */
    @NonNull
    @Override
    public HostRoomViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemHostRoomBinding binding = ItemHostRoomBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new HostRoomViewHolder(binding);
    }

    /**
     * Hiển thị dữ liệu và đăng ký thao tác bấm thẻ phòng.
     * Lấy vị trí hiện tại khi bấm để tránh sử dụng vị trí cũ.
     */
    @Override
    public void onBindViewHolder(
            @NonNull HostRoomViewHolder holder,
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
    }

    /**
     * Trả về số phòng đang hiển thị.
     */
    @Override
    public int getItemCount() {
        return rooms.size();
    }

    /**
     * Thay danh sách phòng và cập nhật giao diện.
     * Sao chép trước khi xóa danh sách cũ.
     */
    public void updateRooms(List<HostRoom> newRooms) {
        List<HostRoom> updatedRooms = new ArrayList<>(newRooms);

        rooms.clear();
        rooms.addAll(updatedRooms);

        notifyDataSetChanged();
    }

    /**
     * Gỡ sự kiện bấm và giải phóng ảnh khi thẻ được tái sử dụng.
     */
    @Override
    public void onViewRecycled(@NonNull HostRoomViewHolder holder) {
        holder.itemView.setOnClickListener(null);
        holder.clearImage();

        super.onViewRecycled(holder);
    }

    /**
     * Giữ các thành phần giao diện của một thẻ phòng.
     */
    static class HostRoomViewHolder extends RecyclerView.ViewHolder {

        private final ItemHostRoomBinding binding;

        /**
         * Khởi tạo ViewHolder từ binding của thẻ phòng.
         */
        HostRoomViewHolder(ItemHostRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Hiển thị thông tin phòng và cập nhật ảnh.
         * Luôn đặt lại trạng thái khi RecyclerView tái sử dụng thẻ.
         */
        void bind(HostRoom room) {
            binding.tvHostRoomCode.setText(
                    "Mã phòng: " + room.getUnitCode()
            );

            binding.tvHostRoomName.setText(room.getName());
            binding.tvHostRoomAddress.setText(room.getAddress());

            binding.tvHostRoomArea.setText(
                    "Diện tích: " + room.getFormattedArea()
            );

            String description = room.getDescription();

            boolean hasDescription = description != null
                    && !description.trim().isEmpty();

            binding.tvHostRoomDescription.setText(
                    hasDescription ? description : ""
            );

            binding.tvHostRoomDescription.setVisibility(
                    hasDescription ? View.VISIBLE : View.GONE
            );

            displayImage(room);
        }

        /**
         * Đọc ảnh từ đường dẫn đã lưu trong phòng mẫu.
         * Hiển thị phần thay thế nếu ảnh bị xóa hoặc mất quyền đọc.
         */
        private void displayImage(HostRoom room) {
            clearImage();

            String imageUri = room.getImageUri();

            if (imageUri == null || imageUri.trim().isEmpty()) {
                return;
            }

            try {
                binding.imgHostRoom.setImageURI(
                        Uri.parse(imageUri)
                );

                boolean imageLoaded =
                        binding.imgHostRoom.getDrawable() != null;

                binding.imgHostRoom.setVisibility(
                        imageLoaded ? View.VISIBLE : View.GONE
                );

                binding.tvHostRoomImagePlaceholder.setVisibility(
                        imageLoaded ? View.GONE : View.VISIBLE
                );

                binding.imgHostRoom.setContentDescription(
                        "Ảnh phòng " + room.getName()
                );

            } catch (SecurityException exception) {
                clearImage();
            }
        }

        /**
         * Xóa ảnh cũ và đưa thẻ về trạng thái chưa có ảnh.
         */
        void clearImage() {
            binding.imgHostRoom.setImageDrawable(null);
            binding.imgHostRoom.setVisibility(View.GONE);
            binding.tvHostRoomImagePlaceholder.setVisibility(View.VISIBLE);
        }
    }
}