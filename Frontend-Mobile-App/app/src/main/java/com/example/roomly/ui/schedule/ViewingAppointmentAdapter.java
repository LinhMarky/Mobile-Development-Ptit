package com.example.roomly.ui.schedule;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.ViewingAppointment;
import com.example.roomly.databinding.ItemViewingAppointmentBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Hiển thị lịch xem phòng cùng trạng thái và thao tác hủy. */
public class ViewingAppointmentAdapter extends
        RecyclerView.Adapter<
                ViewingAppointmentAdapter.AppointmentViewHolder> {

    private final List<ViewingAppointment> appointments =
            new ArrayList<>();

    private OnCancelAppointmentListener onCancelAppointmentListener;

    public interface OnCancelAppointmentListener {

        /** Yêu cầu Fragment kiểm tra và xử lý hủy lịch được chọn. */
        void onCancelAppointment(ViewingAppointment appointment);
    }

    /** Sao chép danh sách lịch hẹn ban đầu. */
    public ViewingAppointmentAdapter(
            List<ViewingAppointment> initialAppointments
    ) {
        appointments.addAll(initialAppointments);
    }

    /** Đăng ký hoặc gỡ nơi nhận yêu cầu hủy lịch. */
    public void setOnCancelAppointmentListener(
            @Nullable OnCancelAppointmentListener listener
    ) {
        onCancelAppointmentListener = listener;
    }

    /** Tạo giao diện thẻ lịch xem phòng. */
    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemViewingAppointmentBinding binding =
                ItemViewingAppointmentBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new AppointmentViewHolder(binding);
    }

    /** Gán dữ liệu và lấy vị trí hiện tại khi người dùng bấm Hủy lịch. */
    @Override
    public void onBindViewHolder(
            @NonNull AppointmentViewHolder holder,
            int position
    ) {
        holder.bind(appointments.get(position));

        holder.binding.btnCancelAppointment.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            ViewingAppointment appointment =
                    appointments.get(currentPosition);

            if (appointment.canCancel()
                    && onCancelAppointmentListener != null) {
                onCancelAppointmentListener
                        .onCancelAppointment(appointment);
            }
        });
    }

    /** Trả về số lịch đang hiển thị. */
    @Override
    public int getItemCount() {
        return appointments.size();
    }

    /** Thay danh sách sau khi trạng thái lịch thay đổi. */
    public void updateAppointments(
            List<ViewingAppointment> newAppointments
    ) {
        List<ViewingAppointment> updated =
                new ArrayList<>(newAppointments);

        appointments.clear();
        appointments.addAll(updated);
        notifyDataSetChanged();
    }

    /** Gỡ thao tác và ảnh trước khi tái sử dụng thẻ. */
    @Override
    public void onViewRecycled(
            @NonNull AppointmentViewHolder holder
    ) {
        holder.binding.btnCancelAppointment.setOnClickListener(null);
        holder.binding.imgAppointmentRoom.setImageDrawable(null);
        super.onViewRecycled(holder);
    }

    /** Giữ giao diện của một lịch xem phòng. */
    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        private final ItemViewingAppointmentBinding binding;

        /** Khởi tạo ViewHolder từ binding của thẻ. */
        AppointmentViewHolder(ItemViewingAppointmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /** Hiển thị thông tin, trạng thái, thời gian và lý do xử lý. */
        void bind(ViewingAppointment appointment) {
            binding.tvAppointmentRoomTitle.setText(
                    appointment.getRoomTitle()
            );
            binding.tvAppointmentAddress.setText(
                    appointment.getRoomAddress()
            );

            displayImage(appointment);

            binding.tvAppointmentTime.setText(
                    appointment.getStatusLabel()
                            + "\nBắt đầu: "
                            + formatTime(
                            appointment.getAppointmentTimeMillis()
                    )
                            + "\nKết thúc: "
                            + formatTime(appointment.getEndTimeMillis())
            );

            String note = appointment.getNote() == null
                    ? "" : appointment.getNote().trim();

            String reason = appointment.getDecisionReason() == null
                    ? "" : appointment.getDecisionReason().trim();

            String details = "";

            if (!note.isEmpty()) {
                details = "Lời nhắn: " + note;
            }

            if (!reason.isEmpty()) {
                details += (details.isEmpty() ? "" : "\n")
                        + "Lý do: " + reason;
            }

            binding.tvAppointmentNote.setText(details);
            binding.tvAppointmentNote.setVisibility(
                    details.isEmpty() ? View.GONE : View.VISIBLE
            );

            boolean canCancel = appointment.canCancel();

            binding.btnCancelAppointment.setVisibility(
                    canCancel ? View.VISIBLE : View.GONE
            );
            binding.btnCancelAppointment.setEnabled(canCancel);
        }

        /** Hiển thị ảnh URI và dùng ảnh drawable nếu có. */
        private void displayImage(ViewingAppointment appointment) {
            binding.imgAppointmentRoom.setImageDrawable(null);

            String imageUri = appointment.getRoomImageUri();

            if (imageUri != null && !imageUri.trim().isEmpty()) {
                try {
                    binding.imgAppointmentRoom.setImageURI(
                            Uri.parse(imageUri)
                    );
                } catch (
                        SecurityException | IllegalArgumentException ignored
                ) {
                    binding.imgAppointmentRoom.setImageDrawable(null);
                }
            }

            if (binding.imgAppointmentRoom.getDrawable() == null
                    && appointment.getRoomImageResId() != 0) {
                binding.imgAppointmentRoom.setImageResource(
                        appointment.getRoomImageResId()
                );
            }

            binding.imgAppointmentRoom.setContentDescription(
                    "Ảnh phòng " + appointment.getRoomTitle()
            );
        }

        /** Định dạng thời gian theo múi giờ thiết bị. */
        private String formatTime(long timeMillis) {
            return new SimpleDateFormat(
                    "HH:mm · dd/MM/yyyy",
                    new Locale("vi", "VN")
            ).format(new Date(timeMillis));
        }
    }
}