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
 * Hiển thị yêu cầu xem phòng và thông báo khi người dùng bấm thẻ.
 */
public class HostAppointmentAdapter
        extends RecyclerView.Adapter<
        HostAppointmentAdapter.AppointmentViewHolder> {

    private final List<HostViewingAppointment> appointments =
            new ArrayList<>();

    private OnAppointmentClickListener clickListener;

    public interface OnAppointmentClickListener {

        /** Thông báo mã yêu cầu được chọn để màn hình đọc lại dữ liệu. */
        void onAppointmentClick(String appointmentId);
    }

    /** Sao chép danh sách ban đầu vào Adapter. */
    public HostAppointmentAdapter(
            List<HostViewingAppointment> initialAppointments
    ) {
        appointments.addAll(initialAppointments);
    }

    /** Đăng ký nơi nhận thao tác bấm thẻ yêu cầu. */
    public void setOnAppointmentClickListener(
            OnAppointmentClickListener listener
    ) {
        clickListener = listener;
    }

    /** Tạo giao diện thẻ từ item_host_appointment.xml. */
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
     * Hiển thị dữ liệu và đăng ký thao tác bấm.
     * Đọc lại vị trí hiện tại để tránh dùng vị trí cũ sau cập nhật.
     */
    @Override
    public void onBindViewHolder(
            @NonNull AppointmentViewHolder holder,
            int position
    ) {
        holder.bind(appointments.get(position));

        holder.itemView.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION
                    || clickListener == null) {
                return;
            }

            clickListener.onAppointmentClick(
                    appointments.get(currentPosition).getId()
            );
        });
    }

    /** Gỡ thao tác bấm khi thẻ được đưa vào vùng tái sử dụng. */
    @Override
    public void onViewRecycled(
            @NonNull AppointmentViewHolder holder
    ) {
        holder.itemView.setOnClickListener(null);

        super.onViewRecycled(holder);
    }

    /** Trả về số yêu cầu đang hiển thị. */
    @Override
    public int getItemCount() {
        return appointments.size();
    }

    /** Sao chép danh sách mới và cập nhật các thẻ dữ liệu mẫu. */
    public void updateAppointments(
            List<HostViewingAppointment> newAppointments
    ) {
        List<HostViewingAppointment> updated =
                new ArrayList<>(newAppointments);

        appointments.clear();
        appointments.addAll(updated);

        notifyDataSetChanged();
    }

    /** Giữ các thành phần giao diện của một thẻ yêu cầu. */
    static class AppointmentViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemHostAppointmentBinding binding;

        /** Khởi tạo ViewHolder bằng binding của thẻ. */
        AppointmentViewHolder(ItemHostAppointmentBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }

        /**
         * Hiển thị thông tin và trạng thái hiện tại.
         * Gộp lý do từ chối vào vùng ghi chú đang có trong XML.
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

            StringBuilder extra = new StringBuilder();

            String note = appointment.getNote().trim();

            if (!note.isEmpty()) {
                extra.append("Ghi chú: ").append(note);
            }

            String reason = appointment.getDecisionReason().trim();

            if (!reason.isEmpty()) {
                if (extra.length() > 0) {
                    extra.append("\n");
                }

                extra.append("Lý do xử lý: ").append(reason);
            }

            binding.tvHostAppointmentNote.setText(extra.toString());

            binding.tvHostAppointmentNote.setVisibility(
                    extra.length() > 0 ? View.VISIBLE : View.GONE
            );
        }

        /** Hiển thị ngày giờ theo múi giờ của thiết bị. */
        private String formatTime(long timeMillis) {
            return new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    new Locale("vi", "VN")
            ).format(new Date(timeMillis));
        }
    }
}