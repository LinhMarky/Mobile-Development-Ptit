package com.example.roomly.ui.host.viewing;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.HostViewingSlot;
import com.example.roomly.databinding.ItemHostViewingSlotBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị danh sách khung giờ xem phòng của chủ trọ.
 */
public class HostViewingSlotAdapter
        extends RecyclerView.Adapter<
        HostViewingSlotAdapter.SlotViewHolder> {

    private final List<HostViewingSlot> slots = new ArrayList<>();

    private OnToggleSlotListener toggleSlotListener;

    public interface OnToggleSlotListener {

        /**
         * Báo cho Fragment đổi trạng thái mở hoặc đóng của khung giờ.
         */
        void onToggleSlot(HostViewingSlot slot);
    }

    /**
     * Sao chép danh sách ban đầu vào Adapter.
     */
    public HostViewingSlotAdapter(List<HostViewingSlot> initialSlots) {
        slots.addAll(initialSlots);
    }

    /**
     * Đăng ký hoặc gỡ bộ xử lý thao tác mở/đóng khung giờ.
     */
    public void setOnToggleSlotListener(
            @Nullable OnToggleSlotListener listener
    ) {
        toggleSlotListener = listener;
    }

    /**
     * Tạo giao diện một thẻ từ item_host_viewing_slot.xml.
     */
    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemHostViewingSlotBinding binding =
                ItemHostViewingSlotBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new SlotViewHolder(binding);
    }

    /**
     * Hiển thị dữ liệu và đăng ký thao tác bấm nút của thẻ.
     * Đọc vị trí hiện tại khi bấm để tránh dùng vị trí đã thay đổi.
     */
    @Override
    public void onBindViewHolder(
            @NonNull SlotViewHolder holder,
            int position
    ) {
        holder.bind(slots.get(position));

        holder.binding.btnHostSlotToggle.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION
                    || toggleSlotListener == null) {
                return;
            }

            toggleSlotListener.onToggleSlot(
                    slots.get(currentPosition)
            );
        });
    }

    /**
     * Trả về số khung giờ đang hiển thị.
     */
    @Override
    public int getItemCount() {
        return slots.size();
    }

    /**
     * Thay danh sách hiện tại bằng dữ liệu mới từ repository.
     */
    public void updateSlots(List<HostViewingSlot> newSlots) {
        List<HostViewingSlot> updatedSlots =
                new ArrayList<>(newSlots);

        slots.clear();
        slots.addAll(updatedSlots);

        notifyDataSetChanged();
    }

    /**
     * Gỡ sự kiện bấm khi thẻ được đưa vào vùng tái sử dụng.
     */
    @Override
    public void onViewRecycled(@NonNull SlotViewHolder holder) {
        holder.binding.btnHostSlotToggle.setOnClickListener(null);

        super.onViewRecycled(holder);
    }

    /**
     * Giữ các thành phần giao diện của một thẻ khung giờ.
     */
    static class SlotViewHolder extends RecyclerView.ViewHolder {

        private final ItemHostViewingSlotBinding binding;

        /**
         * Khởi tạo ViewHolder từ binding của thẻ.
         */
        SlotViewHolder(ItemHostViewingSlotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Hiển thị thời gian theo múi giờ hiện tại của thiết bị.
         * Không cho mở khung giờ đã bắt đầu.
         */
        void bind(HostViewingSlot slot) {
            Locale vietnamese = new Locale("vi", "VN");

            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "EEEE, dd/MM/yyyy",
                    vietnamese
            );

            SimpleDateFormat timeFormat = new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    vietnamese
            );

            binding.tvHostSlotDate.setText(
                    dateFormat.format(
                            new Date(slot.getStartTimeMillis())
                    )
            );

            binding.tvHostSlotTime.setText(
                    "Bắt đầu: "
                            + timeFormat.format(
                            new Date(slot.getStartTimeMillis())
                    )
                            + "\nKết thúc: "
                            + timeFormat.format(
                            new Date(slot.getEndTimeMillis())
                    )
            );

            long now = System.currentTimeMillis();

            boolean hasStarted = slot.getStartTimeMillis() <= now;
            boolean hasEnded = slot.getEndTimeMillis() <= now;

            String status;

            if (hasEnded) {
                status = "Đã kết thúc";
            } else if (hasStarted) {
                status = "Đã bắt đầu • Không nhận lịch mới";
            } else if (slot.isOpen()) {
                status = "Đang mở nhận lịch";
            } else {
                status = "Đã đóng nhận lịch";
            }

            binding.tvHostSlotStatus.setText(status);

            if (hasStarted && !slot.isOpen()) {
                binding.btnHostSlotToggle.setText(
                        "Không thể mở lại"
                );
                binding.btnHostSlotToggle.setEnabled(false);
            } else {
                binding.btnHostSlotToggle.setText(
                        slot.isOpen()
                                ? "Đóng nhận lịch"
                                : "Mở nhận lịch"
                );
                binding.btnHostSlotToggle.setEnabled(true);
            }
        }
    }
}