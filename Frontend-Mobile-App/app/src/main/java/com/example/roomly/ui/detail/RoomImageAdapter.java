package com.example.roomly.ui.detail;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomly.R;
import com.example.roomly.data.model.RoomImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị ảnh drawable hoặc ảnh URI trên thiết bị.
 * Chưa tải ảnh từ backend.
 */
public class RoomImageAdapter
        extends RecyclerView.Adapter<RoomImageAdapter.ImageViewHolder> {

    private final List<RoomImage> images = new ArrayList<>();

    public void submitImages(List<RoomImage> values) {
        List<RoomImage> copy = values == null
                ? new ArrayList<>()
                : new ArrayList<>(values);

        if (copy.contains(null)) {
            throw new IllegalArgumentException(
                    "Ảnh phòng không được null."
            );
        }

        images.clear();
        images.addAll(copy);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        FrameLayout container = new FrameLayout(
                parent.getContext()
        );

        // Mỗi ảnh chiếm toàn bộ khung ảnh.
        container.setLayoutParams(
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        container.setBackgroundColor(
                ContextCompat.getColor(
                        parent.getContext(),
                        R.color.roomly_primary_light
                )
        );

        ImageView imageView = new ImageView(
                parent.getContext()
        );

        imageView.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        imageView.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        container.addView(imageView);

        TextView errorText = new TextView(
                parent.getContext()
        );

        errorText.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        errorText.setGravity(android.view.Gravity.CENTER);
        errorText.setText("Không thể hiển thị ảnh");
        errorText.setTextSize(14);

        errorText.setTextColor(
                ContextCompat.getColor(
                        parent.getContext(),
                        R.color.roomly_text_secondary
                )
        );

        int padding = Math.round(
                16 * parent.getResources()
                        .getDisplayMetrics().density
        );

        errorText.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        errorText.setVisibility(View.GONE);
        container.addView(errorText);

        return new ImageViewHolder(
                container,
                imageView,
                errorText
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull ImageViewHolder holder,
            int position
    ) {
        RoomImage image = images.get(position);

        // Xóa ảnh cũ khi RecyclerView tái sử dụng view.
        holder.imageView.setImageDrawable(null);
        holder.errorText.setVisibility(View.GONE);

        holder.imageView.setContentDescription(
                image.getDescription()
                        + ", ảnh " + (position + 1)
                        + " trên " + images.size()
        );

        try {
            if (image.isDrawable()) {
                holder.imageView.setImageResource(
                        image.getDrawableResId()
                );
            } else {
                holder.imageView.setImageURI(
                        Uri.parse(image.getUri())
                );
            }
        } catch (RuntimeException exception) {
            holder.imageView.setImageDrawable(null);
        }

        boolean loaded =
                holder.imageView.getDrawable() != null;

        holder.imageView.setVisibility(
                loaded ? View.VISIBLE : View.INVISIBLE
        );

        holder.errorText.setVisibility(
                loaded ? View.GONE : View.VISIBLE
        );
    }

    @Override
    public void onViewRecycled(
            @NonNull ImageViewHolder holder
    ) {
        holder.imageView.setImageDrawable(null);
        holder.imageView.setContentDescription(null);
        holder.imageView.setVisibility(View.VISIBLE);
        holder.errorText.setVisibility(View.GONE);

        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static final class ImageViewHolder
            extends RecyclerView.ViewHolder {

        final ImageView imageView;
        final TextView errorText;

        ImageViewHolder(
                @NonNull View itemView,
                ImageView imageView,
                TextView errorText
        ) {
            super(itemView);

            this.imageView = imageView;
            this.errorText = errorText;
        }
    }
}