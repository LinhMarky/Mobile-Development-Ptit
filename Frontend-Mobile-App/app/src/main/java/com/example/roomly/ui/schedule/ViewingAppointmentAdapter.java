package com.example.roomly.ui.schedule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.data.model.ViewingAppointment;
import com.example.roomly.databinding.ItemViewingAppointmentBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hiển thị lịch hẹn bằng giao diện item_viewing_appointment.xml.
 */
public class ViewingAppointmentAdapter extends
        RecyclerView.Adapter<ViewingAppointmentAdapter.AppointmentViewHolder> {

    private final List<ViewingAppointment> appointments =
            new ArrayList<>();

    private OnCancelAppointmentListener onCancelAppointmentListener;

    public interface OnCancelAppointmentListener {

        /**
         * Thông báo lịch được chọn để màn hình xử lý hủy.
         */
        void onCancelAppointment(ViewingAppointment appointment);
    }

    /**
     * Nhận danh sách lịch hẹn ban đầu.
     */
    public ViewingAppointmentAdapter(
            List<ViewingAppointment> initialAppointments
    ) {
        appointments.addAll(initialAppointments);
    }

    /**
     * Đăng ký xử lý khi người dùng bấm Hủy lịch.
     */
    public void setOnCancelAppointmentListener(
            OnCancelAppointmentListener listener
    ) {
        this.onCancelAppointmentListener = listener;
    }

    /**
     * Tạo giao diện cho một thẻ lịch hẹn bằng ViewBinding.
     */
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

    /**
     * Gán dữ liệu và thao tác hủy cho thẻ lịch hẹn.
     */
    @Override
    public void onBindViewHolder(
            @NonNull AppointmentViewHolder holder,
            int position
    ) {
        holder.bind(
                appointments.get(position),
                onCancelAppointmentListener
        );
    }

    /**
     * Trả về tổng số lịch hẹn cần hiển thị.
     */
    @Override
    public int getItemCount() {
        return appointments.size();
    }

    /**
     * Cập nhật danh sách sau khi thêm hoặc hủy lịch.
     */
    public void updateAppointments(
            List<ViewingAppointment> newAppointments
    ) {
        List<ViewingAppointment> updatedAppointments =
                new ArrayList<>(newAppointments);

        appointments.clear();
        appointments.addAll(updatedAppointments);

        notifyDataSetChanged();
    }

    /**
     * Giữ các thành phần giao diện của một thẻ lịch hẹn.
     */
    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        private final ItemViewingAppointmentBinding binding;

        /**
         * Khởi tạo ViewHolder bằng binding của thẻ lịch.
         */
        AppointmentViewHolder(ItemViewingAppointmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Hiển thị phòng, thời gian, lời nhắn và thao tác hủy lịch.
         */
        void bind(
                ViewingAppointment appointment,
                OnCancelAppointmentListener cancelListener
        ) {
            binding.imgAppointmentRoom.setImageResource(
                    appointment.getRoomImageResId()
            );

            binding.tvAppointmentRoomTitle.setText(
                    appointment.getRoomTitle()
            );

            binding.tvAppointmentAddress.setText(
                    appointment.getRoomAddress()
            );

            // Hiển thị thời gian theo múi giờ của thiết bị.
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "HH:mm · dd/MM/yyyy",
                    new Locale("vi", "VN")
            );

            binding.tvAppointmentTime.setText(
                    dateFormat.format(
                            new Date(
                                    appointment.getAppointmentTimeMillis()
                            )
                    )
            );

            String note = appointment.getNote();

            // Ẩn lời nhắn nếu người dùng không nhập.
            if (note == null || note.trim().isEmpty()) {
                binding.tvAppointmentNote.setText("");
                binding.tvAppointmentNote.setVisibility(View.GONE);
            } else {
                binding.tvAppointmentNote.setText(
                        "Lời nhắn: " + note.trim()
                );
                binding.tvAppointmentNote.setVisibility(View.VISIBLE);
            }

            binding.btnCancelAppointment.setOnClickListener(view -> {
                if (cancelListener != null) {
                    cancelListener.onCancelAppointment(appointment);
                }
            });
        }
    }
}