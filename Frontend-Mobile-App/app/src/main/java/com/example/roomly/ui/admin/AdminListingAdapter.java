package com.example.roomly.ui.admin;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.AdminListing;
import com.example.roomly.databinding.ItemAdminListingBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị bài đăng trong danh sách kiểm duyệt.
 */
public class AdminListingAdapter
        extends RecyclerView.Adapter<
        AdminListingAdapter.ListingViewHolder> {

    private final List<AdminListing> listings = new ArrayList<>();

    private OnListingClickListener listingClickListener;

    public interface OnListingClickListener {

        /**
         * Thông báo bài đăng được chọn để mở chi tiết.
         */
        void onListingClick(AdminListing listing);
    }

    /**
     * Nhận danh sách bài đăng ban đầu.
     */
    public AdminListingAdapter(List<AdminListing> initialListings) {
        listings.addAll(initialListings);
    }

    /**
     * Đăng ký hoặc gỡ bộ xử lý mở chi tiết.
     */
    public void setOnListingClickListener(
            @Nullable OnListingClickListener listener
    ) {
        listingClickListener = listener;
    }

    /**
     * Tạo giao diện thẻ bài đăng bằng ViewBinding.
     */
    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemAdminListingBinding binding =
                ItemAdminListingBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new ListingViewHolder(binding);
    }

    /**
     * Hiển thị bài đăng và đăng ký thao tác mở chi tiết.
     */
    @Override
    public void onBindViewHolder(
            @NonNull ListingViewHolder holder,
            int position
    ) {
        holder.bind(listings.get(position));

        holder.binding.btnAdminListingDetail.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION
                    || listingClickListener == null) {
                return;
            }

            listingClickListener.onListingClick(
                    listings.get(currentPosition)
            );
        });
    }

    /**
     * Trả về số bài đăng đang hiển thị.
     */
    @Override
    public int getItemCount() {
        return listings.size();
    }

    /**
     * Thay danh sách đang hiển thị bằng kết quả lọc mới.
     */
    public void updateListings(List<AdminListing> newListings) {
        List<AdminListing> updatedListings =
                new ArrayList<>(newListings);

        listings.clear();
        listings.addAll(updatedListings);
        notifyDataSetChanged();
    }

    /**
     * Xóa ảnh và sự kiện của thẻ khi được đưa vào vùng tái sử dụng.
     */
    @Override
    public void onViewRecycled(@NonNull ListingViewHolder holder) {
        holder.binding.btnAdminListingDetail.setOnClickListener(null);
        holder.binding.imgAdminListing.setImageDrawable(null);

        super.onViewRecycled(holder);
    }

    /**
     * Giữ giao diện của một thẻ bài đăng.
     */
    static class ListingViewHolder extends RecyclerView.ViewHolder {

        private final ItemAdminListingBinding binding;

        /**
         * Khởi tạo ViewHolder từ binding của thẻ.
         */
        ListingViewHolder(ItemAdminListingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Gán thông tin bài đăng và cập nhật ảnh.
         */
        void bind(AdminListing listing) {
            binding.tvAdminListingStatus.setText(
                    listing.getStatusLabel()
            );

            binding.tvAdminListingTitle.setText(listing.getTitle());

            binding.tvAdminListingPrice.setText(
                    listing.getFormattedPrice()
            );

            binding.tvAdminListingHost.setText(
                    "Chủ trọ: " + listing.getHostName()
            );

            binding.tvAdminListingRoomCode.setText(
                    "Mã phòng: " + listing.getUnitCode()
            );

            binding.tvAdminListingAddress.setText(
                    listing.getAddress()
            );

            displayImage(listing.getImageUri());
        }

        /**
         * Hiển thị ảnh URI cục bộ của dữ liệu mẫu.
         * Hiện thông báo thay thế nếu ảnh không đọc được.
         */
        private void displayImage(@Nullable String imageUri) {
            binding.imgAdminListing.setImageDrawable(null);

            boolean hasImage = false;

            if (imageUri != null && !imageUri.trim().isEmpty()) {
                try {
                    binding.imgAdminListing.setImageURI(
                            Uri.parse(imageUri)
                    );

                    hasImage =
                            binding.imgAdminListing.getDrawable() != null;
                } catch (SecurityException | IllegalArgumentException exception) {
                    binding.imgAdminListing.setImageDrawable(null);
                }
            }

            binding.imgAdminListing.setVisibility(
                    hasImage ? View.VISIBLE : View.GONE
            );

            binding.tvAdminListingImagePlaceholder.setVisibility(
                    hasImage ? View.GONE : View.VISIBLE
            );
        }
    }
}