package com.example.roomly.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.example.roomly.data.local.RoomlyDatabaseHelper;

/**
 * Lưu nội dung tin nhắn đang soạn trên điện thoại.
 * Không lưu hoặc gửi tin nhắn chính thức.
 *
 * Mỗi tài khoản có một bản nháp cho từng hội thoại.
 * Các thao tác đọc/ghi phải được gọi trên luồng nền.
 */
public final class LocalMessageDraftRepository {

    private final RoomlyDatabaseHelper databaseHelper;

    public LocalMessageDraftRepository(Context context) {
        databaseHelper = RoomlyDatabaseHelper.getInstance(context);
    }

    /**
     * Lưu mới hoặc cập nhật nội dung đang soạn.
     * Giữ nguyên khoảng trắng của nội dung chưa gửi.
     *
     * Nội dung trống hoặc chỉ có khoảng trắng sẽ xóa bản nháp.
     */
    @WorkerThread
    public void saveDraft(
            String userId,
            String conversationId,
            @Nullable String content
    ) {
        String cleanUserId = requireId(userId, "mã tài khoản");
        String cleanConversationId = requireId(
                conversationId,
                "mã hội thoại"
        );

        String draftContent = content == null ? "" : content;

        if (draftContent.trim().isEmpty()) {
            deleteDraft(cleanUserId, cleanConversationId);
            return;
        }

        ContentValues values = new ContentValues();
        values.put("user_id", cleanUserId);
        values.put("conversation_id", cleanConversationId);
        values.put("content", draftContent);
        values.put("updated_at", System.currentTimeMillis());

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        long result = db.insertWithOnConflict(
                RoomlyDatabaseHelper.TABLE_MESSAGE_DRAFTS,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );

        if (result == -1) {
            throw new IllegalStateException(
                    "Không thể lưu bản nháp tin nhắn."
            );
        }
    }

    /**
     * Trả về nội dung đang soạn.
     * Trả về chuỗi rỗng nếu chưa có bản nháp.
     */
    @WorkerThread
    public String getDraft(
            String userId,
            String conversationId
    ) {
        String cleanUserId = requireId(userId, "mã tài khoản");
        String cleanConversationId = requireId(
                conversationId,
                "mã hội thoại"
        );

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                RoomlyDatabaseHelper.TABLE_MESSAGE_DRAFTS,
                new String[]{"content"},
                "user_id = ? AND conversation_id = ?",
                new String[]{cleanUserId, cleanConversationId},
                null,
                null,
                null,
                "1"
        )) {
            if (!cursor.moveToFirst()) {
                return "";
            }

            return cursor.getString(
                    cursor.getColumnIndexOrThrow("content")
            );
        }
    }

    /**
     * Xóa sau khi gửi tin nhắn thành công
     * hoặc khi người dùng xóa hết nội dung đang soạn.
     */
    @WorkerThread
    public void deleteDraft(
            String userId,
            String conversationId
    ) {
        String cleanUserId = requireId(userId, "mã tài khoản");
        String cleanConversationId = requireId(
                conversationId,
                "mã hội thoại"
        );

        databaseHelper.getWritableDatabase().delete(
                RoomlyDatabaseHelper.TABLE_MESSAGE_DRAFTS,
                "user_id = ? AND conversation_id = ?",
                new String[]{cleanUserId, cleanConversationId}
        );
    }

    private static String requireId(
            String value,
            String fieldName
    ) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Thiếu " + fieldName + "."
            );
        }

        return value.trim();
    }
}