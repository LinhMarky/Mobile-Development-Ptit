package com.example.roomly.ui.host;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.HostListing;
import com.example.roomly.data.model.HostRoom;
import com.example.roomly.data.repository.DemoHostRoomRepository;
import com.example.roomly.databinding.ItemHostListingBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị bản nháp bài đăng và nhận thao tác bấm thẻ.
 * Ảnh và địa chỉ được lấy từ phòng liên kết trong dữ liệu mẫu.
 */
public class HostListingAdapter
        extends RecyclerView.Adapter<
        HostListingAdapter.HostListingViewHolder> {

    private final List<HostListing> listings = new ArrayList<>();

    private OnListingClickListener onListingClickListener;

    public interface OnListingClickListener {

        /**
         * Thông báo bản nháp được chọn để mở màn hình chi tiết.
         */
        void onListingClick(HostListing listing);
    }

    /**
     * Khởi tạo adapter với danh sách rỗng.
     */
    public HostListingAdapter() {
    }

    /**
     * Đăng ký xử lý thao tác bấm thẻ bài đăng.
     * Truyền null để gỡ listener khi màn hình bị hủy.
     */
    public void setOnListingClickListener(
            OnListingClickListener listener
    ) {
        this.onListingClickListener = listener;
    }

    /**
     * Tạo giao diện một thẻ bài đăng bằng ViewBinding.
     */
    @NonNull
    @Override
    public HostListingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemHostListingBinding binding =
                ItemHostListingBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new HostListingViewHolder(binding);
    }

    /**
     * Hiển thị dữ liệu và đăng ký thao tác bấm thẻ bài đăng.
     * Lấy vị trí hiện tại khi bấm để tránh sử dụng vị trí cũ.
     */
    @Override
    public void onBindViewHolder(
            @NonNull HostListingViewHolder holder,
            int position
    ) {
        HostListing listing = listings.get(position);

        HostRoom room = DemoHostRoomRepository.getInstance()
                .getMyRoomById(listing.getRoomId());

        holder.bind(listing, room);

        holder.itemView.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            if (onListingClickListener != null) {
                onListingClickListener.onListingClick(
                        listings.get(currentPosition)
                );
            }
        });
    }

    /**
     * Trả về số bản nháp đang hiển thị.
     */
    @Override
    public int getItemCount() {
        return listings.size();
    }

    /**
     * Thay danh sách bản nháp và cập nhật giao diện.
     * Sao chép trước khi xóa danh sách cũ.
     */
    public void updateListings(List<HostListing> newListings) {
        List<HostListing> updatedListings =
                new ArrayList<>(newListings);

        listings.clear();
        listings.addAll(updatedListings);

        notifyDataSetChanged();
    }

    /**
     * Gỡ sự kiện bấm và giải phóng ảnh khi thẻ được tái sử dụng.
     */
    @Override
    public void onViewRecycled(
            @NonNull HostListingViewHolder holder
    ) {
        holder.itemView.setOnClickListener(null);
        holder.clearImage();

        super.onViewRecycled(holder);
    }

    /**
     * Giữ các thành phần giao diện của một thẻ bài đăng.
     */
    static class HostListingViewHolder extends RecyclerView.ViewHolder {

        private final ItemHostListingBinding binding;

        /**
         * Khởi tạo ViewHolder từ binding của thẻ bài đăng.
         */
        HostListingViewHolder(ItemHostListingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Hiển thị bản nháp và thông tin phòng liên kết.
         * Đặt lại dữ liệu để thẻ không giữ nội dung của lần dùng trước.
         */
        void bind(HostListing listing, HostRoom room) {
            binding.tvHostListingStatus.setText("Bản nháp mẫu");
            binding.tvHostListingTitle.setText(listing.getTitle());

            binding.tvHostListingPrice.setText(
                    listing.getFormattedPrice()
            );

            binding.tvHostListingDescription.setText(
                    listing.getDescription()
            );

            clearImage();

            if (room == null) {
                binding.tvHostListingRoomCode.setText(
                        "Không tìm thấy phòng liên kết"
                );

                binding.tvHostListingAddress.setText("");
                return;
            }

            binding.tvHostListingRoomCode.setText(
                    "Mã phòng: " + room.getUnitCode()
            );

            binding.tvHostListingAddress.setText(room.getAddress());

            displayImage(room);
        }

        /**
         * Hiển thị ảnh của phòng liên kết.
         * Dùng phần thay thế nếu không có ảnh hoặc mất quyền đọc.
         */
        private void displayImage(HostRoom room) {
            String imageUri = room.getImageUri();

            if (imageUri == null || imageUri.trim().isEmpty()) {
                return;
            }

            try {
                binding.imgHostListing.setImageURI(
                        Uri.parse(imageUri)
                );

                boolean imageLoaded =
                        binding.imgHostListing.getDrawable() != null;

                binding.imgHostListing.setVisibility(
                        imageLoaded ? View.VISIBLE : View.GONE
                );

                binding.tvHostListingImagePlaceholder.setVisibility(
                        imageLoaded ? View.GONE : View.VISIBLE
                );

                binding.imgHostListing.setContentDescription(
                        "Ảnh phòng " + room.getName()
                );

            } catch (SecurityException exception) {
                clearImage();
            }
        }

        /**
         * Xóa ảnh cũ và hiện phần thay thế.
         */
        void clearImage() {
            binding.imgHostListing.setImageDrawable(null);
            binding.imgHostListing.setVisibility(View.GONE);

            binding.tvHostListingImagePlaceholder.setVisibility(
                    View.VISIBLE
            );
        }
    }
}