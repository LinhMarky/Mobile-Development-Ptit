package com.example.roomly.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.example.roomly.data.local.RoomlyDatabaseHelper;
import com.example.roomly.data.model.ListingDraft;

/**
 * Đọc và ghi bản nháp bài đăng trên điện thoại.
 * Mọi thao tác đều giới hạn theo tài khoản và phòng.
 *
 * Gọi các hàm đọc/ghi trên luồng nền.
 */
public final class LocalListingDraftRepository {

    private final RoomlyDatabaseHelper databaseHelper;

    public LocalListingDraftRepository(Context context) {
        databaseHelper = RoomlyDatabaseHelper.getInstance(context);
    }

    /**
     * Lưu mới hoặc cập nhật bản nháp của tài khoản và phòng.
     * Nếu bản nháp trống, xóa bản đã lưu trước đó.
     */
    @WorkerThread
    public void saveDraft(ListingDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException(
                    "Thiếu dữ liệu bản nháp."
            );
        }

        if (draft.isEmpty()) {
            deleteDraft(draft.getUserId(), draft.getRoomId());
            return;
        }

        ContentValues values = new ContentValues();
        values.put("user_id", draft.getUserId());
        values.put("room_id", draft.getRoomId());
        values.put("title", draft.getTitle());
        values.put("price_text", draft.getPriceText());
        values.put("deposit_text", draft.getDepositText());
        values.put("description", draft.getDescription());
        values.put("updated_at", draft.getUpdatedAtMillis());

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        long result = db.insertWithOnConflict(
                RoomlyDatabaseHelper.TABLE_LISTING_DRAFTS,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );

        if (result == -1) {
            throw new IllegalStateException(
                    "Không thể lưu bản nháp trên điện thoại."
            );
        }
    }

    /**
     * Trả về bản nháp của đúng tài khoản và phòng.
     * Trả về null nếu chưa có bản nháp.
     */
    @Nullable
    @WorkerThread
    public ListingDraft getDraft(String userId, String roomId) {
        String cleanUserId = requireId(userId, "mã tài khoản");
        String cleanRoomId = requireId(roomId, "mã phòng");

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String[] columns = {
                "user_id",
                "room_id",
                "title",
                "price_text",
                "deposit_text",
                "description",
                "updated_at"
        };

        try (Cursor cursor = db.query(
                RoomlyDatabaseHelper.TABLE_LISTING_DRAFTS,
                columns,
                "user_id = ? AND room_id = ?",
                new String[]{cleanUserId, cleanRoomId},
                null,
                null,
                null,
                "1"
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }

            return new ListingDraft(
                    cursor.getString(
                            cursor.getColumnIndexOrThrow("user_id")
                    ),
                    cursor.getString(
                            cursor.getColumnIndexOrThrow("room_id")
                    ),
                    cursor.getString(
                            cursor.getColumnIndexOrThrow("title")
                    ),
                    cursor.getString(
                            cursor.getColumnIndexOrThrow("price_text")
                    ),
                    cursor.getString(
                            cursor.getColumnIndexOrThrow("deposit_text")
                    ),
                    cursor.getString(
                            cursor.getColumnIndexOrThrow("description")
                    ),
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow("updated_at")
                    )
            );
        }
    }

    /**
     * Xóa bản nháp sau khi lưu bài đăng thành công,
     * hoặc khi người dùng chủ động bỏ bản nháp.
     */
    @WorkerThread
    public void deleteDraft(String userId, String roomId) {
        String cleanUserId = requireId(userId, "mã tài khoản");
        String cleanRoomId = requireId(roomId, "mã phòng");

        databaseHelper.getWritableDatabase().delete(
                RoomlyDatabaseHelper.TABLE_LISTING_DRAFTS,
                "user_id = ? AND room_id = ?",
                new String[]{cleanUserId, cleanRoomId}
        );
    }

    private static String requireId(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Thiếu " + fieldName + "."
            );
        }

        return value.trim();
    }
}