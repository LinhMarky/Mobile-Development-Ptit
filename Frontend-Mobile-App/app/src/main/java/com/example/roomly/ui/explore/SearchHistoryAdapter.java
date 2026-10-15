package com.example.roomly.ui.explore;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.databinding.ItemSearchHistoryBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị từ khóa đã tìm kiếm.
 * Chuyển thao tác chọn và xóa về Fragment xử lý.
 */
public final class SearchHistoryAdapter
        extends RecyclerView.Adapter<
        SearchHistoryAdapter.HistoryViewHolder> {

    public interface Listener {
        void onKeywordSelected(String keyword);

        void onKeywordDeleteRequested(String keyword);
    }

    private final List<String> keywords = new ArrayList<>();

    @Nullable
    private Listener listener;

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    /**
     * Sao chép dữ liệu để không phụ thuộc danh sách bên ngoài.
     * Gọi trên luồng giao diện.
     */
    public void submitKeywords(@NonNull List<String> newKeywords) {
        List<String> copy = new ArrayList<>(newKeywords);

        keywords.clear();
        keywords.addAll(copy);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemSearchHistoryBinding binding =
                ItemSearchHistoryBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull HistoryViewHolder holder,
            int position
    ) {
        String keyword = keywords.get(position);

        holder.binding.tvHistoryKeyword.setText(keyword);

        holder.binding.tvHistoryKeyword.setContentDescription(
                "Tìm lại: " + keyword
        );

        holder.binding.btnDeleteHistory.setContentDescription(
                "Xóa từ khóa: " + keyword
        );
    }

    @Override
    public int getItemCount() {
        return keywords.size();
    }

    final class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final ItemSearchHistoryBinding binding;

        HistoryViewHolder(ItemSearchHistoryBinding binding) {
            super(binding.getRoot());

            this.binding = binding;

            binding.tvHistoryKeyword.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION
                        || listener == null) {
                    return;
                }

                listener.onKeywordSelected(keywords.get(position));
            });

            binding.btnDeleteHistory.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION
                        || listener == null) {
                    return;
                }

                listener.onKeywordDeleteRequested(
                        keywords.get(position)
                );
            });
        }
    }
}