package com.example.roomly.ui.host.viewing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.HostViewingAppointment;
import com.example.roomly.databinding.ItemHostAppointmentBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị danh sách yêu cầu xem phòng gửi đến chủ trọ.
 */
public class HostAppointmentAdapter
        extends RecyclerView.Adapter<
        HostAppointmentAdapter.AppointmentViewHolder> {

    private final List<HostViewingAppointment> appointments =
            new ArrayList<>();

    /**
     * Sao chép danh sách yêu cầu ban đầu vào Adapter.
     */
    public HostAppointmentAdapter(
            List<HostViewingAppointment> initialAppointments
    ) {
        appointments.addAll(initialAppointments);
    }

    /**
     * Tạo giao diện một thẻ từ item_host_appointment.xml.
     */
    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemHostAppointmentBinding binding =
                ItemHostAppointmentBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new AppointmentViewHolder(binding);
    }

    /**
     * Gán dữ liệu yêu cầu vào thẻ tại vị trí tương ứng.
     */
    @Override
    public void onBindViewHolder(
            @NonNull AppointmentViewHolder holder,
            int position
    ) {
        holder.bind(appointments.get(position));
    }

    /**
     * Trả về số yêu cầu đang hiển thị.
     */
    @Override
    public int getItemCount() {
        return appointments.size();
    }

    /**
     * Cập nhật danh sách từ repository và hiển thị lại các thẻ.
     */
    public void updateAppointments(
            List<HostViewingAppointment> newAppointments
    ) {
        List<HostViewingAppointment> updatedAppointments =
                new ArrayList<>(newAppointments);

        appointments.clear();
        appointments.addAll(updatedAppointments);

        notifyDataSetChanged();
    }

    /**
     * Giữ các thành phần giao diện của một thẻ yêu cầu.
     */
    static class AppointmentViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemHostAppointmentBinding binding;

        /**
         * Khởi tạo ViewHolder bằng binding của thẻ.
         */
        AppointmentViewHolder(ItemHostAppointmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Hiển thị phòng, người gửi, thời gian, trạng thái và ghi chú.
         * Luôn cập nhật cả nội dung và độ hiển thị khi tái sử dụng thẻ.
         */
        void bind(HostViewingAppointment appointment) {
            binding.tvHostAppointmentStatus.setText(
                    appointment.getStatusLabel()
            );

            binding.tvHostAppointmentRoom.setText(
                    appointment.getRoomName()
            );

            binding.tvHostAppointmentRoomCode.setText(
                    "Mã phòng: " + appointment.getUnitCode()
            );

            binding.tvHostAppointmentGuest.setText(
                    "Người gửi: " + appointment.getGuestName()
            );

            binding.tvHostAppointmentTime.setText(
                    "Bắt đầu: "
                            + formatTime(appointment.getStartTimeMillis())
                            + "\nKết thúc: "
                            + formatTime(appointment.getEndTimeMillis())
            );

            String note = appointment.getNote().trim();
            boolean hasNote = !note.isEmpty();

            binding.tvHostAppointmentNote.setText(
                    hasNote ? "Ghi chú: " + note : ""
            );

            binding.tvHostAppointmentNote.setVisibility(
                    hasNote ? View.VISIBLE : View.GONE
            );
        }

        /**
         * Hiển thị ngày giờ theo múi giờ hiện tại của thiết bị.
         */
        private String formatTime(long timeMillis) {
            SimpleDateFormat formatter = new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    new Locale("vi", "VN")
            );

            return formatter.format(new Date(timeMillis));
        }
    }
}