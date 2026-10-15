package com.example.roomly.ui.explore;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.ViewedRoomCache;
import com.example.roomly.databinding.ItemViewedRoomBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị phòng đã cache.
 * JSON phải được giải mã trước trên luồng nền.
 */
public final class ViewedRoomAdapter
        extends RecyclerView.Adapter<
        ViewedRoomAdapter.RoomViewHolder> {

    /**
     * Một dòng gồm bản ghi SQLite và thông tin đã giải mã.
     */
    public static final class Item {

        private final ViewedRoomCache cache;
        private final RoomCard room;

        public Item(
                ViewedRoomCache cache,
                RoomCard room
        ) {
            if (cache == null || room == null) {
                throw new IllegalArgumentException(
                        "Thiếu dữ liệu phòng đã xem."
                );
            }

            if (!cache.getListingId().equals(room.getListingId())
                    || !cache.getRoomId().equals(room.getRoomId())) {
                throw new IllegalArgumentException(
                        "Thông tin phòng không khớp bản ghi cache."
                );
            }

            this.cache = cache;
            this.room = room;
        }

        public ViewedRoomCache getCache() {
            return cache;
        }

        public RoomCard getRoom() {
            return room;
        }
    }

    public interface Listener {
        void onOpenRequested(Item item);

        void onDeleteRequested(Item item);
    }

    private final List<Item> items = new ArrayList<>();

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    new Locale("vi", "VN")
            );

    @Nullable
    private Listener listener;

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    /**
     * Gọi trên luồng giao diện.
     */
    public void submitItems(@NonNull List<Item> newItems) {
        List<Item> copy = new ArrayList<>(newItems);

        if (copy.contains(null)) {
            throw new IllegalArgumentException(
                    "Danh sách phòng không được chứa null."
            );
        }

        items.clear();
        items.addAll(copy);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemViewedRoomBinding binding =
                ItemViewedRoomBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new RoomViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RoomViewHolder holder,
            int position
    ) {
        Item item = items.get(position);
        RoomCard room = item.getRoom();

        holder.binding.tvViewedRoomTitle.setText(
                room.getTitle().isEmpty()
                        ? "Phòng đã xem"
                        : room.getTitle()
        );

        holder.binding.tvViewedRoomPrice.setText(
                room.getPrice().isEmpty()
                        ? "Chưa cung cấp giá thuê"
                        : room.getPrice()
        );

        holder.binding.tvViewedRoomAddress.setText(
                room.getAddress().isEmpty()
                        ? "Chưa cung cấp địa chỉ"
                        : room.getAddress()
        );

        holder.binding.tvViewedRoomCachedAt.setText(
                "Dữ liệu lưu lúc: "
                        + dateFormat.format(
                        new Date(
                                item.getCache()
                                        .getCachedAtMillis()
                        )
                )
        );

        holder.binding.btnViewedRoomOpen.setContentDescription(
                "Xem bản lưu: " + room.getTitle()
        );

        holder.binding.btnViewedRoomDelete.setContentDescription(
                "Xóa bản lưu: " + room.getTitle()
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class RoomViewHolder extends RecyclerView.ViewHolder {

        private final ItemViewedRoomBinding binding;

        RoomViewHolder(ItemViewedRoomBinding binding) {
            super(binding.getRoot());

            this.binding = binding;

            binding.btnViewedRoomOpen.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION
                        || listener == null) {
                    return;
                }

                listener.onOpenRequested(items.get(position));
            });

            binding.btnViewedRoomDelete.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION
                        || listener == null) {
                    return;
                }

                listener.onDeleteRequested(items.get(position));
            });
        }
    }
}