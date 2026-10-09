package com.example.roomly.data.model;

/**
 * Một ảnh phòng dùng cho giao diện.
 * Hỗ trợ ảnh drawable hoặc URI ảnh trên thiết bị.
 */
public final class RoomImage {

    private final int drawableResId;
    private final String uri;
    private final String description;

    private RoomImage(
            int drawableResId,
            String uri,
            String description
    ) {
        this.drawableResId = drawableResId;
        this.uri = uri == null ? "" : uri.trim();

        this.description = description == null
                || description.trim().isEmpty()
                ? "Ảnh phòng"
                : description.trim();
    }

    /**
     * Tạo ảnh từ drawable có sẵn trong project.
     */
    public static RoomImage fromDrawable(
            int drawableResId,
            String description
    ) {
        if (drawableResId <= 0) {
            throw new IllegalArgumentException(
                    "Mã ảnh drawable không hợp lệ."
            );
        }

        return new RoomImage(
                drawableResId,
                "",
                description
        );
    }

    /**
     * Tạo ảnh từ URI do người dùng chọn trên thiết bị.
     */
    public static RoomImage fromUri(
            String uri,
            String description
    ) {
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "URI ảnh không được để trống."
            );
        }

        return new RoomImage(
                0,
                uri,
                description
        );
    }

    public int getDrawableResId() {
        return drawableResId;
    }

    public String getUri() {
        return uri;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDrawable() {
        return drawableResId > 0;
    }
}