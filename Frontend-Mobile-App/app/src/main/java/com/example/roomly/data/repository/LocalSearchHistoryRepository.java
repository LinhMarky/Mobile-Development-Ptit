package com.example.roomly.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.WorkerThread;

import com.example.roomly.data.local.RoomlyDatabaseHelper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lưu lịch sử tìm kiếm riêng cho từng tài khoản.
 * Chỉ gọi các thao tác đọc/ghi trên luồng nền.
 */
public final class LocalSearchHistoryRepository {

    private static final int MAX_HISTORY_ITEMS = 20;
    private static final int MAX_KEYWORD_LENGTH = 200;

    private final RoomlyDatabaseHelper databaseHelper;

    public LocalSearchHistoryRepository(Context context) {
        databaseHelper = RoomlyDatabaseHelper.getInstance(context);
    }

    /**
     * Lưu từ khóa sau khi người dùng thực hiện tìm kiếm.
     * Không gọi sau mỗi ký tự được nhập.
     *
     * Từ khóa trùng sẽ được đưa lên đầu.
     * Mỗi tài khoản giữ tối đa 20 từ khóa.
     */
    @WorkerThread
    public void saveSearch(String userId, String keyword) {
        String cleanUserId = requireUserId(userId);
        String cleanKeyword = cleanKeyword(keyword);

        if (cleanKeyword.isEmpty()) {
            return;
        }

        if (cleanKeyword.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Từ khóa không được vượt quá 200 ký tự."
            );
        }

        String keywordKey = createKeywordKey(cleanKeyword);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put("user_id", cleanUserId);
            values.put("keyword_key", keywordKey);
            values.put("keyword", cleanKeyword);
            values.put("searched_at", System.currentTimeMillis());

            long result = db.insertWithOnConflict(
                    RoomlyDatabaseHelper.TABLE_SEARCH_HISTORY,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            );

            if (result == -1) {
                throw new IllegalStateException(
                        "Không thể lưu lịch sử tìm kiếm."
                );
            }

            // Chỉ xóa các mục cũ của đúng tài khoản đang lưu.
            db.execSQL(
                    "DELETE FROM "
                            + RoomlyDatabaseHelper.TABLE_SEARCH_HISTORY
                            + " WHERE user_id = ?"
                            + " AND keyword_key NOT IN ("
                            + "SELECT keyword_key FROM "
                            + RoomlyDatabaseHelper.TABLE_SEARCH_HISTORY
                            + " WHERE user_id = ?"
                            + " ORDER BY searched_at DESC, rowid DESC"
                            + " LIMIT ?"
                            + ")",
                    new Object[]{
                            cleanUserId,
                            cleanUserId,
                            MAX_HISTORY_ITEMS
                    }
            );

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Đọc tối đa 20 từ khóa, mới nhất trước.
     */
    @WorkerThread
    public List<String> getHistory(String userId) {
        String cleanUserId = requireUserId(userId);

        List<String> result = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                RoomlyDatabaseHelper.TABLE_SEARCH_HISTORY,
                new String[]{"keyword"},
                "user_id = ?",
                new String[]{cleanUserId},
                null,
                null,
                "searched_at DESC, rowid DESC",
                String.valueOf(MAX_HISTORY_ITEMS)
        )) {
            int keywordIndex = cursor.getColumnIndexOrThrow(
                    "keyword"
            );

            while (cursor.moveToNext()) {
                result.add(cursor.getString(keywordIndex));
            }
        }

        return result;
    }

    /**
     * Xóa một từ khóa của đúng tài khoản.
     */
    @WorkerThread
    public void deleteSearch(String userId, String keyword) {
        String cleanUserId = requireUserId(userId);
        String cleanKeyword = cleanKeyword(keyword);

        if (cleanKeyword.isEmpty()) {
            return;
        }

        databaseHelper.getWritableDatabase().delete(
                RoomlyDatabaseHelper.TABLE_SEARCH_HISTORY,
                "user_id = ? AND keyword_key = ?",
                new String[]{
                        cleanUserId,
                        createKeywordKey(cleanKeyword)
                }
        );
    }

    /**
     * Xóa toàn bộ lịch sử của một tài khoản.
     * Không ảnh hưởng tài khoản khác.
     */
    @WorkerThread
    public void clearHistory(String userId) {
        String cleanUserId = requireUserId(userId);

        databaseHelper.getWritableDatabase().delete(
                RoomlyDatabaseHelper.TABLE_SEARCH_HISTORY,
                "user_id = ?",
                new String[]{cleanUserId}
        );
    }

    private static String cleanKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }

        // Chuẩn hóa Unicode và gộp khoảng trắng liên tiếp.
        return Normalizer.normalize(
                keyword,
                Normalizer.Form.NFC
        ).replaceAll("[\\s\\p{Z}]+", " ").trim();
    }

    private static String createKeywordKey(String keyword) {
        // Giữ dấu tiếng Việt, chỉ bỏ khác biệt chữ hoa/thường.
        return keyword.toLowerCase(Locale.ROOT);
    }

    private static String requireUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Thiếu mã tài khoản."
            );
        }

        return userId.trim();
    }
}